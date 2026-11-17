package com.aijones.mileage.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyAndDistanceTest {
    private val irsH2 = Money(760_000)

    @Test
    fun `mileage amount uses tenths of a mile and rounds half up to cents`() {
        assertEquals("$13.98", mileageAmount(Distance.ofMiles(18.4), irsH2).formatUsd())
        assertEquals("$14.52", mileageAmount(Distance.ofMiles(19.1), irsH2).formatUsd())
        assertEquals("$12.31", mileageAmount(Distance.ofMiles(16.2), irsH2).formatUsd())
        assertEquals("$642.20", mileageAmount(Distance.ofMiles(845.0), irsH2).formatUsd())
    }

    @Test
    fun `money formats with thousands separators and negative sign`() {
        assertEquals("$1,654.20", Money.ofCents(165_420).formatUsd())
        assertEquals("-$0.05", Money.ofCents(-5).formatUsd())
        assertEquals("$0.00", Money.ZERO.formatUsd())
    }

    @Test
    fun `cents per mile shows a tenth only when needed`() {
        assertEquals("76¢", irsH2.formatCentsPerMile())
        assertEquals("72.5¢", Money(725_000).formatCentsPerMile())
    }

    @Test
    fun `seventy five percent of ninety two dollars is sixty nine`() {
        assertEquals(Money.ofDollars(69), Money.ofDollars(92).percent(75))
    }

    @Test
    fun `one degree of longitude on the equator is about 111 kilometers`() {
        val d = GeoPoint(0.0, 0.0).distanceTo(GeoPoint(0.0, 1.0))
        assertEquals(111_195.08, d.meters, 0.5)
    }

    @Test
    fun `distance formats with tenths and thousands`() {
        assertEquals("9,412.0 mi", Distance.ofMiles(9412.0).formatMiles())
        assertEquals("18.4 mi", Distance.ofMiles(18.44).formatMiles())
    }
}
