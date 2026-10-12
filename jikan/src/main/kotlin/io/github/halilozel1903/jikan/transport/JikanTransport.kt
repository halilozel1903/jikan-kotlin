package io.github.halilozel1903.jikan.transport

/** A raw HTTP answer: the status code, the body and the `Retry-After` header in seconds, when sent. */
public data class JikanHttpResponse(
    val status: Int,
    val body: String,
    val retryAfterSeconds: Long? = null,
)

/**
 * Fetches a Jikan url. The client uses a Ktor based transport by default; supply your own through
 * `JikanConfig.transport` to serve responses from files ([FixtureTransport]) or another HTTP stack.
 *
 * Return non-2xx answers as they are (the client handles 404, 429 and retries); throw only when no answer arrived.
 */
public fun interface JikanTransport {
    public suspend fun get(url: String): JikanHttpResponse
}
