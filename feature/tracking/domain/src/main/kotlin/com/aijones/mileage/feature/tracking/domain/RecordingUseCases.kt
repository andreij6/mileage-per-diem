package com.aijones.mileage.feature.tracking.domain

import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FailureReason
import com.aijones.mileage.core.analytics.TripSource
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.feature.settings.domain.SettingsRepository
import com.aijones.mileage.feature.trips.domain.GetMileageRateUseCase
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripRepository
import com.aijones.mileage.feature.trips.domain.TripStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject

class StartTripUseCase @Inject constructor(
    private val trips: TripRepository,
    private val settings: SettingsRepository,
    private val store: RecordingStateStore,
    private val controller: RecordingController,
    private val analytics: Analytics,
    private val clock: Clock,
) {
    suspend operator fun invoke(source: TripSource): Long {
        trips.getActive()?.let { return it.id }
        val purpose = settings.observe().first().defaultPurpose.trim().ifEmpty { null }
        val startedAt = clock.instant()
        val id = trips.insert(
            Trip(startedAt = startedAt, status = TripStatus.RECORDING, purpose = purpose, needsReview = purpose == null, source = source.wire),
        )
        store.update { RecordingState.Recording(id, startedAt, Distance.ZERO, Duration.ZERO, null, null, RecordingPolicy.HIGH) }
        controller.start(source)
        analytics.track(AnalyticsEvent.TripRecordingStarted(source))
        return id
    }
}

class PauseTripUseCase @Inject constructor(
    private val trips: TripRepository,
    private val store: RecordingStateStore,
    private val controller: RecordingController,
) {
    suspend operator fun invoke() {
        val trip = trips.getActive()?.takeIf { it.status == TripStatus.RECORDING } ?: return
        trips.update(trip.copy(status = TripStatus.PAUSED))
        store.update { s -> if (s is RecordingState.Recording) RecordingState.Paused(s.tripId, s.startedAt, s.distance, s.elapsed) else s }
        controller.pause()
    }
}

class ResumeTripUseCase @Inject constructor(
    private val trips: TripRepository,
    private val store: RecordingStateStore,
    private val controller: RecordingController,
) {
    suspend operator fun invoke() {
        val trip = trips.getActive()?.takeIf { it.status == TripStatus.PAUSED } ?: return
        trips.update(trip.copy(status = TripStatus.RECORDING))
        store.update { s ->
            if (s is RecordingState.Paused) RecordingState.Recording(s.tripId, s.startedAt, s.distance, s.elapsed, null, null, RecordingPolicy.HIGH) else s
        }
        controller.resume()
    }
}

class StopTripUseCase @Inject constructor(
    private val trips: TripRepository,
    private val store: RecordingStateStore,
    private val controller: RecordingController,
) {
    suspend operator fun invoke(): Long? {
        val trip = trips.getActive()
        controller.stop()
        store.update { RecordingState.Idle }
        return trip?.id
    }
}

class FinalizeTripUseCase @Inject constructor(
    private val trips: TripRepository,
    private val getMileageRate: GetMileageRateUseCase,
    private val labeler: PlaceLabeler,
    private val analytics: Analytics,
    private val clock: Clock,
) {
    suspend operator fun invoke(tripId: Long, interrupted: Boolean): Trip? {
        val trip = trips.get(tripId) ?: return null
        if (!trip.isActive) return trip
        val points = trips.trackPoints(tripId)
        val filter = DistanceFilter()
        val kept = points.filter { filter.accept(it.toFix()) }
        val simplified = TrackSimplifier.simplify(kept.map { it.point })
        val start = simplified.firstOrNull()
        val end = simplified.lastOrNull()
        val endedAt = if (interrupted) points.lastOrNull()?.t ?: trip.startedAt else clock.instant()
        val finished = trip.copy(
            status = TripStatus.DONE,
            endedAt = endedAt,
            distance = filter.total,
            recordedDistance = filter.total,
            ratePerMile = trip.ratePerMile ?: getMileageRate(LocalDate.ofInstant(trip.startedAt, clock.zone)),
            start = start,
            end = end,
            startLabel = trip.startLabel ?: start?.let { labelOrNull(it) },
            endLabel = trip.endLabel ?: end?.let { labelOrNull(it) },
        )
        trips.update(finished)
        analytics.track(
            AnalyticsEvent.TripRecordingStopped(
                distanceTenthsMi = finished.distance.tenthsOfMile,
                durationS = finished.duration?.seconds ?: 0,
                fixes = points.size,
                droppedFixes = filter.dropped,
                interrupted = interrupted,
            ),
        )
        return finished
    }

    private suspend fun labelOrNull(point: com.aijones.mileage.core.model.GeoPoint): String? = try {
        labeler.label(point)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}

class RecordFailureUseCase @Inject constructor(
    private val trips: TripRepository,
    private val store: RecordingStateStore,
    private val analytics: Analytics,
) {
    suspend operator fun invoke(reason: FailureReason) {
        analytics.track(AnalyticsEvent.TripRecordingFailed(reason))
        store.update { RecordingState.Idle }
        val active = trips.getActive() ?: return
        if (trips.trackPoints(active.id).isEmpty()) trips.delete(active.id)
    }
}

class ObserveRecordingStateUseCase @Inject constructor(private val store: RecordingStateStore) {
    operator fun invoke(): StateFlow<RecordingState> = store.state
}
