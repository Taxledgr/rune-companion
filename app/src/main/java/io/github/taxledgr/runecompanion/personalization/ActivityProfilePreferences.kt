package io.github.taxledgr.runecompanion.personalization

import android.content.Context
import io.github.taxledgr.runecompanion.alerts.StarFilterSettings
import io.github.taxledgr.runecompanion.overlay.OverlayModule
import io.github.taxledgr.runecompanion.overlay.OverlayPlacement
import io.github.taxledgr.runecompanion.overlay.OverlaySettings
import org.json.JSONArray
import org.json.JSONObject

class ActivityProfilePreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(
        legacyPersonalization: PersonalizationSettings,
        legacyOverlay: OverlaySettings,
        selectedAccount: String?,
        legacyFilters: StarFilterSettings,
    ): ActivityProfileState {
        val fallback = {
            ActivityProfileState.fromLegacy(
                personalization = legacyPersonalization,
                overlay = legacyOverlay,
                selectedAccount = selectedAccount,
                filters = legacyFilters,
            )
        }
        val raw = preferences.getString(KEY_STATE, null)
        val state = raw?.let { encoded ->
            runCatching { decode(JSONObject(encoded)) }.getOrNull()
        } ?: fallback()
        val normalized = state.normalized()
        if (raw == null || normalized != state) save(normalized)
        return normalized
    }

    fun save(state: ActivityProfileState) {
        preferences.edit()
            .putString(KEY_STATE, encode(state.normalized()).toString())
            .apply()
    }

    private fun encode(state: ActivityProfileState) = JSONObject().apply {
        put("schemaVersion", CURRENT_SCHEMA_VERSION)
        put("activeProfileId", state.activeProfileId)
        put("profiles", JSONArray().apply {
            state.profiles.forEach { profile ->
                put(JSONObject().apply {
                    put("id", profile.id)
                    put("name", profile.name)
                    put("symbol", profile.symbol)
                    put("selectedAccount", profile.selectedAccount ?: JSONObject.NULL)
                    put("personalization", profile.personalization.toJson())
                    put("overlay", profile.overlay.toJson())
                    put("starFilters", JSONObject().apply {
                        put("hideDangerousWorlds", profile.starFilters.hideDangerousWorlds)
                        put(
                            "excludedLocations",
                            JSONArray(profile.starFilters.excludedLocations.toList()),
                        )
                    })
                })
            }
        })
    }

    private fun decode(root: JSONObject): ActivityProfileState {
        val schemaVersion = root.optInt("schemaVersion", 1)
        require(schemaVersion in 1..CURRENT_SCHEMA_VERSION)
        val profiles = buildList {
            val array = root.optJSONArray("profiles") ?: JSONArray()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val personalization = item.optJSONObject("personalization")
                    ?.toPersonalizationSettings()
                    ?: PersonalizationSettings()
                val overlay = item.optJSONObject("overlay")
                    ?.toOverlaySettings()
                    ?: OverlaySettings()
                val filters = item.optJSONObject("starFilters")
                add(
                    ActivityProfile(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        symbol = item.optString("symbol"),
                        personalization = personalization,
                        overlay = overlay,
                        selectedAccount = if (item.isNull("selectedAccount")) {
                            null
                        } else {
                            item.optString("selectedAccount").takeIf(String::isNotBlank)
                        },
                        starFilters = ActivityStarFilters(
                            hideDangerousWorlds =
                                filters?.optBoolean("hideDangerousWorlds", true) ?: true,
                            excludedLocations =
                                filters?.optJSONArray("excludedLocations").stringList().toSet(),
                        ),
                    ),
                )
            }
        }
        return ActivityProfileState(
            profiles = profiles,
            activeProfileId = root.optString("activeProfileId"),
        ).normalized()
    }

    private fun PersonalizationSettings.toJson() = JSONObject().apply {
        put("startTab", startTab.name)
        put("startFeatureId", startFeatureId ?: "")
        put("navigationTabs", JSONArray(navigationTabs.map(AppTab::name)))
        put("pinnedFeatureIds", JSONArray(pinnedFeatureIds))
        put("customizationMode", customizationMode.name)
        put("appDensity", appDensity.name)
        put("overlayDensity", overlayDensity.name)
    }

    private fun JSONObject.toPersonalizationSettings() = PersonalizationSettings(
        startTab = enumValue(optString("startTab"), AppTab.STARS),
        startFeatureId = optString("startFeatureId").takeIf(String::isNotBlank),
        navigationTabs = optJSONArray("navigationTabs").stringList()
            .mapNotNull { name -> AppTab.entries.firstOrNull { it.name == name } },
        pinnedFeatureIds = optJSONArray("pinnedFeatureIds").stringList(),
        customizationMode = enumValue(
            optString("customizationMode"),
            CustomizationMode.BASIC,
        ),
        appDensity = enumValue(
            optString("appDensity"),
            LayoutDensity.COMFORTABLE,
        ),
        overlayDensity = enumValue(
            optString("overlayDensity"),
            LayoutDensity.COMFORTABLE,
        ),
    ).normalized()

    private fun OverlaySettings.toJson() = JSONObject().apply {
        put("enabledModules", JSONArray(enabledModules.map(OverlayModule::name)))
        put("selectedModule", selectedModule.name)
        put("moduleOrder", JSONArray(moduleOrder.map(OverlayModule::name)))
        put("compactWidthDp", compactWidthDp)
        put("landscapeWidthDp", landscapeWidthDp)
        put("opacityPercent", opacityPercent)
        put("textScalePercent", textScalePercent)
        put("snapToEdge", snapToEdge)
        put("avoidGameControls", avoidGameControls)
        put("portraitPlacement", portraitPlacement.toJson())
        put("landscapePlacement", landscapePlacement.toJson())
    }

    private fun JSONObject.toOverlaySettings() = OverlaySettings(
        enabledModules = optJSONArray("enabledModules").stringList()
            .mapNotNull { name -> OverlayModule.entries.firstOrNull { it.name == name } }
            .toSet(),
        selectedModule = enumValue(
            optString("selectedModule"),
            OverlayModule.STARS,
        ),
        moduleOrder = optJSONArray("moduleOrder").stringList()
            .mapNotNull { name -> OverlayModule.entries.firstOrNull { it.name == name } },
        compactWidthDp = optInt(
            "compactWidthDp",
            OverlaySettings.DEFAULT_COMPACT_WIDTH_DP,
        ),
        landscapeWidthDp = optInt(
            "landscapeWidthDp",
            OverlaySettings.DEFAULT_LANDSCAPE_WIDTH_DP,
        ),
        opacityPercent = optInt(
            "opacityPercent",
            OverlaySettings.DEFAULT_OPACITY_PERCENT,
        ),
        textScalePercent = optInt(
            "textScalePercent",
            OverlaySettings.DEFAULT_TEXT_SCALE_PERCENT,
        ),
        snapToEdge = optBoolean("snapToEdge", true),
        avoidGameControls = optBoolean("avoidGameControls", true),
        portraitPlacement = optJSONObject("portraitPlacement")?.toPlacement()
            ?: OverlayPlacement(),
        landscapePlacement = optJSONObject("landscapePlacement")?.toPlacement()
            ?: OverlayPlacement(),
    ).normalized()

    private fun OverlayPlacement.toJson() = JSONObject().apply {
        put("panelXFraction", panelXFraction.toDouble())
        put("panelYFraction", panelYFraction.toDouble())
        put("bubbleXFraction", bubbleXFraction.toDouble())
        put("bubbleYFraction", bubbleYFraction.toDouble())
    }

    private fun JSONObject.toPlacement() = OverlayPlacement(
        panelXFraction = optDouble("panelXFraction", 0.04).toFloat(),
        panelYFraction = optDouble("panelYFraction", 0.12).toFloat(),
        bubbleXFraction = optDouble("bubbleXFraction", 0.96).toFloat(),
        bubbleYFraction = optDouble("bubbleYFraction", 0.42).toFloat(),
    ).normalized()

    private fun JSONArray?.stringList(): List<String> = buildList {
        val array = this@stringList ?: return@buildList
        for (index in 0 until array.length()) {
            array.optString(index).takeIf(String::isNotBlank)?.let(::add)
        }
    }

    private inline fun <reified T : Enum<T>> enumValue(name: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: fallback

    companion object {
        const val PREFERENCES_NAME = "rune_companion_activity_profiles"
        private const val KEY_STATE = "state"
        private const val CURRENT_SCHEMA_VERSION = 2
    }
}
