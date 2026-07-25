package io.github.taxledgr.runecompanion.toolkit

import io.github.taxledgr.runecompanion.features.MarketHistoryPoint
import io.github.taxledgr.runecompanion.util.AppUserAgent
import io.github.taxledgr.runecompanion.util.openTrustedHttpsConnection
import io.github.taxledgr.runecompanion.util.readUtf8Response
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class PriceClient {
    suspend fun search(query: String): List<PriceSearchItem> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()
        val mapping = itemMapping()
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

    private suspend fun itemMapping(): List<PriceSearchItem> =
        mappingCache ?: mappingMutex.withLock {
            mappingCache ?: fetchMapping().also { mappingCache = it }
        }

    suspend fun latest(items: List<PriceWatchItem>): List<PriceWatchItem> =
        withContext(Dispatchers.IO) {
            if (items.isEmpty()) return@withContext emptyList()
            val data = latestPrices()
            items.map { item ->
                val price = data[item.id] ?: return@map item
                item.copy(
                    high = price.high,
                    low = price.low,
                    updatedAtEpochSeconds = price.updatedAtEpochSeconds,
                )
            }
        }

    private suspend fun latestPrices(): Map<Int, LatestPrice> {
        val now = System.currentTimeMillis()
        latestCache
            ?.takeIf { now - it.savedAtEpochMillis < LATEST_CACHE_TTL_MILLIS }
            ?.prices
            ?.let { return it }
        return latestMutex.withLock {
            val lockedNow = System.currentTimeMillis()
            latestCache
                ?.takeIf { lockedNow - it.savedAtEpochMillis < LATEST_CACHE_TTL_MILLIS }
                ?.prices
                ?: fetchLatestPrices().also { prices ->
                    latestCache = CachedLatestPrices(lockedNow, prices)
                }
        }
    }

    private fun fetchLatestPrices(): Map<Int, LatestPrice> {
        val response = request("$BASE_URL/latest")
        val data = JSONObject(response).getJSONObject("data")
        return buildMap {
            data.keys().forEach { idText ->
                val id = idText.toIntOrNull() ?: return@forEach
                val price = data.optJSONObject(idText) ?: return@forEach
                put(
                    id,
                    LatestPrice(
                        high = price.optLongOrNull("high"),
                        low = price.optLongOrNull("low"),
                        updatedAtEpochSeconds = maxOf(
                            price.optLong("highTime"),
                            price.optLong("lowTime"),
                        ).takeIf { it > 0 },
                    ),
                )
            }
        }
    }

    suspend fun history(
        itemId: Int,
        timestep: String = "24h",
    ): List<MarketHistoryPoint> = withContext(Dispatchers.IO) {
        require(itemId > 0) { "Select an item" }
        require(timestep in ALLOWED_TIMESTEPS) { "Unsupported price-history interval" }
        val response = request("$BASE_URL/timeseries?timestep=$timestep&id=$itemId")
        val data = JSONObject(response).optJSONArray("data") ?: JSONArray()
        buildList {
            for (index in 0 until data.length()) {
                val point = data.optJSONObject(index) ?: continue
                add(
                    MarketHistoryPoint(
                        timestampEpochSeconds = point.optLong("timestamp"),
                        averageHigh = point.optLongOrNull("avgHighPrice"),
                        averageLow = point.optLongOrNull("avgLowPrice"),
                        highVolume = point.optLong("highPriceVolume"),
                        lowVolume = point.optLong("lowPriceVolume"),
                    ),
                )
            }
        }.sortedBy(MarketHistoryPoint::timestampEpochSeconds)
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
        val connection = openTrustedHttpsConnection(
            endpoint,
            setOf(BASE_HOST),
        )
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(
                "User-Agent",
                AppUserAgent.value,
            )
            if (connection.responseCode !in 200..299) {
                throw IOException("OSRS Wiki prices returned HTTP ${connection.responseCode}")
            }
            return connection.readUtf8Response(MAX_RESPONSE_BYTES)
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)

    private companion object {
        const val BASE_HOST = "prices.runescape.wiki"
        const val BASE_URL = "https://prices.runescape.wiki/api/v1/osrs"
        const val MAX_RESPONSE_BYTES = 12 * 1_024 * 1_024
        const val MAX_SEARCH_RESULTS = 8
        const val LATEST_CACHE_TTL_MILLIS = 60_000L
        val ALLOWED_TIMESTEPS = setOf("5m", "1h", "6h", "24h")
        val mappingMutex = Mutex()
        val latestMutex = Mutex()
        @Volatile
        var mappingCache: List<PriceSearchItem>? = null
        @Volatile
        var latestCache: CachedLatestPrices? = null
    }
}

private data class LatestPrice(
    val high: Long?,
    val low: Long?,
    val updatedAtEpochSeconds: Long?,
)

private data class CachedLatestPrices(
    val savedAtEpochMillis: Long,
    val prices: Map<Int, LatestPrice>,
)
