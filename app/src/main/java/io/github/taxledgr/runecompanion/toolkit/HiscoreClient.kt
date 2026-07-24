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
                "Rune Companion/0.5 (github.com/Taxledgr/rune-companion)",
            )
            when (connection.responseCode) {
                HttpURLConnection.HTTP_NOT_FOUND ->
                    throw FileNotFoundException("Player not found on the official hiscores")
                !in 200..299 ->
                    throw IOException("Official hiscores returned HTTP ${connection.responseCode}")
            }
            val lines = connection.inputStream.bufferedReader().use { it.readLines() }
            val skills = SKILL_NAMES.mapIndexedNotNull { index, name ->
                val values = lines.getOrNull(index)?.split(",") ?: return@mapIndexedNotNull null
                if (values.size < 3) return@mapIndexedNotNull null
                SkillScore(
                    name = name,
                    rank = values[0].toIntOrNull() ?: -1,
                    level = values[1].toIntOrNull() ?: -1,
                    xp = values[2].toLongOrNull() ?: -1,
                )
            }
            HiscoreSummary(player = cleanPlayer, skills = skills)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val ENDPOINT =
            "https://secure.runescape.com/m=hiscore_oldschool/index_lite.ws"
        val SKILL_NAMES = listOf(
            "Overall",
            "Attack",
            "Defence",
            "Strength",
            "Hitpoints",
            "Ranged",
            "Prayer",
            "Magic",
            "Cooking",
            "Woodcutting",
            "Fletching",
            "Fishing",
            "Firemaking",
            "Crafting",
            "Smithing",
            "Mining",
            "Herblore",
            "Agility",
            "Thieving",
            "Slayer",
            "Farming",
            "Runecraft",
            "Hunter",
            "Construction",
        )
    }
}
