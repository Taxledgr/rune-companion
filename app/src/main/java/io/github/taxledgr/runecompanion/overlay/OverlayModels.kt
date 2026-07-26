package io.github.taxledgr.runecompanion.overlay

import android.content.Context
import io.github.taxledgr.runecompanion.data.ShootingStar
import io.github.taxledgr.runecompanion.features.FeatureData
import io.github.taxledgr.runecompanion.features.ExpansionCatalog
import io.github.taxledgr.runecompanion.features.currentValue
import io.github.taxledgr.runecompanion.features.dropChancePercent
import io.github.taxledgr.runecompanion.features.profitAfterTax
import io.github.taxledgr.runecompanion.features.progress
import io.github.taxledgr.runecompanion.features.remainingActions
import io.github.taxledgr.runecompanion.features.remainingXp
import io.github.taxledgr.runecompanion.features.totalXp
import io.github.taxledgr.runecompanion.toolkit.PersistedToolkitData
import io.github.taxledgr.runecompanion.toolkit.formatDuration
import java.text.NumberFormat
import kotlin.math.roundToInt

enum class OverlayModule(
    val label: String,
    val symbol: String,
    val category: String,
) {
    STARS("Shooting Stars", "✦", "Live"),
    TIMERS("Timers", "◷", "Live"),
    SLAYER("Slayer task", "⚔", "Live"),
    TRIP("Trip timer", "◉", "Live"),
    CHECKLIST("Checklist", "✓", "Planning"),
    PLAYER("Player stats", "★", "Progress"),
    SKILL_GOALS("Skill goals", "↗", "Progress"),
    BANKED_XP("Banked XP", "XP", "Progress"),
    COUNTER_GOALS("Public counters", "#", "Progress"),
    COLLECTION_GOALS("Collection goals", "◇", "Progress"),
    COMBAT_ACHIEVEMENTS("Combat achievements", "CA", "Progress"),
    QUESTS_DIARIES("Quests & diaries", "Q", "Progress"),
    FARMING("Farming patches", "♣", "Planning"),
    ROUTINES("Routines", "↻", "Planning"),
    ITINERARY("Itinerary", "⌁", "Planning"),
    LOADOUTS("Loadouts", "▣", "Planning"),
    BOSS_READINESS("Boss readiness", "B", "Planning"),
    SLAYER_CARDS("Slayer cards", "S", "Reference"),
    TELEPORTS("Teleport setup", "⌂", "Reference"),
    GE_WATCHLIST("GE watchlist", "GE", "Economy"),
    GE_ALERTS("GE alerts", "!", "Economy"),
    PORTFOLIO("Portfolio", "₲", "Economy"),
    GEAR_UPGRADES("Gear upgrades", "↑", "Economy"),
    LOOT_LEDGER("Loot ledger", "¤", "Economy"),
    SUPPLIES("Supply locker", "▤", "Economy"),
    WILDERNESS_RISK("Wilderness risk", "☠", "Economy"),
    SESSIONS("Activity sessions", "◎", "History"),
}

data class OverlaySettings(
    val enabledModules: Set<OverlayModule> = setOf(OverlayModule.STARS),
    val selectedModule: OverlayModule = OverlayModule.STARS,
    val moduleOrder: List<OverlayModule> = OverlayModule.entries,
    val compactWidthDp: Int = DEFAULT_COMPACT_WIDTH_DP,
    val landscapeWidthDp: Int = DEFAULT_LANDSCAPE_WIDTH_DP,
    val opacityPercent: Int = DEFAULT_OPACITY_PERCENT,
    val textScalePercent: Int = DEFAULT_TEXT_SCALE_PERCENT,
    val snapToEdge: Boolean = true,
    val avoidGameControls: Boolean = true,
    val portraitPlacement: OverlayPlacement = OverlayPlacement(),
    val landscapePlacement: OverlayPlacement = OverlayPlacement(),
) {
    fun normalized(): OverlaySettings {
        val enabled = enabledModules.ifEmpty { setOf(OverlayModule.STARS) }
        val order = (moduleOrder + OverlayModule.entries).distinct()
        val selected = selectedModule.takeIf(enabled::contains)
            ?: order.first { it in enabled }
        return copy(
            enabledModules = enabled,
            selectedModule = selected,
            moduleOrder = order,
            compactWidthDp = compactWidthDp.coerceIn(
                MIN_COMPACT_WIDTH_DP,
                MAX_COMPACT_WIDTH_DP,
            ),
            landscapeWidthDp = landscapeWidthDp.coerceIn(
                MIN_COMPACT_WIDTH_DP,
                MAX_COMPACT_WIDTH_DP,
            ),
            opacityPercent = opacityPercent.coerceIn(
                MIN_OPACITY_PERCENT,
                MAX_OPACITY_PERCENT,
            ),
            textScalePercent = textScalePercent.coerceIn(
                MIN_TEXT_SCALE_PERCENT,
                MAX_TEXT_SCALE_PERCENT,
            ),
            portraitPlacement = portraitPlacement.normalized(),
            landscapePlacement = landscapePlacement.normalized(),
        )
    }

    fun toggled(module: OverlayModule): OverlaySettings {
        val updated = if (module in enabledModules) {
            if (enabledModules.size == 1) enabledModules else enabledModules - module
        } else {
            enabledModules + module
        }
        return copy(enabledModules = updated).normalized()
    }

    fun selected(module: OverlayModule): OverlaySettings =
        if (module in enabledModules) copy(selectedModule = module) else this

    fun moveModule(module: OverlayModule, delta: Int): OverlaySettings {
        val normalized = normalized()
        val current = normalized.moduleOrder.indexOf(module)
        if (current < 0) return normalized
        val enabledOrder = normalized.orderedModules
        val enabledIndex = enabledOrder.indexOf(module)
        if (enabledIndex < 0) return normalized
        val destinationEnabledIndex =
            (enabledIndex + delta).coerceIn(0, enabledOrder.lastIndex)
        if (destinationEnabledIndex == enabledIndex) return normalized
        val other = enabledOrder[destinationEnabledIndex]
        val otherIndex = normalized.moduleOrder.indexOf(other)
        return normalized.copy(
            moduleOrder = normalized.moduleOrder.toMutableList().apply {
                this[current] = other
                this[otherIndex] = module
            },
        ).normalized()
    }

    val orderedModules: List<OverlayModule>
        get() = moduleOrder.filter(enabledModules::contains)

    companion object {
        const val DEFAULT_COMPACT_WIDTH_DP = 310
        const val DEFAULT_LANDSCAPE_WIDTH_DP = 330
        const val MIN_COMPACT_WIDTH_DP = 270
        const val MAX_COMPACT_WIDTH_DP = 380
        const val DEFAULT_OPACITY_PERCENT = 100
        const val MIN_OPACITY_PERCENT = 65
        const val MAX_OPACITY_PERCENT = 100
        const val DEFAULT_TEXT_SCALE_PERCENT = 100
        const val MIN_TEXT_SCALE_PERCENT = 85
        const val MAX_TEXT_SCALE_PERCENT = 130
    }
}

data class OverlayPlacement(
    val panelXFraction: Float = 0.04f,
    val panelYFraction: Float = 0.12f,
    val bubbleXFraction: Float = 0.96f,
    val bubbleYFraction: Float = 0.42f,
) {
    fun normalized() = copy(
        panelXFraction = panelXFraction.coerceIn(0f, 1f),
        panelYFraction = panelYFraction.coerceIn(0f, 1f),
        bubbleXFraction = bubbleXFraction.coerceIn(0f, 1f),
        bubbleYFraction = bubbleYFraction.coerceIn(0f, 1f),
    )
}

class OverlayPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): OverlaySettings {
        val modules = preferences.getStringSet(KEY_MODULES, null)
            ?.mapNotNull { name -> OverlayModule.entries.firstOrNull { it.name == name } }
            ?.toSet()
            ?: setOf(OverlayModule.STARS)
        val selected = preferences.getString(KEY_SELECTED, null)
            ?.let { name -> OverlayModule.entries.firstOrNull { it.name == name } }
            ?: OverlayModule.STARS
        return OverlaySettings(
            enabledModules = modules,
            selectedModule = selected,
            moduleOrder = preferences.getString(KEY_ORDER, null)
                ?.split(",")
                ?.mapNotNull { name ->
                    OverlayModule.entries.firstOrNull { it.name == name }
                }
                .orEmpty(),
            compactWidthDp = preferences.getInt(
                KEY_COMPACT_WIDTH,
                OverlaySettings.DEFAULT_COMPACT_WIDTH_DP,
            ),
            landscapeWidthDp = preferences.getInt(
                KEY_LANDSCAPE_WIDTH,
                OverlaySettings.DEFAULT_LANDSCAPE_WIDTH_DP,
            ),
            opacityPercent = preferences.getInt(
                KEY_OPACITY,
                OverlaySettings.DEFAULT_OPACITY_PERCENT,
            ),
            textScalePercent = preferences.getInt(
                KEY_TEXT_SCALE,
                OverlaySettings.DEFAULT_TEXT_SCALE_PERCENT,
            ),
            snapToEdge = preferences.getBoolean(KEY_SNAP_TO_EDGE, true),
            avoidGameControls = preferences.getBoolean(KEY_AVOID_GAME_CONTROLS, true),
            portraitPlacement = loadPlacement(PORTRAIT_PREFIX),
            landscapePlacement = loadPlacement(LANDSCAPE_PREFIX),
        ).normalized()
    }

    fun save(settings: OverlaySettings) {
        val normalized = settings.normalized()
        preferences.edit()
            .putStringSet(KEY_MODULES, normalized.enabledModules.map { it.name }.toSet())
            .putString(KEY_SELECTED, normalized.selectedModule.name)
            .putString(KEY_ORDER, normalized.moduleOrder.joinToString(",") { it.name })
            .putInt(KEY_COMPACT_WIDTH, normalized.compactWidthDp)
            .putInt(KEY_LANDSCAPE_WIDTH, normalized.landscapeWidthDp)
            .putInt(KEY_OPACITY, normalized.opacityPercent)
            .putInt(KEY_TEXT_SCALE, normalized.textScalePercent)
            .putBoolean(KEY_SNAP_TO_EDGE, normalized.snapToEdge)
            .putBoolean(KEY_AVOID_GAME_CONTROLS, normalized.avoidGameControls)
            .putPlacement(PORTRAIT_PREFIX, normalized.portraitPlacement)
            .putPlacement(LANDSCAPE_PREFIX, normalized.landscapePlacement)
            .apply()
    }

    private fun loadPlacement(prefix: String) = OverlayPlacement(
        panelXFraction = preferences.getFloat(
            "${prefix}_panel_x",
            OverlayPlacement().panelXFraction,
        ),
        panelYFraction = preferences.getFloat(
            "${prefix}_panel_y",
            OverlayPlacement().panelYFraction,
        ),
        bubbleXFraction = preferences.getFloat(
            "${prefix}_bubble_x",
            OverlayPlacement().bubbleXFraction,
        ),
        bubbleYFraction = preferences.getFloat(
            "${prefix}_bubble_y",
            OverlayPlacement().bubbleYFraction,
        ),
    ).normalized()

    private fun android.content.SharedPreferences.Editor.putPlacement(
        prefix: String,
        placement: OverlayPlacement,
    ) = apply {
        putFloat("${prefix}_panel_x", placement.panelXFraction)
        putFloat("${prefix}_panel_y", placement.panelYFraction)
        putFloat("${prefix}_bubble_x", placement.bubbleXFraction)
        putFloat("${prefix}_bubble_y", placement.bubbleYFraction)
    }

    companion object {
        const val PREFERENCES_NAME = "rune_companion_overlay"
        private const val KEY_MODULES = "enabled_modules"
        private const val KEY_SELECTED = "selected_module"
        private const val KEY_ORDER = "module_order"
        private const val KEY_COMPACT_WIDTH = "compact_width_dp"
        private const val KEY_LANDSCAPE_WIDTH = "landscape_width_dp"
        private const val KEY_OPACITY = "opacity_percent"
        private const val KEY_TEXT_SCALE = "text_scale_percent"
        private const val KEY_SNAP_TO_EDGE = "snap_to_edge"
        private const val KEY_AVOID_GAME_CONTROLS = "avoid_game_controls"
        private const val PORTRAIT_PREFIX = "portrait"
        private const val LANDSCAPE_PREFIX = "landscape"
    }
}

enum class OverlayTone {
    NORMAL,
    GOOD,
    WARNING,
    DANGER,
}

data class OverlayEntry(
    val title: String,
    val detail: String = "",
    val tone: OverlayTone = OverlayTone.NORMAL,
)

data class OverlayContent(
    val summary: String,
    val entries: List<OverlayEntry>,
    val badgeCount: Int,
    val emptyMessage: String,
)

object OverlayContentBuilder {
    fun build(
        module: OverlayModule,
        toolkit: PersistedToolkitData,
        features: FeatureData,
        stars: List<ShootingStar>,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): OverlayContent = when (module) {
        OverlayModule.STARS -> content(
            "${stars.size} likely-active community reports • tap one to pin its route",
            stars.map { star ->
                OverlayEntry(
                    "W${star.world}  T${star.tier}  ${star.locationName}",
                    star.calledBy,
                )
            },
            stars.size,
            "No likely-active Shooting Star reports.",
        )
        OverlayModule.TIMERS -> {
            val entries = toolkit.reminders.sortedBy { it.endsAtEpochMillis }.map { timer ->
                val remaining = timer.endsAtEpochMillis - nowEpochMillis
                OverlayEntry(
                    timer.title,
                    if (remaining <= 0) "Ready now • ${timer.category.label}" else {
                        "${formatDuration(remaining)} remaining • ${timer.category.label}"
                    },
                    if (remaining <= 0) OverlayTone.GOOD else OverlayTone.NORMAL,
                )
            }
            content(
                "${entries.count { it.tone == OverlayTone.GOOD }} ready • ${entries.size} saved",
                entries,
                entries.size,
                "No timers saved. Add one in Timers.",
            )
        }
        OverlayModule.SLAYER -> {
            val task = toolkit.slayerTask
            content(
                if (task == null) "No active Slayer task" else {
                    "${task.remaining} of ${task.target} remaining"
                },
                task?.let {
                    listOf(
                        OverlayEntry(
                            it.monster,
                            "${it.target - it.remaining} completed • ${it.remaining} left",
                            OverlayTone.GOOD,
                        ),
                    )
                }.orEmpty(),
                task?.remaining ?: 0,
                "No active Slayer task. Choose one in Journal.",
            )
        }
        OverlayModule.TRIP -> {
            val timer = toolkit.tripTimer
            val elapsed = timer.elapsedMillis(nowEpochMillis)
            content(
                if (timer.startedAtEpochMillis == null) "Trip paused" else "Trip running",
                listOf(
                    OverlayEntry(
                        timer.label,
                        "${formatDuration(elapsed)} elapsed",
                        if (timer.startedAtEpochMillis == null) {
                            OverlayTone.NORMAL
                        } else {
                            OverlayTone.GOOD
                        },
                    ),
                ),
                (elapsed / 60_000).toInt(),
                "No trip timer data.",
            )
        }
        OverlayModule.CHECKLIST -> {
            val open = toolkit.checklist.filterNot { it.completed }
            content(
                "${open.size} open • ${toolkit.checklist.count { it.completed }} complete",
                open.map { OverlayEntry(it.title, it.category.label) },
                open.size,
                "Checklist complete or empty.",
            )
        }
        OverlayModule.PLAYER -> playerContent(toolkit, features)
        OverlayModule.SKILL_GOALS -> {
            val profile = features.selectedProfile()
            content(
                "${features.goals.size} skill goals",
                features.goals.map { goal ->
                    OverlayEntry(
                        "${goal.skill} → level ${goal.targetLevel}",
                        "${number(goal.remainingXp(profile))} XP • " +
                            "${number(goal.remainingActions(profile))} actions",
                    )
                },
                features.goals.size,
                "No skill goals saved.",
            )
        }
        OverlayModule.BANKED_XP -> content(
            "${features.bankedXp.size} banked-XP entries",
            features.bankedXp.map {
                OverlayEntry(
                    "${number(it.quantity.toLong())} × ${it.item}",
                    "${number(it.totalXp())} ${it.skill} XP",
                )
            },
            features.bankedXp.size,
            "No banked-XP entries saved.",
        )
        OverlayModule.COUNTER_GOALS -> {
            val profile = features.selectedProfile()
            content(
                "${features.counterGoals.size} public counter goals",
                features.counterGoals.map { goal ->
                    OverlayEntry(
                        goal.activity,
                        "${number(goal.currentValue(profile))} / ${number(goal.targetValue)} • " +
                            "${(goal.progress(profile) * 100).roundToInt()}%",
                    )
                },
                features.counterGoals.size,
                "No public counter goals saved.",
            )
        }
        OverlayModule.COLLECTION_GOALS -> {
            val open = features.collectionGoals.filterNot { it.obtained }
            content(
                "${open.size} collection goals remaining",
                open.map {
                    OverlayEntry(
                        it.item,
                        "${number(it.attempts.toLong())} attempts • " +
                            "%.1f%% chance so far".format(it.dropChancePercent()),
                    )
                },
                open.size,
                "No open collection goals.",
            )
        }
        OverlayModule.COMBAT_ACHIEVEMENTS -> {
            val open = features.combatAchievements.filterNot { it.completed }
            content(
                "${open.size} Combat Achievement plans open",
                open.map { OverlayEntry(it.task, it.tier) },
                open.size,
                "No open Combat Achievement plans.",
            )
        }
        OverlayModule.QUESTS_DIARIES -> {
            val skillLevels = features.selectedProfile()
                ?.latest
                ?.summary
                ?.skills
                ?.associate { it.name to it.level }
                .orEmpty()
            val open = ExpansionCatalog.progress.filterNot {
                it.id in features.completedProgressIds
            }
            content(
                "${features.completedProgressIds.size} complete • ${open.size} tracked entries open",
                open.map { entry ->
                    val ready = entry.skills.count { requirement ->
                        (skillLevels[requirement.skill] ?: 1) >= requirement.level
                    }
                    OverlayEntry(
                        entry.name,
                        if (entry.category == "Quest") {
                            "Tap for step-by-step guide • $ready/${entry.skills.size} public " +
                                "skill checks ready • prerequisites: ${entry.prerequisites}"
                        } else {
                            "${entry.category} • $ready/${entry.skills.size} public skill checks " +
                                "ready • prerequisites: ${entry.prerequisites}"
                        },
                        if (ready == entry.skills.size) OverlayTone.GOOD else OverlayTone.NORMAL,
                    )
                },
                open.size,
                "All tracked quests and diaries are marked complete.",
            )
        }
        OverlayModule.FARMING -> {
            val patches = features.farmPatches.sortedBy { it.readyAtEpochMillis }
            content(
                "${patches.count { it.readyAtEpochMillis <= nowEpochMillis }} ready • " +
                    "${patches.size} tracked",
                patches.map {
                    val remaining = it.readyAtEpochMillis - nowEpochMillis
                    OverlayEntry(
                        "${it.patch} • ${it.crop}",
                        if (remaining <= 0) "Ready now" else "${formatDuration(remaining)} remaining",
                        if (remaining <= 0) OverlayTone.GOOD else OverlayTone.NORMAL,
                    )
                },
                patches.count { it.readyAtEpochMillis <= nowEpochMillis },
                "No farming patches tracked.",
            )
        }
        OverlayModule.ROUTINES -> {
            val today = nowEpochMillis / DAY_MILLIS
            val due = features.routines.filter { routine ->
                routine.lastCompletedEpochDay == null ||
                    if (routine.weekly) {
                        today - routine.lastCompletedEpochDay >= 7
                    } else {
                        routine.lastCompletedEpochDay != today
                    }
            }
            content(
                "${due.size} routines due",
                due.map {
                    OverlayEntry(
                        it.title,
                        "${if (it.weekly) "Weekly" else "Daily"} • ${it.streak} streak",
                        OverlayTone.WARNING,
                    )
                },
                due.size,
                "All routines complete or none saved.",
            )
        }
        OverlayModule.ITINERARY -> {
            val open = features.itineraryStops.filterNot { it.completed }
            content(
                "${open.size} itinerary stops remaining",
                open.map {
                    OverlayEntry(
                        it.title,
                        "${it.teleport} • ${it.region}",
                    )
                },
                open.size,
                "No itinerary stops remaining.",
            )
        }
        OverlayModule.LOADOUTS -> content(
            "${features.loadouts.size} loadouts",
            features.loadouts.map {
                OverlayEntry(
                    it.name,
                    listOf(it.equipment, it.inventory)
                        .filter(String::isNotBlank)
                        .joinToString(" • ")
                        .take(180),
                )
            },
            features.loadouts.size,
            "No loadouts saved.",
        )
        OverlayModule.BOSS_READINESS -> content(
            "${features.bossReadinessPlans.size} boss plans",
            features.bossReadinessPlans.map {
                OverlayEntry(
                    it.bossId.humanized(),
                    "${it.confirmedChecks.size} checks confirmed" +
                        it.notes.takeIf(String::isNotBlank)?.let { note -> " • $note" }.orEmpty(),
                )
            },
            features.bossReadinessPlans.size,
            "No boss-readiness plans saved.",
        )
        OverlayModule.SLAYER_CARDS -> content(
            "${features.slayerCards.size} Slayer cards",
            features.slayerCards.map {
                OverlayEntry(
                    it.monster,
                    listOf(it.weakness, it.locations, it.requiredItems)
                        .filter(String::isNotBlank)
                        .joinToString(" • ")
                        .take(180),
                )
            },
            features.slayerCards.size,
            "No Slayer cards saved.",
        )
        OverlayModule.TELEPORTS -> {
            val profile = features.teleportProfile
            val entries = buildList {
                add(OverlayEntry("Spellbooks", profile.spellbooks.joinToString { it.label }))
                add(OverlayEntry("POH location", profile.pohLocation))
                add(
                    OverlayEntry(
                        "POH destinations",
                        "${profile.pohDestinations.size} configured",
                    ),
                )
                add(
                    OverlayEntry(
                        "Items and networks",
                        "${profile.capabilities.size} configured",
                    ),
                )
                addAll(
                    features.customTeleports.map {
                        OverlayEntry(it.name, "${it.destination} • ${it.region}")
                    },
                )
            }
            content(
                "${profile.capabilities.size + profile.pohDestinations.size} travel options",
                entries,
                profile.capabilities.size + profile.pohDestinations.size,
                "No teleport setup saved.",
            )
        }
        OverlayModule.GE_WATCHLIST -> content(
            "${toolkit.priceWatchlist.size} GE watch items",
            toolkit.priceWatchlist.map {
                OverlayEntry(
                    it.name,
                    "Buy ${coins(it.low)} • Sell ${coins(it.high)}",
                )
            },
            toolkit.priceWatchlist.size,
            "No GE watchlist items.",
        )
        OverlayModule.GE_ALERTS -> content(
            "${features.geAlerts.size} GE alerts",
            features.geAlerts.map {
                OverlayEntry(
                    it.itemName,
                    "${if (it.alertWhenAbove) "Above" else "Below"} ${coins(it.targetPrice)} • " +
                        "latest ${coins(it.latestPrice)}",
                    if (it.targetWasMet) OverlayTone.GOOD else OverlayTone.NORMAL,
                )
            },
            features.geAlerts.size,
            "No GE price alerts.",
        )
        OverlayModule.PORTFOLIO -> content(
            "${features.portfolio.size} portfolio positions",
            features.portfolio.map {
                val profit = it.profitAfterTax()
                OverlayEntry(
                    "${number(it.quantity.toLong())} × ${it.itemName}",
                    "Latest ${coins(it.latestPrice)} • ${signedCoins(profit)}",
                    if (profit >= 0) OverlayTone.GOOD else OverlayTone.DANGER,
                )
            },
            features.portfolio.size,
            "No portfolio positions.",
        )
        OverlayModule.GEAR_UPGRADES -> {
            val open = features.gearUpgrades.filterNot { it.obtained }
            content(
                "${open.size} gear upgrades planned",
                open.map {
                    OverlayEntry(
                        "${it.currentItem} → ${it.targetItemName}",
                        "${coins(it.targetPrice)} • budget ${coins(it.budget)} • ${it.benefit}",
                    )
                },
                open.size,
                "No open gear upgrades.",
            )
        }
        OverlayModule.LOOT_LEDGER -> content(
            "${features.lootLedger.size} loot entries • " +
                coins(features.lootLedger.sumOf { it.totalValue }),
            features.lootLedger.sortedByDescending { it.createdAtEpochMillis }.map {
                OverlayEntry(
                    "${number(it.quantity.toLong())} × ${it.itemName}",
                    "${it.activity} • ${coins(it.totalValue)}",
                    OverlayTone.GOOD,
                )
            },
            features.lootLedger.size,
            "No loot ledger entries.",
        )
        OverlayModule.SUPPLIES -> {
            val low = features.supplyLocker.filter { it.quantity <= it.lowAt }
            content(
                "${low.size} low • ${features.supplyLocker.size} tracked",
                features.supplyLocker.sortedBy { it.quantity > it.lowAt }.map {
                    OverlayEntry(
                        it.itemName,
                        "${number(it.quantity.toLong())} held • low at ${number(it.lowAt.toLong())}",
                        if (it.quantity <= it.lowAt) OverlayTone.WARNING else OverlayTone.NORMAL,
                    )
                },
                low.size,
                "No supply-locker items.",
            )
        }
        OverlayModule.WILDERNESS_RISK -> {
            val risk = features.wildernessRisk.filterNot { it.protected }.sumOf { it.totalValue }
            content(
                "${coins(risk)} currently at risk",
                features.wildernessRisk.sortedByDescending { it.totalValue }.map {
                    OverlayEntry(
                        "${number(it.quantity.toLong())} × ${it.itemName}",
                        "${coins(it.totalValue)} • ${if (it.protected) "Protected" else "At risk"}",
                        if (it.protected) OverlayTone.GOOD else OverlayTone.DANGER,
                    )
                },
                features.wildernessRisk.count { !it.protected },
                "No Wilderness risk items.",
            )
        }
        OverlayModule.SESSIONS -> content(
            "${features.sessions.size} activity sessions",
            features.sessions.sortedByDescending { it.createdAtEpochMillis }.map {
                OverlayEntry(
                    it.activity,
                    "${it.kills} kills • ${it.durationMinutes} min • " +
                        "${signedCoins(it.lootValue - it.supplyCost)} net",
                )
            },
            features.sessions.size,
            "No activity sessions logged.",
        )
    }

    private fun playerContent(
        toolkit: PersistedToolkitData,
        features: FeatureData,
    ): OverlayContent {
        val profile = features.selectedProfile()
        val summary = profile?.latest?.summary ?: toolkit.trackedPlayer.latest
        if (summary == null) {
            return content(
                "No player stats loaded",
                emptyList(),
                0,
                "Save a player in More → App settings.",
            )
        }
        val overall = summary.skills.firstOrNull { it.name == "Overall" }
        val entries = buildList {
            add(
                OverlayEntry(
                    summary.player,
                    "Total level ${number(overall?.level?.toLong() ?: 0)} • " +
                        "${number(overall?.xp ?: 0)} XP",
                    OverlayTone.GOOD,
                ),
            )
            addAll(
                summary.skills
                    .filter { it.name in DISPLAY_SKILLS }
                    .map { OverlayEntry(it.name, "Level ${it.level} • ${number(it.xp)} XP") },
            )
        }
        return content(
            "${summary.player} • public Hiscores",
            entries,
            overall?.level ?: entries.size,
            "No player stats loaded.",
        )
    }

    private fun FeatureData.selectedProfile() = accounts.firstOrNull {
        it.username.equals(selectedAccount, ignoreCase = true)
    } ?: accounts.firstOrNull()

    private fun content(
        summary: String,
        entries: List<OverlayEntry>,
        badgeCount: Int,
        emptyMessage: String,
    ) = OverlayContent(summary, entries, badgeCount.coerceAtLeast(0), emptyMessage)

    private fun number(value: Long): String = NumberFormat.getIntegerInstance().format(value)

    private fun coins(value: Long?): String =
        value?.let { "${compactNumber(it)} gp" } ?: "—"

    private fun signedCoins(value: Long): String =
        "${if (value >= 0) "+" else "−"}${compactNumber(kotlin.math.abs(value))} gp"

    private fun compactNumber(value: Long): String = when {
        value >= 1_000_000_000 -> "%.2fb".format(value / 1_000_000_000.0)
        value >= 1_000_000 -> "%.2fm".format(value / 1_000_000.0)
        value >= 1_000 -> "%.1fk".format(value / 1_000.0)
        else -> number(value)
    }

    private fun String.humanized(): String =
        replace('_', ' ').split(' ').joinToString(" ") { word ->
            word.replaceFirstChar(Char::uppercase)
        }

    private val DISPLAY_SKILLS = setOf(
        "Attack",
        "Strength",
        "Defence",
        "Hitpoints",
        "Ranged",
        "Prayer",
        "Magic",
        "Slayer",
        "Agility",
    )
    private const val DAY_MILLIS = 86_400_000L
}
