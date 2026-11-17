package com.aijones.mileage.feature.tracking.domain

import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FailureReason
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.analytics.TripSource
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.feature.settings.domain.FakeSettingsRepository
import com.aijones.mileage.feature.trips.domain.FakeMileageRateRepository
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.GetMileageRateUseCase
import com.aijones.mileage.feature.trips.domain.TrackPoint
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripStatus
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecordingUseCasesTest {
    private val clock = FakeClock()
    private val trips = FakeTripRepository()
    private val settings = FakeSettingsRepository()
    private val store = FakeRecordingStateStore()
    private val controller = FakeRecordingController()
    private val analytics = FakeAnalytics()
    private val rates = FakeMileageRateRepository()

    private val start = StartTripUseCase(trips, settings, store, controller, analytics, clock)
    private fun finalize(labeler: PlaceLabeler = FakePlaceLabeler()) =
        FinalizeTripUseCase(trips, GetMileageRateUseCase(rates), labeler, analytics, clock)

    @Test
    fun startInsertsRecordingTripNeedingReviewWithoutDefaultPurpose() = runTest {
        val id = start(TripSource.WIDGET)
        val trip = trips.get(id)!!
        assertEquals(TripStatus.RECORDING, trip.status)
        assertEquals(clock.instant(), trip.startedAt)
        assertNull(trip.purpose)
        assertTrue(trip.needsReview)
        assertEquals("widget", trip.source)
        assertEquals(id, (store.state.value as RecordingState.Recording).tripId)
        assertEquals(listOf("start:widget"), controller.calls)
        assertEquals(listOf(AnalyticsEvent.TripRecordingStarted(TripSource.WIDGET)), analytics.events)
    }

    @Test
    fun startUsesDefaultPurpose() = runTest {
        settings.update { it.copy(defaultPurpose = "Client visits") }
        val trip = trips.get(start(TripSource.APP))!!
        assertEquals("Client visits", trip.purpose)
        assertFalse(trip.needsReview)
    }

    @Test
    fun startIsRefusedWhileATripIsActive() = runTest {
        val first = start(TripSource.APP)
        analytics.clear()
        controller.calls.clear()
        assertEquals(first, start(TripSource.TILE))
        assertEquals(1, trips.trips.size)
        assertTrue(controller.calls.isEmpty())
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun pauseResumeStop() = runTest {
        val id = start(TripSource.APP)
        PauseTripUseCase(trips, store, controller)()
        assertEquals(TripStatus.PAUSED, trips.get(id)!!.status)
        assertIs<RecordingState.Paused>(store.state.value)

        ResumeTripUseCase(trips, store, controller)()
        assertEquals(TripStatus.RECORDING, trips.get(id)!!.status)
        assertIs<RecordingState.Recording>(store.state.value)

        assertEquals(id, StopTripUseCase(trips, store, controller)())
        assertEquals(RecordingState.Idle, ObserveRecordingStateUseCase(store)().value)
        assertEquals(listOf("start:app", "pause", "resume", "stop"), controller.calls)
    }

    @Test
    fun pauseAndResumeWithoutActiveTripDoNothing() = runTest {
        PauseTripUseCase(trips, store, controller)()
        ResumeTripUseCase(trips, store, controller)()
        assertTrue(controller.calls.isEmpty())
        assertNull(StopTripUseCase(trips, store, controller)())
    }

    @Test
    fun finalizeFiltersSimplifiesRatesLabelsAndTracks() = runTest {
        val id = start(TripSource.APP)
        val t0 = clock.instant()
        val good = (0..10).map { TrackPoint(t0.plusSeconds(it * 2L), GeoPoint(40.7 + it * 0.0005, -74.0), 5f, 25f) }
        val bad = TrackPoint(t0.plusSeconds(3), GeoPoint(41.5, -74.0), 50f, 25f)
        trips.appendTrackPoints(id, good + bad)
        clock.advance(Duration.ofMinutes(30))
        analytics.clear()

        val labeler = FakePlaceLabeler(mapOf(good.first().point to "Office", good.last().point to "Acme HQ"))
        val trip = finalize(labeler)(id, interrupted = false)!!

        assertEquals(TripStatus.DONE, trip.status)
        assertEquals(clock.instant(), trip.endedAt)
        assertEquals(Money(760_000), trip.ratePerMile)
        assertEquals(good.first().point, trip.start)
        assertEquals(good.last().point, trip.end)
        assertEquals("Office", trip.startLabel)
        assertEquals("Acme HQ", trip.endLabel)
        assertEquals(trip.distance, trip.recordedDistance)
        assertEquals(556.0, trip.distance.meters, 2.0)
        assertEquals(trip, trips.get(id))
        assertEquals(
            listOf(AnalyticsEvent.TripRecordingStopped(trip.distance.tenthsOfMile, 1800, fixes = 12, droppedFixes = 1, interrupted = false)),
            analytics.events,
        )
    }

    @Test
    fun finalizeSnapshotsTheRateOfTheStartDay() = runTest {
        val id = trips.insert(Trip(startedAt = Instant.parse("2026-07-01T03:30:00Z"), status = TripStatus.RECORDING))
        assertEquals(Money(725_000), finalize()(id, interrupted = true)!!.ratePerMile)
    }

    @Test
    fun finalizeWithZeroPoints() = runTest {
        val id = start(TripSource.APP)
        analytics.clear()
        val labeler = FakePlaceLabeler()
        val trip = finalize(labeler)(id, interrupted = true)!!
        assertEquals(Distance.ZERO, trip.distance)
        assertEquals(trip.startedAt, trip.endedAt)
        assertNull(trip.start)
        assertNull(trip.startLabel)
        assertTrue(labeler.requested.isEmpty())
        assertEquals(listOf(AnalyticsEvent.TripRecordingStopped(0, 0, 0, 0, interrupted = true)), analytics.events)
    }

    @Test
    fun interruptedTripEndsAtItsLastPoint() = runTest {
        val id = start(TripSource.APP)
        val t0 = clock.instant()
        trips.appendTrackPoints(id, (0..3).map { TrackPoint(t0.plusSeconds(it * 10L), GeoPoint(40.7 + it * 0.001, -74.0), 5f, 12f) })
        clock.advance(Duration.ofHours(5))
        assertEquals(t0.plusSeconds(30), finalize()(id, interrupted = true)!!.endedAt)
    }

    @Test
    fun labelerFailureLeavesLabelsEmpty() = runTest {
        val id = start(TripSource.APP)
        trips.appendTrackPoints(id, listOf(TrackPoint(clock.instant(), GeoPoint(40.7, -74.0), 5f, 0f)))
        val trip = finalize(FakePlaceLabeler(failure = IOException("offline")))(id, interrupted = false)!!
        assertNull(trip.startLabel)
        assertEquals(GeoPoint(40.7, -74.0), trip.start)
    }

    @Test
    fun finalizeIsIdempotentAndIgnoresUnknownTrips() = runTest {
        val id = start(TripSource.APP)
        val done = finalize()(id, interrupted = false)
        analytics.clear()
        assertEquals(done, finalize()(id, interrupted = false))
        assertTrue(analytics.events.isEmpty())
        assertNull(finalize()(404, interrupted = false))
    }

    @Test
    fun failureTracksResetsAndDropsEmptyTrip() = runTest {
        val id = start(TripSource.APP)
        analytics.clear()
        RecordFailureUseCase(trips, store, analytics)(FailureReason.FGS_START_FAILED)
        assertNull(trips.get(id))
        assertEquals(RecordingState.Idle, store.state.value)
        assertEquals(listOf(AnalyticsEvent.TripRecordingFailed(FailureReason.FGS_START_FAILED)), analytics.events)
    }

    @Test
    fun failureKeepsTripThatHasPoints() = runTest {
        val id = start(TripSource.APP)
        trips.appendTrackPoints(id, listOf(TrackPoint(clock.instant(), GeoPoint(40.7, -74.0), 5f)))
        RecordFailureUseCase(trips, store, analytics)(FailureReason.PERMISSION_REVOKED)
        assertEquals(TripStatus.RECORDING, trips.get(id)!!.status)
    }
}
