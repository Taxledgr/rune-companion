package io.github.taxledgr.runecompanion.features

import io.github.taxledgr.runecompanion.toolkit.HiscoreSummary
import io.github.taxledgr.runecompanion.toolkit.PriceSearchItem
import io.github.taxledgr.runecompanion.toolkit.SkillScore
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
    val teleportProfile: TeleportCapabilityProfile = TeleportCapabilityProfile(),
)

data class FeatureState(
    val data: FeatureData = FeatureData(),
    val loading: Boolean = false,
    val message: String? = null,
    val priceSearchResults: List<PriceSearchItem> = emptyList(),
)

fun AccountProfile.magicLevel(): Int =
    latest?.summary?.skills?.firstOrNull { it.name == "Magic" }?.level ?: 1

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
