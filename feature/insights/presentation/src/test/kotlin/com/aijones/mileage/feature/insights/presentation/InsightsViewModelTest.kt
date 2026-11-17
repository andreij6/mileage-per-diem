package com.aijones.mileage.feature.insights.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.featureflags.FakeFeatureFlags
import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.insights.domain.BuildYearInReviewUseCase
import com.aijones.mileage.feature.insights.domain.ObserveMonthInsightsUseCase
import com.aijones.mileage.feature.insights.domain.ObserveYearInReviewEnabledUseCase
import com.aijones.mileage.feature.perdiem.domain.FakeStayRepository
import com.aijones.mileage.feature.trips.domain.FakeMileageRateRepository
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.ObserveTripsUseCase
import com.aijones.mileage.feature.trips.domain.TripCategory
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InsightsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val clock = FakeClock()
    private val analytics = FakeAnalytics()
    private val trips = FakeTripRepository(
        listOf(
            trip(1, "2026-11-10T09:00", 41.2, end = "Acme HQ"),
            trip(2, "2026-11-10T15:00", 44.8, end = "Acme HQ"),
            trip(3, "2026-11-02T08:00", 38.0, end = "Newark Supply"),
            trip(4, "2026-11-03T08:00", 99.0, category = TripCategory.PERSONAL),
            trip(5, "2026-03-03T08:00", 100.0, rateMicros = 725_000),
        ),
    )

    private fun viewModel(flags: FakeFeatureFlags): InsightsViewModel {
        Locale.setDefault(Locale.US)
        val observeTrips = ObserveTripsUseCase(trips, clock)
        return InsightsViewModel(
            ObserveMonthInsightsUseCase(observeTrips, clock),
            ObserveYearInReviewEnabledUseCase(flags),
            BuildYearInReviewUseCase(observeTrips, FakeStayRepository(), FakeMileageRateRepository(), clock),
            analytics,
            clock,
        )
    }

    @Test
    fun flagOnShowsHeatmapHighlightsAndYearCard() = runTest {
        val vm = viewModel(FakeFeatureFlags(FeatureFlag.YEAR_IN_REVIEW to true))

        vm.state.test {
            val ready = awaitItem() as InsightsUiState.Ready
            val ui = ready.insights
            assertEquals("November", ui.monthName)
            assertEquals("124.0 mi", ui.total)
            assertEquals(0, ui.leadingBlanks)
            assertEquals(30, ui.days.size)
            assertEquals(DayCellUi(10, "November 10", "86", false, 4, isToday = false, isFuture = false), ui.days[9])
            assertEquals(true, ui.days[15].isToday)
            assertEquals(true, ui.days[16].isFuture)
            assertEquals(HighlightUi("44.8 mi", "JFK → Acme HQ"), ui.longest)
            assertEquals(HighlightUi("86 mi", "Tue, Nov 10"), ui.busiest)
            assertEquals(2, ui.topStopCount)
            assertEquals("Acme HQ", ui.topStop)
            assertEquals(listOf("S", "M", "T", "W", "T", "F", "S"), ui.weekdays.map { it.letter })
            assertEquals(true, ui.weekdays[2].isMax)
            assertEquals(1f, ui.weekdays[2].fraction)
            assertEquals(YearCardUi(2026, "224"), ready.yearCard)
        }
    }

    @Test
    fun flagOffHidesYearCardAndTurningItOnShowsIt() = runTest {
        val flags = FakeFeatureFlags(FeatureFlag.YEAR_IN_REVIEW to false)
        val vm = viewModel(flags)

        vm.state.test {
            assertNull((awaitItem() as InsightsUiState.Ready).yearCard)

            flags.setOverride(FeatureFlag.YEAR_IN_REVIEW, true)

            assertEquals(YearCardUi(2026, "224"), (awaitItem() as InsightsUiState.Ready).yearCard)
        }
    }

    @Test
    fun tracksScreenAndInsightsViewedOnce() = runTest {
        val vm = viewModel(FakeFeatureFlags())

        vm.state.test {
            skipItems(1)
        }

        assertEquals(listOf(AnalyticsEvent.ScreenView("insights"), AnalyticsEvent.InsightsViewed), analytics.events)
    }
}
