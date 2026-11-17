package com.aijones.mileage.feature.perdiem.domain

import com.aijones.mileage.core.model.Money
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PerDiemCalculatorTest {
    private fun d(m: Int, day: Int, y: Int = 2026) = LocalDate.of(y, m, day)

    @Test
    fun table() {
        data class Case(val name: String, val rates: PerDiemRates, val from: LocalDate, val to: LocalDate, val lodging: Boolean, val days: Int, val mie: Long, val lodgingDollars: Long)
        listOf(
            Case("NYC 8 days with lodging", SamplePerDiem.NYC, d(11, 18), d(11, 25), true, 8, 690, 2002),
            Case("NYC 8 days M&IE only", SamplePerDiem.NYC, d(11, 18), d(11, 25), false, 8, 690, 0),
            Case("NYC 3 days", SamplePerDiem.NYC, d(11, 18), d(11, 20), true, 3, 230, 572),
            Case("one day trip is 75%", SamplePerDiem.NYC, d(11, 18), d(11, 18), true, 1, 69, 0),
            Case("two days both travel days", SamplePerDiem.STANDARD, d(11, 18), d(11, 19), true, 2, 102, 113),
            Case("lodging uses each night's month", SamplePerDiem.NYC, d(10, 30), d(11, 2), true, 4, 69 + 92 + 92 + 69, 330 + 330 + 286),
            Case("year boundary", SamplePerDiem.NYC, d(12, 31), d(1, 2, 2027), true, 3, 230, 286 + 175),
        ).forEach { c ->
            val e = PerDiemCalculator.estimate(c.rates, c.from, c.to, c.lodging)
            assertEquals(c.days, e.days, c.name)
            assertEquals(Money.ofDollars(c.mie), e.mieTotal, c.name)
            assertEquals(Money.ofDollars(c.lodgingDollars), e.lodgingTotal, c.name)
            assertEquals(e.mieTotal + e.lodgingTotal, e.total, c.name)
            assertEquals(c.days, e.lines.size, c.name)
        }
    }

    @Test
    fun threeDayTripTotals802() {
        val e = PerDiemCalculator.estimate(SamplePerDiem.NYC, d(11, 18), d(11, 20), includeLodging = true)
        assertEquals(Money.ofDollars(802), e.total)
        assertEquals(2, e.travelDays)
        assertEquals(1, e.fullDays)
        assertEquals(2, e.nights)
    }

    @Test
    fun fractionalFirstLastDayIsKept() {
        val e = PerDiemCalculator.estimate(SamplePerDiem.BOSTON, d(11, 18), d(11, 19), includeLodging = false)
        assertEquals(Money.ofCents(12_900), e.mieTotal)
    }

    @Test
    fun checkOutBeforeCheckInIsRejected() {
        assertFailsWith<IllegalArgumentException> { PerDiemCalculator.estimate(SamplePerDiem.NYC, d(11, 18), d(11, 17), true) }
    }
}
