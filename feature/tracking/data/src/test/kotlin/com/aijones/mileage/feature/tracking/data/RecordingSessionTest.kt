package com.aijones.mileage.feature.tracking.data

import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.feature.settings.domain.FakeSettingsRepository
import com.aijones.mileage.feature.tracking.domain.FakeDeviceStateMonitor
import com.aijones.mileage.feature.tracking.domain.FakeLocationTracker
import com.aijones.mileage.feature.tracking.domain.FakeRecordingStateStore
import com.aijones.mileage.feature.tracking.domain.Fix
import com.aijones.mileage.feature.tracking.domain.ObserveRecordingPolicyUseCase
import com.aijones.mileage.feature.tracking.domain.RecordingPolicy
import com.aijones.mileage.feature.tracking.domain.RecordingState
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.TrackPoint
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripStatus
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Duration
import kotlin.test.assertEquals

class RecordingSessionTest {
    private val clock = FakeClock()
    private val t0 = clock.instant()
    private val trips = FakeTripRepository(listOf(Trip(id = 1, startedAt = t0, status = TripStatus.RECORDING)))
    private val store = FakeRecordingStateStore(RecordingState.Recording(1, t0, Distance.ZERO, Duration.ZERO, null, null, RecordingPolicy.HIGH))

    private fun fixes(count: Int, from: Int = 0) =
        (from until from + count).map { Fix(t0.plusSeconds(it * 2L), GeoPoint(40.7 + it * 0.0005, -74.0), 5f, 25f) }

    private fun session(tracker: FakeLocationTracker) = RecordingSession(
        ObserveRecordingPolicyUseCase(FakeDeviceStateMonitor(), FakeSettingsRepository(), store, FakeAnalytics(), clock),
        tracker,
        trips,
        store,
        clock,
    )

    private fun TestScope.recording(tracker: FakeLocationTracker) = launch { session(tracker).record(1) }.also { runCurrent() }

    @Test
    fun acceptedFixesUpdateTheStoreAndAreWrittenInBatchesOfTen() = runTest {
        val tracker = FakeLocationTracker(fixes(25))
        val job = recording(tracker)

        assertEquals(20, trips.trackPoints(1).size)
        val state = store.state.value as RecordingState.Recording
        assertEquals(24 * 55.6, state.distance.meters, 5.0)
        assertEquals(5f, state.accuracyM)
        assertEquals(25f, state.speedMps)
        assertEquals(listOf(RecordingPolicy.HIGH), tracker.requestedPolicies)

        job.cancelAndJoin()
        assertEquals(fixes(25).map { it.t }, trips.trackPoints(1).map { it.t })
    }

    @Test
    fun pointsAreFlushedAfterFifteenSecondsAndElapsedTicks() = runTest {
        val job = recording(FakeLocationTracker(fixes(3)))
        assertEquals(0, trips.trackPoints(1).size)

        clock.advance(Duration.ofSeconds(16))
        advanceTimeBy(1_001)

        assertEquals(3, trips.trackPoints(1).size)
        assertEquals(Duration.ofSeconds(16), (store.state.value as RecordingState.Recording).elapsed)
        job.cancelAndJoin()
    }

    @Test
    fun resumingContinuesDistanceAndElapsedFromEarlierPoints() = runTest {
        trips.appendTrackPoints(1, fixes(5).map { TrackPoint(it.t, it.point, it.accuracyM, it.speedMps) })
        store.update { (it as RecordingState.Recording).copy(elapsed = Duration.ofMinutes(3)) }

        val job = recording(FakeLocationTracker(fixes(5, from = 5)))
        clock.advance(Duration.ofSeconds(30))
        advanceTimeBy(1_001)

        val state = store.state.value as RecordingState.Recording
        assertEquals(9 * 55.6, state.distance.meters, 3.0)
        assertEquals(Duration.ofSeconds(210), state.elapsed)
        job.cancelAndJoin()
        assertEquals(10, trips.trackPoints(1).size)
    }

    @Test
    fun droppedFixesAreStoredButDoNotAddDistance() = runTest {
        val teleport = Fix(t0.plusSeconds(1), GeoPoint(41.5, -74.0), 5f, 25f)
        val job = recording(FakeLocationTracker(fixes(2) + teleport))
        job.cancelAndJoin()
        assertEquals(55.6, (store.state.value as RecordingState.Recording).distance.meters, 1.0)
        assertEquals(3, trips.trackPoints(1).size)
    }

    @Test
    fun stateStoreStartsIdleAndApplies() {
        val store = InMemoryRecordingStateStore()
        assertEquals(RecordingState.Idle, store.state.value)
        store.update { RecordingState.Paused(7, t0, Distance.ZERO, Duration.ZERO) }
        assertEquals(RecordingState.Paused(7, t0, Distance.ZERO, Duration.ZERO), store.state.value)
    }

    @Test
    fun shortLabelsSkipStreetNumbers() {
        assertEquals("Acme HQ, Midtown", shortLabel("Acme HQ", "W 34th St", "Midtown"))
        assertEquals("W 34th St, Midtown", shortLabel("350", "W 34th St", "Midtown"))
        assertEquals("Midtown", shortLabel(null, null, "Midtown"))
        assertEquals(null, shortLabel(" ", null, null))
    }
}
