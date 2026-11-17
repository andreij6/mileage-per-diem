package com.aijones.mileage.feature.tracking.data

import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.feature.tracking.domain.Fix
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DerivedSpeedTest {
    private val t0 = Instant.parse("2026-11-16T14:00:00Z")

    @Test
    fun `speed is distance over time between fixes`() {
        val a = Fix(t0, GeoPoint(0.0, 0.0), 5f, null)
        val b = Fix(t0.plusSeconds(10), GeoPoint(0.0, 0.001), 5f, null)
        assertEquals(11.12f, derivedSpeed(a, b)!!, 0.05f)
    }

    @Test
    fun `no speed when fixes share a timestamp`() {
        val a = Fix(t0, GeoPoint(0.0, 0.0), 5f, null)
        assertNull(derivedSpeed(a, a.copy(point = GeoPoint(0.0, 0.001))))
    }
}
