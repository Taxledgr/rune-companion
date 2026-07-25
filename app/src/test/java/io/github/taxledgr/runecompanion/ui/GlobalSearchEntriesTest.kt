package io.github.taxledgr.runecompanion.ui

import io.github.taxledgr.runecompanion.features.CustomTeleport
import io.github.taxledgr.runecompanion.features.FeatureData
import io.github.taxledgr.runecompanion.features.SlayerCard
import io.github.taxledgr.runecompanion.personalization.AppTab
import io.github.taxledgr.runecompanion.toolkit.CompanionReminder
import io.github.taxledgr.runecompanion.toolkit.ReminderCategory
import io.github.taxledgr.runecompanion.toolkit.ToolkitState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalSearchEntriesTest {
    @Test
    fun searchCatalogIncludesSettingsToolsAndSavedGameplayData() {
        val entries = globalSearchEntries(
            data = FeatureData(
                customTeleports = listOf(
                    CustomTeleport("1", "My POH", "Rellekka", "Fremennik", false),
                ),
                slayerCards = listOf(
                    SlayerCard("2", "Abyssal demons", "Slash", "Catacombs", "Nose peg", ""),
                ),
            ),
            toolkit = ToolkitState(
                reminders = listOf(
                    CompanionReminder("3", "Herb run", ReminderCategory.FARMING, 123L),
                ),
            ),
        )

        assertTrue(entries.any { it.featureId == NAVIGATION_CUSTOMIZE })
        assertTrue(entries.any { it.featureId == CompanionFeature.DIAGNOSTICS.name })
        assertTrue(entries.any { it.title == "My POH" && it.featureId == "TELEPORTS" })
        assertTrue(entries.any { it.title == "Abyssal demons" && it.featureId == "SLAYER" })
        assertTrue(entries.any { it.title == "Herb run" && it.tab == AppTab.TIMERS })
        assertEquals(entries.size, entries.distinctBy { "${it.title}:${it.featureId}" }.size)
    }
}
