package com.example

import com.example.utils.ExportUtils
import com.example.utils.FormatUtils
import com.example.utils.UpdateManager
import org.junit.Assert.*
import org.junit.Test

class FormatAndCalculationUnitTest {

    @Test
    fun testFormatDuration_zeroMillis() {
        assertEquals("00:00:00", FormatUtils.formatDuration(0L))
    }

    @Test
    fun testFormatDuration_complexTime() {
        // 1 hour, 24 minutes, 18 seconds = 3600000 + 1440000 + 18000 = 5058000 ms
        val millis = 5058000L
        assertEquals("01:24:18", FormatUtils.formatDuration(millis))
    }

    @Test
    fun testParsePauseEvents_empty() {
        val list = ExportUtils.parsePauseEvents("")
        assertTrue(list.isEmpty())
    }

    @Test
    fun testParsePauseEvents_singlePair() {
        val list = ExportUtils.parsePauseEvents("P:1000,R:2500")
        assertEquals(1, list.size)
        assertEquals(1000L, list[0].first)
        assertEquals(2500L, list[0].second)
    }

    @Test
    fun testParsePauseEvents_multiplePairsAndUnresumed() {
        val list = ExportUtils.parsePauseEvents("P:1000,R:2000,P:5000")
        assertEquals(2, list.size)
        assertEquals(1000L, list[0].first)
        assertEquals(2000L, list[0].second)
        assertEquals(5000L, list[1].first)
        assertNull(list[1].second)
    }

    @Test
    fun testCalculateSessionEarnings_withPercentageAndValueDiscounts() {
        val hourlyRate = 100.0
        val durationMillis = 7200000L // 2 hours
        val durationHours = durationMillis.toDouble() / (1000 * 60 * 60)
        val gross = durationHours * hourlyRate // R$ 200,00

        val discountPct = 10.0 // 10%
        val discountVal = 20.0 // R$ 20,00

        val discountPctVal = gross * (discountPct / 100.0) // R$ 20,00
        val net = maxOf(0.0, gross - discountPctVal - discountVal) // 200 - 20 - 20 = 160

        assertEquals(200.0, gross, 0.001)
        assertEquals(160.0, net, 0.001)
    }

    @Test
    fun testCalculateSessionEarnings_discountExceedsGross() {
        val hourlyRate = 50.0
        val durationMillis = 3600000L // 1 hour = R$ 50,00
        val gross = (durationMillis.toDouble() / (1000 * 60 * 60)) * hourlyRate

        val discountVal = 100.0 // R$ 100,00 discount on R$ 50,00
        val net = maxOf(0.0, gross - discountVal)

        // Should never be negative
        assertEquals(0.0, net, 0.001)
    }

    @Test
    fun testUpdateManager_isNewerVersion() {
        // Newer patch
        assertTrue(UpdateManager.isNewerVersion("1.2.0", "1.2.1"))
        // Newer minor
        assertTrue(UpdateManager.isNewerVersion("1.2.0", "1.3.0"))
        // Newer major
        assertTrue(UpdateManager.isNewerVersion("1.2.0", "2.0.0"))
        // Same version
        assertFalse(UpdateManager.isNewerVersion("1.2.0", "1.2.0"))
        // Older version
        assertFalse(UpdateManager.isNewerVersion("1.2.1", "1.2.0"))
        // Shorter format
        assertTrue(UpdateManager.isNewerVersion("1.2", "1.2.1"))
    }
}
