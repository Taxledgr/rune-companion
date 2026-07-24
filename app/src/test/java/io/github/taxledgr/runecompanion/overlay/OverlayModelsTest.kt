package io.github.taxledgr.runecompanion.overlay

import io.github.taxledgr.runecompanion.features.FeatureData
import io.github.taxledgr.runecompanion.toolkit.CompanionReminder
import io.github.taxledgr.runecompanion.toolkit.PersistedToolkitData
import io.github.taxledgr.runecompanion.toolkit.ReminderCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayModelsTest {
    @Test
    fun everyModuleBuildsFromEmptySavedData() {
        OverlayModule.entries.forEach { module ->
            val content = OverlayContentBuilder.build(
                module = module,
                toolkit = PersistedToolkitData(),
                features = FeatureData(),
                stars = emptyList(),
                nowEpochMillis = 1_000,
            )

            assertTrue("$module should have a summary", content.summary.isNotBlank())
            assertTrue("$module should have an empty-state message", content.emptyMessage.isNotBlank())
            assertTrue("$module badge should never be negative", content.badgeCount >= 0)
        }
    }

    @Test
    fun overlayAlwaysKeepsAtLeastOneEnabledModule() {
        val starsOnly = OverlaySettings().toggled(OverlayModule.STARS)

        assertEquals(setOf(OverlayModule.STARS), starsOnly.enabledModules)
        assertEquals(OverlayModule.STARS, starsOnly.selectedModule)
    }

    @Test
    fun disablingSelectedModuleMovesSelectionToAnEnabledModule() {
        val settings = OverlaySettings(
            enabledModules = setOf(OverlayModule.STARS, OverlayModule.TIMERS),
            selectedModule = OverlayModule.STARS,
        ).toggled(OverlayModule.STARS)

        assertEquals(setOf(OverlayModule.TIMERS), settings.enabledModules)
        assertEquals(OverlayModule.TIMERS, settings.selectedModule)
        assertFalse(OverlayModule.STARS in settings.enabledModules)
    }

    @Test
    fun readyTimersAreHighlightedAndCounted() {
        val content = OverlayContentBuilder.build(
            module = OverlayModule.TIMERS,
            toolkit = PersistedToolkitData(
                reminders = listOf(
                    CompanionReminder(
                        id = "ready",
                        title = "Herb run",
                        category = ReminderCategory.FARMING,
                        endsAtEpochMillis = 999,
                    ),
                    CompanionReminder(
                        id = "waiting",
                        title = "Birdhouse",
                        category = ReminderCategory.BIRDHOUSE,
                        endsAtEpochMillis = 61_000,
                    ),
                ),
            ),
            features = FeatureData(),
            stars = emptyList(),
            nowEpochMillis = 1_000,
        )

        assertEquals(2, content.badgeCount)
        assertEquals(OverlayTone.GOOD, content.entries.first().tone)
        assertTrue(content.summary.startsWith("1 ready"))
    }
}
