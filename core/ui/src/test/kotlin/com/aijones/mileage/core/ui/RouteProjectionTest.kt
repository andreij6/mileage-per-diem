package com.aijones.mileage.core.ui

import com.aijones.mileage.core.model.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteProjectionTest {
    private val route = listOf(GeoPoint(40.70, -74.00), GeoPoint(40.75, -73.98), GeoPoint(40.76, -73.95))

    @Test
    fun `projected route fits inside the padded canvas`() {
        val p = RouteProjection(route, 400f, 200f, 20f)
        route.forEach {
            assertTrue(p.x(it) in 19.9f..380.1f)
            assertTrue(p.y(it) in 19.9f..180.1f)
        }
    }

    @Test
    fun `north is up`() {
        val p = RouteProjection(route, 400f, 200f, 20f)
        assertTrue(p.y(route[2]) < p.y(route[0]))
    }

    @Test
    fun `prefix by length stops halfway along a straight line`() {
        val pts = listOf(0f to 0f, 10f to 0f, 20f to 0f)
        assertEquals(listOf(0f to 0f, 10f to 0f), prefixByLength(pts, 0.5f))
        assertEquals(listOf(0f to 0f, 5f to 0f), prefixByLength(pts, 0.25f))
        assertEquals(pts, prefixByLength(pts, 1f))
    }

    @Test
    fun `scale bar picks a round distance`() {
        val (_, label) = RouteProjection(route, 400f, 200f, 20f).niceScale(48f)
        assertTrue(label.endsWith(" mi"))
    }
}
