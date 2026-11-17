package com.aijones.mileage.feature.tracking.domain

import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.TripSource
import com.aijones.mileage.feature.trips.domain.TripRepository
import com.aijones.mileage.feature.trips.domain.TripStatus
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import javax.inject.Inject

class RecordTrackingPermissionUseCase @Inject constructor(private val analytics: Analytics) {
    operator fun invoke(permission: String, granted: Boolean, precise: Boolean? = null) =
        analytics.track(AnalyticsEvent.PermissionResult(permission, granted, precise))
}

class ObserveDeviceStateUseCase @Inject constructor(private val device: DeviceStateMonitor) {
    operator fun invoke(): Flow<DeviceState> = device.observe()
}

class ResumeInterruptedTripUseCase @Inject constructor(
    private val trips: TripRepository,
    private val store: RecordingStateStore,
    private val controller: RecordingController,
) {
    suspend operator fun invoke(): Long? {
        val trip = trips.getActive() ?: return null
        if (trip.status == TripStatus.PAUSED) trips.update(trip.copy(status = TripStatus.RECORDING))
        val points = trips.trackPoints(trip.id)
        val filter = DistanceFilter().apply { points.forEach { accept(it.toFix()) } }
        val elapsed = Duration.between(trip.startedAt, points.lastOrNull()?.t ?: trip.startedAt)
        store.update { RecordingState.Recording(trip.id, trip.startedAt, filter.total, elapsed, null, null, RecordingPolicy.HIGH) }
        controller.start(TripSource.APP)
        return trip.id
    }
}
