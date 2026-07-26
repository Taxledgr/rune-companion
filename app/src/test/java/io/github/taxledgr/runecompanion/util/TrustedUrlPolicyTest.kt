package io.github.taxledgr.runecompanion.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedUrlPolicyTest {
    @Test
    fun wikiPolicyRequiresExactHttpsOrigin() {
        assertTrue(
            TrustedUrlPolicy.isWikiUrl(
                "https://oldschool.runescape.wiki/w/Shooting_Stars",
            ),
        )
        assertFalse(
            TrustedUrlPolicy.isWikiUrl(
                "http://oldschool.runescape.wiki/w/Shooting_Stars",
            ),
        )
        assertFalse(
            TrustedUrlPolicy.isWikiUrl(
                "https://evil.oldschool.runescape.wiki/w/Shooting_Stars",
            ),
        )
        assertFalse(
            TrustedUrlPolicy.isWikiUrl(
                "https://oldschool.runescape.wiki.attacker.example/",
            ),
        )
    }

    @Test
    fun normalizationRejectsCredentialsPortsAndControlCharacters() {
        val hosts = setOf(TrustedUrlPolicy.WIKI_HOST)
        assertNull(
            TrustedUrlPolicy.normalizeHttpsUrl(
                "https://user@oldschool.runescape.wiki/w/Main_Page",
                hosts,
            ),
        )
        assertNull(
            TrustedUrlPolicy.normalizeHttpsUrl(
                "https://oldschool.runescape.wiki:444/w/Main_Page",
                hosts,
            ),
        )
        assertNull(
            TrustedUrlPolicy.normalizeHttpsUrl(
                "https://oldschool.runescape.wiki/\nmalicious",
                hosts,
            ),
        )
    }

    @Test
    fun normalizationPreservesAnAllowedEncodedUrl() {
        val url = "https://map.starminers.site/?location=East%20Falador"
        assertEquals(
            url,
            TrustedUrlPolicy.normalizeHttpsUrl(
                url,
                setOf(TrustedUrlPolicy.STAR_MINERS_HOST),
            ),
        )
    }
}
