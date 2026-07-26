package io.github.taxledgr.runecompanion.overlay

import io.github.taxledgr.runecompanion.personalization.AppTab

data class OverlayEditorRoute(
    val tab: AppTab,
    val featureId: String? = null,
)

fun OverlayModule.editorRoute(): OverlayEditorRoute = when (this) {
    OverlayModule.STARS -> OverlayEditorRoute(AppTab.STARS)
    OverlayModule.TIMERS,
    OverlayModule.TRIP,
    -> OverlayEditorRoute(AppTab.TIMERS)

    OverlayModule.SLAYER,
    OverlayModule.CHECKLIST,
    -> OverlayEditorRoute(AppTab.JOURNAL)

    OverlayModule.GE_WATCHLIST -> OverlayEditorRoute(AppTab.TOOLS)
    OverlayModule.PLAYER -> OverlayEditorRoute(AppTab.MORE, "ACCOUNTS")
    OverlayModule.SKILL_GOALS -> OverlayEditorRoute(AppTab.MORE, "GOALS")
    OverlayModule.BANKED_XP -> OverlayEditorRoute(AppTab.MORE, "BANKED_XP")
    OverlayModule.COUNTER_GOALS -> OverlayEditorRoute(AppTab.MORE, "COUNTER_GOALS")
    OverlayModule.COLLECTION_GOALS -> OverlayEditorRoute(AppTab.MORE, "COLLECTION")
    OverlayModule.COMBAT_ACHIEVEMENTS ->
        OverlayEditorRoute(AppTab.MORE, "COMBAT_ACHIEVEMENTS")
    OverlayModule.QUESTS_DIARIES ->
        OverlayEditorRoute(AppTab.MORE, "PROGRESS_NAVIGATOR")
    OverlayModule.FARMING -> OverlayEditorRoute(AppTab.MORE, "FARMING")
    OverlayModule.ROUTINES -> OverlayEditorRoute(AppTab.MORE, "ROUTINES")
    OverlayModule.ITINERARY -> OverlayEditorRoute(AppTab.MORE, "ITINERARY")
    OverlayModule.LOADOUTS -> OverlayEditorRoute(AppTab.MORE, "LOADOUTS")
    OverlayModule.BOSS_READINESS -> OverlayEditorRoute(AppTab.MORE, "BOSS_READINESS")
    OverlayModule.SLAYER_CARDS -> OverlayEditorRoute(AppTab.MORE, "SLAYER")
    OverlayModule.TELEPORTS -> OverlayEditorRoute(AppTab.MORE, "TELEPORTS")
    OverlayModule.GE_ALERTS -> OverlayEditorRoute(AppTab.MORE, "GE_ALERTS")
    OverlayModule.PORTFOLIO -> OverlayEditorRoute(AppTab.MORE, "PORTFOLIO")
    OverlayModule.GEAR_UPGRADES -> OverlayEditorRoute(AppTab.MORE, "GEAR_UPGRADES")
    OverlayModule.LOOT_LEDGER -> OverlayEditorRoute(AppTab.MORE, "LOOT_LEDGER")
    OverlayModule.SUPPLIES -> OverlayEditorRoute(AppTab.MORE, "SUPPLY_LOCKER")
    OverlayModule.WILDERNESS_RISK -> OverlayEditorRoute(AppTab.MORE, "WILDERNESS_RISK")
    OverlayModule.SESSIONS -> OverlayEditorRoute(AppTab.MORE, "SESSIONS")
}
