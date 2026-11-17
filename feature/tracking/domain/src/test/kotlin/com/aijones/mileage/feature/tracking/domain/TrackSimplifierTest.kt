package com.aijones.mileage.feature.tracking.domain

import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.model.distanceTo
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackSimplifierTest {
    private fun length(points: List<GeoPoint>) = points.zipWithNext().sumOf { (a, b) -> a.distanceTo(b).meters }

    @Test
    fun curvedPathKeepsDistanceWithinHalfPercent() {
        val center = GeoPoint(40.7, -74.0)
        val radiusM = 1_000.0
        val arc = (0..400).map { i ->
            val angle = Math.PI * i / 400
            GeoPoint(
                center.lat + radiusM * sin(angle) / 111_195.0,
                center.lng + radiusM * cos(angle) / (111_195.0 * cos(Math.toRadians(center.lat))),
            )
        }
        val simplified = TrackSimplifier.simplify(arc)
        assertTrue(simplified.size < arc.size / 4, "kept ${simplified.size}")
        assertTrue(abs(length(simplified) - length(arc)) / length(arc) < 0.005)
        assertEquals(arc.first(), simplified.first())
        assertEquals(arc.last(), simplified.last())
    }

    @Test
    fun straightLineCollapsesToEndpoints() {
        val line = (0..50).map { GeoPoint(40.0 + it * 0.0001, -74.0) }
        assertEquals(listOf(line.first(), line.last()), TrackSimplifier.simplify(line))
    }

    @Test
    fun cornerIsKept() {
        val corner = listOf(GeoPoint(40.0, -74.0), GeoPoint(40.001, -74.0), GeoPoint(40.001, -73.999))
        assertEquals(corner, TrackSimplifier.simplify(corner))
    }

    @Test
    fun shortTracksAreReturnedAsIs() {
        assertEquals(emptyList(), TrackSimplifier.simplify(emptyList()))
        val one = listOf(GeoPoint(1.0, 2.0))
        assertEquals(one, TrackSimplifier.simplify(one))
    }
}
