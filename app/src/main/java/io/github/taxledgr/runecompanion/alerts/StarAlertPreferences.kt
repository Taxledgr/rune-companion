package io.github.taxledgr.runecompanion.alerts

import android.content.Context

class StarAlertPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): StarAlertSettings = StarAlertSettings(
        enabled = preferences.getBoolean(KEY_ENABLED, false),
        worlds = readIntSet(KEY_WORLDS),
        tiers = readIntSet(KEY_TIERS),
        locations = preferences.getStringSet(KEY_LOCATIONS, emptySet()).orEmpty(),
        quietHoursEnabled = preferences.getBoolean(KEY_QUIET_ENABLED, false),
        quietStartHour = preferences.getInt(KEY_QUIET_START, 22),
        quietEndHour = preferences.getInt(KEY_QUIET_END, 7),
    )

    fun save(settings: StarAlertSettings) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putStringSet(KEY_WORLDS, settings.worlds.map(Int::toString).toSet())
            .putStringSet(KEY_TIERS, settings.tiers.map(Int::toString).toSet())
            .putStringSet(KEY_LOCATIONS, settings.locations)
            .putBoolean(KEY_QUIET_ENABLED, settings.quietHoursEnabled)
            .putInt(KEY_QUIET_START, settings.quietStartHour)
            .putInt(KEY_QUIET_END, settings.quietEndHour)
            .apply()
    }

    fun readSeenIds(): Set<String> =
        preferences.getStringSet(KEY_SEEN_STAR_IDS, emptySet()).orEmpty().toSet()

    fun saveSeenIds(ids: Set<String>) {
        preferences.edit().putStringSet(KEY_SEEN_STAR_IDS, ids.toSet()).apply()
    }

    fun clearSeenIds() {
        preferences.edit().remove(KEY_SEEN_STAR_IDS).apply()
    }

    private fun readIntSet(key: String): Set<Int> =
        preferences.getStringSet(key, emptySet())
            .orEmpty()
            .mapNotNull(String::toIntOrNull)
            .toSet()

    private companion object {
        const val PREFERENCES_NAME = "shooting_star_alerts"
        const val KEY_ENABLED = "enabled"
        const val KEY_WORLDS = "worlds"
        const val KEY_TIERS = "tiers"
        const val KEY_LOCATIONS = "locations"
        const val KEY_QUIET_ENABLED = "quiet_enabled"
        const val KEY_QUIET_START = "quiet_start"
        const val KEY_QUIET_END = "quiet_end"
        const val KEY_SEEN_STAR_IDS = "seen_star_ids"
    }
}
