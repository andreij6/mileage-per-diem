package com.aijones.mileage.feature.tracking.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.navigation.LiveTripRoute
import com.aijones.mileage.core.navigation.TripReceiptRoute
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.tracking.domain.RecordingPolicy
import com.aijones.mileage.feature.tracking.domain.RecordingState
import com.aijones.mileage.feature.trips.domain.TrackPoint
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripStatus
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class InterruptedTripViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val g = TrackingTestGraph()
    private val start = Instant.parse("2026-11-16T14:00:00Z")

    private suspend fun interruptedTrip(): Long {
        val id = g.trips.insert(Trip(startedAt = start, status = TripStatus.RECORDING))
        val points = (0..82).map { TrackPoint(start.plusSeconds(it * 20L), GeoPoint(40.7 + it * 0.001447, -74.0), 5f, 9f) }
        g.trips.appendTrackPoints(id, points)
        return id
    }

    private val shown = InterruptedTripUi(1, "9:27 AM", "8.2 mi")

    @Test
    fun resumeRestartsRecordingAndOpensLiveTrip() = runTest {
        interruptedTrip()
        val vm = g.interrupted()
        vm.state.test {
            assertEquals(InterruptedTripUiState(shown), expectMostRecentItem())
            vm.resume()
            assertEquals(InterruptedTripUiState(nav = LiveTripRoute()), expectMostRecentItem())
            vm.onNavigated()
            assertEquals(InterruptedTripUiState(), expectMostRecentItem())
        }
        assertIs<RecordingState.Recording>(g.store.state.value)
        assertEquals(listOf("start:app"), g.controller.calls)
    }

    @Test
    fun finishFinalizesAsInterruptedAndOpensReceipt() = runTest {
        val id = interruptedTrip()
        val vm = g.interrupted()
        vm.state.test {
            expectMostRecentItem()
            vm.finish()
            assertEquals(InterruptedTripUiState(nav = TripReceiptRoute(id)), expectMostRecentItem())
        }
        val trip = g.trips.get(id)!!
        assertEquals(TripStatus.DONE, trip.status)
        assertEquals(start.plusSeconds(82 * 20L), trip.endedAt)
        assertEquals(true, g.analytics.eventsOf<AnalyticsEvent.TripRecordingStopped>().single().interrupted)
    }

    @Test
    fun discardDeletesTheTrip() = runTest {
        val id = interruptedTrip()
        val vm = g.interrupted()
        vm.state.test {
            expectMostRecentItem()
            vm.discard()
            assertEquals(InterruptedTripUiState(), expectMostRecentItem())
        }
        assertNull(g.trips.get(id))
        assertEquals(listOf(AnalyticsEvent.TripDeleted), g.analytics.events)
    }

    @Test
    fun aTripStoppedInThisProcessIsNotInterrupted() = runTest {
        val vm = g.interrupted()
        vm.state.test {
            assertEquals(InterruptedTripUiState(), expectMostRecentItem())
            val id = interruptedTrip()
            g.store.update { RecordingState.Recording(id, start, Distance.ZERO, Duration.ZERO, null, null, RecordingPolicy.HIGH) }
            g.store.update { RecordingState.Idle }
            assertEquals(InterruptedTripUiState(), expectMostRecentItem())
        }
    }

    @Test
    fun dismissHidesTheSheet() = runTest {
        interruptedTrip()
        val vm = g.interrupted()
        vm.state.test {
            assertEquals(shown, expectMostRecentItem().trip)
            vm.dismiss()
            assertEquals(InterruptedTripUiState(), expectMostRecentItem())
        }
    }
}
