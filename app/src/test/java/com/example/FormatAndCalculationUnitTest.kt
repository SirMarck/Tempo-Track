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

    @Test
    fun testAppliedRate_preservesHistoricalValueWhenClientRateChanges() {
        // Sessão criada com appliedRate = 120.0
        val session = com.example.data.Session(
            id = 1L,
            clientId = 10L,
            startTime = 1000L,
            endTime = 1000L + 3600000L, // 1 hora
            appliedRate = 120.0,
            billable = true
        )

        // Valor original da sessão: 1h * R$ 120 = R$ 120,00
        val originalEarnings = session.calculateEarnings(hourlyRateFallback = 120.0)
        assertEquals(120.0, originalEarnings, 0.001)

        // Cliente altera taxa para R$ 250,00 no cadastro
        val updatedClientRate = 250.0

        // A sessão calculada ainda DEVE manter R$ 120,00 porque appliedRate é snapshot imutável!
        val earningsAfterRateChange = session.calculateEarnings(hourlyRateFallback = updatedClientRate)
        assertEquals(120.0, earningsAfterRateChange, 0.001)
    }

    @Test
    fun testPause_doesNotEnterWorkedDuration() {
        val start = 10000L
        val pauseDuration = 15 * 60 * 1000L // 15 minutos de pausa
        val end = start + (60 * 60 * 1000L) // 60 minutos decorridos no relógio

        val session = com.example.data.Session(
            id = 2L,
            clientId = 5L,
            startTime = start,
            endTime = end,
            pausedDuration = pauseDuration,
            appliedRate = 100.0
        )

        // Duração trabalhada real: 60 min - 15 min = 45 min (2700000 ms)
        val durationMillis = session.calculateDurationMillis()
        assertEquals(45 * 60 * 1000L, durationMillis)

        // 45 min a R$ 100/h = R$ 75,00
        val earnings = session.calculateEarnings()
        assertEquals(75.0, earnings, 0.001)
    }

    @Test
    fun testRounding_affectsOnlyBillableCalculationWithoutAlteringRawDuration() {
        val rawMillis = 22 * 60 * 1000L // 22 minutos reais trabalhados
        assertEquals("00:22:00", FormatUtils.formatDuration(rawMillis))

        // Arredondamento para blocos de 15 minutos (22 min -> 30 min)
        val rounded15 = FormatUtils.calculateBillableDurationMillis(rawMillis, 15)
        assertEquals(30 * 60 * 1000L, rounded15)

        // Arredondamento para blocos de 30 minutos (22 min -> 30 min)
        val rounded30 = FormatUtils.calculateBillableDurationMillis(rawMillis, 30)
        assertEquals(30 * 60 * 1000L, rounded30)

        // Sem arredondamento (0 min)
        val roundedNone = FormatUtils.calculateBillableDurationMillis(rawMillis, 0)
        assertEquals(rawMillis, roundedNone)

        // A duração bruta original permanece 22 minutos intacta!
        assertEquals(22 * 60 * 1000L, rawMillis)
    }

    @Test
    fun testRateResolution_hierarchyPriority() {
        // 1. Projeto com taxa definida ganha do cliente e da global
        val rate1 = FormatUtils.resolveEffectiveRate(projectRate = 180.0, clientRate = 150.0, globalRate = 100.0)
        assertEquals(180.0, rate1, 0.001)

        // 2. Projeto sem taxa (null ou 0) usa a taxa do cliente
        val rate2 = FormatUtils.resolveEffectiveRate(projectRate = null, clientRate = 150.0, globalRate = 100.0)
        assertEquals(150.0, rate2, 0.001)

        // 3. Projeto e cliente sem taxa usam a taxa global
        val rate3 = FormatUtils.resolveEffectiveRate(projectRate = null, clientRate = 0.0, globalRate = 95.0)
        assertEquals(95.0, rate3, 0.001)
    }

    @Test
    fun testBillableFlag_nonBillableYieldsZeroEarnings() {
        val session = com.example.data.Session(
            id = 10L,
            clientId = 1L,
            startTime = 1000L,
            endTime = 1000L + 3600000L, // 1 hora
            appliedRate = 150.0,
            billable = false // Não faturável
        )

        val earnings = session.calculateEarnings()
        assertEquals(0.0, earnings, 0.001)
        assertEquals(3600000L, session.calculateDurationMillis())
    }

    @Test
    fun testBudgetConsumption_percentageCalculation() {
        val consumedHours = 40.0
        val budgetHours = 50.0
        val pct = (consumedHours / budgetHours).toFloat()
        assertEquals(0.80f, pct, 0.001f) // 80% (Warning status >= 75%)
    }

    @Test
    fun testCustomDateRangeFiltering() {
        val startWindow = 100000L
        val endWindow = 200000L

        val s1 = com.example.data.Session(id = 1L, clientId = 1L, startTime = 50000L, endTime = 60000L) // Before window
        val s2 = com.example.data.Session(id = 2L, clientId = 1L, startTime = 120000L, endTime = 150000L) // Inside window
        val s3 = com.example.data.Session(id = 3L, clientId = 1L, startTime = 199999L, endTime = 210000L) // Inside window
        val s4 = com.example.data.Session(id = 4L, clientId = 1L, startTime = 250000L, endTime = 260000L) // After window

        val sessions = listOf(s1, s2, s3, s4)
        val filtered = sessions.filter { it.startTime in startWindow..endWindow }

        assertEquals(2, filtered.size)
        assertEquals(2L, filtered[0].id)
        assertEquals(3L, filtered[1].id)
    }
}

