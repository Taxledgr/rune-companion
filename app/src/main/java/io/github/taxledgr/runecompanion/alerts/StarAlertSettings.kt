package io.github.taxledgr.runecompanion.alerts

import io.github.taxledgr.runecompanion.data.ShootingStar

data class StarAlertSettings(
    val enabled: Boolean = false,
    val worlds: Set<Int> = emptySet(),
    val tiers: Set<Int> = emptySet(),
) {
    fun matches(star: ShootingStar): Boolean {
        val matchesWorld = worlds.isEmpty() || star.world in worlds
        val matchesTier = tiers.isEmpty() || star.tier in tiers
        return matchesWorld && matchesTier
    }
}

fun parseWorlds(input: String): Set<Int> =
    input
        .split(Regex("[,\\s]+"))
        .mapNotNull(String::toIntOrNull)
        .filter { it in 300..999 }
        .toSortedSet()
