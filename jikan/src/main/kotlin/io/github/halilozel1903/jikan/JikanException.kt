package io.github.halilozel1903.jikan

/** Every failure of [JikanClient]. Catch the subclasses you care about, or all of them. */
public sealed class JikanException(
    /** The requested url. */
    public val url: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** Jikan answered 404: there is no entry with that id (or MyAnimeList has none). */
    public class NotFound(url: String, public val apiMessage: String? = null) :
        JikanException(url, "Not found: $url" + (apiMessage?.let { " ($it)" } ?: ""))

    /** Jikan still answered 429 after every retry. Slow down; see [JikanRateLimiter]. */
    public class RateLimited(url: String, public val retryAfterSeconds: Long? = null) :
        JikanException(url, "Rate limited by Jikan: $url")

    /** Jikan answered with another unsuccessful [status] (400 for bad parameters, 5xx when MyAnimeList is down). */
    public class Http(url: String, public val status: Int, public val apiMessage: String? = null) :
        JikanException(url, "HTTP $status for $url" + (apiMessage?.let { ": $it" } ?: ""))

    /** The request failed before a response arrived (offline, timeout, DNS). */
    public class Network(url: String, cause: Throwable) :
        JikanException(url, "Request failed for $url: ${cause.message ?: cause::class.simpleName}", cause)

    /** The response was not the JSON this library expects. */
    public class Decoding(url: String, cause: Throwable) :
        JikanException(url, "Could not decode the response of $url: ${cause.message}", cause)
}
