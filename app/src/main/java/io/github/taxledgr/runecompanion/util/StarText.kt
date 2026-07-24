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

fun starTimingSummary(
    calledAt: Instant,
    tier: Int,
    minimumArrival: Instant?,
    maximumArrival: Instant?,
    now: Instant = Instant.now(),
): String? {
    if (tier in 1..9) {
        val estimatedEnd = calledAt.plusSeconds(tier * 7L * 60L)
        val remainingSeconds = Duration.between(now, estimatedEnd).seconds
        return if (remainingSeconds >= 0) {
            val remainingMinutes = (remainingSeconds + 59) / 60
            "Estimate: up to $remainingMinutes min left"
        } else {
            "Estimate: depletion window passed"
        }
    }

    if (minimumArrival == null && maximumArrival == null) return null
    val minimumMinutes = minimumArrival?.let {
        Duration.between(now, it).toMinutes().coerceAtLeast(0)
    }
    val maximumMinutes = maximumArrival?.let {
        Duration.between(now, it).toMinutes().coerceAtLeast(0)
    }
    return when {
        maximumArrival != null && now.isAfter(maximumArrival) -> "Arrival window passed"
        minimumArrival != null && now.isBefore(minimumArrival) && maximumMinutes != null ->
            "Arrival in ${minimumMinutes ?: 0}–$maximumMinutes min"
        maximumMinutes != null -> "Landing window now • within $maximumMinutes min"
        minimumMinutes != null -> "Expected in about $minimumMinutes min"
        else -> null
    }
}
