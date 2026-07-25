package io.github.taxledgr.runecompanion.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URI

object TrustedUrlPolicy {
    const val WIKI_HOST = "oldschool.runescape.wiki"
    const val STAR_MINERS_HOST = "map.starminers.site"

    val externalHosts = setOf(
        WIKI_HOST,
        STAR_MINERS_HOST,
        "oldschool.runescape.com",
        "secure.runescape.com",
        "support.runescape.com",
        "jagex.com",
        "www.jagex.com",
        "github.com",
    )

    fun normalizeHttpsUrl(
        url: String,
        allowedHosts: Set<String>,
    ): String? = runCatching {
        val candidate = url.trim()
        require(candidate.isNotEmpty() && candidate.length <= MAX_URL_LENGTH)
        require(candidate.none(Char::isISOControl))
        val uri = URI(candidate)
        val host = uri.host?.lowercase() ?: error("URL host is missing")
        require(uri.scheme.equals("https", ignoreCase = true))
        require(uri.rawUserInfo == null)
        require(uri.port == -1 || uri.port == 443)
        require(host in allowedHosts)
        uri.toASCIIString()
    }.getOrNull()

    fun isWikiUrl(url: String): Boolean =
        normalizeHttpsUrl(url, setOf(WIKI_HOST)) != null

    private const val MAX_URL_LENGTH = 4_096
}

fun Context.openTrustedExternalUrl(url: String): Boolean {
    val trustedUrl = TrustedUrlPolicy.normalizeHttpsUrl(
        url,
        TrustedUrlPolicy.externalHosts,
    ) ?: return false
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trustedUrl))
        if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
