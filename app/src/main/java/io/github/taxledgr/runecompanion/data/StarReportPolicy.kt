package io.github.taxledgr.runecompanion.data

import java.time.Duration
import java.time.Instant
import java.util.Locale

object StarReportPolicy {
    private const val LAYER_DURATION_SECONDS = 7L * 60L

    fun estimatedDepletionAt(star: ShootingStar): Instant? =
        star.tier
            .takeIf { it in 1..9 }
            ?.let { tier -> star.calledAt.plusSeconds(tier * LAYER_DURATION_SECONDS) }

    fun estimatedRemainingSeconds(
        star: ShootingStar,
        now: Instant,
    ): Long? = estimatedDepletionAt(star)?.let { depletion ->
        Duration.between(now, depletion).seconds
    }

    fun currentReports(
        reports: List<ShootingStar>,
        now: Instant,
    ): List<ShootingStar> {
        val newestReportPerWorld = reports
            .sortedByDescending(ShootingStar::calledAt)
            .distinctBy(ShootingStar::world)

        return newestReportPerWorld
            .filter { star ->
                estimatedRemainingSeconds(star, now)?.let { it > 0 } ?: true
            }
            .sortedWith(
                compareByDescending<ShootingStar> { star ->
                    estimatedRemainingSeconds(star, now) ?: Long.MIN_VALUE
                }.thenByDescending(ShootingStar::calledAt),
            )
    }
}

fun ShootingStar.stableReportKey(): String =
    "$world|${locationName.trim().lowercase(Locale.ROOT)}"
