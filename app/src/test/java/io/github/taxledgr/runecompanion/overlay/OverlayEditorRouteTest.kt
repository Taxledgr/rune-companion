package io.github.taxledgr.runecompanion.overlay

import io.github.taxledgr.runecompanion.personalization.AppTab
import io.github.taxledgr.runecompanion.ui.CompanionFeature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayEditorRouteTest {
    @Test
    fun everyOverlayModuleHasAnEditorRoute() {
        assertEquals(OverlayModule.entries.size, OverlayModule.entries.map { it.editorRoute() }.size)
        val validFeatureIds = CompanionFeature.entries.map { it.name }.toSet()
        OverlayModule.entries
            .mapNotNull { it.editorRoute().featureId }
            .forEach { featureId ->
                assertTrue(featureId in validFeatureIds)
            }
    }

    @Test
    fun compactModulesOpenTheirExistingMainEditors() {
        assertRoute(OverlayModule.STARS, AppTab.STARS)
        assertRoute(OverlayModule.TIMERS, AppTab.TIMERS)
        assertRoute(OverlayModule.TRIP, AppTab.TIMERS)
        assertRoute(OverlayModule.SLAYER, AppTab.JOURNAL)
        assertRoute(OverlayModule.CHECKLIST, AppTab.JOURNAL)
        assertRoute(OverlayModule.GE_WATCHLIST, AppTab.TOOLS)
    }

    @Test
    fun featureModulesOpenTheMatchingFeatureEditor() {
        assertRoute(OverlayModule.PLAYER, AppTab.MORE, "ACCOUNTS")
        assertRoute(OverlayModule.TELEPORTS, AppTab.MORE, "TELEPORTS")
        assertRoute(OverlayModule.SLAYER_CARDS, AppTab.MORE, "SLAYER")
        assertRoute(OverlayModule.SUPPLIES, AppTab.MORE, "SUPPLY_LOCKER")
        assertRoute(OverlayModule.QUESTS_DIARIES, AppTab.MORE, "PROGRESS_NAVIGATOR")
        assertRoute(OverlayModule.SESSIONS, AppTab.MORE, "SESSIONS")
    }

    private fun assertRoute(
        module: OverlayModule,
        expectedTab: AppTab,
        expectedFeatureId: String? = null,
    ) {
        val route = module.editorRoute()
        assertEquals(expectedTab, route.tab)
        if (expectedFeatureId == null) {
            assertNull(route.featureId)
        } else {
            assertEquals(expectedFeatureId, route.featureId)
        }
    }
}
