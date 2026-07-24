package io.github.taxledgr.runecompanion.toolkit

import java.io.FileNotFoundException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HiscoreClient {
    suspend fun lookup(player: String): HiscoreSummary = withContext(Dispatchers.IO) {
        val cleanPlayer = player.trim()
        require(cleanPlayer.isNotBlank()) { "Enter an OSRS display name" }
        val encoded = URLEncoder.encode(cleanPlayer, StandardCharsets.UTF_8.name())
        val connection = URL("$ENDPOINT?player=$encoded").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.setRequestProperty(
                "User-Agent",
                "Rune Companion/1.3 (github.com/Taxledgr/rune-companion)",
            )
            when (connection.responseCode) {
                HttpURLConnection.HTTP_NOT_FOUND ->
                    throw FileNotFoundException("Player not found on the official hiscores")
                !in 200..299 ->
                    throw IOException("Official hiscores returned HTTP ${connection.responseCode}")
            }
            val lines = connection.inputStream.bufferedReader().use { it.readLines() }
            parseHiscoreLines(cleanPlayer, lines)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val ENDPOINT =
            "https://secure.runescape.com/m=hiscore_oldschool/index_lite.ws"
    }
}

internal fun parseHiscoreLines(player: String, lines: List<String>): HiscoreSummary {
    val skills = HiscoreCatalog.skillNames.mapIndexedNotNull { index, name ->
        val values = lines.getOrNull(index)?.trim()?.split(",")
            ?: return@mapIndexedNotNull null
        if (values.size < 3) return@mapIndexedNotNull null
        SkillScore(
            name = name,
            rank = values[0].toIntOrNull() ?: -1,
            level = values[1].toIntOrNull() ?: -1,
            xp = values[2].toLongOrNull() ?: -1,
        )
    }
    val activities = HiscoreCatalog.activityNames.mapIndexedNotNull { index, name ->
        val values = lines.getOrNull(HiscoreCatalog.skillNames.size + index)
            ?.trim()?.split(",") ?: return@mapIndexedNotNull null
        if (values.size < 2) return@mapIndexedNotNull null
        val score = values[1].toLongOrNull() ?: return@mapIndexedNotNull null
        if (score <= 0) return@mapIndexedNotNull null
        ActivityScore(
            name = name,
            rank = values[0].toIntOrNull() ?: -1,
            score = score,
        )
    }
    return HiscoreSummary(player = player, skills = skills, activities = activities)
}
