package io.github.halilozel1903.jikan

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Seasons, schedules, top lists, genres, manga, characters and people. */
class CatalogTest {

    @Test
    fun loadsSeasons() = runTest {
        MockJikan().use { api ->
            val now = api.client.seasonNow(sfw = true, limit = 24)
            assertEquals(12, now.data.size)
            assertEquals(24, now.pagination.items?.perPage)
            assertEquals(12, now.pagination.items?.total)
            assertTrue(now.data.all { it.season == "fall" && it.year == 2026 })

            val upcoming = api.client.seasonUpcoming(sfw = true, limit = 20)
            assertTrue(upcoming.data.all { it.score == null && it.status == "Not yet aired" })
            assertEquals("Jan 2027 to ?", upcoming.data.first().aired.string)

            val summer = api.client.season(2026, Season.Summer, sfw = true, limit = 24)
            assertEquals("Harbor of a Thousand Bells", summer.data.first().displayTitle)
            assertTrue(summer.data.all { it.parsedSeason == Season.Summer })

            val archive = api.client.seasonList()
            assertEquals(2027, archive.first().year)
            assertEquals(listOf(Season.Winter), archive.first().parsedSeasons)
            assertEquals(Season.entries, archive[1].parsedSeasons)

            assertEquals(
                listOf(
                    "https://api.jikan.moe/v4/seasons/now?sfw=true&limit=24",
                    "https://api.jikan.moe/v4/seasons/upcoming?sfw=true&limit=20",
                    "https://api.jikan.moe/v4/seasons/2026/summer?sfw=true&limit=24",
                    "https://api.jikan.moe/v4/seasons",
                ),
                api.urls,
            )
        }
    }

    @Test
    fun seasonFiltersUseJikanParameterNames() = runTest {
        MockJikan().use { api ->
            runCatching { api.client.seasonNow(type = AnimeType.Tv, continuing = true, page = 2) }
            assertEquals("https://api.jikan.moe/v4/seasons/now?filter=tv&continuing=true&page=2", api.urls.single())
        }
    }

    @Test
    fun loadsTopAiringAndSchedules() = runTest {
        MockJikan().use { api ->
            val top = api.client.topAnime(filter = TopAnimeFilter.Airing, sfw = true, limit = 20)
            assertEquals("https://api.jikan.moe/v4/top/anime?filter=airing&sfw=true&limit=20", api.urls.last())
            assertTrue(top.hasNextPage)
            assertEquals(3, top.pagination.lastVisiblePage)
            val scores = top.data.mapNotNull { it.score }
            assertEquals(scores.sortedDescending(), scores)

            val friday = api.client.schedules(ScheduleDay.Friday)
            assertEquals("https://api.jikan.moe/v4/schedules?filter=friday", api.urls.last())
            assertTrue(friday.data.all { it.broadcast?.day == "Fridays" })
        }
    }

    @Test
    fun loadsGenres() = runTest {
        MockJikan().use { api ->
            val genres = api.client.animeGenres()
            assertEquals("Action", genres.first().name)
            assertEquals(1, genres.first().malId)
            assertEquals(4170, genres.first { it.name == "Fantasy" }.count)
            runCatching { api.client.animeGenres(GenreFilter.Themes) }
            assertEquals("https://api.jikan.moe/v4/genres/anime?filter=themes", api.urls.last())
        }
    }

    @Test
    fun loadsManga() = runTest {
        MockJikan().use { api ->
            val manga = api.client.manga(70001)
            assertEquals("Harbor of a Thousand Bells", manga.displayTitle)
            assertTrue(manga.publishing)
            assertNull(manga.chapters)
            assertEquals("Umino, Sayo", manga.authors.single().name)
            assertEquals("Monthly Lantern", manga.serializations.single().name)
            assertEquals(java.time.LocalDate.of(2021, 5, 25), manga.published.fromDate)

            assertEquals("Adaptation", api.client.mangaFull(70001).relations.single().relation)
            assertEquals(3, api.client.mangaCharacters(70001).size)
            assertEquals(70001, api.client.topManga().data.single().malId)
            assertEquals(70001, api.client.searchManga(MangaSearchQuery(query = "harbor")).data.single().malId)
        }
    }

    @Test
    fun loadsCharactersAndPeople() = runTest {
        MockJikan().use { api ->
            val aoi = api.client.character(80001)
            assertEquals("Aoi Kanemori", aoi.displayName)
            assertEquals("金森 葵", aoi.nameKanji)
            assertEquals(listOf("Bell Girl"), aoi.nicknames)
            assertTrue(aoi.anime.isEmpty())

            val full = api.client.characterFull(80001)
            assertEquals(60201, full.anime.single().anime.malId)
            assertEquals("Main", full.manga.single().role)
            assertEquals(listOf("Japanese", "English"), full.voices.map { it.language })

            val mio = api.client.person(90001)
            assertEquals("Mio Sakurada", mio.displayName)
            assertEquals("桜田", mio.familyName)
            assertEquals(2140, mio.favorites)
        }
    }
}
