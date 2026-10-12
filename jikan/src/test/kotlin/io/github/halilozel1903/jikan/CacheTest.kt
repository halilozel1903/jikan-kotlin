package io.github.halilozel1903.jikan

import io.github.halilozel1903.jikan.cache.MemoryCache
import io.github.halilozel1903.jikan.transport.FixtureTransport
import io.github.halilozel1903.jikan.transport.JikanHttpResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

class CacheTest {

    @Test
    fun reusesCachedResponses() = runTest {
        MockJikan().use { api ->
            repeat(3) { api.client.anime(60101) }
            api.client.seasonNow(sfw = true, limit = 24)
            api.client.seasonNow(sfw = true, limit = 24)
            assertEquals(2, api.requests.size)
            api.client.clearCache()
            api.client.anime(60101)
            assertEquals(3, api.requests.size)
        }
    }

    @Test
    fun noCacheMeansEveryCallRequests() = runTest {
        MockJikan(cache = null).use { api ->
            repeat(3) { api.client.anime(60101) }
            assertEquals(3, api.requests.size)
        }
    }

    @Test
    fun doesNotCacheErrors() = runTest {
        MockJikan().use { api ->
            repeat(2) { runCatching { api.client.anime(424242) } }
            assertEquals(2, api.requests.size)
        }
    }

    @Test
    fun memoryCacheExpiresAndEvicts() = runTest {
        var now = 0L
        val cache = MemoryCache(maxEntries = 2, ttl = 10.minutes, clock = { now })
        cache.put("a", "1")
        cache.put("b", "2")
        cache.get("a")
        cache.put("c", "3")
        assertNull(cache.get("b"), "the least recently used entry is evicted")
        assertEquals("1", cache.get("a"))
        now += 10.minutes.inWholeMilliseconds
        assertNull(cache.get("a"))
        assertEquals(1, cache.size)
    }

    @Test
    fun identicalRequestsRunningTogetherShareOneCall() = runTest {
        val calls = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        val fixtures = FixtureTransport { path -> fixture(path) }
        val client = JikanClient {
            rateLimiter = null
            transport = io.github.halilozel1903.jikan.transport.JikanTransport { url ->
                calls.incrementAndGet()
                gate.await()
                fixtures.get(url)
            }
        }
        val results = (1..5).map { async { client.anime(60101) } }
        testScheduler.advanceUntilIdle()
        gate.complete(Unit)
        assertEquals(setOf(60101), results.awaitAll().map { it.malId }.toSet())
        assertEquals(1, calls.get())
    }

    @Test
    fun fixtureTransportAnswers404ForMissingFiles() = runTest {
        val transport = FixtureTransport { null }
        val response: JikanHttpResponse = transport.get("https://api.jikan.moe/v4/anime/1")
        assertEquals(404, response.status)
        assertEquals(listOf("https://api.jikan.moe/v4/anime/1"), transport.requests)
    }
}
