package com.aijones.mileage.feature.perdiem.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.perdiem.domain.CurrentFiscalYearUseCase
import com.aijones.mileage.feature.perdiem.domain.EstimateStayUseCase
import com.aijones.mileage.feature.perdiem.domain.GetRatesForLocationUseCase
import com.aijones.mileage.feature.perdiem.domain.ObserveRecentSearchesUseCase
import com.aijones.mileage.feature.perdiem.domain.PerDiemRates
import com.aijones.mileage.feature.perdiem.domain.SearchLocationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.Month
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@Immutable
data class MonthBar(val month: Month, val lodging: Money, val fraction: Float, val isCurrent: Boolean)

@Immutable
data class PerDiemUiState(
    val fiscalYear: Int,
    val loading: Boolean = true,
    val locationId: Long? = null,
    val locationLabel: String = "",
    val isStandard: Boolean = false,
    val currentMonth: Month = Month.JANUARY,
    val lodgingThisMonth: Money = Money.ZERO,
    val mieDaily: Money = Money.ZERO,
    val bars: List<MonthBar> = emptyList(),
    val breakfast: Money = Money.ZERO,
    val lunch: Money = Money.ZERO,
    val dinner: Money = Money.ZERO,
    val incidentals: Money = Money.ZERO,
    val tripEstimate: Money = Money.ZERO,
)

@HiltViewModel
class PerDiemViewModel @Inject constructor(
    observeRecentSearches: ObserveRecentSearchesUseCase,
    private val searchLocations: SearchLocationsUseCase,
    private val getRates: GetRatesForLocationUseCase,
    private val estimateStay: EstimateStayUseCase,
    currentFiscalYear: CurrentFiscalYearUseCase,
    private val analytics: Analytics,
    private val clock: Clock,
) : ViewModel() {
    private val fiscalYear = currentFiscalYear()

    val uiState: StateFlow<PerDiemUiState> = observeRecentSearches()
        .map { it.firstOrNull()?.id }
        .distinctUntilChanged()
        .mapLatest { recentId -> getRates(recentId ?: defaultLocationId())?.let(::toUiState) ?: PerDiemUiState(fiscalYear, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerDiemUiState(fiscalYear))

    fun onShown() = analytics.track(AnalyticsEvent.ScreenView(SCREEN))

    private suspend fun defaultLocationId(): Long =
        searchLocations(DEFAULT_CITY, fiscalYear).firstOrNull { it.label == DEFAULT_LABEL }?.id ?: UNKNOWN_LOCATION_ID

    private fun toUiState(rates: PerDiemRates): PerDiemUiState {
        val today = LocalDate.now(clock)
        val max = FISCAL_MONTHS.maxOf { rates.lodgingByMonth[it]?.micros ?: 0L }.coerceAtLeast(1L)
        return PerDiemUiState(
            fiscalYear = rates.location.fiscalYear,
            loading = false,
            locationId = rates.location.id,
            locationLabel = rates.location.label,
            isStandard = rates.location.isStandard,
            currentMonth = today.month,
            lodgingThisMonth = rates.lodgingFor(today),
            mieDaily = rates.mie.total,
            bars = FISCAL_MONTHS.map { month ->
                val lodging = rates.lodgingByMonth[month] ?: Money.ZERO
                MonthBar(month, lodging, lodging.micros.toFloat() / max, month == today.month)
            },
            breakfast = rates.mie.breakfast,
            lunch = rates.mie.lunch,
            dinner = rates.mie.dinner,
            incidentals = rates.mie.incidentals,
            tripEstimate = estimateStay(rates, today, today.plusDays(TRIP_DAYS - 1L), includeLodging = true).total,
        )
    }

    private companion object {
        const val SCREEN = "per_diem"
        const val DEFAULT_CITY = "New York City"
        const val DEFAULT_LABEL = "New York City, NY"
        const val TRIP_DAYS = 3
    }
}
