package io.github.taxledgr.runecompanion.data

import io.github.taxledgr.runecompanion.util.AppUserAgent
import io.github.taxledgr.runecompanion.util.openTrustedHttpsConnection
import io.github.taxledgr.runecompanion.util.readUtf8Response
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WorldInfo(
    val world: Int,
    val members: Boolean,
    val region: String,
    val activity: String,
) {
    val dangerous: Boolean
        get() = isDangerousWorldActivity(activity)
}

fun isDangerousWorldActivity(activity: String): Boolean {
    val normalized = activity.lowercase()
    return DANGEROUS_WORLD_LABELS.any(normalized::contains)
}

class WorldDirectoryClient {
    suspend fun fetch(): List<WorldInfo> = withContext(Dispatchers.IO) {
        val connection = openTrustedHttpsConnection(
            ENDPOINT,
            setOf(ENDPOINT_HOST),
        )
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.setRequestProperty(
                "User-Agent",
                AppUserAgent.value,
            )
            if (connection.responseCode !in 200..299) {
                throw IOException("Official world list returned HTTP ${connection.responseCode}")
            }
            parse(connection.readUtf8Response(MAX_RESPONSE_BYTES))
        } finally {
            connection.disconnect()
        }
    }

    internal fun parse(html: String): List<WorldInfo> =
        ROW_REGEX.findAll(html).mapNotNull { match ->
            val world = match.groups["world"]?.value?.toIntOrNull() ?: return@mapNotNull null
            val type = match.groups["type"]?.value ?: return@mapNotNull null
            val country = match.groups["country"]?.value.orEmpty()
            val activity = match.groups["activity"]?.value
                .orEmpty()
                .replace(Regex("<[^>]+>"), "")
                .replace("&amp;", "&")
                .trim()
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
                activity = activity,
            )
        }.distinctBy(WorldInfo::world).toList()

    private companion object {
        const val ENDPOINT_HOST = "oldschool.runescape.com"
        const val ENDPOINT = "https://oldschool.runescape.com/slu?order=wlmAp"
        const val MAX_RESPONSE_BYTES = 2 * 1_024 * 1_024
        val ROW_REGEX = Regex(
            """<tr class='server-list__row[^']*'>.*?id='slu-world-(?<world>\d+)'.*?server-list__row-cell--country\s+server-list__row-cell--(?<country>[A-Z]+)'.*?server-list__row-cell--type'>(?<type>Members|Free)</td>\s*<td class='server-list__row-cell'>(?<activity>.*?)</td>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
        )
    }
}

private val DANGEROUS_WORLD_LABELS = listOf(
    "pvp",
    "bounty hunter",
    "high risk",
    "wilderness pk",
    "deadman",
    "targeting",
    "dmm",
)
