package io.github.taxledgr.runecompanion.features

data class StarTravelRoute(
    val method: String,
    val steps: String,
    val sourceKeys: List<String> = emptyList(),
    val agilityLevel: Int? = null,
    val requirements: List<String> = emptyList(),
    val dangerous: Boolean = false,
)

data class StarTravelGuide(
    val locationName: String,
    val routes: List<StarTravelRoute>,
)

data class RankedStarTravelRoute(
    val route: StarTravelRoute,
    val available: Boolean,
    val missing: List<String>,
    val wikiRank: Int,
)

object StarTravelPlanner {
    fun rank(guide: StarTravelGuide, data: FeatureData): List<RankedStarTravelRoute> {
        val account = data.accounts.firstOrNull {
            it.username.equals(data.selectedAccount, ignoreCase = true)
        }
        val agility = account?.agilityLevel() ?: 1
        return guide.routes.mapIndexed { index, route ->
            val sourceReady = route.sourceKeys.isEmpty() ||
                route.sourceKeys.any { sourceAvailable(it, data, account?.magicLevel() ?: 1) }
            val agilityReady = route.agilityLevel?.let { agility >= it } ?: true
            val missing = buildList {
                if (!sourceReady) add("Not selected in My teleports")
                route.agilityLevel?.takeIf { !agilityReady }?.let {
                    add("$it Agility required (current $agility)")
                }
            }
            RankedStarTravelRoute(
                route = route,
                available = sourceReady && agilityReady,
                missing = missing,
                wikiRank = index,
            )
        }.sortedWith(
            compareByDescending<RankedStarTravelRoute> { it.available }
                .thenBy { it.wikiRank },
        )
    }

    private fun sourceAvailable(
        key: String,
        data: FeatureData,
        magicLevel: Int,
    ): Boolean {
        val profile = data.teleportProfile
        val capabilities = profile.capabilities
        if (key == "always") return true
        if (key.startsWith("house:")) {
            val location = key.substringAfter(":")
            return "item_construction" in capabilities ||
                (
                    "poh_access" in capabilities &&
                        profile.pohLocation.equals(location, ignoreCase = true)
                    )
        }
        when (key) {
            "network_fairy" -> return "item_fairy_ring" in capabilities ||
                "travel_fairy" in capabilities ||
                ("poh_fairy_ring" in capabilities && "poh_access" in capabilities)
            "network_spirit" -> return "item_spirit_tree" in capabilities ||
                "travel_spirit" in capabilities ||
                ("poh_spirit_tree" in capabilities && "poh_access" in capabilities)
            "network_quetzal" -> return "item_quetzal" in capabilities ||
                "travel_quetzal" in capabilities
            "network_obelisk" -> return "poh_obelisk" in capabilities &&
                "poh_access" in capabilities
        }
        if (key.startsWith("custom:")) {
            val query = key.substringAfter(":")
            return data.customTeleports.any {
                it.name.contains(query, ignoreCase = true) ||
                    it.destination.contains(query, ignoreCase = true)
            }
        }
        if (key in capabilities) return true
        if (
            key in setOf("jew_games", "jew_dueling", "jew_combat", "jew_skills", "jew_wealth", "jew_glory") &&
            "poh_jewellery" in capabilities &&
            "poh_access" in capabilities
        ) return true
        if (
            key == "item_xeric" &&
            "poh_xeric" in capabilities &&
            "poh_access" in capabilities
        ) return true
        if (
            key == "jew_digsite" &&
            "poh_digsite" in capabilities &&
            "poh_access" in capabilities
        ) return true

        val option = TeleportCatalog.all.firstOrNull { it.id == key } ?: return false
        if (option.isAvailable(profile, magicLevel)) return true
        if (option.kind == TeleportKind.SPELL) {
            val nexus = TeleportCatalog.nexusDestinations.firstOrNull {
                it.id == "poh_nexus_${option.id}"
            }
            if (nexus?.isAvailable(profile, magicLevel) == true) return true
        }
        return false
    }
}

