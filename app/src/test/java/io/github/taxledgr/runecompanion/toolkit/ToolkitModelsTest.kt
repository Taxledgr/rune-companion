package io.github.taxledgr.runecompanion.toolkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolkitModelsTest {
    @Test
    fun `xp formula matches well known OSRS levels`() {
        assertEquals(0, xpForLevel(1))
        assertEquals(83, xpForLevel(2))
        assertEquals(13_034_431, xpForLevel(99))
    }

    @Test
    fun `drop calculator returns cumulative probability`() {
        assertEquals(0.0, dropChancePercent(0, 5_000), 0.0)
        assertTrue(dropChancePercent(5_000, 5_000) in 63.1..63.3)
        assertTrue(attemptsForChance(50.0, 5_000) in 3_465..3_467)
    }
}
