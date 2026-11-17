package com.aijones.mileage.feature.trips.domain

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.testing.FakeClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TripUseCasesTest {
    private val clock = FakeClock()
    private val analytics = FakeAnalytics()
    private val rates = FakeMileageRateRepository()

    private fun trip(
        id: Long,
        at: String,
        miles: Double,
        status: TripStatus = TripStatus.DONE,
        category: TripCategory = TripCategory.BUSINESS,
        needsReview: Boolean = false,
    ) = Trip(
        id = id, startedAt = Instant.parse(at), endedAt = Instant.parse(at).plusSeconds(1800), status = status,
        distance = Distance.ofMiles(miles), ratePerMile = Money(760_000), category = category, needsReview = needsReview,
    )

    @Test
    fun observeTripsUsesLocalDaysInclusive() = runTest {
        val repo = FakeTripRepository(
            listOf(
                trip(1, "2026-11-01T03:30:00Z", 1.0),
                trip(2, "2026-11-01T05:30:00Z", 2.0),
                trip(3, "2026-11-30T23:00:00Z", 3.0),
                trip(4, "2026-12-01T05:30:00Z", 4.0),
            ),
        )
        val result = ObserveTripsUseCase(repo, clock)(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)).first()
        assertEquals(listOf(3L, 2L), result.map { it.id })
    }

    @Test
    fun monthSummaryCountsFinishedBusinessTripsOnly() = runTest {
        val repo = FakeTripRepository(
            listOf(
                trip(1, "2026-11-03T14:00:00Z", 18.4),
                trip(2, "2026-11-04T14:00:00Z", 10.0),
                trip(3, "2026-11-05T14:00:00Z", 50.0, category = TripCategory.PERSONAL),
                trip(4, "2026-11-16T14:00:00Z", 5.0, status = TripStatus.RECORDING),
                trip(5, "2026-10-31T14:00:00Z", 7.0),
            ),
        )
        val useCase = ObserveMonthSummaryUseCase(ObserveTripsUseCase(repo, clock), ObserveCurrentRateUseCase(rates, clock))
        val summary = useCase(YearMonth.of(2026, 11)).first()
        assertEquals(2, summary.trips)
        assertEquals(284L, summary.distance.tenthsOfMile)
        assertEquals(Money(13_984_000 + 7_600_000), summary.reimbursement)
        assertEquals(Money(760_000), summary.currentRate)
    }

    @Test
    fun recentTripsAreNewestFirstAndLimited() = runTest {
        val repo = FakeTripRepository((1L..5L).map { trip(it, "2026-11-0${it}T14:00:00Z", 1.0) })
        assertEquals(listOf(5L, 4L), ObserveRecentTripsUseCase(repo)(limit = 2).first().map { it.id })
    }

    @Test
    fun observeTripAndTrack() = runTest {
        val repo = FakeTripRepository(listOf(trip(1, "2026-11-03T14:00:00Z", 1.0)))
        val point = TrackPoint(Instant.parse("2026-11-03T14:00:00Z"), com.aijones.mileage.core.model.GeoPoint(40.0, -74.0), 5f)
        ObserveTrackUseCase(repo)(1).test {
            assertEquals(emptyList(), awaitItem())
            repo.appendTrackPoints(1, listOf(point))
            assertEquals(listOf(point), awaitItem())
        }
        assertEquals(1L, ObserveTripUseCase(repo)(1).first()?.id)
        assertNull(ObserveTripUseCase(repo)(99).first())
    }

    @Test
    fun updateDetailsTracksChangedFieldsAndMarksDistanceEdits() = runTest {
        val repo = FakeTripRepository(listOf(trip(1, "2026-11-03T14:00:00Z", 18.4, needsReview = true)))
        val update = UpdateTripDetailsUseCase(repo, analytics)
        val result = update(1, "Client visit", TripCategory.BUSINESS, 7, Distance.ofMiles(20.0), " ")
        assertEquals("Client visit", result?.purpose)
        assertTrue(result!!.edited)
        assertFalse(result.needsReview)
        assertEquals(200L, repo.get(1)!!.distance.tenthsOfMile)
        assertEquals(listOf(AnalyticsEvent.TripEdited(listOf("purpose", "vehicle", "distance"))), analytics.events)
    }

    @Test
    fun updateWithoutDistanceChangeIsNotMarkedEdited() = runTest {
        val repo = FakeTripRepository(listOf(trip(1, "2026-11-03T14:00:00Z", 18.4)))
        val result = UpdateTripDetailsUseCase(repo, analytics)(1, null, TripCategory.PERSONAL, null, Distance.ofMiles(18.41), "n")
        assertFalse(result!!.edited)
        assertEquals(listOf(AnalyticsEvent.TripEdited(listOf("category", "notes"))), analytics.events)
    }

    @Test
    fun updateWithNoChangesTracksNothing() = runTest {
        val original = trip(1, "2026-11-03T14:00:00Z", 18.4)
        val repo = FakeTripRepository(listOf(original))
        UpdateTripDetailsUseCase(repo, analytics)(1, null, TripCategory.BUSINESS, null, original.distance, null)
        assertTrue(analytics.events.isEmpty())
        assertNull(UpdateTripDetailsUseCase(repo, analytics)(42, null, TripCategory.BUSINESS, null, Distance.ZERO, null))
    }

    @Test
    fun deleteRemovesTripAndTracks() = runTest {
        val repo = FakeTripRepository(listOf(trip(1, "2026-11-03T14:00:00Z", 1.0)))
        DeleteTripUseCase(repo, analytics)(1)
        assertNull(repo.get(1))
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.TripDeleted), analytics.events)
    }

    @Test
    fun addManualTripSnapshotsTheRateForItsDate() = runTest {
        val repo = FakeTripRepository()
        val add = AddManualTripUseCase(repo, GetMileageRateUseCase(rates), analytics, clock)
        val juneId = add(LocalDate.of(2026, 6, 30), "Office", "Acme", Distance.ofMiles(10.0), false, "Kickoff")
        val julyId = add(LocalDate.of(2026, 7, 1), "Office", "Acme", Distance.ofMiles(10.0), false, null)
        val june = repo.get(juneId)!!
        assertEquals(Money(725_000), june.ratePerMile)
        assertEquals(Money(760_000), repo.get(julyId)!!.ratePerMile)
        assertEquals(TripStatus.MANUAL, june.status)
        assertEquals("Office", june.startLabel)
        assertEquals(Instant.parse("2026-06-30T04:00:00Z"), june.startedAt)
        assertEquals(Money.ofCents(725), june.reimbursement)
    }

    @Test
    fun addManualRoundTripDoublesDistance() = runTest {
        val repo = FakeTripRepository()
        val id = AddManualTripUseCase(repo, GetMileageRateUseCase(rates), analytics, clock)(
            LocalDate.of(2026, 11, 10), "Home", "Site", Distance.ofMiles(12.3), true, "Inspection",
        )
        val stored = repo.get(id)!!
        assertEquals(246L, stored.distance.tenthsOfMile)
        assertTrue(stored.roundTrip)
        assertEquals(listOf(AnalyticsEvent.TripAddedManual(246)), analytics.events)
    }

    @Test
    fun interruptedTripIsActiveTripWithNoLiveRecording() = runTest {
        val repo = FakeTripRepository(listOf(trip(1, "2026-11-16T13:00:00Z", 3.0, status = TripStatus.RECORDING)))
        val live = MutableStateFlow(false)
        ObserveInterruptedTripUseCase(repo)(live).test {
            assertEquals(1L, awaitItem()?.id)
            live.value = true
            assertNull(awaitItem())
        }
    }

    @Test
    fun noActiveTripMeansNothingInterrupted() = runTest {
        val repo = FakeTripRepository(listOf(trip(1, "2026-11-16T13:00:00Z", 3.0)))
        assertNull(ObserveInterruptedTripUseCase(repo)(MutableStateFlow(false)).first())
    }

    @Test
    fun classifyClearsReviewAndUndoRestores() = runTest {
        val original = trip(1, "2026-11-03T14:00:00Z", 1.0, needsReview = true)
        val repo = FakeTripRepository(listOf(original, trip(2, "2026-11-04T14:00:00Z", 1.0, needsReview = true)))
        val needing = ObserveTripsNeedingReviewUseCase(repo)
        assertEquals(2, needing().first().size)

        val previous = ClassifyTripUseCase(repo, analytics)(1, TripCategory.PERSONAL, null, "swipe")
        assertEquals(original, previous)
        assertEquals(TripCategory.PERSONAL, repo.get(1)!!.category)
        assertFalse(repo.get(1)!!.needsReview)
        assertEquals(listOf(2L), needing().first().map { it.id })
        assertEquals(listOf(AnalyticsEvent.TripClassified("personal", "swipe")), analytics.events)

        UndoClassificationUseCase(repo)(previous!!)
        assertEquals(original, repo.get(1))
    }

    @Test
    fun classifyKeepsPurposeWhenNoneGiven() = runTest {
        val repo = FakeTripRepository(listOf(trip(1, "2026-11-03T14:00:00Z", 1.0, needsReview = true).copy(purpose = "Audit")))
        ClassifyTripUseCase(repo, analytics)(1, TripCategory.BUSINESS, "  ", "button")
        assertEquals("Audit", repo.get(1)!!.purpose)
        ClassifyTripUseCase(repo, analytics)(1, TripCategory.BUSINESS, "Client", "button")
        assertEquals("Client", repo.get(1)!!.purpose)
        assertNull(ClassifyTripUseCase(repo, analytics)(9, TripCategory.BUSINESS, null, "button"))
    }

    @Test
    fun recordingTripIsNotOfferedForReview() = runTest {
        val repo = FakeTripRepository(listOf(trip(1, "2026-11-16T13:00:00Z", 3.0, status = TripStatus.RECORDING, needsReview = true)))
        assertTrue(ObserveTripsNeedingReviewUseCase(repo)().first().isEmpty())
    }
}
