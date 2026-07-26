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
    fun overlayAppearanceAndPlacementAreClampedSafely() {
        val settings = OverlaySettings(
            compactWidthDp = 900,
            opacityPercent = 5,
            portraitPlacement = OverlayPlacement(
                panelXFraction = -1f,
                bubbleYFraction = 2f,
            ),
        ).normalized()

        assertEquals(OverlaySettings.MAX_COMPACT_WIDTH_DP, settings.compactWidthDp)
        assertEquals(OverlaySettings.MIN_OPACITY_PERCENT, settings.opacityPercent)
        assertEquals(0f, settings.portraitPlacement.panelXFraction)
        assertEquals(1f, settings.portraitPlacement.bubbleYFraction)
    }

    @Test
    fun displayControlsAreClampedWithoutChangingOrientationPlacements() {
        val settings = OverlaySettings(
            compactWidthDp = 100,
            landscapeWidthDp = 900,
            textScalePercent = 200,
            portraitPlacement = OverlayPlacement(panelXFraction = 0.2f),
            landscapePlacement = OverlayPlacement(panelXFraction = 0.8f),
        ).normalized()

        assertEquals(OverlaySettings.MIN_COMPACT_WIDTH_DP, settings.compactWidthDp)
        assertEquals(OverlaySettings.MAX_COMPACT_WIDTH_DP, settings.landscapeWidthDp)
        assertEquals(OverlaySettings.MAX_TEXT_SCALE_PERCENT, settings.textScalePercent)
        assertEquals(0.2f, settings.portraitPlacement.panelXFraction)
        assertEquals(0.8f, settings.landscapePlacement.panelXFraction)
    }

    @Test
    fun enabledOverlaySectionsCanBeReordered() {
        val settings = OverlaySettings(
            enabledModules = setOf(
                OverlayModule.STARS,
                OverlayModule.TIMERS,
                OverlayModule.SLAYER,
            ),
        ).moveModule(OverlayModule.SLAYER, -1)

        assertEquals(
            listOf(
                OverlayModule.STARS,
                OverlayModule.SLAYER,
                OverlayModule.TIMERS,
            ),
            settings.orderedModules,
        )
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

    @Test
    fun questOverlayRowsExplainThatTheyOpenTheGuide() {
        val content = OverlayContentBuilder.build(
            module = OverlayModule.QUESTS_DIARIES,
            toolkit = PersistedToolkitData(),
            features = FeatureData(),
            stars = emptyList(),
        )

        assertEquals("Waterfall Quest", content.entries.first().title)
        assertTrue(content.entries.first().detail.startsWith("Tap for step-by-step guide"))
    }
}
