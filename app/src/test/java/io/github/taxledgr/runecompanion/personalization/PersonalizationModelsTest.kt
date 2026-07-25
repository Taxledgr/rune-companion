package io.github.taxledgr.runecompanion.personalization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalizationModelsTest {
    @Test
    fun existingUsersKeepStarsAsTheDefaultFocus() {
        val settings = PersonalizationSettings()

        assertEquals(AppTab.STARS, settings.effectiveStartTab)
        assertEquals(AppTab.entries, settings.navigationTabs)
        assertTrue("TELEPORTS" in settings.pinnedFeatureIds)
        assertEquals(CustomizationMode.BASIC, settings.customizationMode)
        assertEquals(LayoutDensity.COMFORTABLE, settings.appDensity)
        assertEquals(LayoutDensity.COMFORTABLE, settings.overlayDensity)
    }

    @Test
    fun densityAndCustomizationModeRemainIndependentAcrossEdits() {
        val settings = PersonalizationSettings(
            customizationMode = CustomizationMode.ADVANCED,
            appDensity = LayoutDensity.COMPACT,
            overlayDensity = LayoutDensity.LARGE,
        )
            .withStartTab(AppTab.JOURNAL)
            .withPinnedFeature("SLAYER", true)

        assertEquals(CustomizationMode.ADVANCED, settings.customizationMode)
        assertEquals(LayoutDensity.COMPACT, settings.appDensity)
        assertEquals(LayoutDensity.LARGE, settings.overlayDensity)
        assertEquals(AppTab.JOURNAL, settings.startTab)
        assertTrue("SLAYER" in settings.pinnedFeatureIds)
    }

    @Test
    fun normalizationRepairsNavigationAndInvalidFeatureStart() {
        val normalized = PersonalizationSettings(
            startTab = AppTab.WIKI,
            startFeatureId = "NOT_PINNED",
            navigationTabs = listOf(AppTab.TIMERS, AppTab.TIMERS),
            pinnedFeatureIds = listOf("GOALS", "GOALS", ""),
        ).normalized()

        assertEquals(PersonalizationSettings.MINIMUM_NAVIGATION_TABS, normalized.navigationTabs.size)
        assertTrue(AppTab.MORE in normalized.navigationTabs)
        assertEquals(normalized.navigationTabs.first(), normalized.startTab)
        assertNull(normalized.startFeatureId)
        assertEquals(listOf("GOALS"), normalized.pinnedFeatureIds)
    }

    @Test
    fun hidingTheStartTabSelectsAnotherVisibleTab() {
        val updated = PersonalizationSettings()
            .withStartTab(AppTab.TIMERS)
            .withTabVisibility(AppTab.TIMERS, false)

        assertFalse(AppTab.TIMERS in updated.navigationTabs)
        assertTrue(updated.startTab in updated.navigationTabs)
        assertNull(updated.startFeatureId)
    }

    @Test
    fun featureStartMustRemainPinned() {
        val started = PersonalizationSettings()
            .withPinnedFeature("PROGRESS_NAVIGATOR", true)
            .withStartFeature("PROGRESS_NAVIGATOR")
        val unpinned = started.withPinnedFeature("PROGRESS_NAVIGATOR", false)

        assertEquals(AppTab.MORE, started.effectiveStartTab)
        assertEquals("PROGRESS_NAVIGATOR", started.startFeatureId)
        assertNull(unpinned.startFeatureId)
    }

    @Test
    fun pinLimitAndFocusPresetsStaySafe() {
        val tooManyPins = (1..10).fold(
            PersonalizationSettings(pinnedFeatureIds = emptyList()),
        ) { settings, index ->
            settings.withPinnedFeature("FEATURE_$index", true)
        }

        assertEquals(PersonalizationSettings.MAX_PINNED_FEATURES, tooManyPins.pinnedFeatureIds.size)
        ExperiencePreset.entries.forEach { preset ->
            val settings = preset.settings.normalized()
            assertTrue(AppTab.MORE in settings.navigationTabs)
            assertTrue(settings.navigationTabs.size >= PersonalizationSettings.MINIMUM_NAVIGATION_TABS)
            assertTrue(settings.pinnedFeatureIds.size <= PersonalizationSettings.MAX_PINNED_FEATURES)
            assertTrue(
                settings.startFeatureId == null ||
                    settings.startFeatureId in settings.pinnedFeatureIds,
            )
        }
    }
}
