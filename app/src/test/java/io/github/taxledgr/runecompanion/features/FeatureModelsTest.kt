package io.github.taxledgr.runecompanion.features

import io.github.taxledgr.runecompanion.toolkit.HiscoreSummary
import io.github.taxledgr.runecompanion.toolkit.ActivityScore
import io.github.taxledgr.runecompanion.toolkit.SkillScore
import io.github.taxledgr.runecompanion.toolkit.TrackedPlayerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureModelsTest {
    @Test
    fun currentPortalNexusCatalogueHasFortyOneDestinations() {
        assertEquals(41, TeleportCatalog.nexusDestinations.size)
    }

    @Test
    fun spellTeleportRequiresMagicSpellbookAndUnlock() {
        val option = TeleportCatalog.spellbooks.first { it.id == "spell_ardougne" }
        val locked = TeleportCapabilityProfile(spellbooks = setOf(Spellbook.STANDARD))
        val unlocked = locked.copy(capabilities = setOf("quest_plague_city"))

        assertFalse(option.isAvailable(locked, magicLevel = 99))
        assertFalse(option.isAvailable(unlocked, magicLevel = 50))
        assertTrue(option.isAvailable(unlocked, magicLevel = 51))
    }

    @Test
    fun nexusDestinationRequiresHouseAccessAndConfiguredDestination() {
        val option = TeleportCatalog.nexusDestinations.first()
        val key = option.id.removePrefix("poh_nexus_")

        assertFalse(
            option.isAvailable(
                TeleportCapabilityProfile(pohDestinations = setOf(key)),
                magicLevel = 99,
            ),
        )
        assertTrue(
            option.isAvailable(
                TeleportCapabilityProfile(
                    capabilities = setOf("poh_access"),
                    pohDestinations = setOf(key),
                ),
                magicLevel = 99,
            ),
        )
    }

    @Test
    fun xpHistoryCalculatesDailyGainFromSnapshots() {
        val now = 1_000_000_000L
        fun summary(xp: Long) = HiscoreSummary(
            "Player",
            listOf(SkillScore("Mining", 1, 80, xp)),
        )
        val profile = AccountProfile(
            "Player",
            snapshots = listOf(
                StatSnapshot(now - 2 * 24 * 60 * 60_000L, summary(1_000)),
                StatSnapshot(now - 24 * 60 * 60_000L, summary(1_200)),
                StatSnapshot(now, summary(1_500)),
            ),
        )

        assertEquals(300, profile.xpGained("Mining", now - 24 * 60 * 60_000L))
    }

    @Test
    fun portfolioUsesOnePercentTaxWithFiveMillionPerItemCap() {
        val normal = PortfolioEntry("1", 1, "Item", 10, 900, 1_000)
        val capped = PortfolioEntry("2", 2, "Expensive", 2, 1, 600_000_000)

        assertEquals(100, normal.tax())
        assertEquals(10_000_000, capped.tax())
        assertEquals(900, normal.profitAfterTax())
    }

    @Test
    fun trackedPlayerMigratesIntoMultiAccountDataWithoutReplacingExistingData() {
        val baseline = HiscoreSummary("Player", listOf(SkillScore("Mining", 2, 80, 2_000)))
        val latest = HiscoreSummary("Player", listOf(SkillScore("Mining", 1, 81, 2_500)))
        val migrated = FeatureData(
            goals = listOf(SkillGoal("goal", "Mining", 90, 50.0)),
        ).withTrackedPlayer(
            TrackedPlayerProfile(
                username = "Player",
                autoRefreshEnabled = true,
                baseline = baseline,
                latest = latest,
                lastUpdatedEpochMillis = 10_000,
            ),
        )

        assertEquals("Player", migrated.selectedAccount)
        assertEquals(1, migrated.accounts.size)
        assertEquals(2, migrated.accounts.single().snapshots.size)
        assertEquals(1, migrated.goals.size)
        assertEquals(migrated, migrated.withTrackedPlayer(TrackedPlayerProfile("player")))
    }

    @Test
    fun activityHistoryCalculatesAutomaticBossGains() {
        val now = 2_000_000_000L
        fun summary(score: Long) = HiscoreSummary(
            player = "Player",
            skills = emptyList(),
            activities = listOf(ActivityScore("Vorkath", 1, score)),
        )
        val profile = AccountProfile(
            username = "Player",
            snapshots = listOf(
                StatSnapshot(now - 2 * 24 * 60 * 60_000L, summary(100)),
                StatSnapshot(now - 24 * 60 * 60_000L, summary(110)),
                StatSnapshot(now, summary(125)),
            ),
        )

        assertEquals(15L, profile.activityGained("Vorkath", now - 24 * 60 * 60_000L))
        assertEquals(0L, profile.activityGained("Zulrah", now - 24 * 60 * 60_000L))
    }

    @Test
    fun accountRefreshPolicySkipsFreshAndDisabledProfiles() {
        val now = 1_000_000L
        val interval = 100_000L
        val summary = HiscoreSummary("Player", emptyList())
        val fresh = AccountProfile(
            username = "Player",
            snapshots = listOf(StatSnapshot(now - interval + 1, summary)),
        )
        val stale = fresh.copy(
            snapshots = listOf(StatSnapshot(now - interval, summary)),
        )

        assertFalse(fresh.needsRefresh(now, interval))
        assertTrue(stale.needsRefresh(now, interval))
        assertTrue(AccountProfile("New player").needsRefresh(now, interval))
        assertFalse(stale.copy(autoRefresh = false).needsRefresh(now, interval))
    }

    @Test
    fun publicCounterGoalUsesMatchingAccountAndClampsProgress() {
        val summary = HiscoreSummary(
            player = "Player",
            skills = emptyList(),
            activities = listOf(ActivityScore("Vorkath", 1, 175)),
        )
        val profile = AccountProfile(
            username = "Player",
            snapshots = listOf(StatSnapshot(1, summary)),
        )
        val goal = PublicCounterGoal("goal", "Player", "Vorkath", 100, 200)

        assertEquals(175, goal.currentValue(profile))
        assertEquals(0.75f, goal.progress(profile), 0.001f)
        assertEquals(100, goal.currentValue(profile.copy(username = "Other")))
    }

    @Test
    fun expansionValuesUseQuantityAndSafeMidpointFallbacks() {
        assertEquals(
            7_500,
            LootLedgerEntry("1", "Boss", 1, "Drop", 3, 2_500, 0).totalValue,
        )
        assertEquals(
            4_000,
            SupplyLockerItem("2", 2, "Supply", 20, 5, 200).stockValue,
        )
        assertEquals(150L, MarketHistoryPoint(1, 200, 100, 0, 0).midpoint)
        assertEquals(100L, MarketHistoryPoint(1, null, 100, 0, 0).midpoint)
    }
}
