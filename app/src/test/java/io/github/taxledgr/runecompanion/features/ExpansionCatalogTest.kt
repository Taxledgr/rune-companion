package io.github.taxledgr.runecompanion.features

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpansionCatalogTest {
    @Test
    fun expansionCataloguesHaveStableUniqueEntries() {
        assertTrue(ExpansionCatalog.bosses.size >= 12)
        assertEquals(
            ExpansionCatalog.bosses.size,
            ExpansionCatalog.bosses.map(BossPreset::id).distinct().size,
        )
        assertTrue(ExpansionCatalog.progress.size >= 20)
        assertEquals(
            ExpansionCatalog.progress.size,
            ExpansionCatalog.progress.map(ProgressPreset::id).distinct().size,
        )
        assertTrue(ExpansionCatalog.monsters.size >= 25)
        assertTrue(ExpansionCatalog.itinerary.size >= 12)
    }

    @Test
    fun catalogueReferencesAreSuitableForInAppWikiLookup() {
        assertTrue(ExpansionCatalog.bosses.all { it.wikiTitle.isNotBlank() })
        assertTrue(ExpansionCatalog.progress.all { it.wikiTitle.isNotBlank() })
        assertTrue(ExpansionCatalog.monsters.all { it.wikiTitle.isNotBlank() })
        assertTrue(
            ExpansionCatalog.bosses
                .flatMap(BossPreset::skills)
                .all { it.skill in CompanionCatalog.skills && it.level in 1..99 },
        )
    }

    @Test
    fun itineraryRegionGuessKeepsDangerousTravelSeparate() {
        assertEquals("Wilderness", ExpansionCatalog.guessRegion("Wilderness altar"))
        assertEquals("Fossil Island", ExpansionCatalog.guessRegion("Birdhouse run"))
        assertEquals(
            "Review risk before travelling",
            ExpansionCatalog.suggestedTeleport("Wilderness"),
        )
    }
}
