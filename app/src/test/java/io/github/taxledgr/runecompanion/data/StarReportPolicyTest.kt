package io.github.taxledgr.runecompanion.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class StarReportPolicyTest {
    private val now = Instant.parse("2026-01-01T12:00:00Z")

    @Test
    fun `hides expired estimates and orders useful travel targets first`() {
        val reports = listOf(
            star(world = 301, tier = 1, ageMinutes = 8, location = "Expired"),
            star(world = 302, tier = 2, ageMinutes = 4, location = "Eleven minutes"),
            star(world = 303, tier = 7, ageMinutes = 10, location = "Thirty-nine minutes"),
        )

        val current = StarReportPolicy.currentReports(reports, now)

        assertEquals(listOf(303, 302), current.map(ShootingStar::world))
    }

    @Test
    fun `newest scout supersedes older location in the same world`() {
        val reports = listOf(
            star(world = 301, tier = 5, ageMinutes = 20, location = "Old location"),
            star(world = 301, tier = 4, ageMinutes = 2, location = "Corrected location"),
        )

        val current = StarReportPolicy.currentReports(reports, now)

        assertEquals(listOf("Corrected location"), current.map(ShootingStar::locationName))
    }

    @Test
    fun `stable key survives tier and timestamp updates`() {
        val first = star(world = 330, tier = 8, ageMinutes = 3, location = "Rimmington mine")
        val rescout = star(world = 330, tier = 6, ageMinutes = 0, location = "Rimmington mine")

        assertEquals(first.stableReportKey(), rescout.stableReportKey())
    }

    private fun star(
        world: Int,
        tier: Int,
        ageMinutes: Long,
        location: String,
    ) = ShootingStar(
        world = world,
        locationId = 0,
        calledBy = "Scout",
        locationName = location,
        calledAt = now.minusSeconds(ageMinutes * 60),
        minimumArrival = null,
        maximumArrival = null,
        tier = tier,
    )
}
