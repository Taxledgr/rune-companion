package io.github.taxledgr.runecompanion.toolkit

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class PriceClient {
    @Volatile
    private var mappingCache: List<PriceSearchItem>? = null

    suspend fun search(query: String): List<PriceSearchItem> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()
        val mapping = mappingCache ?: fetchMapping().also { mappingCache = it }
        mapping.asSequence()
            .filter { it.name.contains(cleanQuery, ignoreCase = true) }
            .sortedWith(
                compareBy<PriceSearchItem> {
                    !it.name.startsWith(cleanQuery, ignoreCase = true)
                }.thenBy { it.name.length },
            )
            .take(MAX_SEARCH_RESULTS)
            .toList()
    }

    suspend fun latest(items: List<PriceWatchItem>): List<PriceWatchItem> =
        withContext(Dispatchers.IO) {
            if (items.isEmpty()) return@withContext emptyList()
            val response = request("$BASE_URL/latest")
            val data = JSONObject(response).getJSONObject("data")
            items.map { item ->
                val price = data.optJSONObject(item.id.toString()) ?: return@map item
                item.copy(
                    high = price.optLongOrNull("high"),
                    low = price.optLongOrNull("low"),
                    updatedAtEpochSeconds = maxOf(
                        price.optLong("highTime"),
                        price.optLong("lowTime"),
                    ).takeIf { it > 0 },
                )
            }
        }

    private fun fetchMapping(): List<PriceSearchItem> {
        val array = JSONArray(request("$BASE_URL/mapping"))
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    PriceSearchItem(
                        id = item.getInt("id"),
                        name = item.getString("name"),
                        members = item.optBoolean("members"),
                    ),
                )
            }
        }
    }

    private fun request(endpoint: String): String {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(
                "User-Agent",
                "Rune Companion/0.3 (github.com/Taxledgr/rune-companion)",
            )
            if (connection.responseCode !in 200..299) {
                throw IOException("OSRS Wiki prices returned HTTP ${connection.responseCode}")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)

    private companion object {
        const val BASE_URL = "https://prices.runescape.wiki/api/v1/osrs"
        const val MAX_SEARCH_RESULTS = 8
    }
}
