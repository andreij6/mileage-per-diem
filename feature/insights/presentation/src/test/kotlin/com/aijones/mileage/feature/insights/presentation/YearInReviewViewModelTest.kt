package com.aijones.mileage.feature.insights.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.navigation.YearInReviewRoute
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.insights.domain.BuildYearInReviewUseCase
import com.aijones.mileage.feature.insights.domain.ExportShareCardUseCase
import com.aijones.mileage.feature.insights.domain.FakeShareCardWriter
import com.aijones.mileage.feature.perdiem.domain.FakeStayRepository
import com.aijones.mileage.feature.trips.domain.FakeMileageRateRepository
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.ObserveTripsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class YearInReviewViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val clock = FakeClock()
    private val analytics = FakeAnalytics()
    private val writer = FakeShareCardWriter()
    private val trips = FakeTripRepository(
        listOf(
            trip(1, "2026-03-03T08:00", 4_980.0, end = "Acme HQ, Midtown", rateMicros = 725_000),
            trip(2, "2026-09-10T09:00", 4_432.0, end = "JFK Airport"),
            trip(3, "2026-10-10T09:00", 0.0, end = "Acme HQ, Midtown"),
        ),
    )

    private fun viewModel(): YearInReviewViewModel {
        val observeTrips = ObserveTripsUseCase(trips, clock)
        return YearInReviewViewModel(
            YearInReviewRoute(2026),
            BuildYearInReviewUseCase(observeTrips, FakeStayRepository(), FakeMileageRateRepository(), clock),
            ExportShareCardUseCase(writer, analytics),
            analytics,
        )
    }

    private val expectedStories = StoriesUi(
        year = 2026,
        tenths = 94_120,
        earthPercent = 38,
        places = listOf(PlaceUi(1, "Acme HQ, Midtown", 2, 1f), PlaceUi(2, "JFK Airport", 1, 0.5f)),
        total = "$6,978.82",
        periods = listOf(PeriodUi("Jan – Jun", "4,980", "72.5¢", "$3,610.50"), PeriodUi("Jul – Dec", "4,432", "76¢", "$3,368.32")),
        perDiem = null,
        cities = 0,
    )

    @Test
    fun loadsStoriesStartingAtTheFirst() = runTest {
        viewModel().state.test {
            assertEquals(YearInReviewUiState.Ready(expectedStories, current = 0), awaitItem())
        }
        assertEquals(listOf(AnalyticsEvent.ScreenView("year_in_review"), AnalyticsEvent.YearInReviewViewed(1)), analytics.events)
    }

    @Test
    fun storiesAdvanceAndGoBackWithinBoundsTrackingEachStoryOnce() = runTest {
        val vm = viewModel()
        vm.state.test {
            skipItems(1)

            vm.showStory(1)
            assertEquals(1, (awaitItem() as YearInReviewUiState.Ready).current)
            vm.showStory(2)
            assertEquals(2, (awaitItem() as YearInReviewUiState.Ready).current)
            vm.showStory(3)
            vm.showStory(0)
            assertEquals(0, (awaitItem() as YearInReviewUiState.Ready).current)
            vm.showStory(-1)
            vm.showStory(1)
            assertEquals(1, (awaitItem() as YearInReviewUiState.Ready).current)
        }
        assertEquals((1..3).map { AnalyticsEvent.YearInReviewViewed(it) }, analytics.eventsOf<AnalyticsEvent.YearInReviewViewed>())
    }

    @Test
    fun shareExportsTheMoneyCardAndExposesTheUriUntilHandled() = runTest {
        val vm = viewModel()
        val png = byteArrayOf(1, 2, 3)
        vm.state.test {
            skipItems(1)

            vm.share(png)

            assertEquals(true, (awaitItem() as YearInReviewUiState.Ready).sharing)
            val shared = awaitItem() as YearInReviewUiState.Ready
            assertEquals("/cache/share/year-in-review-3.png", shared.shareUri)
            assertEquals(false, shared.sharing)

            vm.onShareHandled()
            assertEquals(null, (awaitItem() as YearInReviewUiState.Ready).shareUri)
        }
        assertContentEquals(png, writer.saved["year-in-review-3.png"])
        assertEquals(listOf(AnalyticsEvent.ShareCardExported(3)), analytics.eventsOf<AnalyticsEvent.ShareCardExported>())
    }
}
