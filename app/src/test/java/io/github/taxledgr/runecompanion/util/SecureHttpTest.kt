package io.github.taxledgr.runecompanion.util

import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class SecureHttpTest {
    @Test
    fun boundedReaderAcceptsResponseAtLimit() {
        val input = ByteArrayInputStream("stars".toByteArray())
        assertEquals("stars", input.readUtf8Limited(5))
    }

    @Test(expected = IOException::class)
    fun boundedReaderRejectsOversizedResponse() {
        val input = ByteArrayInputStream("too-large".toByteArray())
        input.readUtf8Limited(4)
    }
}
