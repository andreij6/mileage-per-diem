package com.aijones.mileage.feature.tracking.domain

import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.model.distanceTo
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DistanceFilterTest {
    private val t0 = Instant.parse("2026-11-16T14:00:00Z")
    private val origin = GeoPoint(40.7000, -74.0000)
    private val metersPerDegLat = 111_195.0

    private fun north(meters: Double) = GeoPoint(origin.lat + meters / metersPerDegLat, origin.lng)
    private fun fix(s: Long, point: GeoPoint, accuracy: Float = 5f, speed: Float? = 15f) = Fix(t0.plusSeconds(s), point, accuracy, speed)

    @Test
    fun highwayDriveAccumulatesDistance() {
        val filter = DistanceFilter()
        (0..30).forEach { assertTrue(filter.accept(fix(it * 2L, north(it * 60.0), speed = 30f))) }
        assertEquals(1800.0, filter.total.meters, 2.0)
        assertEquals(31, filter.accepted)
        assertEquals(0, filter.dropped)
    }

    @Test
    fun table() {
        data class Case(val name: String, val second: Fix, val accepted: Boolean)
        listOf(
            Case("low accuracy", fix(2, north(30.0), accuracy = 36f), false),
            Case("accuracy at limit", fix(2, north(30.0), accuracy = 35f), true),
            Case("teleport", fix(2, north(500.0)), false),
            Case("fast but plausible", fix(2, north(130.0)), true),
            Case("stationary jitter", fix(2, north(4.0), accuracy = 8f, speed = 0f), false),
            Case("stationary jitter without speed", fix(2, north(6.0), accuracy = 8f, speed = null), false),
            Case("slow move beyond accuracy", fix(20, north(12.0), accuracy = 8f, speed = 0.4f), true),
            Case("moving small step", fix(1, north(4.0), accuracy = 8f, speed = 4f), true),
            Case("same timestamp", fix(0, north(10.0)), false),
        ).forEach { c ->
            val filter = DistanceFilter()
            filter.accept(fix(0, origin))
            assertEquals(c.accepted, filter.accept(c.second), c.name)
            assertEquals(if (c.accepted) 0 else 1, filter.dropped, c.name)
        }
    }

    @Test
    fun parkingLotJitterAddsNoDistance() {
        val filter = DistanceFilter()
        filter.accept(fix(0, origin, accuracy = 10f, speed = 0f))
        val wobble = listOf(3.0, -4.0, 6.0, -2.0, 5.0, -7.0, 1.0, 8.0, -5.0, 2.0)
        wobble.forEachIndexed { i, m -> filter.accept(fix(5L * (i + 1), north(m), accuracy = 10f, speed = 0f)) }
        assertEquals(0.0, filter.total.meters)
        assertEquals(10, filter.dropped)
    }

    @Test
    fun droppedFixDoesNotBecomeTheReference() {
        val filter = DistanceFilter()
        filter.accept(fix(0, origin))
        assertFalse(filter.accept(fix(2, north(5_000.0))))
        assertTrue(filter.accept(fix(4, north(60.0))))
        assertEquals(origin.distanceTo(north(60.0)).meters, filter.total.meters, 0.01)
    }
}
