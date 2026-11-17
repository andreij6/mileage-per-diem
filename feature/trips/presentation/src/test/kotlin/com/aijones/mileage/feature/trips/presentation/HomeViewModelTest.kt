package com.aijones.mileage.feature.trips.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.trips.domain.FakeMileageRateRepository
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.ObserveCurrentRateUseCase
import com.aijones.mileage.feature.trips.domain.ObserveMonthSummaryUseCase
import com.aijones.mileage.feature.trips.domain.ObserveRecentTripsUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTripsNeedingReviewUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTripsUseCase
import com.aijones.mileage.feature.trips.domain.TripCategory
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class HomeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val clock = FakeClock()
    private val analytics = FakeAnalytics()

    fun viewModel(repo: FakeTripRepository) = HomeViewModel(
        ObserveMonthSummaryUseCase(ObserveTripsUseCase(repo, clock), ObserveCurrentRateUseCase(FakeMileageRateRepository(), clock)),
        ObserveRecentTripsUseCase(repo),
        ObserveTripsNeedingReviewUseCase(repo),
        analytics,
        clock,
    )

    @Test
    fun showsMonthTotalsReviewCountAndRecentTripsWithRelativeDays() = runTest {
        val repo = FakeTripRepository(
            listOf(
                trip(1, "2026-11-16T09:12"),
                trip(2, "2026-11-15T06:40", minutes = 42, miles = 16.2, to = "JFK Airport", purpose = "Travel"),
                trip(3, "2026-11-13T11:05", miles = 24.8, needsReview = true, purpose = null),
                trip(4, "2026-11-12T08:00", miles = 10.0, category = TripCategory.PERSONAL),
                trip(5, "2026-10-31T08:00", miles = 99.0),
            ),
        )
        viewModel(repo).state.test {
            val s = awaitItem()
            assertEquals(
                HomeUiState(
                    loading = false,
                    month = "November 2026",
                    reimbursement = "$26.30",
                    odometerTenths = 346,
                    tripCount = 2,
                    rate = "76¢",
                    needsReview = 1,
                    recent = listOf(
                        TripRowUi(1, "Office", "Acme HQ", DayLabel.Today, "9:12 – 9:41", "18.4 mi", "$13.98", "Client meeting"),
                        TripRowUi(2, "Office", "JFK Airport", DayLabel.Yesterday, "6:40 – 7:22", "16.2 mi", "$12.31", "Travel"),
                        TripRowUi(3, "Office", "Acme HQ", DayLabel.On("Nov 13"), "11:05 – 11:34", "24.8 mi", "$18.85", null),
                        TripRowUi(4, "Office", "Acme HQ", DayLabel.On("Nov 12"), "8:00 – 8:29", "10.0 mi", "$0.00", "Client meeting"),
                    ),
                ),
                s,
            )
        }
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("trips")), analytics.events)
    }

    @Test
    fun emptyMonthShowsEmptyStateAtCurrentRate() = runTest {
        viewModel(FakeTripRepository()).state.test {
            val s = awaitItem()
            assertEquals(true, s.empty)
            assertEquals("$0.00", s.reimbursement)
            assertEquals("76¢", s.rate)
        }
    }

    @Test
    fun newTripUpdatesTheHomeScreenLive() = runTest {
        val repo = FakeTripRepository()
        viewModel(repo).state.test {
            assertEquals(true, awaitItem().empty)
            repo.insert(trip(0, "2026-11-16T09:12"))
            val s = expectMostRecentItem()
            assertEquals(1, s.tripCount)
            assertEquals(184, s.odometerTenths)
            assertEquals("$13.98", s.reimbursement)
        }
    }
}
