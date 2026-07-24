package io.github.taxledgr.runecompanion.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShootingStarJsonParserTest {
    @Test
    fun `parses and sorts Star Miners reports newest first`() {
        val json = """
            [
              {
                "world": 301,
                "location": 12,
                "calledBy": "Scout A",
                "calledLocation": "Falador",
                "calledAt": 1700000000.0,
                "minTime": null,
                "maxTime": null,
                "tier": 4
              },
              {
                "world": 302,
                "location": 15,
                "calledBy": "Scout B",
                "calledLocation": "Fremennik",
                "calledAt": 1700000100.0,
                "minTime": 1700000200.0,
                "maxTime": 1700000300.0,
                "tier": 7
              }
            ]
        """.trimIndent()

        val result = ShootingStarJsonParser.parse(json)

        assertEquals(listOf(302, 301), result.map(ShootingStar::world))
        assertEquals(7, result.first().tier)
        assertEquals("Fremennik", result.first().locationName)
        assertEquals(Instant.ofEpochSecond(1700000200), result.first().minimumArrival)
        assertNull(result.last().minimumArrival)
    }
}
