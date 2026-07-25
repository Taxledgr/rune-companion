package io.github.taxledgr.runecompanion.personalization

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class PersonalizationPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): PersonalizationSettings {
        val raw = preferences.getString(KEY_SETTINGS, null)
            ?: return PersonalizationSettings()
        return runCatching {
            val root = JSONObject(raw)
            PersonalizationSettings(
                startTab = root.enumOrDefault("startTab", AppTab.STARS),
                startFeatureId = root.optString("startFeatureId")
                    .takeIf(String::isNotBlank),
                navigationTabs = root.optJSONArray("navigationTabs")
                    .enumList<AppTab>()
                    .ifEmpty { AppTab.entries },
                pinnedFeatureIds = root.optJSONArray("pinnedFeatureIds").stringList(),
                customizationMode = root.enumOrDefault(
                    "customizationMode",
                    CustomizationMode.BASIC,
                ),
                appDensity = root.enumOrDefault(
                    "appDensity",
                    LayoutDensity.COMFORTABLE,
                ),
                overlayDensity = root.enumOrDefault(
                    "overlayDensity",
                    LayoutDensity.COMFORTABLE,
                ),
            ).normalized()
        }.getOrDefault(PersonalizationSettings())
    }

    fun save(settings: PersonalizationSettings) {
        val normalized = settings.normalized()
        val root = JSONObject().apply {
            put("schemaVersion", CURRENT_SCHEMA_VERSION)
            put("startTab", normalized.startTab.name)
            put("startFeatureId", normalized.startFeatureId ?: "")
            put("navigationTabs", JSONArray(normalized.navigationTabs.map(AppTab::name)))
            put("pinnedFeatureIds", JSONArray(normalized.pinnedFeatureIds))
            put("customizationMode", normalized.customizationMode.name)
            put("appDensity", normalized.appDensity.name)
            put("overlayDensity", normalized.overlayDensity.name)
        }
        preferences.edit().putString(KEY_SETTINGS, root.toString()).apply()
    }

    private inline fun <reified T : Enum<T>> JSONObject.enumOrDefault(
        key: String,
        default: T,
    ): T = enumValues<T>().firstOrNull { it.name == optString(key) } ?: default

    private inline fun <reified T : Enum<T>> JSONArray?.enumList(): List<T> =
        this?.stringList()?.mapNotNull { name ->
            enumValues<T>().firstOrNull { it.name == name }
        } ?: emptyList()

    private fun JSONArray?.stringList(): List<String> = buildList {
        val array = this@stringList ?: return@buildList
        for (index in 0 until array.length()) {
            array.optString(index).takeIf(String::isNotBlank)?.let(::add)
        }
    }

    companion object {
        const val PREFERENCES_NAME = "rune_companion_personalization"
        private const val KEY_SETTINGS = "settings"
        private const val CURRENT_SCHEMA_VERSION = 2
    }
}
