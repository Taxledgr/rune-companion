package io.github.taxledgr.runecompanion.features

import io.github.taxledgr.runecompanion.toolkit.HiscoreCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionCatalogTest {
    @Test
    fun slayerTaskPickerCoversCurrentRuneLiteTaskNames() {
        assertEquals(151, CompanionCatalog.slayerTaskNames.size)
        assertTrue("Araxytes" in CompanionCatalog.slayerTaskNames)
        assertTrue("The Shellbane Gryphon" in CompanionCatalog.slayerTaskNames)
        assertTrue(CompanionCatalog.slayer.size >= 25)
    }

    @Test
    fun automaticActivityAndCollectionSourcesUseOfficialHiscoreNames() {
        val officialNames = HiscoreCatalog.activityNames.toSet()

        assertTrue(CompanionCatalog.activities.all { it.name in officialNames })
        assertTrue(
            CompanionCatalog.collections
                .mapNotNull { it.sourceActivity }
                .all { it in officialNames },
        )
        assertTrue(CompanionCatalog.collections.all { it.denominator > 0 })
    }

    @Test
    fun selectablePresetCataloguesProvideUsefulCoverage() {
        assertTrue(CompanionCatalog.farmPatches.size >= 25)
        assertTrue(CompanionCatalog.crops.size >= 30)
        assertTrue(CompanionCatalog.bankedXp.size >= 15)
        assertTrue(CompanionCatalog.routines.size >= 10)
        assertTrue(CompanionCatalog.loadouts.size >= 8)
    }
}
