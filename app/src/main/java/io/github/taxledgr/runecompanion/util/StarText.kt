package io.github.taxledgr.runecompanion.util

import java.time.Duration
import java.time.Instant

fun reportAge(calledAt: Instant, now: Instant = Instant.now()): String {
    val minutes = Duration.between(calledAt, now).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "just now"
        minutes == 1L -> "1 min ago"
        minutes < 60 -> "$minutes mins ago"
        minutes < 120 -> "1 hr ago"
        else -> "${minutes / 60} hrs ago"
    }
}
