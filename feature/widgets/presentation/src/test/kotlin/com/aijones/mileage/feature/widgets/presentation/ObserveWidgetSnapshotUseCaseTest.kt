package com.aijones.mileage.feature.widgets.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.feature.perdiem.domain.FakePerDiemRateRepository
import com.aijones.mileage.feature.perdiem.domain.FakeRecentSearchRepository
import com.aijones.mileage.feature.perdiem.domain.FakeStayRepository
import com.aijones.mileage.feature.perdiem.domain.GetRatesForLocationUseCase
import com.aijones.mileage.feature.perdiem.domain.ObserveRecentSearchesUseCase
import com.aijones.mileage.feature.perdiem.domain.ObserveStaysUseCase
import com.aijones.mileage.feature.perdiem.domain.SamplePerDiem
import com.aijones.mileage.feature.perdiem.domain.Stay
import com.aijones.mileage.feature.tracking.domain.FakeRecordingStateStore
import com.aijones.mileage.feature.tracking.domain.ObserveRecordingStateUseCase
import com.aijones.mileage.feature.tracking.domain.RecordingPolicy
import com.aijones.mileage.feature.tracking.domain.RecordingState
import com.aijones.mileage.feature.trips.domain.FakeMileageRateRepository
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.ObserveCurrentRateUseCase
import com.aijones.mileage.feature.trips.domain.ObserveMonthSummaryUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTripsUseCase
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripStatus
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveWidgetSnapshotUseCaseTest {
    private val clock = FakeClock()
    private val trips = FakeTripRepository(
        listOf(
            Trip(1, Instant.parse("2026-11-10T14:00:00Z"), Instant.parse("2026-11-10T15:00:00Z"), TripStatus.DONE, Distance.ofMiles(845.0), ratePerMile = Money(760_000)),
            Trip(2, Instant.parse("2026-10-10T14:00:00Z"), Instant.parse("2026-10-10T15:00:00Z"), TripStatus.DONE, Distance.ofMiles(50.0), ratePerMile = Money(760_000)),
        ),
    )
    private val store = FakeRecordingStateStore()
    private val stays = FakeStayRepository()
    private val recent = FakeRecentSearchRepository()

    private fun useCase(): ObserveWidgetSnapshotUseCase {
        val rates = FakeMileageRateRepository()
        val currentRate = ObserveCurrentRateUseCase(rates, clock)
        return ObserveWidgetSnapshotUseCase(
            ObserveMonthSummaryUseCase(ObserveTripsUseCase(trips, clock), currentRate),
            ObserveRecordingStateUseCase(store),
            currentRate,
            ObserveStaysUseCase(stays),
            ObserveRecentSearchesUseCase(recent),
            GetRatesForLocationUseCase(FakePerDiemRateRepository(), recent),
            clock,
        )
    }

    private fun stay(location: Long, label: String) = Stay(
        locationId = location, locationLabel = label, fiscalYear = 2027,
        checkIn = LocalDate.of(2026, 11, 15), checkOut = LocalDate.of(2026, 11, 18), includeLodging = true,
        mieDaily = Money.ZERO, mieTotal = Money.ZERO, lodgingTotal = Money.ZERO,
    )

    @Test
    fun combinesMonthTotalPerDiemAndRecording() = runTest {
        useCase()(Locale.US).test {
            val idle = awaitItem()
            assertEquals(MonthWidgetUi("November", "$642.20", "0845", "0"), idle.month)
            assertNull(idle.perDiem)
            assertEquals(RecordingWidgetUi.Idle, idle.recording)

            recent.record(SamplePerDiem.BOSTON_LOCATION)
            assertEquals(PerDiemWidgetUi("$86", "Boston", "$239"), awaitItem().perDiem)

            stays.add(stay(SamplePerDiem.NYC_LOCATION.id, "New York City, NY"))
            assertEquals(PerDiemWidgetUi("$92", "New York City", "$286"), awaitItem().perDiem)

            store.update { RecordingState.Recording(9, clock.instant(), Distance.ofMiles(12.6), Duration.ofSeconds(1_458), null, null, RecordingPolicy.HIGH) }
            assertEquals(RecordingWidgetUi.Active(false, "12.6 mi", "24:18", "$9.58"), awaitItem().recording)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
