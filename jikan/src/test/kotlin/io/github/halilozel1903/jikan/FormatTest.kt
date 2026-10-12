package io.github.halilozel1903.jikan

import io.github.halilozel1903.jikan.transport.FixtureTransport
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FormatTest {

    @Test
    fun formatsNamesScoresAndCounts() {
        assertEquals("Aoi Kanemori", JikanFormat.naturalName("Kanemori, Aoi"))
        assertEquals("Bellkeeper", JikanFormat.naturalName("Bellkeeper"))
        assertEquals("8.91", JikanFormat.score(8.91))
        assertEquals("8.50", JikanFormat.score(8.5))
        assertEquals("N/A", JikanFormat.score(null))
        assertEquals("950", JikanFormat.compactCount(950))
        assertEquals("12.4K", JikanFormat.compactCount(12_400))
        assertEquals("41K", JikanFormat.compactCount(41_000))
        assertEquals("212K", JikanFormat.compactCount(212_400))
        assertEquals("1.2M", JikanFormat.compactCount(1_234_567))
        assertEquals("1 ep", JikanFormat.episodes(1))
        assertNull(JikanFormat.episodes(null))
    }

    @Test
    fun summarizesAnAnime() = runTest {
        MockJikan().use { api ->
            assertEquals("TV · 12 eps · Fall 2026 · Studio Lumen", JikanFormat.summary(api.client.anime(60101)))
            val upcoming = api.client.seasonUpcoming(sfw = true, limit = 20).data.first()
            assertEquals("TV · Winter 2027 · Studio Lumen", JikanFormat.summary(upcoming))
        }
    }

    @Test
    fun parsesSeasons() {
        assertEquals(Season.Fall, Season.fromApi("autumn"))
        assertEquals(Season.Summer, Season.fromApi("Summer"))
        assertNull(Season.fromApi("monsoon"))
        assertEquals(Season.Winter, Season.ofMonth(1))
        assertEquals(Season.Fall, Season.ofMonth(10))
    }

    @Test
    fun mapsUrlsToFixturePaths() {
        assertEquals("anime/52991.json", FixtureTransport.pathFor("https://api.jikan.moe/v4/anime/52991"))
        assertEquals("anime/52991/full.json", FixtureTransport.pathFor("https://api.jikan.moe/v4/anime/52991/full/"))
        assertEquals("seasons.json", FixtureTransport.pathFor("https://api.jikan.moe/v4/seasons"))
        assertEquals(
            "seasons/now/limit-24_sfw-true.json",
            FixtureTransport.pathFor("https://api.jikan.moe/v4/seasons/now?sfw=true&limit=24"),
        )
        assertEquals(
            "anime/genres-10-2_order_by-score_q-star-map.json",
            FixtureTransport.pathFor("https://api.jikan.moe/v4/anime?q=Star%20Map&order_by=score&genres=10%2C2"),
        )
        assertEquals("anime/1.json", FixtureTransport.pathFor("http://localhost:8080/v4/anime/1"))
        assertNull(FixtureTransport.pathFor("https://example.com/anime/1"))
    }
}
