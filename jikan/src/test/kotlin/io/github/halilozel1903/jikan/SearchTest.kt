package io.github.halilozel1903.jikan

import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchTest {

    @Test
    fun searchesByText() = runTest {
        MockJikan().use { api ->
            val page = api.client.searchAnime("star")
            assertEquals("https://api.jikan.moe/v4/anime?q=star", api.urls.single())
            assertEquals(listOf(60101, 60307), page.data.map { it.malId })
        }
    }

    @Test
    fun sendsEveryFilterWithJikanNames() = runTest {
        val body = fixture("anime/q-star.json")!!
        MockJikan(override = { respond(body, HttpStatusCode.OK, jsonHeaders) }).use { api ->
            api.client.searchAnime(
                AnimeSearchQuery(
                    query = "  star map ",
                    type = AnimeType.Tv,
                    status = AnimeStatus.Airing,
                    rating = AnimeRating.Pg13,
                    genres = listOf(10, 2),
                    genresExclude = listOf(4),
                    minScore = 7.5,
                    maxScore = 10.0,
                    orderBy = AnimeOrderBy.Score,
                    sort = SortOrder.Descending,
                    sfw = true,
                    page = 2,
                    limit = 25,
                ),
            )
            val url = api.requests.single().url
            assertEquals("/v4/anime", url.encodedPath)
            val params = url.parameters
            assertEquals("star map", params["q"])
            assertEquals("tv", params["type"])
            assertEquals("airing", params["status"])
            assertEquals("pg13", params["rating"])
            assertEquals("10,2", params["genres"])
            assertEquals("4", params["genres_exclude"])
            assertEquals("7.5", params["min_score"])
            assertEquals("10", params["max_score"])
            assertEquals("score", params["order_by"])
            assertEquals("desc", params["sort"])
            assertEquals("true", params["sfw"])
            assertEquals("2", params["page"])
            assertEquals("25", params["limit"])
            assertEquals(
                "https://api.jikan.moe/v4/anime?q=star%20map&type=tv&status=airing&rating=pg13&genres=10%2C2&genres_exclude=4" +
                    "&min_score=7.5&max_score=10&order_by=score&sort=desc&sfw=true&page=2&limit=25",
                url.toString(),
            )
        }
    }

    @Test
    fun leavesOutUnsetParameters() = runTest {
        MockJikan().use { api ->
            api.client.searchAnime(AnimeSearchQuery(query = "star", type = AnimeType.Tv))
            assertEquals("https://api.jikan.moe/v4/anime?q=star&type=tv", api.urls.single())
            assertEquals("https://api.jikan.moe/v4/anime", api.client.url("anime", AnimeSearchQuery().parameters()))
        }
    }

    @Test
    fun mangaSearchUsesMangaNames() = runTest {
        MockJikan().use { api ->
            runCatching {
                api.client.searchManga(
                    MangaSearchQuery(
                        query = "harbor",
                        type = MangaType.LightNovel,
                        status = MangaStatus.Hiatus,
                        orderBy = MangaOrderBy.Chapters,
                        sort = SortOrder.Ascending,
                        magazines = listOf(4101),
                    ),
                )
            }
            assertEquals(
                "https://api.jikan.moe/v4/manga?q=harbor&type=lightnovel&status=hiatus&order_by=chapters&sort=asc&magazines=4101",
                api.urls.single(),
            )
        }
    }
}
