package io.github.halilozel1903.jikan

import io.github.halilozel1903.jikan.cache.JikanCache
import io.github.halilozel1903.jikan.cache.MemoryCache
import io.github.halilozel1903.jikan.transport.FixtureTransport
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.util.concurrent.CopyOnWriteArrayList

/** Reads `src/test/resources/fixtures/<path>`. */
internal fun fixture(path: String): String? =
    Fixtures::class.java.getResource("/fixtures/$path")?.readText()

internal object Fixtures

internal val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

/**
 * A client on a Ktor [MockEngine] that serves the JSON fixtures and answers 404 like Jikan for anything else.
 * [override] answers a request itself when it returns non-null.
 */
internal class MockJikan(
    cache: JikanCache? = MemoryCache(),
    rateLimiter: JikanRateLimiter? = null,
    retries: Int = 0,
    rateLimitRetries: Int = 3,
    private val override: (suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData?)? = null,
) : AutoCloseable {
    val requests: MutableList<HttpRequestData> = CopyOnWriteArrayList()

    val engine = MockEngine { request ->
        requests += request
        override?.invoke(this, request) ?: run {
            val body = FixtureTransport.pathFor(request.url.toString())?.let { fixture(it) }
            if (body == null) {
                respond(
                    """{"status":404,"type":"BadResponseException","message":"Resource does not exist","error":"404 on ${request.url}"}""",
                    HttpStatusCode.NotFound,
                    jsonHeaders,
                )
            } else {
                respond(body, HttpStatusCode.OK, jsonHeaders)
            }
        }
    }

    val client = JikanClient {
        engine = this@MockJikan.engine
        this.cache = cache
        this.rateLimiter = rateLimiter
        this.retries = retries
        this.rateLimitRetries = rateLimitRetries
    }

    val urls: List<String> get() = requests.map { it.url.toString() }

    override fun close() {
        client.close()
        engine.close()
    }
}
