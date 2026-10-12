package io.github.halilozel1903.jikan

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** At most [permits] requests in any window of [per]. */
public data class RateLimit(val permits: Int, val per: Duration) {
    init {
        require(permits > 0) { "permits must be positive, was $permits" }
        require(per.isPositive() && per.isFinite()) { "per must be a positive, finite duration, was $per" }
    }
}

/**
 * A sliding window rate limiter for coroutines. [acquire] suspends until a request fits in every [limits] window.
 *
 * Jikan allows 3 requests per second and 60 per minute per IP address ([JIKAN_LIMITS]), so every [JikanClient]
 * uses [shared] by default. After a 429 response the client calls [pause], which holds back every request
 * that goes through this limiter.
 *
 * @param clock milliseconds from a monotonic clock; tests pass a virtual clock.
 */
public class JikanRateLimiter(
    public val limits: List<RateLimit> = JIKAN_LIMITS,
    private val clock: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    init {
        require(limits.isNotEmpty()) { "At least one limit is needed" }
    }

    private val mutex = Mutex()
    private val granted = ArrayDeque<Long>()
    private val longestWindow = limits.maxOf { it.per.inWholeMilliseconds }
    private var pausedUntil = Long.MIN_VALUE

    /** Suspends until one more request is allowed, and counts it. */
    public suspend fun acquire() {
        while (true) {
            val wait = mutex.withLock {
                val now = clock()
                val wait = waitMillis(now)
                if (wait <= 0) granted.addLast(now)
                wait
            }
            if (wait <= 0) return
            delay(wait)
        }
    }

    /** Milliseconds until the next request would be allowed; 0 when it is allowed now. Does not count a request. */
    public suspend fun nextDelayMillis(): Long = mutex.withLock { waitMillis(clock()).coerceAtLeast(0) }

    /** Holds back every request for [millis], for example after a 429 response. */
    public suspend fun pause(millis: Long) {
        if (millis <= 0) return
        mutex.withLock { pausedUntil = maxOf(pausedUntil, clock() + millis) }
    }

    /** Forgets every counted request and pause. */
    public suspend fun reset() {
        mutex.withLock {
            granted.clear()
            pausedUntil = Long.MIN_VALUE
        }
    }

    private fun waitMillis(now: Long): Long {
        while (granted.isNotEmpty() && now - granted.first() >= longestWindow) granted.removeFirst()
        var wait = if (pausedUntil > now) pausedUntil - now else 0L
        for (limit in limits) {
            val window = limit.per.inWholeMilliseconds
            val inWindow = granted.count { now - it < window }
            if (inWindow >= limit.permits) {
                // The request that has to leave the window before another one fits.
                val blocker = granted[granted.size - limit.permits]
                wait = maxOf(wait, blocker + window - now)
            }
        }
        return wait
    }

    public companion object {
        /** Jikan's public limits: 3 requests per second and 60 per minute. */
        public val JIKAN_LIMITS: List<RateLimit> = listOf(RateLimit(3, 1.seconds), RateLimit(60, 1.minutes))

        /** The limiter every client uses unless configured otherwise, so all clients in a process share the budget. */
        public val shared: JikanRateLimiter = JikanRateLimiter()
    }
}
