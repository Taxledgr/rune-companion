package io.github.taxledgr.runecompanion.features

import io.github.taxledgr.runecompanion.toolkit.HiscoreSummary
import io.github.taxledgr.runecompanion.toolkit.PriceSearchItem
import io.github.taxledgr.runecompanion.toolkit.SkillScore
import io.github.taxledgr.runecompanion.toolkit.TrackedPlayerProfile
import io.github.taxledgr.runecompanion.toolkit.xpForLevel
import kotlin.math.max
import kotlin.math.pow

data class StatSnapshot(
    val capturedAtEpochMillis: Long,
    val summary: HiscoreSummary,
)

data class AccountProfile(
    val username: String,
    val snapshots: List<StatSnapshot> = emptyList(),
    val autoRefresh: Boolean = true,
) {
    val latest: StatSnapshot? get() = snapshots.maxByOrNull { it.capturedAtEpochMillis }
}

fun AccountProfile.needsRefresh(nowEpochMillis: Long, intervalMillis: Long): Boolean =
    autoRefresh &&
        (latest?.capturedAtEpochMillis ?: 0L) <= nowEpochMillis - intervalMillis

data class SkillGoal(
    val id: String,
    val skill: String,
    val targetLevel: Int,
    val xpPerAction: Double,
)

data class BankedXpEntry(
    val id: String,
    val item: String,
    val skill: String,
    val quantity: Int,
    val xpEach: Double,
)

data class GeAlert(
    val id: String,
    val itemId: Int,
    val itemName: String,
    val targetPrice: Long,
    val alertWhenAbove: Boolean,
    val latestPrice: Long? = null,
    val targetWasMet: Boolean = false,
)

data class PortfolioEntry(
    val id: String,
    val itemId: Int,
    val itemName: String,
    val quantity: Int,
    val buyPrice: Long,
    val latestPrice: Long? = null,
)

data class FarmPatch(
    val id: String,
    val patch: String,
    val crop: String,
    val readyAtEpochMillis: Long,
    val note: String,
)

data class SlayerCard(
    val id: String,
    val monster: String,
    val weakness: String,
    val locations: String,
    val requiredItems: String,
    val notes: String,
)

data class ActivitySession(
    val id: String,
    val activity: String,
    val kills: Int,
    val durationMinutes: Int,
    val lootValue: Long,
    val supplyCost: Long,
    val createdAtEpochMillis: Long,
)

data class CollectionGoal(
    val id: String,
    val item: String,
    val dropRateDenominator: Int,
    val attempts: Int,
    val obtained: Boolean,
    val sourceActivity: String? = null,
)

data class Routine(
    val id: String,
    val title: String,
    val weekly: Boolean,
    val lastCompletedEpochDay: Long? = null,
    val streak: Int = 0,
)

data class CombatAchievementPlan(
    val id: String,
    val task: String,
    val tier: String,
    val completed: Boolean,
)

data class LoadoutTemplate(
    val id: String,
    val name: String,
    val inventory: String,
    val equipment: String,
    val notes: String,
)

data class BossReadinessPlan(
    val id: String,
    val bossId: String,
    val confirmedChecks: Set<String> = emptySet(),
    val notes: String = "",
)

data class ItineraryStop(
    val id: String,
    val title: String,
    val region: String,
    val teleport: String,
    val completed: Boolean = false,
)

data class GearUpgradePlan(
    val id: String,
    val style: String,
    val currentItem: String,
    val targetItemId: Int,
    val targetItemName: String,
    val targetPrice: Long?,
    val budget: Long,
    val benefit: String,
    val obtained: Boolean = false,
)

data class LootLedgerEntry(
    val id: String,
    val activity: String,
    val itemId: Int,
    val itemName: String,
    val quantity: Int,
    val unitValue: Long,
    val createdAtEpochMillis: Long,
) {
    val totalValue: Long get() = quantity.coerceAtLeast(0) * unitValue.coerceAtLeast(0)
}

data class PublicCounterGoal(
    val id: String,
    val account: String,
    val activity: String,
    val startValue: Long,
    val targetValue: Long,
)

data class SupplyLockerItem(
    val id: String,
    val itemId: Int,
    val itemName: String,
    val quantity: Int,
    val lowAt: Int,
    val unitValue: Long,
) {
    val stockValue: Long get() = quantity.coerceAtLeast(0) * unitValue.coerceAtLeast(0)
}

data class WildernessRiskItem(
    val id: String,
    val itemId: Int,
    val itemName: String,
    val quantity: Int,
    val unitValue: Long,
    val protected: Boolean,
) {
    val totalValue: Long get() = quantity.coerceAtLeast(0) * unitValue.coerceAtLeast(0)
}

data class MarketHistoryPoint(
    val timestampEpochSeconds: Long,
    val averageHigh: Long?,
    val averageLow: Long?,
    val highVolume: Long,
    val lowVolume: Long,
) {
    val midpoint: Long?
        get() = when {
            averageHigh != null && averageLow != null -> (averageHigh + averageLow) / 2
            averageHigh != null -> averageHigh
            else -> averageLow
        }
}

data class CustomTeleport(
    val id: String,
    val name: String,
    val destination: String,
    val region: String,
    val dangerous: Boolean,
)

enum class Spellbook(val label: String) {
    STANDARD("Standard"),
    ANCIENT("Ancient"),
    LUNAR("Lunar"),
    ARCEUUS("Arceuus"),
}

enum class TeleportKind(val label: String) {
    HOME("Home"),
    SPELL("Magic spell"),
    TABLET("Teleport tablet"),
    JEWELLERY("Jewellery"),
    ITEM("Special item"),
    POH("Player-owned house"),
}

data class TeleportCapabilityProfile(
    val spellbooks: Set<Spellbook> = setOf(Spellbook.STANDARD),
    val capabilities: Set<String> = setOf("home_lumbridge"),
    val pohLocation: String = "Rimmington",
    val pohDestinations: Set<String> = emptySet(),
)

data class TeleportOption(
    val id: String,
    val name: String,
    val destination: String,
    val region: String,
    val kind: TeleportKind,
    val magicLevel: Int = 1,
    val spellbook: Spellbook? = null,
    val capability: String? = null,
    val dangerous: Boolean = false,
    val note: String = "",
) {
    fun isAvailable(profile: TeleportCapabilityProfile, magicLevel: Int): Boolean {
        if (magicLevel < this.magicLevel) return false
        if (spellbook != null && spellbook !in profile.spellbooks) return false
        if (capability != null && capability !in profile.capabilities) return false
        if (kind == TeleportKind.POH && id.startsWith("poh_nexus_")) {
            return id.removePrefix("poh_nexus_") in profile.pohDestinations &&
                "poh_access" in profile.capabilities
        }
        if (kind == TeleportKind.POH && id != "poh_access") {
            return "poh_access" in profile.capabilities
        }
        return true
    }
}

data class FeatureData(
    val accounts: List<AccountProfile> = emptyList(),
    val selectedAccount: String? = null,
    val goals: List<SkillGoal> = emptyList(),
    val bankedXp: List<BankedXpEntry> = emptyList(),
    val geAlerts: List<GeAlert> = emptyList(),
    val portfolio: List<PortfolioEntry> = emptyList(),
    val farmPatches: List<FarmPatch> = emptyList(),
    val slayerCards: List<SlayerCard> = emptyList(),
    val sessions: List<ActivitySession> = emptyList(),
    val collectionGoals: List<CollectionGoal> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val combatAchievements: List<CombatAchievementPlan> = emptyList(),
    val loadouts: List<LoadoutTemplate> = emptyList(),
    val bossReadinessPlans: List<BossReadinessPlan> = emptyList(),
    val itineraryStops: List<ItineraryStop> = emptyList(),
    val gearUpgrades: List<GearUpgradePlan> = emptyList(),
    val lootLedger: List<LootLedgerEntry> = emptyList(),
    val counterGoals: List<PublicCounterGoal> = emptyList(),
    val completedProgressIds: Set<String> = emptySet(),
    val supplyLocker: List<SupplyLockerItem> = emptyList(),
    val wildernessRisk: List<WildernessRiskItem> = emptyList(),
    val customTeleports: List<CustomTeleport> = emptyList(),
    val teleportProfile: TeleportCapabilityProfile = TeleportCapabilityProfile(),
)

data class FeatureState(
    val data: FeatureData = FeatureData(),
    val initializing: Boolean = false,
    val loading: Boolean = false,
    val message: String? = null,
    val priceSearchResults: List<PriceSearchItem> = emptyList(),
    val marketItem: PriceSearchItem? = null,
    val marketHistory: List<MarketHistoryPoint> = emptyList(),
    val marketHistoryLoading: Boolean = false,
    val restoreGeneration: Int = 0,
)

fun PublicCounterGoal.currentValue(profile: AccountProfile?): Long =
    profile
        ?.takeIf { it.username.equals(account, ignoreCase = true) }
        ?.latest
        ?.summary
        ?.activities
        ?.firstOrNull { it.name.equals(activity, ignoreCase = true) }
        ?.score
        ?.coerceAtLeast(0)
        ?: startValue

fun PublicCounterGoal.progress(profile: AccountProfile?): Float {
    val required = (targetValue - startValue).coerceAtLeast(1)
    val gained = (currentValue(profile) - startValue).coerceAtLeast(0)
    return (gained.toDouble() / required).coerceIn(0.0, 1.0).toFloat()
}

fun FeatureData.withTrackedPlayer(
    profile: TrackedPlayerProfile,
    migratedAtEpochMillis: Long = System.currentTimeMillis(),
): FeatureData {
    val username = profile.username.trim().take(12)
    if (username.isBlank() || accounts.any { it.username.equals(username, ignoreCase = true) }) {
        return this
    }
    val capturedAt = profile.lastUpdatedEpochMillis ?: migratedAtEpochMillis
    val snapshots = buildList {
        profile.baseline?.let { baseline ->
            add(StatSnapshot((capturedAt - 1).coerceAtLeast(0), baseline))
        }
        profile.latest?.let { latest ->
            if (latest != profile.baseline) add(StatSnapshot(capturedAt, latest))
        }
    }
    return copy(
        accounts = accounts + AccountProfile(
            username = username,
            snapshots = snapshots,
            autoRefresh = profile.autoRefreshEnabled,
        ),
        selectedAccount = selectedAccount ?: username,
    )
}

fun AccountProfile.magicLevel(): Int =
    latest?.summary?.skills?.firstOrNull { it.name == "Magic" }?.level ?: 1

fun AccountProfile.agilityLevel(): Int =
    latest?.summary?.skills?.firstOrNull { it.name == "Agility" }?.level ?: 1

fun SkillGoal.remainingXp(profile: AccountProfile?): Long {
    val currentXp = profile?.latest?.summary?.skills
        ?.firstOrNull { it.name == skill }?.xp?.coerceAtLeast(0) ?: 0
    return (xpForLevel(targetLevel) - currentXp).coerceAtLeast(0)
}

fun SkillGoal.remainingActions(profile: AccountProfile?): Long =
    if (xpPerAction <= 0) 0 else kotlin.math.ceil(remainingXp(profile) / xpPerAction).toLong()

fun BankedXpEntry.totalXp(): Long = (quantity.coerceAtLeast(0) * xpEach.coerceAtLeast(0.0)).toLong()

fun PortfolioEntry.tax(): Long {
    val sell = latestPrice ?: return 0
    val perItemTax = (sell / 100).coerceAtMost(5_000_000)
    return perItemTax * quantity.coerceAtLeast(0)
}

fun PortfolioEntry.profitAfterTax(): Long {
    val sell = latestPrice ?: return 0
    return (sell - buyPrice) * quantity.coerceAtLeast(0) - tax()
}

fun CollectionGoal.dropChancePercent(): Double {
    if (attempts <= 0 || dropRateDenominator <= 0) return 0.0
    return (1 - (1 - 1.0 / dropRateDenominator).pow(attempts)) * 100
}

fun HiscoreSummary.skillOrEmpty(name: String): SkillScore =
    skills.firstOrNull { it.name == name } ?: SkillScore(name, -1, 1, 0)

fun AccountProfile.xpGained(skill: String, sinceEpochMillis: Long): Long {
    val ordered = snapshots.sortedBy { it.capturedAtEpochMillis }
    val latestXp = ordered.lastOrNull()?.summary?.skillOrEmpty(skill)?.xp ?: return 0
    val baselineXp = ordered.lastOrNull { it.capturedAtEpochMillis <= sinceEpochMillis }
        ?.summary?.skillOrEmpty(skill)?.xp
        ?: ordered.firstOrNull()?.summary?.skillOrEmpty(skill)?.xp
        ?: latestXp
    return max(0, latestXp - baselineXp)
}

fun AccountProfile.activityGained(activity: String, sinceEpochMillis: Long): Long {
    val ordered = snapshots.sortedBy { it.capturedAtEpochMillis }
    val latestScore = ordered.lastOrNull()?.summary?.activities
        ?.firstOrNull { it.name.equals(activity, ignoreCase = true) }?.score ?: return 0
    val baselineScore = ordered.lastOrNull { it.capturedAtEpochMillis <= sinceEpochMillis }
        ?.summary?.activities
        ?.firstOrNull { it.name.equals(activity, ignoreCase = true) }?.score
        ?: ordered.firstOrNull()?.summary?.activities
            ?.firstOrNull { it.name.equals(activity, ignoreCase = true) }?.score
        ?: latestScore
    return max(0, latestScore - baselineScore)
}

fun compactSnapshots(
    snapshots: List<StatSnapshot>,
    nowEpochMillis: Long = System.currentTimeMillis(),
): List<StatSnapshot> {
    val recentCutoff = nowEpochMillis - 24 * 60 * 60_000L
    val recent = snapshots.filter { it.capturedAtEpochMillis >= recentCutoff }
    val hourlyHistory = snapshots
        .filter { it.capturedAtEpochMillis < recentCutoff }
        .groupBy { it.capturedAtEpochMillis / (60 * 60_000L) }
        .values
        .mapNotNull { bucket -> bucket.maxByOrNull { it.capturedAtEpochMillis } }
    return (hourlyHistory + recent)
        .sortedBy { it.capturedAtEpochMillis }
        .takeLast(MAX_RETAINED_SNAPSHOTS)
}

private const val MAX_RETAINED_SNAPSHOTS = 900
