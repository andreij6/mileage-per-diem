package com.aijones.mileage.feature.trips.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.formatMiles
import com.aijones.mileage.core.model.formatUsd
import com.aijones.mileage.core.model.sum
import com.aijones.mileage.feature.trips.domain.ObserveTripsUseCase
import com.aijones.mileage.feature.trips.domain.ObserveVehiclesUseCase
import com.aijones.mileage.feature.trips.domain.TripCategory
import com.aijones.mileage.feature.trips.domain.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@Immutable
data class TripGroupUi(val day: DayLabel, val trips: List<TripRowUi>)

@Immutable
data class TripListUiState(
    val loading: Boolean = true,
    val month: String = "",
    val businessOnly: Boolean = false,
    val vehicle: String? = null,
    val searching: Boolean = false,
    val query: String = "",
    val count: Int = 0,
    val distance: String = "",
    val total: String = "",
    val groups: List<TripGroupUi> = emptyList(),
)

private data class Filters(val businessOnly: Boolean = false, val vehicleId: Long? = null, val searching: Boolean = false, val query: String = "")

@HiltViewModel
class TripListViewModel @Inject constructor(
    observeTrips: ObserveTripsUseCase,
    observeVehicles: ObserveVehiclesUseCase,
    analytics: Analytics,
    private val clock: Clock,
) : ViewModel() {
    private val month = YearMonth.now(clock)
    private val monthLabel = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
    private val filters = MutableStateFlow(Filters())
    private var vehicles: List<Vehicle> = emptyList()

    val state: StateFlow<TripListUiState> = combine(
        observeTrips(month.atDay(1), month.atEndOfMonth()),
        observeVehicles(),
        filters,
    ) { trips, vehicles, f ->
        this.vehicles = vehicles
        val q = f.query.trim()
        val shown = trips.filter { trip ->
            (!f.businessOnly || trip.category == TripCategory.BUSINESS) &&
                (f.vehicleId == null || trip.vehicleId == f.vehicleId) &&
                (q.isEmpty() || listOfNotNull(trip.startLabel, trip.endLabel, trip.purpose).any { it.contains(q, ignoreCase = true) })
        }
        TripListUiState(
            loading = false,
            month = monthLabel,
            businessOnly = f.businessOnly,
            vehicle = vehicles.firstOrNull { it.id == f.vehicleId }?.name,
            searching = f.searching,
            query = f.query,
            count = shown.size,
            distance = shown.fold(Distance.ZERO) { acc, t -> acc + t.distance }.formatMiles(),
            total = shown.filter { it.isReimbursable }.map { it.reimbursement }.sum().formatUsd(),
            groups = shown.groupBy { it.localDate(clock) }.map { (date, dayTrips) ->
                TripGroupUi(dayLabel(date, clock, weekdayDate), dayTrips.map { it.toRowUi(clock) })
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripListUiState(month = monthLabel))

    init {
        analytics.track(AnalyticsEvent.ScreenView("all_trips"))
    }

    fun toggleBusinessOnly() = filters.update { it.copy(businessOnly = !it.businessOnly) }

    fun cycleVehicle() = filters.update { f ->
        val ids = listOf<Long?>(null) + vehicles.map { it.id }
        f.copy(vehicleId = ids[(ids.indexOf(f.vehicleId) + 1) % ids.size])
    }

    fun toggleSearch() = filters.update { if (it.searching) it.copy(searching = false, query = "") else it.copy(searching = true) }

    fun search(query: String) = filters.update { it.copy(query = query) }
}
