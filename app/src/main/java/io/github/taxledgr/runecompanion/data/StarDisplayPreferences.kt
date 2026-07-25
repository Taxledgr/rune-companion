package io.github.taxledgr.runecompanion.data

import android.content.Context

data class StarDisplaySettings(
    val expandedStarKey: String? = null,
    val preferredRoutes: Map<String, String> = emptyMap(),
)

class StarDisplayPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): StarDisplaySettings = StarDisplaySettings(
        expandedStarKey = preferences.getString(KEY_EXPANDED_STAR, null),
        preferredRoutes = preferences.getStringSet(KEY_PREFERRED_ROUTES, emptySet())
            .orEmpty()
            .mapNotNull { encoded ->
                val separator = encoded.indexOf(SEPARATOR)
                if (separator <= 0 || separator == encoded.lastIndex) {
                    null
                } else {
                    encoded.substring(0, separator) to encoded.substring(separator + 1)
                }
            }
            .toMap(),
    )

    fun save(settings: StarDisplaySettings) {
        preferences.edit()
            .putString(KEY_EXPANDED_STAR, settings.expandedStarKey)
            .putStringSet(
                KEY_PREFERRED_ROUTES,
                settings.preferredRoutes.mapTo(mutableSetOf()) { (location, route) ->
                    "$location$SEPARATOR$route"
                },
            )
            .apply()
    }

    companion object {
        const val PREFERENCES_NAME = "rune_companion_star_display"
        private const val KEY_EXPANDED_STAR = "expanded_star_key"
        private const val KEY_PREFERRED_ROUTES = "preferred_routes"
        private const val SEPARATOR = '\u001F'
    }
}
