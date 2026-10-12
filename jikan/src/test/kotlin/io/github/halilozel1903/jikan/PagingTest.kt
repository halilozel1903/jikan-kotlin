package io.github.halilozel1903.jikan

import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PagingTest {

    @Test
    fun followsPagesUntilTheLastOne() = runTest {
        MockJikan().use { api ->
            val pages = api.client.topAnimePages(limit = 5).toList()
            assertEquals(listOf(1, 2, 3), pages.map { it.page })
            assertEquals(listOf(5, 5, 3), pages.map { it.data.size })
            assertEquals(
                listOf(
                    "https://api.jikan.moe/v4/top/anime?page=1&limit=5",
                    "https://api.jikan.moe/v4/top/anime?page=2&limit=5",
                    "https://api.jikan.moe/v4/top/anime?page=3&limit=5",
                ),
                api.urls,
            )
            val scores = pages.flatMap { page -> page.data.map { it.score!! } }
            assertEquals(scores.sortedDescending(), scores)
        }
    }

    @Test
    fun itemsAreLoadedLazily() = runTest {
        MockJikan().use { api ->
            val first = api.client.topAnimePages(limit = 5).items().take(3).map { it.malId }.toList()
            assertEquals(listOf(60101, 60201, 60401), first)
            assertEquals(1, api.requests.size)
        }
        MockJikan().use { api ->
            assertEquals(13, api.client.topAnimePages(limit = 5).items().count())
            assertEquals(2, api.client.topAnimePages(limit = 5, maxPages = 2).count())
        }
    }

    @Test
    fun pagesEpisodes() = runTest {
        MockJikan().use { api ->
            val episodes = api.client.episodePages(60203).items().toList()
            assertEquals(130, episodes.size)
            assertEquals((1..130).toList(), episodes.map { it.malId })
            assertEquals(listOf(40, 80, 120), episodes.filter { it.filler }.map { it.malId })
            assertEquals(2, api.requests.size)
        }
    }

    @Test
    fun genericPagesStopOnEmptyPages() = runTest {
        MockJikan().use { api ->
            val pages = api.client.pages(startPage = 4) { page ->
                io.github.halilozel1903.jikan.model.JikanPage(
                    data = if (page < 6) listOf(page) else emptyList(),
                    pagination = io.github.halilozel1903.jikan.model.Pagination(hasNextPage = true, currentPage = page),
                )
            }.toList()
            assertEquals(listOf(4, 5, 6), pages.map { it.page })
        }
    }
}
