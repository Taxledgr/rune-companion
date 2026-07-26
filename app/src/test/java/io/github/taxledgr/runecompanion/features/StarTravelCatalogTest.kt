package io.github.taxledgr.runecompanion.features

import io.github.taxledgr.runecompanion.data.StarLocationCatalog
import io.github.taxledgr.runecompanion.toolkit.HiscoreSummary
import io.github.taxledgr.runecompanion.toolkit.SkillScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StarTravelCatalogTest {
    @Test
    fun everyStarMinersLocationHasRoutes() {
        assertEquals(82, StarTravelCatalog.guides.size)
        assertEquals(StarLocationCatalog.allNames.toSet(), StarTravelCatalog.guides.keys)
        assertTrue(StarTravelCatalog.guides.values.all { it.routes.isNotEmpty() })
        assertTrue(
            StarTravelCatalog.guides.values
                .flatMap { it.routes }
                .all { it.method.isNotBlank() && it.steps.isNotBlank() },
        )
    }

    @Test
    fun everyRouteSourceMapsToTeleportConfiguration() {
        val configuredIds = TeleportCatalog.all.map { it.id }.toSet()
        val aliases = setOf(
            "always",
            "network_fairy",
            "network_spirit",
            "network_quetzal",
            "network_obelisk",
        )
        val unknown = StarTravelCatalog.guides.values
            .flatMap { it.routes }
            .flatMap { it.sourceKeys }
            .filterNot {
                it in configuredIds ||
                    it in aliases ||
                    it.startsWith("house:") ||
                    it.startsWith("custom:")
            }
            .toSet()

        assertTrue("Unknown route source keys: $unknown", unknown.isEmpty())
    }

    @Test
    fun configuredRouteRanksAheadOfUnavailableWikiOption() {
        val data = FeatureData(
            teleportProfile = TeleportCapabilityProfile(
                capabilities = setOf("item_minigame"),
            ),
        )
        val guide = requireNotNull(StarTravelCatalog.guideFor("Rimmington mine"))
        val ranked = StarTravelPlanner.rank(guide, data)

        assertEquals("Minigame teleport", ranked.first().route.method)
        assertTrue(ranked.first().available)
        assertFalse(ranked.last().available)
    }

    @Test
    fun agilityShortcutUsesPublicHiscoreLevel() {
        fun data(agility: Int) = FeatureData(
            accounts = listOf(
                AccountProfile(
                    username = "Player",
                    snapshots = listOf(
                        StatSnapshot(
                            1,
                            HiscoreSummary(
                                "Player",
                                listOf(
                                    SkillScore("Magic", 1, 99, 13_034_431),
                                    SkillScore("Agility", 1, agility, 0),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            selectedAccount = "Player",
            teleportProfile = TeleportCapabilityProfile(
                spellbooks = setOf(Spellbook.STANDARD),
            ),
        )
        val guide = requireNotNull(StarTravelCatalog.guideFor("West Falador mine"))

        assertFalse(
            StarTravelPlanner.rank(guide, data(4))
                .first { it.route.method == "Falador Teleport + wall shortcut" }
                .available,
        )
        assertTrue(
            StarTravelPlanner.rank(guide, data(5))
                .first { it.route.method == "Falador Teleport + wall shortcut" }
                .available,
        )
    }

    @Test
    fun allWildernessRoutesCarryDangerWarning() {
        val wildernessNames = StarLocationCatalog.areas.getValue("Wilderness")
        assertTrue(
            wildernessNames
                .flatMap { StarTravelCatalog.guides.getValue(it).routes }
                .all { it.dangerous },
        )
    }
}

