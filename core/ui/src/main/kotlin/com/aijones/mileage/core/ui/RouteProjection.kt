package com.aijones.mileage.core.ui

import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.GeoPoint
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

class RouteProjection(points: List<GeoPoint>, private val width: Float, private val height: Float, private val pad: Float) {
    private val latMid = if (points.isEmpty()) 0.0 else (points.minOf { it.lat } + points.maxOf { it.lat }) / 2
    private val kx = cos(Math.toRadians(latMid))
    private val minX = points.minOfOrNull { it.lng * kx } ?: 0.0
    private val maxX = points.maxOfOrNull { it.lng * kx } ?: 0.0
    private val minY = points.minOfOrNull { it.lat } ?: 0.0
    private val maxY = points.maxOfOrNull { it.lat } ?: 0.0
    private val spanX = max(maxX - minX, 1e-6)
    private val spanY = max(maxY - minY, 1e-6)
    private val scale = min((width - 2 * pad) / spanX, (height - 2 * pad) / spanY)
    private val offX = (width - spanX * scale) / 2
    private val offY = (height - spanY * scale) / 2

    fun x(p: GeoPoint): Float = (offX + (p.lng * kx - minX) * scale).toFloat()
    fun y(p: GeoPoint): Float = (height - offY - (p.lat - minY) * scale).toFloat()

    val metersPerPx: Double get() = 111_195.0 / scale

    fun niceScale(targetPx: Float): Pair<Float, String> {
        val miles = targetPx * metersPerPx / Distance.METERS_PER_MILE
        val nice = listOf(0.05, 0.1, 0.25, 0.5, 1.0, 2.0, 5.0, 10.0, 25.0, 50.0).lastOrNull { it <= miles } ?: 0.05
        val px = (nice * Distance.METERS_PER_MILE / metersPerPx).toFloat()
        val label = if (nice < 1) "${nice.toString().trimEnd('0')} mi".replace("0.", ".").let { "0$it" } else "${nice.toInt()} mi"
        return px to label
    }
}

fun prefixByLength(points: List<Pair<Float, Float>>, fraction: Float): List<Pair<Float, Float>> {
    if (points.size < 2 || fraction >= 1f) return points
    val seg = points.zipWithNext { a, b -> kotlin.math.hypot(b.first - a.first, b.second - a.second) }
    var left = seg.sum() * fraction.coerceIn(0f, 1f)
    val out = mutableListOf(points.first())
    for (i in seg.indices) {
        val (a, b) = points[i] to points[i + 1]
        if (left >= seg[i]) {
            out += b
            left -= seg[i]
            if (left <= 0f) break
        } else {
            val k = if (seg[i] == 0f) 0f else left / seg[i]
            out += (a.first + (b.first - a.first) * k) to (a.second + (b.second - a.second) * k)
            break
        }
    }
    return out
}
