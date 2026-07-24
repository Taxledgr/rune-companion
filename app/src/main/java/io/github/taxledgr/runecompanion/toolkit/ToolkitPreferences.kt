package io.github.taxledgr.runecompanion.toolkit

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ToolkitPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): PersistedToolkitData {
        val raw = preferences.getString(KEY_DATA, null) ?: return PersistedToolkitData()
        return runCatching { decode(JSONObject(raw)) }.getOrDefault(PersistedToolkitData())
    }

    fun save(data: PersistedToolkitData) {
        preferences.edit().putString(KEY_DATA, encode(data).toString()).apply()
    }

    private fun encode(data: PersistedToolkitData) = JSONObject().apply {
        put("reminders", JSONArray().apply {
            data.reminders.forEach { reminder ->
                put(JSONObject().apply {
                    put("id", reminder.id)
                    put("title", reminder.title)
                    put("category", reminder.category.name)
                    put("endsAt", reminder.endsAtEpochMillis)
                })
            }
        })
        put("slayer", data.slayerTask?.let { task ->
            JSONObject().apply {
                put("monster", task.monster)
                put("target", task.target)
                put("remaining", task.remaining)
            }
        } ?: JSONObject.NULL)
        put("checklist", JSONArray().apply {
            data.checklist.forEach { entry ->
                put(JSONObject().apply {
                    put("id", entry.id)
                    put("title", entry.title)
                    put("category", entry.category.name)
                    put("completed", entry.completed)
                })
            }
        })
        put("trip", JSONObject().apply {
            put("label", data.tripTimer.label)
            put("startedAt", data.tripTimer.startedAtEpochMillis ?: JSONObject.NULL)
            put("elapsed", data.tripTimer.elapsedBeforeStartMillis)
        })
        put("watchlist", JSONArray().apply {
            data.priceWatchlist.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("high", item.high ?: JSONObject.NULL)
                    put("low", item.low ?: JSONObject.NULL)
                    put("updatedAt", item.updatedAtEpochSeconds ?: JSONObject.NULL)
                })
            }
        })
    }

    private fun decode(root: JSONObject): PersistedToolkitData {
        val reminders = root.optJSONArray("reminders").mapObjects { item ->
            CompanionReminder(
                id = item.getString("id"),
                title = item.getString("title"),
                category = item.enumOrDefault("category", ReminderCategory.CUSTOM),
                endsAtEpochMillis = item.getLong("endsAt"),
            )
        }
        val slayer = root.optJSONObject("slayer")?.let { item ->
            SlayerTask(
                monster = item.getString("monster"),
                target = item.getInt("target"),
                remaining = item.getInt("remaining"),
            )
        }
        val checklist = root.optJSONArray("checklist").mapObjects { item ->
            ChecklistEntry(
                id = item.getString("id"),
                title = item.getString("title"),
                category = item.enumOrDefault("category", ChecklistCategory.QUEST),
                completed = item.optBoolean("completed"),
            )
        }
        val tripObject = root.optJSONObject("trip")
        val trip = if (tripObject == null) {
            TripTimer()
        } else {
            TripTimer(
                label = tripObject.optString("label").ifBlank { "Boss / raid trip" },
                startedAtEpochMillis = tripObject.optLongOrNull("startedAt"),
                elapsedBeforeStartMillis = tripObject.optLong("elapsed"),
            )
        }
        val watchlist = root.optJSONArray("watchlist").mapObjects { item ->
            PriceWatchItem(
                id = item.getInt("id"),
                name = item.getString("name"),
                high = item.optLongOrNull("high"),
                low = item.optLongOrNull("low"),
                updatedAtEpochSeconds = item.optLongOrNull("updatedAt"),
            )
        }
        return PersistedToolkitData(reminders, slayer, checklist, trip, watchlist)
    }

    private inline fun <reified T : Enum<T>> JSONObject.enumOrDefault(
        key: String,
        default: T,
    ): T = enumValues<T>().firstOrNull { it.name == optString(key) } ?: default

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)

    private fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                runCatching { transform(getJSONObject(index)) }.getOrNull()?.let(::add)
            }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "rune_companion_toolkit"
        const val KEY_DATA = "toolkit_data"
    }
}
