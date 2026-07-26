package io.github.taxledgr.runecompanion.alerts

import android.content.Context
import io.github.taxledgr.runecompanion.data.ShootingStar
import io.github.taxledgr.runecompanion.data.WorldInfo

data class StarFilterSettings(
    val hideDangerousWorlds: Boolean = true,
    val excludedLocations: Set<String> = emptySet(),
    val dangerousWorlds: Set<Int> = emptySet(),
    val worldSafetyLoaded: Boolean = false,
) {
    fun includesLocation(star: ShootingStar): Boolean =
        excludedLocations.none { excluded ->
            star.locationName.startsWith(excluded, ignoreCase = true)
        }

    fun includes(star: ShootingStar): Boolean =
        includesLocation(star) &&
            (
                !hideDangerousWorlds ||
                    (worldSafetyLoaded && star.world !in dangerousWorlds)
                )
}

class StarFilterPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load() = StarFilterSettings(
        hideDangerousWorlds = preferences.getBoolean(KEY_HIDE_DANGEROUS, true),
        excludedLocations = preferences.getStringSet(KEY_EXCLUDED_LOCATIONS, emptySet())
            .orEmpty()
            .toSet(),
        dangerousWorlds = preferences.getStringSet(KEY_DANGEROUS_WORLDS, emptySet())
            .orEmpty()
            .mapNotNull(String::toIntOrNull)
            .toSet(),
        worldSafetyLoaded = preferences.getBoolean(KEY_WORLD_SAFETY_LOADED, false),
    )

    fun save(settings: StarFilterSettings) {
        preferences.edit()
            .putBoolean(KEY_HIDE_DANGEROUS, settings.hideDangerousWorlds)
            .putStringSet(KEY_EXCLUDED_LOCATIONS, settings.excludedLocations.toSet())
            .putStringSet(
                KEY_DANGEROUS_WORLDS,
                settings.dangerousWorlds.map(Int::toString).toSet(),
            )
            .putBoolean(KEY_WORLD_SAFETY_LOADED, settings.worldSafetyLoaded)
            .apply()
    }

    fun updateWorldSafety(worlds: List<WorldInfo>): StarFilterSettings {
        val updated = load().copy(
            dangerousWorlds = worlds
                .filter(WorldInfo::dangerous)
                .map(WorldInfo::world)
                .toSet(),
            worldSafetyLoaded = true,
        )
        save(updated)
        return updated
    }

    private companion object {
        const val PREFERENCES_NAME = "shooting_star_filters"
        const val KEY_HIDE_DANGEROUS = "hide_dangerous_worlds"
        const val KEY_EXCLUDED_LOCATIONS = "excluded_locations"
        const val KEY_DANGEROUS_WORLDS = "dangerous_worlds"
        const val KEY_WORLD_SAFETY_LOADED = "world_safety_loaded"
    }
}
