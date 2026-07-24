package io.github.taxledgr.runecompanion.alerts

import io.github.taxledgr.runecompanion.data.ShootingStar
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StarAlertSettingsTest {
    private val star = ShootingStar(
        world = 330,
        locationId = 12,
        calledBy = "Scout",
        locationName = "Falador",
        calledAt = Instant.EPOCH,
        minimumArrival = null,
        maximumArrival = null,
        tier = 7,
    )

    @Test
    fun `empty filters match every star`() {
        assertTrue(StarAlertSettings().matches(star))
    }

    @Test
    fun `world and tier filters must both match`() {
        assertTrue(StarAlertSettings(worlds = setOf(330), tiers = setOf(7)).matches(star))
        assertFalse(StarAlertSettings(worlds = setOf(301), tiers = setOf(7)).matches(star))
        assertFalse(StarAlertSettings(worlds = setOf(330), tiers = setOf(9)).matches(star))
    }

    @Test
    fun `location filters are case insensitive partial matches`() {
        assertTrue(StarAlertSettings(locations = setOf("fala")).matches(star))
        assertFalse(StarAlertSettings(locations = setOf("Wilderness")).matches(star))
    }

    @Test
    fun `quiet hours support overnight windows`() {
        val settings = StarAlertSettings(
            quietHoursEnabled = true,
            quietStartHour = 22,
            quietEndHour = 7,
        )
        assertTrue(settings.isQuietAt(23))
        assertTrue(settings.isQuietAt(6))
        assertFalse(settings.isQuietAt(12))
    }

    @Test
    fun `world parser accepts commas and spaces and ignores invalid values`() {
        assertEquals(
            setOf(301, 330, 444),
            parseWorlds("301, 330 444, nope, 12"),
        )
    }
}
