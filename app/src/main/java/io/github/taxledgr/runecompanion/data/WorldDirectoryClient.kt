package io.github.taxledgr.runecompanion.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WorldInfo(
    val world: Int,
    val members: Boolean,
    val region: String,
)

class WorldDirectoryClient {
    suspend fun fetch(): List<WorldInfo> = withContext(Dispatchers.IO) {
        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.setRequestProperty(
                "User-Agent",
                "Rune Companion/0.3 (github.com/Taxledgr/rune-companion)",
            )
            if (connection.responseCode !in 200..299) {
                throw IOException("Official world list returned HTTP ${connection.responseCode}")
            }
            parse(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    internal fun parse(html: String): List<WorldInfo> =
        ROW_REGEX.findAll(html).mapNotNull { match ->
            val world = match.groups["world"]?.value?.toIntOrNull() ?: return@mapNotNull null
            val type = match.groups["type"]?.value ?: return@mapNotNull null
            val country = match.groups["country"]?.value.orEmpty()
            WorldInfo(
                world = world,
                members = type == "Members",
                region = when (country) {
                    "AU" -> "Australia"
                    "US" -> "United States"
                    "GB" -> "United Kingdom"
                    "DE" -> "Germany"
                    "JP" -> "Japan"
                    "BR" -> "Brazil"
                    else -> "Other"
                },
            )
        }.distinctBy(WorldInfo::world).toList()

    private companion object {
        const val ENDPOINT = "https://oldschool.runescape.com/slu?order=wlmAp"
        val ROW_REGEX = Regex(
            """<tr class='server-list__row[^']*'>.*?id='slu-world-(?<world>\d+)'.*?server-list__row-cell--country\s+server-list__row-cell--(?<country>[A-Z]+)'.*?server-list__row-cell--type'>(?<type>Members|Free)</td>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
        )
    }
}
