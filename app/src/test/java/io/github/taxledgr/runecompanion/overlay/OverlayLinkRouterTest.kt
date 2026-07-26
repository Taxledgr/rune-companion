package io.github.taxledgr.runecompanion.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverlayLinkRouterTest {
    @Test
    fun osrsWikiLinksStayInsideTheOverlay() {
        val route = routeOverlayLink(
            "https://oldschool.runescape.wiki/w/Waterfall_Quest/Quick_guide",
        )

        assertEquals(OverlayLinkTarget.IN_OVERLAY_WIKI, route?.target)
    }

    @Test
    fun trustedNonWikiLinksStillUseTheExternalBrowser() {
        val route = routeOverlayLink("https://map.starminers.site/")

        assertEquals(OverlayLinkTarget.EXTERNAL_BROWSER, route?.target)
    }

    @Test
    fun untrustedLinksAreBlocked() {
        assertNull(routeOverlayLink("https://oldschool.runescape.wiki.attacker.example/"))
    }
}
