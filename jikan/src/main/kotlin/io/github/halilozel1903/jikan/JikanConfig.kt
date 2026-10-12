package io.github.halilozel1903.jikan

import io.github.halilozel1903.jikan.cache.JikanCache
import io.github.halilozel1903.jikan.cache.MemoryCache
import io.github.halilozel1903.jikan.transport.JikanTransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine

/** Settings for [JikanClient], set in its builder block. */
public class JikanConfig internal constructor() {
    /** The API root. Change it for a self-hosted Jikan instance. */
    public var baseUrl: String = JikanClient.DEFAULT_BASE_URL

    /**
     * The Ktor engine. `null` uses CIO. Pass `OkHttp.create()` (add `io.ktor:ktor-client-okhttp`) to share your
     * app's OkHttp stack, or a `MockEngine` in tests. The client does not close an engine you pass.
     */
    public var engine: HttpClientEngine? = null

    /** A ready Ktor client to use as is, instead of [engine]. [requestTimeoutMillis] then does not apply. */
    public var httpClient: HttpClient? = null

    /** Replaces HTTP entirely, for example with a [io.github.halilozel1903.jikan.transport.FixtureTransport]. */
    public var transport: JikanTransport? = null

    /**
     * Spaces out requests to stay inside Jikan's limits (3 per second, 60 per minute). The limit is per IP
     * address, so share one limiter between clients. `null` turns limiting off (self-hosted instances).
     */
    public var rateLimiter: JikanRateLimiter? = JikanRateLimiter.shared

    /** Where responses are cached. An in-memory LRU cache by default; `null` turns caching off. */
    public var cache: JikanCache? = MemoryCache()

    /** Sent with every request. */
    public var userAgent: String = "jikan-kotlin/1.0.0 (+https://github.com/halilozel1903/jikan-kotlin)"

    /** Timeout of one request, in milliseconds. */
    public var requestTimeoutMillis: Long = 30_000

    /** Retries on network errors and 5xx responses, with exponential backoff. */
    public var retries: Int = 2

    /** Retries on 429 (rate limited) responses. Each one waits for `Retry-After`, or backs off exponentially. */
    public var rateLimitRetries: Int = 3

    /** The first backoff delay in milliseconds; it doubles with each retry, up to [maxRetryDelayMillis]. */
    public var retryDelayMillis: Long = 1_000

    /** The longest backoff delay in milliseconds. */
    public var maxRetryDelayMillis: Long = 16_000
}
