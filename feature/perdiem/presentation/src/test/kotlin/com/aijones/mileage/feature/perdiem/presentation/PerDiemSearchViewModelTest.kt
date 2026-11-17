package com.aijones.mileage.feature.perdiem.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.perdiem.domain.CurrentFiscalYearUseCase
import com.aijones.mileage.feature.perdiem.domain.FakePerDiemRateRepository
import com.aijones.mileage.feature.perdiem.domain.FakeRecentSearchRepository
import com.aijones.mileage.feature.perdiem.domain.GetRatesForLocationUseCase
import com.aijones.mileage.feature.perdiem.domain.ObserveRecentSearchesUseCase
import com.aijones.mileage.feature.perdiem.domain.PerDiemRateRepository
import com.aijones.mileage.feature.perdiem.domain.SamplePerDiem
import com.aijones.mileage.feature.perdiem.domain.SearchLocationsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class PerDiemSearchViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val analytics = FakeAnalytics()
    private val recent = FakeRecentSearchRepository()
    private val fake = FakePerDiemRateRepository()
    private val rates = object : PerDiemRateRepository by fake {
        override suspend fun rates(locationId: Long) = fake.rates(locationId) ?: SamplePerDiem.STANDARD
    }

    private fun viewModel() = PerDiemSearchViewModel(
        SearchLocationsUseCase(rates, analytics),
        GetRatesForLocationUseCase(rates, recent),
        ObserveRecentSearchesUseCase(recent),
        CurrentFiscalYearUseCase(rates, FakeClock()),
        analytics,
    )

    private val standard = StandardRate(Money.ofDollars(113), Money.ofDollars(68))
    private val boston = LocationRow(3, "Boston, MA", "Suffolk")
    private val nyc = LocationRow(2, "New York City, NY", "New York")

    @Test
    fun `typing is debounced 150 ms and searches once`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(PerDiemSearchUiState(2027, standard = standard), awaitItem())
            vm.onQueryChange("b")
            assertEquals(PerDiemSearchUiState(2027, query = "b", standard = standard), awaitItem())
            vm.onQueryChange("bo")
            assertEquals(PerDiemSearchUiState(2027, query = "bo", standard = standard), awaitItem())
            vm.onQueryChange("bos")
            assertEquals(PerDiemSearchUiState(2027, query = "bos", standard = standard), awaitItem())
            advanceTimeBy(149)
            runCurrent()
            expectNoEvents()
            advanceTimeBy(2)
            assertEquals(PerDiemSearchUiState(2027, "bos", listOf(boston), "bos", standard = standard), awaitItem())
            vm.clear()
            assertEquals(PerDiemSearchUiState(2027, "", listOf(boston), "bos", standard = standard), awaitItem())
            assertEquals(PerDiemSearchUiState(2027, standard = standard), awaitItem())
        }
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("per_diem_search"), AnalyticsEvent.PerDiemSearched(1)), analytics.events)
    }

    @Test
    fun `a query with no match reports an empty search`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            vm.onQueryChange("zzz")
            skipItems(1)
            assertEquals(PerDiemSearchUiState(2027, "zzz", emptyList(), "zzz", standard = standard), awaitItem())
        }
        assertEquals(listOf(AnalyticsEvent.PerDiemSearched(0)), analytics.eventsOf<AnalyticsEvent.PerDiemSearched>())
    }

    @Test
    fun `selecting a result records it as the most recent and finishes`() = runTest {
        recent.record(SamplePerDiem.NYC_LOCATION)
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(PerDiemSearchUiState(2027, recents = listOf(nyc), standard = standard), awaitItem())
            vm.select(SamplePerDiem.BOSTON_LOCATION.id)
            assertEquals(PerDiemSearchUiState(2027, recents = listOf(boston, nyc), standard = standard, selected = true), expectMostRecentItem())
        }
        assertEquals(SamplePerDiem.BOSTON_LOCATION, recent.observe().first().first())
    }
}
