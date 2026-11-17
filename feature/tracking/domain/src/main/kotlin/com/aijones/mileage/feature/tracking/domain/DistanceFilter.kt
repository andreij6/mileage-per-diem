package com.aijones.mileage.feature.tracking.domain

import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.distanceTo
import java.time.Duration

class DistanceFilter(
    private val maxAccuracyM: Float = 35f,
    private val maxSpeedMps: Double = 70.0,
    private val stationarySpeedMps: Float = 1f,
) {
    private var last: Fix? = null

    var total: Distance = Distance.ZERO
        private set
    var accepted: Int = 0
        private set
    var dropped: Int = 0
        private set

    fun accept(fix: Fix): Boolean {
        val keep = shouldKeep(fix)
        if (keep) {
            last?.let { total += it.point.distanceTo(fix.point) }
            last = fix
            accepted++
        } else {
            dropped++
        }
        return keep
    }

    private fun shouldKeep(fix: Fix): Boolean {
        if (fix.accuracyM > maxAccuracyM) return false
        val previous = last ?: return true
        val seconds = Duration.between(previous.t, fix.t).toMillis() / 1000.0
        if (seconds <= 0) return false
        val meters = previous.point.distanceTo(fix.point).meters
        if (meters / seconds > maxSpeedMps) return false
        val stationary = (fix.speedMps ?: 0f) < stationarySpeedMps
        val jitterRadius = maxOf(fix.accuracyM, previous.accuracyM, MIN_JITTER_M)
        return !(stationary && meters < jitterRadius)
    }

    private companion object {
        const val MIN_JITTER_M = 3f
    }
}
