package io.github.halilozel1903.jikan

import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnimeTest {

    @Test
    fun decodesAnAnime() = runTest {
        MockJikan().use { api ->
            val anime = api.client.anime(60101)
            assertEquals("https://api.jikan.moe/v4/anime/60101", api.urls.single())
            assertEquals(60101, anime.malId)
            assertEquals("Hoshi no Chizushi", anime.title)
            assertEquals("Starlit Cartographers", anime.displayTitle)
            assertEquals("星の地図師", anime.titleJapanese)
            assertEquals(listOf("Default", "Japanese", "English"), anime.titles.map { it.type })
            assertEquals("TV", anime.type)
            assertEquals(12, anime.episodes)
            assertEquals("Currently Airing", anime.status)
            assertTrue(anime.airing)
            assertEquals(8.91, anime.score)
            assertEquals(41250, anime.scoredBy)
            assertEquals(18, anime.rank)
            assertEquals(LocalDate.of(2026, 10, 2), anime.aired.fromDate)
            assertNull(anime.aired.toDate)
            assertEquals("Oct 2, 2026 to ?", anime.aired.string)
            assertEquals(2026, anime.aired.prop?.from?.year)
            assertEquals(Season.Fall, anime.parsedSeason)
            assertEquals("Fall 2026", anime.seasonLabel)
            assertEquals(listOf("Adventure", "Fantasy"), anime.genres.map { it.name })
            assertEquals(listOf(2, 10), anime.genres.map { it.malId })
            assertEquals("anime", anime.genres.first().type)
            assertEquals("https://myanimelist.net/anime/genre/2/Adventure", anime.genres.first().url)
            assertEquals(listOf("Adventure", "Fantasy", "Space", "Seinen"), anime.allGenres.map { it.name })
            assertEquals("Studio Lumen", anime.studios.single().name)
            assertEquals("Fridays", anime.broadcast?.day)
            assertTrue(anime.images.large!!.endsWith("200101l.webp"))
            assertTrue(anime.images.jpg.largeImageUrl!!.endsWith("200101l.jpg"))
            assertTrue(anime.images.small!!.endsWith("200101t.webp"))
            assertTrue(anime.relations.isEmpty())
        }
    }

    @Test
    fun decodesTheFullAnime() = runTest {
        MockJikan().use { api ->
            val anime = api.client.animeFull(60201)
            assertEquals("https://api.jikan.moe/v4/anime/60201/full", api.urls.single())
            assertEquals("Harbor of a Thousand Bells", anime.displayTitle)
            assertEquals("Manga", anime.source)
            assertEquals(listOf("Thousand Bells"), anime.titleSynonyms)
            assertEquals(listOf("Adaptation", "Side story"), anime.relations.map { it.relation })
            assertEquals("manga", anime.relations.first().entry.single().type)
            assertEquals(1, anime.theme?.openings?.size)
            assertEquals(2, anime.theme?.endings?.size)
            assertEquals("Official Site", anime.external.single().name)
            assertEquals(1, anime.streaming.size)
        }
    }

    @Test
    fun decodesCharactersStaffAndEpisodes() = runTest {
        MockJikan().use { api ->
            val characters = api.client.animeCharacters(60201)
            assertEquals(7, characters.size)
            val aoi = characters.first()
            assertEquals("Kanemori, Aoi", aoi.character.name)
            assertEquals("Aoi Kanemori", aoi.character.displayName)
            assertTrue(aoi.isMain)
            assertEquals("Mio Sakurada", aoi.voiceActor()?.displayName)
            assertEquals("Rachel Ortiz", aoi.voiceActor("English")?.displayName)
            assertNull(characters[2].voiceActor("English"))
            assertTrue(aoi.character.images.jpg.imageUrl!!.endsWith("80001.jpg"))

            val staff = api.client.animeStaff(60201)
            assertEquals(listOf("Director", "Storyboard"), staff.first().positions)

            val episodes = api.client.animeEpisodes(60201)
            assertEquals(14, episodes.data.size)
            assertFalse(episodes.hasNextPage)
            assertEquals(1, episodes.page)
            assertNull(episodes.pagination.items)
            assertEquals("The Bell That Rang Twice", episodes.data.first().title)
            assertEquals(LocalDate.of(2026, 7, 3), episodes.data.first().airedDate)
            assertEquals(listOf(8), episodes.data.filter { it.recap }.map { it.malId })

            val episode = api.client.animeEpisode(60201, 1)
            assertEquals(1420, episode.duration)
            assertEquals("Aoi hears a bell ring for a ship that sank twenty years ago.", episode.synopsis)
        }
    }

    @Test
    fun decodesRecommendationsPicturesAndStatistics() = runTest {
        MockJikan().use { api ->
            val recommendations = api.client.animeRecommendations(60201)
            assertEquals(7, recommendations.size)
            assertEquals(60103, recommendations.first().entry.malId)
            assertEquals("Kokonoka no Shiokaze Tourou", recommendations.first().entry.title)
            assertEquals(41, recommendations.first().votes)

            assertEquals(3, api.client.animePictures(60201).size)

            val stats = api.client.animeStatistics(60201)
            assertEquals(402100, stats.total)
            assertEquals(139100, stats.planToWatch)
            assertEquals(10, stats.scores.size)
            assertEquals(9, stats.scores.maxBy { it.votes }.score)
        }
    }

    @Test
    fun randomAnimeIsNeverCached() = runTest {
        MockJikan().use { api ->
            api.client.randomAnime()
            api.client.randomAnime()
            assertEquals(2, api.requests.size)
        }
    }

    @Test
    fun rejectsBadArguments() = runTest {
        MockJikan().use { api ->
            kotlin.test.assertFailsWith<IllegalArgumentException> { api.client.anime(0) }
            kotlin.test.assertFailsWith<IllegalArgumentException> { api.client.topAnime(limit = 26) }
            kotlin.test.assertFailsWith<IllegalArgumentException> { api.client.seasonNow(page = 0) }
            kotlin.test.assertFailsWith<IllegalArgumentException> { api.client.animeEpisode(1, 0) }
            assertTrue(api.requests.isEmpty())
        }
    }
}
