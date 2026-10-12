package io.github.halilozel1903.jikan

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The sample's screenshot scenes read these fixtures through FixtureTransport. This test makes exactly the calls of
 * `sample/.../AnimeRepository.kt`, so a renamed fixture or a changed parameter fails here instead of on the emulator.
 */
class SampleFixturesTest {

    @Test
    fun homeSceneFixturesExist() = runTest {
        MockJikan().use { api ->
            val season = api.client.seasonNow(sfw = true, limit = 24).data
            val hero = season.filter { it.score != null }.maxBy { it.score!! }
            assertEquals("Starlit Cartographers", hero.displayTitle)
            assertEquals(12, api.client.topAnime(filter = TopAnimeFilter.Airing, sfw = true, limit = 20).data.size)
            assertEquals(8, api.client.seasonUpcoming(sfw = true, limit = 20).data.size)
            for (genre in listOf(10, 24)) {
                val rail = api.client.searchAnime(
                    AnimeSearchQuery(genres = listOf(genre), orderBy = AnimeOrderBy.Score, sort = SortOrder.Descending, sfw = true, limit = 20),
                ).data
                assertTrue(rail.isNotEmpty())
                assertTrue(rail.all { anime -> anime.genres.any { it.malId == genre } })
            }
        }
    }

    @Test
    fun detailSceneFixturesExist() = runTest {
        MockJikan().use { api ->
            assertEquals("Harbor of a Thousand Bells", api.client.animeFull(60201).displayTitle)
            assertEquals(14, api.client.animeEpisodes(60201).data.size)
            assertEquals(7, api.client.animeCharacters(60201).size)
            assertEquals(7, api.client.animeRecommendations(60201).size)
        }
    }

    @Test
    fun seasonSceneFixturesExist() = runTest {
        MockJikan().use { api ->
            val archive = api.client.seasonList()
            assertTrue(archive.any { it.year == 2026 && "summer" in it.seasons })
            assertEquals(12, api.client.season(2026, Season.Summer, sfw = true, limit = 24).data.size)
        }
    }
}
