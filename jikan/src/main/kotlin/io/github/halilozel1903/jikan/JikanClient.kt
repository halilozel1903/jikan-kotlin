package io.github.halilozel1903.jikan

import io.github.halilozel1903.jikan.model.Anime
import io.github.halilozel1903.jikan.model.AnimeCharacter
import io.github.halilozel1903.jikan.model.AnimeStatistics
import io.github.halilozel1903.jikan.model.Character
import io.github.halilozel1903.jikan.model.Episode
import io.github.halilozel1903.jikan.model.EpisodeDetail
import io.github.halilozel1903.jikan.model.Genre
import io.github.halilozel1903.jikan.model.JikanPage
import io.github.halilozel1903.jikan.model.JikanResponse
import io.github.halilozel1903.jikan.model.Manga
import io.github.halilozel1903.jikan.model.MangaCharacter
import io.github.halilozel1903.jikan.model.Person
import io.github.halilozel1903.jikan.model.Picture
import io.github.halilozel1903.jikan.model.Recommendation
import io.github.halilozel1903.jikan.model.SeasonArchive
import io.github.halilozel1903.jikan.model.StaffMember
import io.github.halilozel1903.jikan.transport.JikanHttpResponse
import io.github.halilozel1903.jikan.transport.JikanTransport
import io.github.halilozel1903.jikan.transport.KtorTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import kotlin.coroutines.cancellation.CancellationException

/**
 * A coroutine client for the [Jikan API v4](https://docs.api.jikan.moe), the unofficial MyAnimeList API.
 *
 * ```kotlin
 * val jikan = JikanClient()
 * val airing = jikan.topAnime(filter = TopAnimeFilter.Airing, limit = 10).data
 * val season = jikan.seasonNow().data
 * val frieren = jikan.animeFull(52991)
 * ```
 *
 * Every function is main-safe and throws a [JikanException] on failure. Requests wait for the shared
 * [JikanRateLimiter] (3 per second, 60 per minute), 429 answers are retried after `Retry-After`, responses are
 * cached in memory by url (see [JikanConfig.cache]) and identical requests running at the same time share one
 * network call. Close the client when you no longer need it.
 */
public class JikanClient(configure: JikanConfig.() -> Unit = {}) : AutoCloseable {

    private val config = JikanConfig().apply(configure)

    /** The API root, always ending with a slash. */
    public val baseUrl: String = config.baseUrl.let { if (it.endsWith('/')) it else "$it/" }

    private val transport: JikanTransport = config.transport ?: KtorTransport(
        httpClient = config.httpClient,
        engine = config.engine,
        userAgent = config.userAgent,
        requestTimeoutMillis = config.requestTimeoutMillis,
    )
    private val cache = config.cache
    private val rateLimiter = config.rateLimiter
    private val inFlightLock = Mutex()
    private val inFlight = HashMap<String, CompletableDeferred<String>>()

    // region Anime

    /** `GET /anime/{id}`. */
    public suspend fun anime(id: Int): Anime = single("anime/${checkId(id)}", Anime.serializer())

    /** `GET /anime/{id}/full`: the anime with relations, theme songs, external and streaming links. */
    public suspend fun animeFull(id: Int): Anime = single("anime/${checkId(id)}/full", Anime.serializer())

    /** `GET /anime/{id}/characters`: characters with their role and voice actors. */
    public suspend fun animeCharacters(id: Int): List<AnimeCharacter> =
        single("anime/${checkId(id)}/characters", ListSerializer(AnimeCharacter.serializer()))

    /** `GET /anime/{id}/staff`. */
    public suspend fun animeStaff(id: Int): List<StaffMember> =
        single("anime/${checkId(id)}/staff", ListSerializer(StaffMember.serializer()))

    /** `GET /anime/{id}/episodes?page=`: up to 100 episodes per page. */
    public suspend fun animeEpisodes(id: Int, page: Int? = null): JikanPage<Episode> =
        page("anime/${checkId(id)}/episodes", Episode.serializer(), listOfNotNull(page?.let { "page" to checkPage(it).toString() }))

    /** `GET /anime/{id}/episodes/{episode}`: one episode with its synopsis. */
    public suspend fun animeEpisode(id: Int, episode: Int): EpisodeDetail {
        require(episode >= 1) { "Episodes start at 1, was $episode" }
        return single("anime/${checkId(id)}/episodes/$episode", EpisodeDetail.serializer())
    }

    /** `GET /anime/{id}/recommendations`: entries users recommend to fans of this one, most votes first. */
    public suspend fun animeRecommendations(id: Int): List<Recommendation> =
        single("anime/${checkId(id)}/recommendations", ListSerializer(Recommendation.serializer()))

    /** `GET /anime/{id}/pictures`. */
    public suspend fun animePictures(id: Int): List<Picture> =
        single("anime/${checkId(id)}/pictures", ListSerializer(Picture.serializer()))

    /** `GET /anime/{id}/statistics`: list counts and the score histogram. */
    public suspend fun animeStatistics(id: Int): AnimeStatistics =
        single("anime/${checkId(id)}/statistics", AnimeStatistics.serializer())

    /** `GET /anime` with [query]: search and filter. */
    public suspend fun searchAnime(query: AnimeSearchQuery = AnimeSearchQuery()): JikanPage<Anime> =
        page("anime", Anime.serializer(), query.parameters())

    /** `GET /anime?q=` with only a text query. */
    public suspend fun searchAnime(text: String, page: Int? = null, limit: Int? = null, sfw: Boolean? = null): JikanPage<Anime> =
        searchAnime(AnimeSearchQuery(query = text, page = page, limit = limit, sfw = sfw))

    /** `GET /top/anime`: ranked by score, or by [filter] (airing, upcoming, popularity, favorites). */
    public suspend fun topAnime(
        filter: TopAnimeFilter? = null,
        type: AnimeType? = null,
        rating: AnimeRating? = null,
        sfw: Boolean? = null,
        page: Int? = null,
        limit: Int? = null,
    ): JikanPage<Anime> = page(
        "top/anime",
        Anime.serializer(),
        buildList<Pair<String, String>> {
            type?.let { add("type" to it.apiValue) }
            filter?.let { add("filter" to it.apiValue) }
            rating?.let { add("rating" to it.apiValue) }
            sfw?.let { add("sfw" to it.toString()) }
            addPaging(page, limit)
        },
    )

    /** `GET /random/anime`. Never cached. */
    public suspend fun randomAnime(): Anime = single("random/anime", Anime.serializer(), cacheable = false)

    // endregion

    // region Seasons and schedules

    /** `GET /seasons/now`: anime of the current season. [type] filters by format (`filter=tv`). */
    public suspend fun seasonNow(
        type: AnimeType? = null,
        sfw: Boolean? = null,
        continuing: Boolean = false,
        page: Int? = null,
        limit: Int? = null,
    ): JikanPage<Anime> = page("seasons/now", Anime.serializer(), seasonParameters(type, sfw, continuing, page, limit))

    /** `GET /seasons/upcoming`: announced anime of the coming seasons. */
    public suspend fun seasonUpcoming(
        type: AnimeType? = null,
        sfw: Boolean? = null,
        page: Int? = null,
        limit: Int? = null,
    ): JikanPage<Anime> = page("seasons/upcoming", Anime.serializer(), seasonParameters(type, sfw, false, page, limit))

    /** `GET /seasons/{year}/{season}`: anime of any season, such as summer 2026. */
    public suspend fun season(
        year: Int,
        season: Season,
        type: AnimeType? = null,
        sfw: Boolean? = null,
        continuing: Boolean = false,
        page: Int? = null,
        limit: Int? = null,
    ): JikanPage<Anime> {
        require(year in 1900..2200) { "year looks wrong: $year" }
        return page("seasons/$year/${season.apiValue}", Anime.serializer(), seasonParameters(type, sfw, continuing, page, limit))
    }

    /** `GET /seasons`: every year with the seasons MyAnimeList has, newest first. */
    public suspend fun seasonList(): List<SeasonArchive> = single("seasons", ListSerializer(SeasonArchive.serializer()))

    /** `GET /schedules?filter={day}`: anime airing on [day] (every day when `null`). */
    public suspend fun schedules(
        day: ScheduleDay? = null,
        sfw: Boolean? = null,
        kids: Boolean? = null,
        page: Int? = null,
        limit: Int? = null,
    ): JikanPage<Anime> = page(
        "schedules",
        Anime.serializer(),
        buildList<Pair<String, String>> {
            day?.let { add("filter" to it.apiValue) }
            kids?.let { add("kids" to it.toString()) }
            sfw?.let { add("sfw" to it.toString()) }
            addPaging(page, limit)
        },
    )

    // endregion

    // region Manga

    /** `GET /manga/{id}`. */
    public suspend fun manga(id: Int): Manga = single("manga/${checkId(id)}", Manga.serializer())

    /** `GET /manga/{id}/full`: the manga with relations and external links. */
    public suspend fun mangaFull(id: Int): Manga = single("manga/${checkId(id)}/full", Manga.serializer())

    /** `GET /manga/{id}/characters`. */
    public suspend fun mangaCharacters(id: Int): List<MangaCharacter> =
        single("manga/${checkId(id)}/characters", ListSerializer(MangaCharacter.serializer()))

    /** `GET /manga/{id}/recommendations`. */
    public suspend fun mangaRecommendations(id: Int): List<Recommendation> =
        single("manga/${checkId(id)}/recommendations", ListSerializer(Recommendation.serializer()))

    /** `GET /manga` with [query]: search and filter. */
    public suspend fun searchManga(query: MangaSearchQuery = MangaSearchQuery()): JikanPage<Manga> =
        page("manga", Manga.serializer(), query.parameters())

    /** `GET /top/manga`: ranked by score, or by [filter]. */
    public suspend fun topManga(
        filter: TopMangaFilter? = null,
        type: MangaType? = null,
        page: Int? = null,
        limit: Int? = null,
    ): JikanPage<Manga> = page(
        "top/manga",
        Manga.serializer(),
        buildList<Pair<String, String>> {
            type?.let { add("type" to it.apiValue) }
            filter?.let { add("filter" to it.apiValue) }
            addPaging(page, limit)
        },
    )

    /** `GET /random/manga`. Never cached. */
    public suspend fun randomManga(): Manga = single("random/manga", Manga.serializer(), cacheable = false)

    // endregion

    // region Genres, characters and people

    /** `GET /genres/anime`: genres with their MyAnimeList id and number of entries. */
    public suspend fun animeGenres(filter: GenreFilter? = null): List<Genre> =
        single("genres/anime", ListSerializer(Genre.serializer()), listOfNotNull(filter?.let { "filter" to it.apiValue }))

    /** `GET /genres/manga`. */
    public suspend fun mangaGenres(filter: GenreFilter? = null): List<Genre> =
        single("genres/manga", ListSerializer(Genre.serializer()), listOfNotNull(filter?.let { "filter" to it.apiValue }))

    /** `GET /characters/{id}`. */
    public suspend fun character(id: Int): Character = single("characters/${checkId(id)}", Character.serializer())

    /** `GET /characters/{id}/full`: the character with anime and manga roles and voice actors. */
    public suspend fun characterFull(id: Int): Character = single("characters/${checkId(id)}/full", Character.serializer())

    /** `GET /characters?q=`, most favorited first. */
    public suspend fun searchCharacters(text: String, page: Int? = null, limit: Int? = null): JikanPage<Character> =
        page(
            "characters",
            Character.serializer(),
            buildList<Pair<String, String>> {
                text.takeIf { it.isNotBlank() }?.let { add("q" to it.trim()) }
                add("order_by" to "favorites")
                add("sort" to SortOrder.Descending.apiValue)
                addPaging(page, limit)
            },
        )

    /** `GET /people/{id}`. */
    public suspend fun person(id: Int): Person = single("people/${checkId(id)}", Person.serializer())

    /** `GET /people?q=`, most favorited first. */
    public suspend fun searchPeople(text: String, page: Int? = null, limit: Int? = null): JikanPage<Person> =
        page(
            "people",
            Person.serializer(),
            buildList<Pair<String, String>> {
                text.takeIf { it.isNotBlank() }?.let { add("q" to it.trim()) }
                add("order_by" to "favorites")
                add("sort" to SortOrder.Descending.apiValue)
                addPaging(page, limit)
            },
        )

    // endregion

    // region Paging

    /**
     * Every page from [startPage] on, requested lazily as the flow is collected, until a page says
     * `has_next_page: false` or [maxPages] pages were emitted. Each request waits for the rate limiter.
     *
     * ```kotlin
     * jikan.pages { page -> jikan.searchAnime(query.copy(page = page)) }.items().take(60).toList()
     * ```
     */
    public fun <T> pages(
        startPage: Int = 1,
        maxPages: Int = Int.MAX_VALUE,
        load: suspend (page: Int) -> JikanPage<T>,
    ): Flow<JikanPage<T>> = flow {
        checkPage(startPage)
        require(maxPages > 0) { "maxPages must be positive, was $maxPages" }
        var page = startPage
        var emitted = 0
        while (emitted < maxPages) {
            val result = load(page)
            emit(result)
            emitted++
            if (!result.hasNextPage || result.data.isEmpty()) break
            page++
        }
    }

    /** Every page of an anime search. */
    public fun searchAnimePages(query: AnimeSearchQuery, maxPages: Int = Int.MAX_VALUE): Flow<JikanPage<Anime>> =
        pages(query.page ?: 1, maxPages) { searchAnime(query.copy(page = it)) }

    /** Every page of a manga search. */
    public fun searchMangaPages(query: MangaSearchQuery, maxPages: Int = Int.MAX_VALUE): Flow<JikanPage<Manga>> =
        pages(query.page ?: 1, maxPages) { searchManga(query.copy(page = it)) }

    /** Every page of `/top/anime`. */
    public fun topAnimePages(
        filter: TopAnimeFilter? = null,
        type: AnimeType? = null,
        limit: Int? = null,
        maxPages: Int = Int.MAX_VALUE,
    ): Flow<JikanPage<Anime>> = pages(1, maxPages) { topAnime(filter = filter, type = type, page = it, limit = limit) }

    /** Every page of the current season. */
    public fun seasonNowPages(type: AnimeType? = null, limit: Int? = null, maxPages: Int = Int.MAX_VALUE): Flow<JikanPage<Anime>> =
        pages(1, maxPages) { seasonNow(type = type, page = it, limit = limit) }

    /** Every page of an anime's episode list. */
    public fun episodePages(id: Int, maxPages: Int = Int.MAX_VALUE): Flow<JikanPage<Episode>> =
        pages(1, maxPages) { animeEpisodes(id, page = it) }

    // endregion

    // region Raw access

    /** Loads `GET {baseUrl}{path}?{parameters}` and decodes it with [deserializer], through the cache and limiter. */
    public suspend fun <T> get(
        path: String,
        deserializer: DeserializationStrategy<T>,
        parameters: List<Pair<String, String>> = emptyList(),
        cacheable: Boolean = true,
    ): T = fetch(url(path, parameters), deserializer, cacheable)

    /** The full url of [path] with [parameters], as this client requests it. */
    public fun url(path: String, parameters: List<Pair<String, String>> = emptyList()): String {
        val base = baseUrl + path.trim('/')
        if (parameters.isEmpty()) return base
        return base + "?" + parameters.joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
    }

    /** Removes every cached response. */
    public suspend fun clearCache() {
        cache?.clear()
    }

    /** Releases the HTTP client this instance created. */
    override fun close() {
        (transport as? AutoCloseable)?.close()
    }

    // endregion

    private suspend fun <T> single(
        path: String,
        serializer: KSerializer<T>,
        parameters: List<Pair<String, String>> = emptyList(),
        cacheable: Boolean = true,
    ): T = get(path, JikanResponse.serializer(serializer), parameters, cacheable).data

    private suspend fun <T> page(
        path: String,
        serializer: KSerializer<T>,
        parameters: List<Pair<String, String>>,
    ): JikanPage<T> = get(path, JikanPage.serializer(serializer), parameters)

    private suspend fun <T> fetch(url: String, deserializer: DeserializationStrategy<T>, cacheable: Boolean): T {
        val body = if (cacheable) load(url) else request(url, cacheable = false)
        return try {
            json.decodeFromString(deserializer, body)
        } catch (e: SerializationException) {
            cache?.remove(url)
            throw JikanException.Decoding(url, e)
        } catch (e: IllegalArgumentException) {
            cache?.remove(url)
            throw JikanException.Decoding(url, e)
        }
    }

    private suspend fun load(url: String): String {
        cache?.get(url)?.let { return it }
        return loadShared(url)
    }

    /** Joins a request for the same url that is already running, or starts one. */
    private suspend fun loadShared(url: String): String {
        var owner = false
        val deferred = inFlightLock.withLock {
            inFlight[url] ?: CompletableDeferred<String>().also {
                inFlight[url] = it
                owner = true
            }
        }
        if (!owner) {
            return try {
                deferred.await()
            } catch (e: CancellationException) {
                // The coroutine that made the request was cancelled, not this one: try again.
                currentCoroutineContext().ensureActive()
                loadShared(url)
            }
        }
        val result = runCatching { request(url, cacheable = true) }
        withContext(NonCancellable) { inFlightLock.withLock { inFlight.remove(url) } }
        result.fold(
            onSuccess = { deferred.complete(it) },
            onFailure = { deferred.completeExceptionally(it) },
        )
        return result.getOrThrow()
    }

    /** One request with rate limiting and retries: network errors and 5xx back off, 429 waits for `Retry-After`. */
    private suspend fun request(url: String, cacheable: Boolean): String {
        var failures = 0
        var rateLimited = 0
        while (true) {
            rateLimiter?.acquire()
            val response: JikanHttpResponse = try {
                transport.get(url)
            } catch (e: CancellationException) {
                throw e
            } catch (e: JikanException) {
                throw e
            } catch (e: Exception) {
                if (failures < config.retries) {
                    delay(backoff(failures++))
                    continue
                }
                throw JikanException.Network(url, e)
            }
            val status = response.status
            when {
                status in 200..299 -> {
                    if (cacheable) cache?.put(url, response.body)
                    return response.body
                }
                status == 404 -> throw JikanException.NotFound(url, apiMessage(response.body))
                status == 429 -> {
                    if (rateLimited >= config.rateLimitRetries) {
                        throw JikanException.RateLimited(url, response.retryAfterSeconds)
                    }
                    val wait = response.retryAfterSeconds?.let { it * 1_000 } ?: backoff(rateLimited)
                    rateLimited++
                    if (rateLimiter != null) rateLimiter.pause(wait) else delay(wait)
                }
                status >= 500 && failures < config.retries -> delay(backoff(failures++))
                else -> throw JikanException.Http(url, status, apiMessage(response.body))
            }
        }
    }

    private fun backoff(attempt: Int): Long {
        val delay = config.retryDelayMillis shl attempt.coerceIn(0, 20)
        return delay.coerceIn(0, config.maxRetryDelayMillis)
    }

    private fun apiMessage(body: String): String? = try {
        json.parseToJsonElement(body).jsonObject["message"]?.jsonPrimitive?.contentOrNull
    } catch (e: Exception) {
        null
    }

    private fun seasonParameters(type: AnimeType?, sfw: Boolean?, continuing: Boolean, page: Int?, limit: Int?) =
        buildList<Pair<String, String>> {
            type?.let { add("filter" to it.apiValue) }
            sfw?.let { add("sfw" to it.toString()) }
            if (continuing) add("continuing" to "true")
            addPaging(page, limit)
        }

    public companion object {
        /** `https://api.jikan.moe/v4/`. */
        public const val DEFAULT_BASE_URL: String = "https://api.jikan.moe/v4/"

        /** The largest `limit` Jikan accepts. */
        public const val MAX_PAGE_SIZE: Int = 25

        internal val json: Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
            isLenient = true
        }
    }
}

private fun MutableList<Pair<String, String>>.addPaging(page: Int?, limit: Int?) {
    page?.let { add("page" to checkPage(it).toString()) }
    limit?.let { add("limit" to checkLimit(it).toString()) }
}

private fun checkId(id: Int): Int {
    require(id > 0) { "MyAnimeList ids start at 1, was $id" }
    return id
}

private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")
