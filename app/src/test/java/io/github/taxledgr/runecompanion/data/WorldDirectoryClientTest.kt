package io.github.taxledgr.runecompanion.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldDirectoryClientTest {
    @Test
    fun `parses member status and region from official world rows`() {
        val html = """
            <tr class='server-list__row server-list__row--members'>
              <a id='slu-world-302'>Old School 2</a>
              <td class='server-list__row-cell server-list__row-cell--country server-list__row-cell--GB'>United Kingdom</td>
              <td class='server-list__row-cell server-list__row-cell--type'>Members</td>
              <td class='server-list__row-cell'>Bounty Hunter World</td>
            </tr>
            <tr class='server-list__row'>
              <a id='slu-world-301'>Old School 1</a>
              <td class='server-list__row-cell server-list__row-cell--country server-list__row-cell--US'>United States</td>
              <td class='server-list__row-cell server-list__row-cell--type'>Free</td>
              <td class='server-list__row-cell'>Trade - Free</td>
            </tr>
        """.trimIndent()

        val worlds = WorldDirectoryClient().parse(html)

        assertEquals(2, worlds.size)
        assertTrue(worlds.first { it.world == 302 }.members)
        assertEquals("United Kingdom", worlds.first { it.world == 302 }.region)
        assertTrue(worlds.first { it.world == 302 }.dangerous)
        assertFalse(worlds.first { it.world == 301 }.members)
        assertFalse(worlds.first { it.world == 301 }.dangerous)
    }

    @Test
    fun `danger classifier covers pvp bounty hunter high risk and deadman`() {
        assertTrue(isDangerousWorldActivity("PvP World"))
        assertTrue(isDangerousWorldActivity("Bounty Hunter World"))
        assertTrue(isDangerousWorldActivity("High Risk World"))
        assertTrue(isDangerousWorldActivity("Wilderness PK - Free"))
        assertTrue(isDangerousWorldActivity("Deadman"))
        assertFalse(isDangerousWorldActivity("Guardians of the Rift"))
    }
}
