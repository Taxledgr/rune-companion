package io.github.taxledgr.runecompanion.personalization

enum class AppTab(val label: String, val symbol: String) {
    STARS("Stars", "✦"),
    TIMERS("Timers", "◷"),
    JOURNAL("Journal", "✓"),
    TOOLS("Tools", "⌁"),
    WIKI("Wiki", "W"),
    MORE("More", "☰"),
}

data class PersonalizationSettings(
    val startTab: AppTab = AppTab.STARS,
    val startFeatureId: String? = null,
    val navigationTabs: List<AppTab> = AppTab.entries,
    val pinnedFeatureIds: List<String> = listOf("TELEPORTS", "XP_CHARTS", "GOALS"),
) {
    val effectiveStartTab: AppTab
        get() = if (startFeatureId == null) startTab else AppTab.MORE

    fun normalized(): PersonalizationSettings {
        val pins = pinnedFeatureIds
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .take(MAX_PINNED_FEATURES)
        val feature = startFeatureId
            ?.takeIf { it in pins }
        var tabs = navigationTabs.distinct()
        if (AppTab.MORE !in tabs) tabs = tabs + AppTab.MORE
        if (tabs.size < MINIMUM_NAVIGATION_TABS) {
            tabs = (tabs + AppTab.entries).distinct().take(MINIMUM_NAVIGATION_TABS)
            if (AppTab.MORE !in tabs) {
                tabs = tabs.dropLast(1) + AppTab.MORE
            }
        }
        val resolvedStart = when {
            feature != null -> startTab
            startTab in tabs -> startTab
            else -> tabs.first()
        }
        return copy(
            startTab = resolvedStart,
            startFeatureId = feature,
            navigationTabs = tabs,
            pinnedFeatureIds = pins,
        )
    }

    fun withStartTab(tab: AppTab): PersonalizationSettings =
        copy(
            startTab = tab,
            startFeatureId = null,
            navigationTabs = if (tab in navigationTabs) {
                navigationTabs
            } else {
                listOf(tab) + navigationTabs
            },
        ).normalized()

    fun withStartFeature(featureId: String): PersonalizationSettings =
        copy(
            startFeatureId = featureId,
            pinnedFeatureIds = if (featureId in pinnedFeatureIds) {
                pinnedFeatureIds
            } else {
                (pinnedFeatureIds + featureId).take(MAX_PINNED_FEATURES)
            },
        ).normalized()

    fun withTabVisibility(tab: AppTab, visible: Boolean): PersonalizationSettings {
        if (tab == AppTab.MORE && !visible) return this
        if (!visible && navigationTabs.size <= MINIMUM_NAVIGATION_TABS) return this
        val updatedTabs = if (visible) {
            navigationTabs + tab
        } else {
            navigationTabs - tab
        }
        val updatedStart = if (
            !visible &&
            startFeatureId == null &&
            startTab == tab
        ) {
            updatedTabs.firstOrNull() ?: AppTab.MORE
        } else {
            startTab
        }
        return copy(
            startTab = updatedStart,
            navigationTabs = updatedTabs,
        ).normalized()
    }

    fun moveTab(tab: AppTab, delta: Int): PersonalizationSettings {
        val currentIndex = navigationTabs.indexOf(tab)
        if (currentIndex < 0) return this
        val destinationIndex =
            (currentIndex + delta).coerceIn(0, navigationTabs.lastIndex)
        if (destinationIndex == currentIndex) return this
        return copy(
            navigationTabs = navigationTabs.toMutableList().apply {
                removeAt(currentIndex)
                add(destinationIndex, tab)
            },
        ).normalized()
    }

    fun withPinnedFeature(featureId: String, pinned: Boolean): PersonalizationSettings {
        val updatedPins = if (pinned) {
            if (featureId in pinnedFeatureIds) {
                pinnedFeatureIds
            } else {
                (pinnedFeatureIds + featureId).take(MAX_PINNED_FEATURES)
            }
        } else {
            pinnedFeatureIds - featureId
        }
        return copy(
            startFeatureId = startFeatureId.takeIf { it in updatedPins },
            pinnedFeatureIds = updatedPins,
        ).normalized()
    }

    companion object {
        const val MAX_PINNED_FEATURES = 6
        const val MINIMUM_NAVIGATION_TABS = 3
    }
}

enum class ExperiencePreset(
    val label: String,
    val description: String,
    val settings: PersonalizationSettings,
) {
    SHOOTING_STARS(
        label = "Shooting Stars",
        description = "Stars first, with routes and progress tools pinned.",
        settings = PersonalizationSettings(
            startTab = AppTab.STARS,
            navigationTabs = AppTab.entries,
            pinnedFeatureIds = listOf("TELEPORTS", "XP_CHARTS", "GOALS"),
        ),
    ),
    SKILLING(
        label = "Skilling / Mining",
        description = "XP, goals, banked supplies, prices, and timers first.",
        settings = PersonalizationSettings(
            startTab = AppTab.MORE,
            startFeatureId = "XP_CHARTS",
            navigationTabs = listOf(
                AppTab.MORE,
                AppTab.TOOLS,
                AppTab.TIMERS,
                AppTab.WIKI,
                AppTab.JOURNAL,
            ),
            pinnedFeatureIds = listOf(
                "XP_CHARTS",
                "GOALS",
                "BANKED_XP",
                "MARKET_HISTORY",
            ),
        ),
    ),
    QUESTING(
        label = "Questing",
        description = "Quest readiness, routes, clues, and the Wiki first.",
        settings = PersonalizationSettings(
            startTab = AppTab.MORE,
            startFeatureId = "PROGRESS_NAVIGATOR",
            navigationTabs = listOf(
                AppTab.MORE,
                AppTab.WIKI,
                AppTab.JOURNAL,
                AppTab.TOOLS,
                AppTab.STARS,
            ),
            pinnedFeatureIds = listOf(
                "PROGRESS_NAVIGATOR",
                "TELEPORTS",
                "CLUES",
                "MONSTER_EXPLORER",
            ),
        ),
    ),
    SLAYER(
        label = "Slayer",
        description = "Slayer tasks, monster reference, loadouts, and trips first.",
        settings = PersonalizationSettings(
            startTab = AppTab.JOURNAL,
            navigationTabs = listOf(
                AppTab.JOURNAL,
                AppTab.MORE,
                AppTab.TIMERS,
                AppTab.WIKI,
                AppTab.TOOLS,
            ),
            pinnedFeatureIds = listOf(
                "SLAYER",
                "MONSTER_EXPLORER",
                "LOADOUTS",
                "CONSUMABLES",
            ),
        ),
    ),
    BOSSING(
        label = "Bossing",
        description = "Readiness, loadouts, DPS, supplies, and timers first.",
        settings = PersonalizationSettings(
            startTab = AppTab.MORE,
            startFeatureId = "BOSS_READINESS",
            navigationTabs = listOf(
                AppTab.MORE,
                AppTab.TIMERS,
                AppTab.JOURNAL,
                AppTab.TOOLS,
                AppTab.WIKI,
            ),
            pinnedFeatureIds = listOf(
                "BOSS_READINESS",
                "LOADOUTS",
                "DPS",
                "CONSUMABLES",
            ),
        ),
    ),
}
