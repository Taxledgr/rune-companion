package io.github.taxledgr.runecompanion.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StarMapCatalogTest {
    @Test
    fun `every landing site has one current map point`() {
        assertEquals(82, StarMapCatalog.points.size)
        assertEquals(StarLocationCatalog.allNames.toSet(), StarMapCatalog.points.keys)
        assertTrue(StarMapCatalog.points.values.all { it.region.isNotBlank() })
    }

    @Test
    fun `leaflet coordinates convert to source-image pixels`() {
        val rimmington = requireNotNull(StarMapCatalog.pointFor("Rimmington mine"))

        assertEquals(5_769, rimmington.imageX)
        assertEquals(2_676, rimmington.imageY)
        assertTrue(rimmington.imageX in 0 until StarMapCatalog.MAP_WIDTH)
        assertTrue(rimmington.imageY in 0 until StarMapCatalog.MAP_HEIGHT)
    }

    @Test
    fun `preview is pinned to the selected location and uses the live map image`() {
        val point = requireNotNull(
            StarMapCatalog.pointFor("Fossil Island Volcanic Mine entrance"),
        )
        val html = StarMapCatalog.previewHtml(point)

        assertTrue(html.contains("Fossil Island Volcanic Mine entrance"))
        assertTrue(html.contains("Fossil &amp; Mos Le&#39;Harmless"))
        assertTrue(html.contains(StarMapCatalog.MAP_IMAGE_URL))
        assertTrue(html.contains("id=\"pin\""))
    }
}
