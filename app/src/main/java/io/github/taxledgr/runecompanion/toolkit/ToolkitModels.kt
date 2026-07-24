package io.github.taxledgr.runecompanion.toolkit

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow

enum class ReminderCategory(val label: String) {
    FARMING("Farming"),
    BIRDHOUSE("Birdhouse"),
    DAILY("Daily"),
    CUSTOM("Custom"),
}

data class CompanionReminder(
    val id: String,
    val title: String,
    val category: ReminderCategory,
    val endsAtEpochMillis: Long,
)

data class SlayerTask(
    val monster: String,
    val target: Int,
    val remaining: Int,
)

enum class ChecklistCategory(val label: String) {
    QUEST("Quests"),
    DIARY("Diaries"),
    COLLECTION("Collection log"),
    LOADOUT("Loadouts"),
}

data class ChecklistEntry(
    val id: String,
    val title: String,
    val category: ChecklistCategory,
    val completed: Boolean = false,
)

data class TripTimer(
    val label: String = "Boss / raid trip",
    val startedAtEpochMillis: Long? = null,
    val elapsedBeforeStartMillis: Long = 0,
) {
    fun elapsedMillis(nowEpochMillis: Long): Long =
        elapsedBeforeStartMillis +
            (startedAtEpochMillis?.let { (nowEpochMillis - it).coerceAtLeast(0) } ?: 0)
}

data class PriceSearchItem(
    val id: Int,
    val name: String,
    val members: Boolean,
)

data class PriceWatchItem(
    val id: Int,
    val name: String,
    val high: Long? = null,
    val low: Long? = null,
    val updatedAtEpochSeconds: Long? = null,
)

data class SkillScore(
    val name: String,
    val rank: Int,
    val level: Int,
    val xp: Long,
)

data class HiscoreSummary(
    val player: String,
    val skills: List<SkillScore>,
)

data class TrackedPlayerProfile(
    val username: String = "",
    val autoRefreshEnabled: Boolean = false,
    val baseline: HiscoreSummary? = null,
    val latest: HiscoreSummary? = null,
    val lastUpdatedEpochMillis: Long? = null,
)

data class ToolkitState(
    val reminders: List<CompanionReminder> = emptyList(),
    val slayerTask: SlayerTask? = null,
    val checklist: List<ChecklistEntry> = emptyList(),
    val tripTimer: TripTimer = TripTimer(),
    val priceWatchlist: List<PriceWatchItem> = emptyList(),
    val priceSearchResults: List<PriceSearchItem> = emptyList(),
    val priceLoading: Boolean = false,
    val priceError: String? = null,
    val hiscore: HiscoreSummary? = null,
    val hiscoreLoading: Boolean = false,
    val hiscoreError: String? = null,
    val trackedPlayer: TrackedPlayerProfile = TrackedPlayerProfile(),
    val trackedPlayerLoading: Boolean = false,
    val trackedPlayerError: String? = null,
    val nowEpochMillis: Long = System.currentTimeMillis(),
)

data class PersistedToolkitData(
    val reminders: List<CompanionReminder> = emptyList(),
    val slayerTask: SlayerTask? = null,
    val checklist: List<ChecklistEntry> = emptyList(),
    val tripTimer: TripTimer = TripTimer(),
    val priceWatchlist: List<PriceWatchItem> = emptyList(),
    val trackedPlayer: TrackedPlayerProfile = TrackedPlayerProfile(),
)

fun HiscoreSummary.skill(name: String): SkillScore? =
    skills.firstOrNull { it.name == name }

fun HiscoreSummary.xpGainedSince(baseline: HiscoreSummary, skillName: String): Long {
    val currentXp = skill(skillName)?.xp ?: return 0
    val baselineXp = baseline.skill(skillName)?.xp ?: return 0
    return (currentXp - baselineXp).coerceAtLeast(0)
}

fun xpForLevel(level: Int): Long {
    val safeLevel = level.coerceIn(1, 126)
    var points = 0L
    for (current in 1 until safeLevel) {
        points += floor(current + 300.0 * 2.0.pow(current / 7.0)).toLong()
    }
    return points / 4
}

fun dropChancePercent(attempts: Int, denominator: Int): Double {
    if (attempts <= 0 || denominator <= 0) return 0.0
    return (1.0 - (1.0 - 1.0 / denominator).pow(attempts)) * 100.0
}

fun attemptsForChance(chancePercent: Double, denominator: Int): Int {
    if (chancePercent <= 0.0 || chancePercent >= 100.0 || denominator <= 1) return 0
    val missChance = 1.0 - chancePercent / 100.0
    return ceil(kotlin.math.ln(missChance) / kotlin.math.ln(1.0 - 1.0 / denominator)).toInt()
}

fun formatDuration(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0) / 1_000)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
