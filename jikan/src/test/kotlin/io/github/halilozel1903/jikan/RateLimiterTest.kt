package io.github.halilozel1903.jikan

import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RateLimiterTest {

    private fun TestScope.limiter(vararg limits: RateLimit) =
        JikanRateLimiter(limits.toList().ifEmpty { JikanRateLimiter.JIKAN_LIMITS }) { testScheduler.currentTime }

    @Test
    fun allowsThreeRequestsPerSecond() = runTest {
        val limiter = limiter()
        val times = (1..7).map {
            limiter.acquire()
            testScheduler.currentTime
        }
        assertEquals(listOf(0L, 0L, 0L, 1000L, 1000L, 1000L, 2000L), times)
    }

    @Test
    fun allowsSixtyRequestsPerMinute() = runTest {
        val limiter = limiter()
        val times = (1..61).map {
            limiter.acquire()
            testScheduler.currentTime
        }
        // 3 per second for 20 seconds uses the minute's budget; the 61st request waits for the first to expire.
        assertEquals(19_000L, times[59])
        assertEquals(60_000L, times[60])
    }

    @Test
    fun concurrentCallersShareTheBudget() = runTest {
        val limiter = limiter(RateLimit(2, 1.seconds))
        val times = (1..5).map { async { limiter.acquire(); testScheduler.currentTime } }.awaitAll()
        assertEquals(listOf(0L, 0L, 1000L, 1000L, 2000L), times.sorted())
    }

    @Test
    fun pauseHoldsBackEveryRequest() = runTest {
        val limiter = limiter()
        limiter.acquire()
        limiter.pause(5_000)
        assertEquals(5_000L, limiter.nextDelayMillis())
        limiter.acquire()
        assertEquals(5_000L, testScheduler.currentTime)
        limiter.reset()
        assertEquals(0L, limiter.nextDelayMillis())
    }

    @Test
    fun rejectsBadLimits() {
        assertFailsWith<IllegalArgumentException> { RateLimit(0, 1.seconds) }
        assertFailsWith<IllegalArgumentException> { JikanRateLimiter(emptyList()) }
    }

    @Test
    fun clientRequestsWaitForTheLimiter() = runTest {
        MockJikan(rateLimiter = limiter()).use { api ->
            for (id in listOf(60101, 60201)) {
                api.client.anime(id)
                api.client.animeFull(60201)
            }
            api.client.animeCharacters(60201)
            api.client.animeEpisodes(60201)
            // 5 network requests (the second full response comes from the cache): the 4th and 5th wait a second.
            assertEquals(5, api.requests.size)
            assertEquals(1000L, testScheduler.currentTime)
        }
    }

    @Test
    fun retriesAfterRetryAfterOn429() = runTest {
        val calls = AtomicInteger()
        MockJikan(
            rateLimiter = limiter(),
            override = { request ->
                if (calls.incrementAndGet() == 1) {
                    respond(
                        """{"status":429,"type":"RateLimitException","message":"You are being rate-limited.","error":null}""",
                        HttpStatusCode.TooManyRequests,
                        headersOf(HttpHeaders.RetryAfter, "2"),
                    )
                } else {
                    null
                }
            },
        ).use { api ->
            val anime = api.client.anime(60101)
            assertEquals(60101, anime.malId)
            assertEquals(2, api.requests.size)
            assertEquals(2_000L, testScheduler.currentTime)
        }
    }

    @Test
    fun backsOffWithoutRetryAfterAndGivesUp() = runTest {
        MockJikan(
            rateLimiter = null,
            rateLimitRetries = 2,
            override = { respond("""{"status":429,"message":"slow down"}""", HttpStatusCode.TooManyRequests, jsonHeaders) },
        ).use { api ->
            val error = assertFailsWith<JikanException.RateLimited> { api.client.anime(60101) }
            assertEquals("https://api.jikan.moe/v4/anime/60101", error.url)
            assertEquals(3, api.requests.size)
            // 1 s, then 2 s of backoff.
            assertEquals(3_000L, testScheduler.currentTime)
        }
    }

    @Test
    fun retriesServerErrorsThenSucceeds() = runTest {
        val calls = AtomicInteger()
        MockJikan(
            retries = 2,
            override = {
                if (calls.incrementAndGet() <= 2) respond("""{"status":503,"message":"MyAnimeList is down"}""", HttpStatusCode.ServiceUnavailable, jsonHeaders) else null
            },
        ).use { api ->
            assertEquals(60101, api.client.anime(60101).malId)
            assertEquals(3, api.requests.size)
            assertTrue(testScheduler.currentTime >= 3_000L)
        }
    }

    @Test
    fun reportsErrors() = runTest {
        MockJikan().use { api ->
            val notFound = assertFailsWith<JikanException.NotFound> { api.client.anime(999999) }
            assertEquals("Resource does not exist", notFound.apiMessage)
        }
        MockJikan(retries = 1, override = { respond("""{"status":500,"message":"Upstream"}""", HttpStatusCode.InternalServerError, jsonHeaders) }).use { api ->
            val error = assertFailsWith<JikanException.Http> { api.client.anime(60101) }
            assertEquals(500, error.status)
            assertEquals("Upstream", error.apiMessage)
            assertEquals(2, api.requests.size)
        }
        MockJikan(override = { respond("""{"status":400,"message":"Invalid filter"}""", HttpStatusCode.BadRequest, jsonHeaders) }).use { api ->
            val error = assertFailsWith<JikanException.Http> { api.client.topAnime() }
            assertEquals(400, error.status)
            assertEquals(1, api.requests.size)
        }
        MockJikan(override = { respond("""{"data": {"title": "no id"}}""", HttpStatusCode.OK, jsonHeaders) }).use { api ->
            assertFailsWith<JikanException.Decoding> { api.client.anime(60101) }
        }
        MockJikan(retries = 1, override = { throw java.io.IOException("offline") }).use { api ->
            val error = assertFailsWith<JikanException.Network> { api.client.anime(60101) }
            assertTrue(error.cause is java.io.IOException)
            assertEquals(2, api.requests.size)
        }
    }
}
