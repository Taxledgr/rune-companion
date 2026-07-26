package io.github.taxledgr.runecompanion.alerts

import io.github.taxledgr.runecompanion.data.ShootingStar

data class StarAlertSettings(
    val enabled: Boolean = false,
    val worlds: Set<Int> = emptySet(),
    val tiers: Set<Int> = emptySet(),
    val locations: Set<String> = emptySet(),
    val quietHoursEnabled: Boolean = false,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
) {
    fun matches(star: ShootingStar): Boolean {
        val matchesWorld = worlds.isEmpty() || star.world in worlds
        val matchesTier = tiers.isEmpty() || star.tier in tiers
        val matchesLocation = locations.isEmpty() || locations.any { location ->
            star.locationName.contains(location, ignoreCase = true)
        }
        return matchesWorld && matchesTier && matchesLocation
    }

    fun isQuietAt(hourOfDay: Int): Boolean {
        if (!quietHoursEnabled || quietStartHour == quietEndHour) return false
        return if (quietStartHour < quietEndHour) {
            hourOfDay in quietStartHour until quietEndHour
        } else {
            hourOfDay >= quietStartHour || hourOfDay < quietEndHour
        }
    }
}

fun parseWorlds(input: String): Set<Int> =
    input
        .split(Regex("[,\\s]+"))
        .mapNotNull(String::toIntOrNull)
        .filter { it in 300..999 }
        .toSortedSet()

fun parseLocations(input: String): Set<String> =
    input
        .split(',', '\n')
        .map(String::trim)
        .filter { it.length >= 2 }
        .toSortedSet(String.CASE_INSENSITIVE_ORDER)
