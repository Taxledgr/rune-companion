package io.github.taxledgr.runecompanion.data

import java.io.IOException
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object StarRepository {
    private const val MINIMUM_FETCH_INTERVAL_MS = 60_000L

    private val mutex = Mutex()
    private val client = StarFeedClient()
    private var cachedFeed: StarFeed? = null
    private var lastAttemptAt: Instant? = null
    private var lastFailure: Throwable? = null

    suspend fun latest(): StarFeed = mutex.withLock {
        val now = Instant.now()
        val insideCooldown = lastAttemptAt?.let {
            Duration.between(it, now).toMillis() < MINIMUM_FETCH_INTERVAL_MS
        } == true

        if (insideCooldown) {
            cachedFeed?.let { return@withLock it }
            lastFailure?.let {
                throw IOException("Star Miners feed is cooling down after an error", it)
            }
        }

        lastAttemptAt = now
        runCatching { client.fetch() }
            .onSuccess { feed ->
                cachedFeed = feed
                lastFailure = null
            }
            .onFailure { throwable ->
                lastFailure = throwable
            }
            .getOrElse { throwable ->
                cachedFeed ?: throw throwable
            }
    }
}
