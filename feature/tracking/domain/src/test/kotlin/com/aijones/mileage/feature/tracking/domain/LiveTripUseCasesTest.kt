package com.aijones.mileage.feature.tracking.domain

import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.TrackPoint
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LiveTripUseCasesTest {
    private val clock = FakeClock()
    private val trips = FakeTripRepository()
    private val store = FakeRecordingStateStore()
    private val controller = FakeRecordingController()

    @Test
    fun recordPermissionTracksResult() {
        val analytics = FakeAnalytics()
        RecordTrackingPermissionUseCase(analytics)("location", granted = true, precise = false)
        assertEquals(listOf(AnalyticsEvent.PermissionResult("location", true, false)), analytics.events)
    }

    @Test
    fun observeDeviceStateReadsMonitor() = runTest {
        val device = FakeDeviceStateMonitor(DeviceState(powerSave = true, batteryPct = 12))
        assertEquals(DeviceState(true, 12), ObserveDeviceStateUseCase(device)().first())
    }

    @Test
    fun resumeInterruptedRestoresDistanceAndStartsService() = runTest {
        val startedAt = clock.instant()
        val id = trips.insert(Trip(startedAt = startedAt, status = TripStatus.PAUSED))
        val points = (0..10).map { TrackPoint(startedAt.plusSeconds(it * 6L), GeoPoint(40.7 + it * 0.001, -74.0), 5f, 20f) }
        trips.appendTrackPoints(id, points)

        assertEquals(id, ResumeInterruptedTripUseCase(trips, store, controller)())

        assertEquals(TripStatus.RECORDING, trips.get(id)!!.status)
        val state = store.state.value as RecordingState.Recording
        assertEquals(id, state.tripId)
        assertEquals(Duration.ofSeconds(60), state.elapsed)
        assertEquals(1112.0, state.distance.meters, 5.0)
        assertEquals(listOf("start:app"), controller.calls)
    }

    @Test
    fun resumeInterruptedWithoutActiveTripDoesNothing() = runTest {
        assertNull(ResumeInterruptedTripUseCase(trips, store, controller)())
        assertTrue(controller.calls.isEmpty())
        assertEquals(RecordingState.Idle, store.state.value)
    }
}
