package com.aijones.mileage.feature.tracking.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FailureReason
import com.aijones.mileage.core.analytics.TripSource
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.tracking.domain.RecordingPolicy
import com.aijones.mileage.feature.tracking.domain.RecordingState
import com.aijones.mileage.feature.trips.domain.TripStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LiveTripViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val g = TrackingTestGraph()

    @Test
    fun startRecordStopNavigatesToReceipt() = runTest {
        val vm = g.liveTrip("notification")
        vm.state.test {
            assertEquals(PermissionStep.Location, expectMostRecentItem().permissionStep)

            vm.onLocationPermission(fine = true, coarse = true, asked = true)
            assertEquals(PermissionStep.Notifications, expectMostRecentItem().permissionStep)
            vm.onNotificationPermission(granted = true, asked = true)
            assertEquals(PermissionStep.LocationSettings, expectMostRecentItem().permissionStep)
            vm.onLocationSettings(enabled = true)

            val id = g.trips.trips.single().id
            assertEquals(
                LiveTripUiState(status = LiveStatus.Recording, purpose = "Client meeting", batteryPct = 80),
                expectMostRecentItem(),
            )
            assertEquals(listOf("start:notification"), g.controller.calls)

            g.store.update { (it as RecordingState.Recording).copy(distance = Distance.ofMiles(12.6), elapsed = Duration.ofSeconds(1458), accuracyM = 5f, speedMps = 13.9f, policy = RecordingPolicy.BALANCED) }
            assertEquals(
                LiveTripUiState(
                    status = LiveStatus.Recording, tenths = 126, amount = Money(9_576_000), elapsedSeconds = 1458, speedMph = 31,
                    accuracyM = 5, purpose = "Client meeting", policy = "balanced", batteryPct = 80,
                ),
                expectMostRecentItem(),
            )

            vm.endTrip()
            assertEquals(LiveTripNav.Receipt(id), expectMostRecentItem().nav)
            vm.onNavigated()
            assertNull(expectMostRecentItem().nav)
        }
        assertEquals("stop", g.controller.calls.last())
        assertEquals(
            listOf(
                AnalyticsEvent.ScreenView("live_trip"),
                AnalyticsEvent.PermissionResult("location", true, true),
                AnalyticsEvent.PermissionResult("notifications", true, null),
                AnalyticsEvent.TripRecordingStarted(TripSource.NOTIFICATION),
            ),
            g.analytics.events,
        )
    }

    @Test
    fun alreadyGrantedPermissionsAreNotTracked() = runTest {
        val vm = g.liveTrip("app")
        vm.onLocationPermission(fine = true, coarse = true, asked = false)
        vm.onNotificationPermission(granted = true, asked = false)
        vm.onLocationSettings(enabled = true)
        assertEquals(listOf(AnalyticsEvent.ScreenView("live_trip"), AnalyticsEvent.TripRecordingStarted(TripSource.APP)), g.analytics.events)
    }

    @Test
    fun approximateOnlyGoesToPreciseLocationOff() = runTest {
        val vm = g.liveTrip("app")
        vm.onLocationPermission(fine = false, coarse = true, asked = true)
        assertEquals(LiveTripUiState(nav = LiveTripNav.PreciseLocationOff, batteryPct = 80), collected(vm))
        assertTrue(g.trips.trips.isEmpty())
        assertEquals(AnalyticsEvent.PermissionResult("location", true, false), g.analytics.events.last())
    }

    @Test
    fun deniedLocationGoesBack() = runTest {
        val vm = g.liveTrip("widget")
        vm.onLocationPermission(fine = false, coarse = false, asked = true)
        assertEquals(LiveTripNav.Back, collected(vm).nav)
        assertTrue(g.trips.trips.isEmpty())
    }

    @Test
    fun locationServicesOffRecordsFailureAndGoesBack() = runTest {
        val vm = g.liveTrip("app")
        vm.onLocationPermission(fine = true, coarse = true, asked = false)
        vm.onNotificationPermission(granted = false, asked = true)
        vm.onLocationSettings(enabled = false)
        assertEquals(LiveTripNav.Back, collected(vm).nav)
        assertEquals(AnalyticsEvent.TripRecordingFailed(FailureReason.LOCATION_OFF), g.analytics.events.last())
    }

    @Test
    fun serviceStartFailureDropsTheTripAndGoesBack() = runTest {
        g.controller.failOnStart = IllegalStateException("background")
        val vm = g.liveTrip("tile")
        vm.onLocationSettings(enabled = true)
        assertEquals(LiveTripNav.Back, collected(vm).nav)
        assertTrue(g.trips.trips.isEmpty())
        assertEquals(AnalyticsEvent.TripRecordingFailed(FailureReason.FGS_START_FAILED), g.analytics.events.last())
    }

    @Test
    fun openingWithoutSourceWhileIdleGoesBack() = runTest {
        assertEquals(LiveTripNav.Back, collected(g.liveTrip(null)).nav)
    }

    @Test
    fun pauseAndResumeToggleTheTrip() = runTest {
        val vm = g.liveTrip("app")
        vm.onLocationSettings(enabled = true)
        val id = g.trips.trips.single().id
        vm.togglePause()
        assertEquals(LiveStatus.Paused, collected(vm).status)
        assertEquals(TripStatus.PAUSED, g.trips.get(id)!!.status)
        vm.togglePause()
        assertEquals(LiveStatus.Recording, collected(vm).status)
        assertEquals(listOf("start:app", "pause", "resume"), g.controller.calls)
    }

    @Test
    fun stoppingFromTheNotificationClosesTheScreen() = runTest {
        val vm = g.liveTrip("app")
        vm.onLocationSettings(enabled = true)
        g.store.update { RecordingState.Idle }
        assertEquals(LiveTripNav.Back, collected(vm).nav)
    }

    private fun kotlinx.coroutines.test.TestScope.collected(vm: LiveTripViewModel): LiveTripUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        return vm.state.value
    }
}
