package io.github.taxledgr.runecompanion.overlay

import io.github.taxledgr.runecompanion.util.TrustedUrlPolicy

internal enum class OverlayLinkTarget {
    IN_OVERLAY_WIKI,
    EXTERNAL_BROWSER,
}

internal data class OverlayLinkRoute(
    val url: String,
    val target: OverlayLinkTarget,
)

internal fun routeOverlayLink(url: String): OverlayLinkRoute? {
    val trustedUrl = TrustedUrlPolicy.normalizeHttpsUrl(
        url,
        TrustedUrlPolicy.externalHosts,
    ) ?: return null
    val target = if (TrustedUrlPolicy.isWikiUrl(trustedUrl)) {
        OverlayLinkTarget.IN_OVERLAY_WIKI
    } else {
        OverlayLinkTarget.EXTERNAL_BROWSER
    }
    return OverlayLinkRoute(url = trustedUrl, target = target)
}
