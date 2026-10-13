package io.github.halilozel1903.jikan.sample

import io.github.halilozel1903.jikan.AnimeOrderBy
import io.github.halilozel1903.jikan.AnimeSearchQuery
import io.github.halilozel1903.jikan.JikanClient
import io.github.halilozel1903.jikan.JikanException
import io.github.halilozel1903.jikan.Season
import io.github.halilozel1903.jikan.SortOrder
import io.github.halilozel1903.jikan.TopAnimeFilter
import io.github.halilozel1903.jikan.model.Anime
import io.github.halilozel1903.jikan.model.AnimeCharacter
import io.github.halilozel1903.jikan.model.Episode
import io.github.halilozel1903.jikan.model.Recommendation
import io.github.halilozel1903.jikan.model.SeasonArchive
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Calendar

/** A rail of the home screen built from a genre search. */
data class GenreRail(val genreId: Int, val name: String, val anime: List<Anime>)

/** Everything the home screen shows. */
data class HomeData(
    /** The best rated show of the current season. */
    val hero: Anime?,
    val topAiring: List<Anime>,
    val thisSeason: List<Anime>,
    val upcoming: List<Anime>,
    val genres: List<GenreRail>,
)

/** Everything the detail screen shows. */
data class DetailData(
    val anime: Anime,
    val episodes: List<Episode>,
    val characters: List<AnimeCharacter>,
    val recommendations: List<Recommendation>,
)

/** One season of the season browser, with the seasons it can switch to. */
data class SeasonData(
    val year: Int,
    val season: Season,
    val anime: List<Anime>,
    val archive: List<SeasonArchive>,
)

/**
 * The sample's only data source: the live Jikan API. Every request goes through [JikanClient], its rate limiter
 * (3 per second, 60 per minute), its retries and its cache.
 *
 * When [strict], a failing extra section (episodes, characters, a rail) fails the whole screen instead of being left
 * out. Screenshot scenes use this, so a half loaded screen fails CI rather than ending up in the README.
 */
class AnimeRepository(private val client: JikanClient, private val strict: Boolean = false) {

    suspend fun home(): HomeData = coroutineScope {
        val season = async { client.seasonNow(sfw = true, limit = 24).data }
        val top = async { client.topAnime(filter = TopAnimeFilter.Airing, sfw = true, limit = 20).data }
        val upcoming = async { optional { client.seasonUpcoming(sfw = true, limit = 20).data } }
        val genres = HOME_GENRES.map { (id, name) ->
            async {
                val query = AnimeSearchQuery(
                    genres = listOf(id),
                    orderBy = AnimeOrderBy.Score,
                    sort = SortOrder.Descending,
                    sfw = true,
                    limit = 20,
                )
                GenreRail(id, name, optional { client.searchAnime(query).data })
            }
        }
        val now = season.await().unique()
        HomeData(
            hero = now.filter { it.score != null }.maxByOrNull { it.score ?: 0.0 } ?: now.firstOrNull(),
            topAiring = top.await().unique(),
            thisSeason = now,
            upcoming = upcoming.await().unique(),
            genres = genres.awaitAll().map { it.copy(anime = it.anime.unique()) }.filter { it.anime.isNotEmpty() },
        )
    }

    suspend fun detail(id: Int): DetailData = coroutineScope {
        val episodes = async { optional { client.animeEpisodes(id).data } }
        val characters = async { optional { client.animeCharacters(id) } }
        val recommendations = async { optional { client.animeRecommendations(id) } }
        val anime = client.animeFull(id)
        DetailData(
            anime = anime,
            episodes = episodes.await().distinctBy { it.malId },
            characters = characters.await().distinctBy { it.character.malId }.sortedByDescending { it.isMain },
            recommendations = recommendations.await().distinctBy { it.entry.malId },
        )
    }

    suspend fun season(year: Int, season: Season): SeasonData = coroutineScope {
        val archive = async { optional { client.seasonList() } }
        val anime = client.season(year, season, sfw = true, limit = 24).data.unique()
        SeasonData(year, season, anime, archive.await())
    }

    /** Jikan lists can repeat an entry; lazy lists need unique keys. */
    private fun List<Anime>.unique(): List<Anime> = distinctBy { it.malId }

    /** Extra sections of a screen should not fail the whole screen. */
    private suspend fun <T> optional(block: suspend () -> List<T>): List<T> = try {
        block()
    } catch (e: JikanException) {
        if (strict) throw e
        emptyList()
    }

    companion object {
        /** MyAnimeList genre ids shown as rails on the home screen. */
        val HOME_GENRES: List<Pair<Int, String>> = listOf(10 to "Fantasy", 24 to "Sci-Fi")

        /** The anime season of today, such as fall 2026. */
        fun currentSeason(): Pair<Int, Season> {
            val calendar = Calendar.getInstance()
            return calendar.get(Calendar.YEAR) to Season.ofMonth(calendar.get(Calendar.MONTH) + 1)
        }
    }
}

/** The app's repositories, created on first use. */
object Repositories {
    private var live: AnimeRepository? = null
    private var scenes: AnimeRepository? = null

    private fun client(retries: Int, apiUrl: String? = null) = JikanClient {
        if (apiUrl != null) baseUrl = apiUrl
        // Shares OkHttp with Coil. The client keeps the default limiter (3 per second, 60 per minute), shared by
        // every client in the process.
        engine = OkHttp.create()
        userAgent = "jikan-kotlin-sample/1.0.0 (+https://github.com/halilozel1903/jikan-kotlin)"
        this.retries = retries
        rateLimitRetries = retries + 1
    }

    /** Normal use: a failing extra section is left out. */
    @Synchronized
    fun live(): AnimeRepository = live ?: AnimeRepository(client(retries = 2)).also { live = it }

    /**
     * Screenshot scenes: more retries for a busy CI network, and every section must load. [apiUrl] replaces
     * api.jikan.moe, for the Jikan server CI runs.
     */
    @Synchronized
    fun scenes(apiUrl: String? = null): AnimeRepository =
        scenes ?: AnimeRepository(client(retries = 4, apiUrl = apiUrl), strict = true).also { scenes = it }
}
