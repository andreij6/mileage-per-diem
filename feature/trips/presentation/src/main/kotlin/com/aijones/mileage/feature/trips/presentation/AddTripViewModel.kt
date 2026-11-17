package com.aijones.mileage.feature.trips.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.model.formatCentsPerMile
import com.aijones.mileage.core.model.formatMiles
import com.aijones.mileage.core.model.formatUsd
import com.aijones.mileage.core.model.mileageAmount
import com.aijones.mileage.feature.trips.domain.AddManualTripUseCase
import com.aijones.mileage.feature.trips.domain.GetMileageRateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@Immutable
data class AddTripForm(
    val date: LocalDate,
    val from: String = "",
    val to: String = "",
    val distance: String = "",
    val purpose: String = "",
    val roundTrip: Boolean = false,
)

@Immutable
data class AddTripEstimate(val distance: String, val rate: String, val amount: String)

@Immutable
data class AddTripUiState(
    val form: AddTripForm,
    val dateLabel: String,
    val rateDate: String,
    val today: LocalDate,
    val estimate: AddTripEstimate? = null,
    val canAdd: Boolean = false,
    val added: Boolean = false,
)

@HiltViewModel
class AddTripViewModel @Inject constructor(
    private val addManualTrip: AddManualTripUseCase,
    private val getMileageRate: GetMileageRateUseCase,
    analytics: Analytics,
    clock: Clock,
) : ViewModel() {
    private val today = LocalDate.now(clock)
    private val form = MutableStateFlow(AddTripForm(date = today))
    private val added = MutableStateFlow(false)
    private var saving = false
    private val rate = form.map { it.date }.distinctUntilChanged().flatMapLatest { date -> flow<Money?> { emit(getMileageRate(date)) } }

    val state: StateFlow<AddTripUiState> = combine(form, rate, added) { f, rate, added -> f.toState(rate, added) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value.toState(null, false))

    init {
        analytics.track(AnalyticsEvent.ScreenView("trip_add"))
    }

    fun edit(change: (AddTripForm) -> AddTripForm) = form.update { change(it).let { f -> f.copy(date = minOf(f.date, today)) } }

    fun add() {
        val f = form.value
        val distance = parseMiles(f.distance) ?: return
        if (saving) return
        saving = true
        viewModelScope.launch {
            addManualTrip(f.date, f.from, f.to, distance, f.roundTrip, f.purpose)
            added.value = true
        }
    }

    private fun AddTripForm.toState(rate: Money?, added: Boolean): AddTripUiState {
        val miles = parseMiles(distance)?.let { if (roundTrip) it * 2 else it }
        return AddTripUiState(
            form = this,
            dateLabel = date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)),
            rateDate = date.format(shortDate),
            today = today,
            estimate = if (miles != null && rate != null) AddTripEstimate(miles.formatMiles(), rate.formatCentsPerMile(), mileageAmount(miles, rate).formatUsd()) else null,
            canAdd = miles != null && !added,
            added = added,
        )
    }
}
