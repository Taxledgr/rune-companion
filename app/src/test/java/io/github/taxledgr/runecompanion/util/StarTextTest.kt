package io.github.taxledgr.runecompanion.util

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class StarTextTest {
    private val now = Instant.parse("2026-01-01T12:00:00Z")

    @Test
    fun `formats recent reports`() {
        assertEquals("just now", reportAge(now.minusSeconds(20), now))
        assertEquals("1 min ago", reportAge(now.minusSeconds(60), now))
        assertEquals("12 mins ago", reportAge(now.minusSeconds(12 * 60), now))
        assertEquals("2 hrs ago", reportAge(now.minusSeconds(2 * 60 * 60), now))
    }
}
