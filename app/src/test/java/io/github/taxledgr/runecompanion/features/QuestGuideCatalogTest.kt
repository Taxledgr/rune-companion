package io.github.taxledgr.runecompanion.features

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestGuideCatalogTest {
    @Test
    fun everyTrackedQuestHasACompleteCompanionGuide() {
        val quests = ExpansionCatalog.progress.filter { it.category == "Quest" }

        assertEquals(quests.map { it.id }.toSet(), QuestGuideCatalog.guides.keys)
        QuestGuideCatalog.guides.values.forEach { guide ->
            assertTrue(guide.inventory.isNotEmpty())
            assertTrue(guide.equipment.isNotEmpty())
            assertTrue(guide.teleports.isNotEmpty())
            assertTrue(guide.steps.size >= 7)
            assertEquals(guide.steps.size, guide.steps.map { it.id }.distinct().size)
        }
    }

    @Test
    fun savedChecklistIdsStayScopedToTheirQuest() {
        assertEquals(
            "waterfall:inventory:2",
            QuestGuideCatalog.checklistId("waterfall", "inventory", 2),
        )
        assertEquals(
            "waterfall:step:tomb",
            QuestGuideCatalog.stepId("waterfall", "tomb"),
        )
    }
}
