package com.aijones.mileage.feature.perdiem.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.perdiem.domain.CurrentFiscalYearUseCase
import com.aijones.mileage.feature.perdiem.domain.GetRatesForLocationUseCase
import com.aijones.mileage.feature.perdiem.domain.ObserveRecentSearchesUseCase
import com.aijones.mileage.feature.perdiem.domain.PerDiemLocation
import com.aijones.mileage.feature.perdiem.domain.SearchLocationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Month
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class LocationRow(val id: Long, val label: String, val county: String?, val mieDaily: Money? = null)

@Immutable
data class StandardRate(val lodging: Money, val mie: Money)

@Immutable
data class PerDiemSearchUiState(
    val fiscalYear: Int,
    val query: String = "",
    val results: List<LocationRow> = emptyList(),
    val searchedQuery: String = "",
    val recents: List<LocationRow> = emptyList(),
    val standard: StandardRate? = null,
    val selected: Boolean = false,
)

@HiltViewModel
class PerDiemSearchViewModel @Inject constructor(
    private val searchLocations: SearchLocationsUseCase,
    private val getRates: GetRatesForLocationUseCase,
    observeRecentSearches: ObserveRecentSearchesUseCase,
    currentFiscalYear: CurrentFiscalYearUseCase,
    analytics: Analytics,
) : ViewModel() {
    private val fiscalYear = currentFiscalYear()
    private val query = MutableStateFlow("")
    private val selected = MutableStateFlow(false)

    private val results = query
        .debounce(DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .mapLatest { q -> q to searchLocations(q, fiscalYear).map { it.toRow() } }
        .onStart { emit("" to emptyList()) }

    private val standard = flow {
        emit(getRates(UNKNOWN_LOCATION_ID)?.takeIf { it.location.isStandard }?.let { StandardRate(it.lodgingByMonth[Month.OCTOBER] ?: Money.ZERO, it.mie.total) })
    }

    val uiState: StateFlow<PerDiemSearchUiState> = combine(query, results, observeRecentSearches(), standard, selected) { q, (searched, rows), recents, std, done ->
        PerDiemSearchUiState(fiscalYear, q, rows, searched, recents.map { it.toRow() }, std, done)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerDiemSearchUiState(fiscalYear))

    init {
        analytics.track(AnalyticsEvent.ScreenView(SCREEN))
    }

    fun onQueryChange(value: String) = query.update { value }

    fun clear() = query.update { "" }

    fun select(locationId: Long) {
        viewModelScope.launch {
            getRates(locationId)
            selected.value = true
        }
    }

    private fun PerDiemLocation.toRow() = LocationRow(id, label, county)

    private companion object {
        const val SCREEN = "per_diem_search"
        const val DEBOUNCE_MS = 150L
    }
}
