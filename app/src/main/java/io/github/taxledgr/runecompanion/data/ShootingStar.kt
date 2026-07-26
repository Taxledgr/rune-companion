package io.github.taxledgr.runecompanion.data

import java.time.Instant

data class ShootingStar(
    val world: Int,
    val locationId: Int,
    val calledBy: String,
    val locationName: String,
    val calledAt: Instant,
    val minimumArrival: Instant?,
    val maximumArrival: Instant?,
    val tier: Int,
)

data class StarFeed(
    val stars: List<ShootingStar>,
    val fetchedAt: Instant,
    val sourceReportCount: Int = stars.size,
    val excludedReportCount: Int = 0,
)
