package com.aijones.mileage.feature.trips.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.featureflags.FakeFeatureFlags
import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.trips.domain.DeleteTripUseCase
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.FakeVehicleRepository
import com.aijones.mileage.feature.trips.domain.ObserveRouteReplayEnabledUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTrackUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTripUseCase
import com.aijones.mileage.feature.trips.domain.ObserveVehiclesUseCase
import com.aijones.mileage.feature.trips.domain.TripCategory
import com.aijones.mileage.feature.trips.domain.Vehicle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TripDetailViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val analytics = FakeAnalytics()
    private val repo = FakeTripRepository(listOf(trip(1, "2026-11-16T09:12", from = "Brooklyn office", to = "Acme HQ, Midtown")))
    private val vehicles = FakeVehicleRepository(listOf(Vehicle(1, "2021 Civic", "Personal", true)))
    private val points = track("2026-11-16T09:12")

    private suspend fun viewModel(flags: FakeFeatureFlags, id: Long = 1): TripDetailViewModel {
        repo.appendTrackPoints(1, points)
        return TripDetailViewModel(
            id,
            ObserveTripUseCase(repo),
            ObserveTrackUseCase(repo),
            ObserveVehiclesUseCase(vehicles),
            ObserveRouteReplayEnabledUseCase(flags),
            DeleteTripUseCase(repo, analytics),
            analytics,
            FakeClock(),
        )
    }

    @Test
    fun showsTheTripWithReplayWhenTheFlagIsOn() = runTest {
        viewModel(FakeFeatureFlags(FeatureFlag.ROUTE_REPLAY to true)).state.test {
            val s = awaitItem()
            assertEquals(
                TripDetailUiState(
                    loading = false,
                    points = points.map { it.point },
                    from = "Brooklyn office",
                    to = "Acme HQ, Midtown",
                    startTime = "9:12 AM",
                    endTime = "9:41 AM",
                    distance = "18.4 mi",
                    durationMinutes = 29,
                    rate = "76¢",
                    amount = "$13.98",
                    purpose = "Client meeting",
                    category = TripCategory.BUSINESS,
                    vehicle = "Personal · 2021 Civic",
                    replay = s.replay,
                ),
                s,
            )
            val replay = assertNotNull(s.replay)
            assertEquals("9:41 AM", replay.time)
            assertEquals("9:12", replay.start)
            assertEquals("9:41", replay.end)
            assertEquals(1f, replay.fraction)
            assertEquals(20, replay.speeds.size)
        }
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("trip_detail")), analytics.events)
    }

    @Test
    fun scrubbingMovesTheTimeSpeedAndMarker() = runTest {
        val vm = viewModel(FakeFeatureFlags())
        vm.state.test {
            awaitItem()
            vm.scrub(0.5f)
            val replay = assertNotNull(awaitItem().replay)
            assertEquals(0.5f, replay.fraction)
            assertEquals("9:26 AM", replay.time)
            assertEquals(34, replay.mph)
            assertEquals(0.5f, replay.routeFraction, 0.01f)
            vm.setPlaying(true)
            assertTrue(awaitItem().replay!!.playing)
            vm.scrub(1f)
            awaitItem()
            vm.setPlaying(false)
            vm.setPlaying(true)
            assertEquals(0f, expectMostRecentItem().replay!!.fraction)
        }
    }

    @Test
    fun replayIsHiddenWhenTheFlagIsOffAndFollowsChanges() = runTest {
        val flags = FakeFeatureFlags(FeatureFlag.ROUTE_REPLAY to false)
        viewModel(flags).state.test {
            val s = awaitItem()
            assertNull(s.replay)
            assertEquals("18.4 mi", s.distance)
            flags.setOverride(FeatureFlag.ROUTE_REPLAY, true)
            assertNotNull(awaitItem().replay)
        }
    }

    @Test
    fun deletingTheTripTracksItAndMarksTheScreenMissing() = runTest {
        val vm = viewModel(FakeFeatureFlags())
        vm.state.test {
            awaitItem()
            vm.delete()
            assertTrue(expectMostRecentItem().missing)
        }
        assertNull(repo.get(1))
        assertEquals(AnalyticsEvent.TripDeleted, analytics.events.last())
    }

    @Test
    fun unknownTripIsMissing() = runTest {
        viewModel(FakeFeatureFlags(), id = 99).state.test {
            assertEquals(TripDetailUiState(loading = false, missing = true), awaitItem())
        }
    }

    @Test
    fun routeFractionFollowsTimeNotPointIndex() {
        val t = points.first().t
        assertEquals(0f, routeFractionAt(points, t))
        assertEquals(1f, routeFractionAt(points, points.last().t))
        assertEquals(0.5f, routeFractionAt(points, t.plusSeconds(29 * 30)), 0.01f)
        assertEquals(0f, routeFractionAt(points.take(1), t))
    }
}
