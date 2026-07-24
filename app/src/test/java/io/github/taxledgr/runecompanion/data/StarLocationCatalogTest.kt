package io.github.taxledgr.runecompanion.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StarLocationCatalogTest {
    @Test
    fun `catalog contains every unique Star Miners landing site`() {
        assertEquals(15, StarLocationCatalog.areas.size)
        assertEquals(82, StarLocationCatalog.allNames.size)
        assertEquals(82, StarLocationCatalog.allNames.distinct().size)
    }
}
