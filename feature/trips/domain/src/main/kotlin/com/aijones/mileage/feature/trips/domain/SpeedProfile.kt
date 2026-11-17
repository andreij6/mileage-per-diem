package com.aijones.mileage.feature.trips.domain

import com.aijones.mileage.core.model.distanceTo
import java.time.Duration
import java.time.Instant

data class SpeedSample(val t: Instant, val mph: Float)

data class SpeedProfile(val samples: List<Float>, val start: Instant, val end: Instant) {

    val duration: Duration get() = Duration.between(start, end)

    val maxMph: Float get() = samples.maxOrNull() ?: 0f

    fun sampleAt(fraction: Double): SpeedSample {
        val f = fraction.coerceIn(0.0, 1.0)
        val t = start.plusMillis((duration.toMillis() * f).toLong())
        val mph = if (samples.isEmpty()) 0f else samples[(f * samples.size).toInt().coerceAtMost(samples.lastIndex)]
        return SpeedSample(t, mph)
    }

    companion object {
        private const val MPS_TO_MPH = 2.2369363f

        fun from(points: List<TrackPoint>, buckets: Int = 20): SpeedProfile {
            require(buckets > 0)
            val sorted = points.sortedBy { it.t }
            if (sorted.size < 2) {
                val t = sorted.firstOrNull()?.t ?: Instant.EPOCH
                return SpeedProfile(emptyList(), t, t)
            }
            val start = sorted.first().t
            val end = sorted.last().t
            val spanMs = Duration.between(start, end).toMillis().coerceAtLeast(1)
            val sums = DoubleArray(buckets)
            val counts = IntArray(buckets)
            sorted.forEachIndexed { i, p ->
                val mps = p.speedMps?.toDouble() ?: if (i == 0) 0.0 else impliedSpeed(sorted[i - 1], p)
                val bucket = (Duration.between(start, p.t).toMillis() * buckets / spanMs).toInt().coerceAtMost(buckets - 1)
                sums[bucket] += mps
                counts[bucket]++
            }
            var previous = 0f
            val samples = (0 until buckets).map { b ->
                if (counts[b] > 0) previous = (sums[b] / counts[b]).toFloat() * MPS_TO_MPH
                previous
            }
            return SpeedProfile(samples, start, end)
        }

        private fun impliedSpeed(a: TrackPoint, b: TrackPoint): Double {
            val seconds = Duration.between(a.t, b.t).toMillis() / 1000.0
            return if (seconds <= 0) 0.0 else a.point.distanceTo(b.point).meters / seconds
        }
    }
}
