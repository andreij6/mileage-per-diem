package com.aijones.mileage.feature.tracking.domain

import com.aijones.mileage.core.analytics.TripSource
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.feature.trips.domain.TrackPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.Duration
import java.time.Instant

data class Fix(val t: Instant, val point: GeoPoint, val accuracyM: Float, val speedMps: Float? = null)

fun TrackPoint.toFix() = Fix(t, point, accuracyM, speedMps)

fun Fix.toTrackPoint() = TrackPoint(t, point, accuracyM, speedMps)

data class RecordingPolicy(val name: String, val highAccuracy: Boolean, val intervalMs: Long, val minDistanceM: Float) {
    companion object {
        val HIGH = RecordingPolicy("high", highAccuracy = true, intervalMs = 2_000, minDistanceM = 5f)
        val BALANCED = RecordingPolicy("balanced", highAccuracy = false, intervalMs = 15_000, minDistanceM = 25f)
        val SAVER = RecordingPolicy("saver", highAccuracy = false, intervalMs = 10_000, minDistanceM = 20f)
    }
}

data class DeviceState(val powerSave: Boolean, val batteryPct: Int)

sealed interface RecordingState {
    data object Idle : RecordingState

    data class Recording(
        val tripId: Long,
        val startedAt: Instant,
        val distance: Distance,
        val elapsed: Duration,
        val accuracyM: Float?,
        val speedMps: Float?,
        val policy: RecordingPolicy,
    ) : RecordingState

    data class Paused(val tripId: Long, val startedAt: Instant, val distance: Distance, val elapsed: Duration) : RecordingState
}

interface LocationTracker {
    fun fixes(policy: RecordingPolicy): Flow<Fix>
}

interface RecordingController {
    fun start(source: TripSource)
    fun pause()
    fun resume()
    fun stop()
}

interface RecordingStateStore {
    val state: StateFlow<RecordingState>
    fun update(transform: (RecordingState) -> RecordingState)
}

interface DeviceStateMonitor {
    fun observe(): Flow<DeviceState>
}

interface PlaceLabeler {
    suspend fun label(point: GeoPoint): String?
}
