package io.github.halilozel1903.jikan.transport

import io.github.halilozel1903.jikan.JikanClient
import java.net.URLDecoder

/**
 * Serves Jikan responses from local JSON files, for tests, previews and offline demos. [read] gets a relative path
 * and returns the file's text, or `null` when there is no such file (answered as a 404). Urls map to paths like this:
 *
 * | Url | Path |
 * | --- | --- |
 * | `/anime/52991` | `anime/52991.json` |
 * | `/anime/52991/full` | `anime/52991/full.json` |
 * | `/seasons/now?limit=12` | `seasons/now/limit-12.json` |
 * | `/anime?q=star map&order_by=score` | `anime/order_by-score_q-star-map.json` |
 *
 * Query parameters are sorted by name, so their order in the url does not matter.
 *
 * ```kotlin
 * val client = JikanClient {
 *     transport = FixtureTransport { path -> javaClass.getResource("/fixtures/$path")?.readText() }
 *     rateLimiter = null
 * }
 * ```
 */
public class FixtureTransport(
    private val baseUrl: String = JikanClient.DEFAULT_BASE_URL,
    private val read: (path: String) -> String?,
) : JikanTransport {

    private val lock = Any()
    private val recorded = mutableListOf<String>()

    /** Every url requested so far, in order. */
    public val requests: List<String> get() = synchronized(lock) { recorded.toList() }

    override suspend fun get(url: String): JikanHttpResponse {
        synchronized(lock) { recorded += url }
        val body = pathFor(url, baseUrl)?.let(read)
            ?: return JikanHttpResponse(404, NOT_FOUND_BODY)
        return JikanHttpResponse(200, body)
    }

    public companion object {
        private const val NOT_FOUND_BODY: String =
            """{"status":404,"type":"BadResponseException","message":"Resource does not exist","error":null}"""

        /** The fixture path for [url], or `null` when the url is not a Jikan url. */
        public fun pathFor(url: String, baseUrl: String = JikanClient.DEFAULT_BASE_URL): String? {
            val base = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
            val relative = when {
                url.startsWith(base) -> url.removePrefix(base)
                "/v4/" in url -> url.substringAfter("/v4/")
                else -> return null
            }
            val path = relative.substringBefore('?').trim('/')
            if (path.isEmpty()) return null
            val query = relative.substringAfter('?', missingDelimiterValue = "")
            if (query.isEmpty()) return "$path.json"
            val parameters = query.split('&')
                .filter { it.isNotEmpty() }
                .map { part ->
                    val key = decode(part.substringBefore('='))
                    val value = decode(part.substringAfter('=', ""))
                    key to slug(value)
                }
                .sortedBy { it.first }
            if (parameters.isEmpty()) return "$path.json"
            return "$path/" + parameters.joinToString("_") { (key, value) -> "$key-$value" } + ".json"
        }

        private fun decode(value: String): String = URLDecoder.decode(value, Charsets.UTF_8)

        private fun slug(value: String): String =
            value.lowercase().map { if (it in 'a'..'z' || it in '0'..'9' || it == '.') it else '-' }.joinToString("")
    }
}
