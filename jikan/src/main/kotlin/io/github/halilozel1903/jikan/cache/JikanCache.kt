package io.github.halilozel1903.jikan.cache

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Stores response bodies by request url. Jikan itself caches MyAnimeList for 24 hours, so a client side cache
 * saves requests (and rate limit budget) without making data noticeably staler.
 *
 * Implement it to store responses elsewhere (a database, files).
 */
public interface JikanCache {
    /** The body stored for [key], or `null` when there is none or it expired. */
    public suspend fun get(key: String): String?

    /** Stores [value] for [key]. */
    public suspend fun put(key: String, value: String)

    /** Removes the entry for [key]. */
    public suspend fun remove(key: String)

    /** Removes every entry. */
    public suspend fun clear()
}

/** A thread safe, least recently used, in-memory [JikanCache] whose entries live for [ttl]. */
public class MemoryCache(
    public val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    public val ttl: Duration = DEFAULT_TTL,
    private val clock: () -> Long = System::currentTimeMillis,
) : JikanCache {

    init {
        require(maxEntries > 0) { "maxEntries must be positive, was $maxEntries" }
        require(ttl.isPositive()) { "ttl must be positive, was $ttl" }
    }

    private class Entry(val value: String, val storedAt: Long)

    private val lock = Any()
    private val entries = object : LinkedHashMap<String, Entry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean = size > maxEntries
    }

    /** The number of stored entries, expired ones included until they are read. */
    public val size: Int get() = synchronized(lock) { entries.size }

    override suspend fun get(key: String): String? = synchronized(lock) {
        val entry = entries[key] ?: return@synchronized null
        if (isExpired(entry.storedAt)) {
            entries.remove(key)
            null
        } else {
            entry.value
        }
    }

    override suspend fun put(key: String, value: String) {
        synchronized(lock) { entries[key] = Entry(value, clock()) }
    }

    override suspend fun remove(key: String) {
        synchronized(lock) { entries.remove(key) }
    }

    override suspend fun clear() {
        synchronized(lock) { entries.clear() }
    }

    private fun isExpired(storedAt: Long): Boolean =
        !ttl.isInfinite() && clock() - storedAt >= ttl.inWholeMilliseconds

    public companion object {
        /** Responses kept at most. */
        public const val DEFAULT_MAX_ENTRIES: Int = 256

        /** How long a response is reused: one hour. */
        public val DEFAULT_TTL: Duration = 1.hours
    }
}
