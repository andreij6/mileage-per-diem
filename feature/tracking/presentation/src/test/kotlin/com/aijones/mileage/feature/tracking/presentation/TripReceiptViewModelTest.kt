package com.aijones.mileage.feature.tracking.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.trips.domain.TrackPoint
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripStatus
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals

class TripReceiptViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val g = TrackingTestGraph()
    private val start = Instant.parse("2026-11-16T14:12:00Z")
    private val recording = Trip(
        id = 186, startedAt = start, status = TripStatus.RECORDING, purpose = "Client meeting",
        startLabel = "Brooklyn office", endLabel = "Acme HQ",
    )

    @Test
    fun showsReceiptOnceTheTripIsFinalized() = runTest {
        g.trips.insert(recording)
        g.trips.appendTrackPoints(186, listOf(TrackPoint(start, GeoPoint(40.0, -74.0), 5f), TrackPoint(start.plusSeconds(60), GeoPoint(40.1, -74.0), 5f)))
        val vm = g.receipt(186)
        vm.state.test {
            assertEquals(TripReceiptUiState(), expectMostRecentItem())
            g.trips.update(
                recording.copy(status = TripStatus.DONE, endedAt = start.plusSeconds(29 * 60), distance = Distance.ofMiles(18.4), ratePerMile = Money(760_000)),
            )
            assertEquals(
                TripReceiptUiState(
                    ReceiptUi(
                        tripId = 186, number = "#0186", dateLine = "Mon, Nov 16 · 9:12 – 9:41 AM",
                        route = listOf(GeoPoint(40.0, -74.0), GeoPoint(40.1, -74.0)), places = "Brooklyn office → Acme HQ",
                        distance = "18.4 mi", minutes = 29, rateFrom = "Jul 1", rate = "76¢", purpose = "Client meeting", total = "$13.98",
                    ),
                ),
                awaitItem(),
            )
        }
        assertEquals(listOf(AnalyticsEvent.ScreenView("trip_receipt")), g.analytics.events)
    }

    @Test
    fun customRateIsLabelledAsCompanyRate() = runTest {
        g.rates.setCustomRate(Money(700_000))
        g.trips.insert(recording.copy(status = TripStatus.DONE, endedAt = start.plusSeconds(3_900), distance = Distance.ofMiles(10.0), ratePerMile = Money(700_000)))
        g.receipt(186).state.test {
            val receipt = expectMostRecentItem().receipt!!
            assertEquals(null, receipt.rateFrom)
            assertEquals("70¢", receipt.rate)
            assertEquals("$7.00", receipt.total)
            assertEquals(65, receipt.minutes)
        }
    }

    @Test
    fun missingTripIsReported() = runTest {
        g.receipt(9).state.test { assertEquals(TripReceiptUiState(missing = true), expectMostRecentItem()) }
    }
}
