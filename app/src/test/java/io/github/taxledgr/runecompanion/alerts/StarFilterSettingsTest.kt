package io.github.taxledgr.runecompanion.alerts

import io.github.taxledgr.runecompanion.data.ShootingStar
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StarFilterSettingsTest {
    private val star = ShootingStar(
        world = 318,
        locationId = 1,
        calledBy = "Scout",
        locationName = "Rimmington mine",
        calledAt = Instant.EPOCH,
        minimumArrival = null,
        maximumArrival = null,
        tier = 7,
    )

    @Test
    fun `dangerous worlds are hidden when safety filter is enabled`() {
        assertFalse(
            StarFilterSettings(
                hideDangerousWorlds = true,
                dangerousWorlds = setOf(318),
                worldSafetyLoaded = true,
            ).includes(star),
        )
        assertTrue(
            StarFilterSettings(
                hideDangerousWorlds = false,
                dangerousWorlds = setOf(318),
            ).includes(star),
        )
    }

    @Test
    fun `deselected landing sites are excluded case insensitively`() {
        assertFalse(
            StarFilterSettings(
                excludedLocations = setOf("RIMMINGTON MINE"),
            ).includes(star),
        )
    }

    @Test
    fun `deselected site also excludes Star Miners naming variants`() {
        val variant = star.copy(locationName = "Rimmington mine (east side)")
        assertFalse(
            StarFilterSettings(
                excludedLocations = setOf("Rimmington mine"),
            ).includes(variant),
        )
    }
}
