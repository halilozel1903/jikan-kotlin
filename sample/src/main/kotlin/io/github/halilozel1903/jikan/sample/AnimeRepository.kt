package io.github.halilozel1903.jikan.sample

import android.content.Context
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
import io.github.halilozel1903.jikan.transport.FixtureTransport
import io.github.halilozel1903.jikan.transport.JikanTransport
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
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
 * The sample's only data source; every request goes through [JikanClient], its rate limiter and its cache.
 * `SampleFixturesTest` in the `jikan` module makes the same calls against the fixtures the screenshot scenes use.
 */
class AnimeRepository(private val client: JikanClient, val isDemo: Boolean) {

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
        val anime = try {
            client.animeFull(id)
        } catch (e: JikanException.NotFound) {
            // The bundled demo data only has the full record of some titles.
            client.anime(id)
        }
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

/** One repository for the live API and one for the bundled fixtures, created on first use. */
object Repositories {
    private var live: AnimeRepository? = null
    private var demo: AnimeRepository? = null

    @Synchronized
    fun live(context: Context): AnimeRepository = live ?: AnimeRepository(
        JikanClient {
            // Shares OkHttp with Coil. The client keeps the default limiter (3 per second, 60 per minute).
            engine = OkHttp.create()
            userAgent = "jikan-kotlin-sample/1.0.0 (+https://github.com/halilozel1903/jikan-kotlin)"
        },
        isDemo = false,
    ).also { live = it }

    @Synchronized
    fun demo(context: Context): AnimeRepository = demo ?: run {
        val assets = context.applicationContext.assets
        val fixtures = FixtureTransport { path ->
            try {
                assets.open("fixtures/$path").bufferedReader().use { it.readText() }
            } catch (e: IOException) {
                null
            }
        }
        AnimeRepository(
            JikanClient {
                transport = JikanTransport { url -> withContext(Dispatchers.IO) { fixtures.get(url) } }
                // Local files: no need to space out requests.
                rateLimiter = null
            },
            isDemo = true,
        )
    }.also { demo = it }
}
