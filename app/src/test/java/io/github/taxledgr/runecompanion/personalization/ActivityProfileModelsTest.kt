package io.github.taxledgr.runecompanion.personalization

import io.github.taxledgr.runecompanion.alerts.StarFilterSettings
import io.github.taxledgr.runecompanion.overlay.OverlayModule
import io.github.taxledgr.runecompanion.overlay.OverlaySettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityProfileModelsTest {
    private fun initialState() = ActivityProfileState.fromLegacy(
        personalization = ExperiencePreset.SHOOTING_STARS.settings,
        overlay = OverlaySettings(enabledModules = setOf(OverlayModule.STARS)),
        selectedAccount = "Star Miner",
        filters = StarFilterSettings(
            hideDangerousWorlds = true,
            excludedLocations = setOf("Wilderness"),
        ),
    )

    @Test
    fun legacySetupBecomesActiveProfileWithoutLosingChoices() {
        val state = initialState()

        assertEquals(ActivityProfileTemplate.SHOOTING_STARS.id, state.activeProfileId)
        assertEquals("Star Miner", state.activeProfile.selectedAccount)
        assertEquals(setOf(OverlayModule.STARS), state.activeProfile.overlay.enabledModules)
        assertEquals(setOf("Wilderness"), state.activeProfile.starFilters.excludedLocations)
        assertEquals(ActivityProfileTemplate.entries.size, state.profiles.size)
    }

    @Test
    fun switchingProfilesKeepsTheirLayoutsIndependent() {
        val stars = initialState().updateActive {
            it.copy(overlay = it.overlay.copy(opacityPercent = 70))
        }
        val slayer = stars.select(ActivityProfileTemplate.SLAYER.id).updateActive {
            it.copy(overlay = it.overlay.copy(compactWidthDp = 360))
        }
        val switchedBack = slayer.select(ActivityProfileTemplate.SHOOTING_STARS.id)

        assertEquals(70, switchedBack.activeProfile.overlay.opacityPercent)
        assertEquals(
            360,
            slayer.profiles.first { it.id == ActivityProfileTemplate.SLAYER.id }
                .overlay.compactWidthDp,
        )
    }

    @Test
    fun customProfileCopiesCurrentSetupAndCanBeRenamed() {
        val original = initialState().select(ActivityProfileTemplate.SLAYER.id)
        val copied = original.addCopy("custom-id", "Burst tasks")
        val renamed = copied.renameActive("Boss tasks")

        assertEquals("custom-id", copied.activeProfileId)
        assertEquals(original.activeProfile.overlay, copied.activeProfile.overlay)
        assertEquals("Boss tasks", renamed.activeProfile.name)
    }

    @Test
    fun deletingActiveProfileSelectsASafeReplacement() {
        val state = initialState().select(ActivityProfileTemplate.QUESTING.id)
        val deleted = state.deleteActive()

        assertFalse(deleted.profiles.any { it.id == ActivityProfileTemplate.QUESTING.id })
        assertTrue(deleted.profiles.any { it.id == deleted.activeProfileId })
    }

    @Test
    fun allTemplatesHaveUsefulDistinctOverlaySetups() {
        ActivityProfileTemplate.entries.forEach { template ->
            val profile = template.profile()
            assertTrue(profile.overlay.enabledModules.isNotEmpty())
            assertTrue(profile.overlay.selectedModule in profile.overlay.enabledModules)
            assertTrue(profile.personalization.navigationTabs.size >= 3)
        }
        assertEquals(
            ActivityProfileTemplate.entries.size,
            ActivityProfileTemplate.entries.map(ActivityProfileTemplate::id).distinct().size,
        )
    }
}
