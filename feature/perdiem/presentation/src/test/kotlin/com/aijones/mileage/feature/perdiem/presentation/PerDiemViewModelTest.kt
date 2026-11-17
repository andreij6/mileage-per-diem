package com.aijones.mileage.feature.perdiem.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.perdiem.domain.CurrentFiscalYearUseCase
import com.aijones.mileage.feature.perdiem.domain.EstimateStayUseCase
import com.aijones.mileage.feature.perdiem.domain.FakePerDiemRateRepository
import com.aijones.mileage.feature.perdiem.domain.FakeRecentSearchRepository
import com.aijones.mileage.feature.perdiem.domain.GetRatesForLocationUseCase
import com.aijones.mileage.feature.perdiem.domain.ObserveRecentSearchesUseCase
import com.aijones.mileage.feature.perdiem.domain.PerDiemRateRepository
import com.aijones.mileage.feature.perdiem.domain.PerDiemRates
import com.aijones.mileage.feature.perdiem.domain.SamplePerDiem
import com.aijones.mileage.feature.perdiem.domain.SearchLocationsUseCase
import java.time.Month
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class PerDiemViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val analytics = FakeAnalytics()
    private val recent = FakeRecentSearchRepository()

    private fun viewModel(rates: PerDiemRateRepository = FakePerDiemRateRepository()) = PerDiemViewModel(
        ObserveRecentSearchesUseCase(recent),
        SearchLocationsUseCase(rates, analytics),
        GetRatesForLocationUseCase(rates, recent),
        EstimateStayUseCase(),
        CurrentFiscalYearUseCase(rates, FakeClock()),
        analytics,
        FakeClock(),
    )

    private fun loaded(rates: PerDiemRates, lodgingNov: Long, tripEstimate: Money): PerDiemUiState {
        val max = FISCAL_MONTHS.maxOf { rates.lodgingByMonth.getValue(it).micros }
        return PerDiemUiState(
            fiscalYear = 2027,
            loading = false,
            locationId = rates.location.id,
            locationLabel = rates.location.label,
            isStandard = rates.location.isStandard,
            currentMonth = Month.NOVEMBER,
            lodgingThisMonth = Money.ofDollars(lodgingNov),
            mieDaily = rates.mie.total,
            bars = FISCAL_MONTHS.map { MonthBar(it, rates.lodgingByMonth.getValue(it), rates.lodgingByMonth.getValue(it).micros.toFloat() / max, it == Month.NOVEMBER) },
            breakfast = rates.mie.breakfast,
            lunch = rates.mie.lunch,
            dinner = rates.mie.dinner,
            incidentals = rates.mie.incidentals,
            tripEstimate = tripEstimate,
        )
    }

    @Test
    fun `with no recent search it shows New York City and a 3-day estimate with lodging`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(loaded(SamplePerDiem.NYC, 286, Money.ofDollars(802)), awaitItem())
        }
        assertEquals(listOf(SamplePerDiem.NYC_LOCATION), recent.observe().first())
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.PerDiemSearched(1)), analytics.events)
    }

    @Test
    fun `the most recent search is the selection and follows new selections`() = runTest {
        recent.record(SamplePerDiem.BOSTON_LOCATION)
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(loaded(SamplePerDiem.BOSTON, 239, Money.ofCents(6_450 * 2 + 8_600 + 23_900 * 2)), awaitItem())
            recent.record(SamplePerDiem.NYC_LOCATION)
            assertEquals(loaded(SamplePerDiem.NYC, 286, Money.ofDollars(802)), awaitItem())
        }
        assertEquals(emptyList(), analytics.eventsOf<AnalyticsEvent.PerDiemSearched>())
    }

    @Test
    fun `without recents or New York City the standard rate is shown`() = runTest {
        val onlyStandard = FakePerDiemRateRepository(listOf(SamplePerDiem.STANDARD))
        val withFallback = object : PerDiemRateRepository by onlyStandard {
            override suspend fun rates(locationId: Long) = onlyStandard.rates(locationId) ?: SamplePerDiem.STANDARD
        }
        viewModel(withFallback).uiState.test {
            assertEquals(loaded(SamplePerDiem.STANDARD, 113, Money.ofCents(5_100 * 2 + 6_800 + 11_300 * 2)), awaitItem())
        }
        assertEquals(emptyList(), recent.observe().first())
    }

    @Test
    fun `showing the screen tracks a screen view`() {
        viewModel().onShown()
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("per_diem")), analytics.events)
    }
}
