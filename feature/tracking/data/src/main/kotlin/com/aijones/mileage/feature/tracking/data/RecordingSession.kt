package com.aijones.mileage.feature.tracking.data

import com.aijones.mileage.feature.tracking.domain.DistanceFilter
import com.aijones.mileage.feature.tracking.domain.Fix
import com.aijones.mileage.feature.tracking.domain.LocationTracker
import com.aijones.mileage.feature.tracking.domain.ObserveRecordingPolicyUseCase
import com.aijones.mileage.feature.tracking.domain.RecordingPolicy
import com.aijones.mileage.feature.tracking.domain.RecordingState
import com.aijones.mileage.feature.tracking.domain.RecordingStateStore
import com.aijones.mileage.feature.tracking.domain.toFix
import com.aijones.mileage.feature.tracking.domain.toTrackPoint
import com.aijones.mileage.feature.trips.domain.TrackPoint
import com.aijones.mileage.feature.trips.domain.TripRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

class RecordingSession @Inject constructor(
    private val observePolicy: ObserveRecordingPolicyUseCase,
    private val tracker: LocationTracker,
    private val trips: TripRepository,
    private val store: RecordingStateStore,
    private val clock: Clock,
) {
    suspend fun record(tripId: Long) {
        val filter = DistanceFilter()
        trips.trackPoints(tripId).forEach { filter.accept(it.toFix()) }
        val baseElapsed = (store.state.value as? RecordingState.Recording)?.takeIf { it.tripId == tripId }?.elapsed ?: Duration.ZERO
        val resumedAt = clock.instant()
        val buffer = mutableListOf<TrackPoint>()
        var lastFlush = resumedAt

        fun elapsed() = baseElapsed + Duration.between(resumedAt, clock.instant())

        suspend fun flush() {
            lastFlush = clock.instant()
            if (buffer.isEmpty()) return
            val batch = buffer.toList()
            buffer.clear()
            trips.appendTrackPoints(tripId, batch)
        }

        suspend fun flushIfDue() {
            if (buffer.size >= BATCH_SIZE || Duration.between(lastFlush, clock.instant()) >= BATCH_INTERVAL) flush()
        }

        fun onFix(policy: RecordingPolicy, fix: Fix) {
            filter.accept(fix)
            buffer += fix.toTrackPoint()
            store.update { s ->
                if (s is RecordingState.Recording && s.tripId == tripId) {
                    s.copy(distance = filter.total, elapsed = elapsed(), accuracyM = fix.accuracyM, speedMps = fix.speedMps, policy = policy)
                } else {
                    s
                }
            }
        }

        try {
            coroutineScope {
                launch {
                    while (true) {
                        delay(TICK_MS)
                        store.update { s -> if (s is RecordingState.Recording && s.tripId == tripId) s.copy(elapsed = elapsed()) else s }
                        flushIfDue()
                    }
                }
                observePolicy()
                    .flatMapLatest { policy -> tracker.fixes(policy).map { policy to it } }
                    .collect { (policy, fix) ->
                        onFix(policy, fix)
                        flushIfDue()
                    }
            }
        } finally {
            withContext(NonCancellable) { flush() }
        }
    }

    internal companion object {
        const val BATCH_SIZE = 10
        const val TICK_MS = 1_000L
        val BATCH_INTERVAL: Duration = Duration.ofSeconds(15)
    }
}

