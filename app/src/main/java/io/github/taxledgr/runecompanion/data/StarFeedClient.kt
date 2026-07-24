package io.github.taxledgr.runecompanion.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class StarFeedClient(
    private val endpoint: String = "https://map.starminers.site/data2",
    private val now: () -> Instant = Instant::now,
) {
    suspend fun fetch(): StarFeed = withContext(Dispatchers.IO) {
        val requestUrl = URL("$endpoint?timestamp=${System.currentTimeMillis()}")
        val connection = requestUrl.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(
                "User-Agent",
                "Rune Companion/1.2 (github.com/Taxledgr/rune-companion)",
            )

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("Star Miners returned HTTP $responseCode")
            }

            val json = connection.inputStream.bufferedReader().use { it.readText() }
            StarFeed(
                stars = ShootingStarJsonParser.parse(json),
                fetchedAt = now(),
            )
        } finally {
            connection.disconnect()
        }
    }
}

object ShootingStarJsonParser {
    fun parse(json: String): List<ShootingStar> {
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                add(array.getJSONObject(index).toShootingStar())
            }
        }.sortedByDescending(ShootingStar::calledAt)
    }

    private fun JSONObject.toShootingStar() = ShootingStar(
        world = getInt("world"),
        locationId = getInt("location"),
        calledBy = optString("calledBy").ifBlank { "Unknown scout" },
        locationName = optString("calledLocation").ifBlank { "Unknown location" },
        calledAt = Instant.ofEpochSecond(getDouble("calledAt").toLong()),
        minimumArrival = epochSecondsOrNull("minTime"),
        maximumArrival = epochSecondsOrNull("maxTime"),
        tier = optInt("tier", 0),
    )

    private fun JSONObject.epochSecondsOrNull(key: String): Instant? {
        if (isNull(key)) return null
        val seconds = optDouble(key, Double.NaN)
        return if (seconds.isNaN()) null else Instant.ofEpochSecond(seconds.toLong())
    }
}
