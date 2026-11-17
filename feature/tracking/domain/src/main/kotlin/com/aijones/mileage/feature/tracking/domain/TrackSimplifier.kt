package com.aijones.mileage.feature.tracking.domain

import com.aijones.mileage.core.model.GeoPoint
import kotlin.math.cos
import kotlin.math.hypot

object TrackSimplifier {
    private const val METERS_PER_DEGREE = 111_320.0

    fun simplify(points: List<GeoPoint>, toleranceM: Double = 5.0): List<GeoPoint> {
        if (points.size < 3) return points
        val cosLat = cos(Math.toRadians(points.sumOf { it.lat } / points.size))
        val xy = points.map { doubleArrayOf(it.lng * METERS_PER_DEGREE * cosLat, it.lat * METERS_PER_DEGREE) }
        val keep = BooleanArray(points.size).also { it[0] = true; it[points.lastIndex] = true }
        val stack = ArrayDeque<IntArray>().apply { add(intArrayOf(0, points.lastIndex)) }
        while (stack.isNotEmpty()) {
            val (first, last) = stack.removeLast()
            var maxDistance = 0.0
            var index = -1
            for (i in first + 1 until last) {
                val d = distanceToSegment(xy[i], xy[first], xy[last])
                if (d > maxDistance) {
                    maxDistance = d
                    index = i
                }
            }
            if (index >= 0 && maxDistance > toleranceM) {
                keep[index] = true
                stack.add(intArrayOf(first, index))
                stack.add(intArrayOf(index, last))
            }
        }
        return points.filterIndexed { i, _ -> keep[i] }
    }

    private fun distanceToSegment(p: DoubleArray, a: DoubleArray, b: DoubleArray): Double {
        val dx = b[0] - a[0]
        val dy = b[1] - a[1]
        val lengthSq = dx * dx + dy * dy
        val t = if (lengthSq == 0.0) 0.0 else (((p[0] - a[0]) * dx + (p[1] - a[1]) * dy) / lengthSq).coerceIn(0.0, 1.0)
        return hypot(p[0] - (a[0] + t * dx), p[1] - (a[1] + t * dy))
    }
}
