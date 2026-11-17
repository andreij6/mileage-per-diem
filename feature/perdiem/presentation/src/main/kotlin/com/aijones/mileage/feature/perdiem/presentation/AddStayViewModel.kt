package com.aijones.mileage.feature.perdiem.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.model.sum
import com.aijones.mileage.core.navigation.AddStayRoute
import com.aijones.mileage.feature.perdiem.domain.AddStayUseCase
import com.aijones.mileage.feature.perdiem.domain.EstimateStayUseCase
import com.aijones.mileage.feature.perdiem.domain.GetRatesForLocationUseCase
import com.aijones.mileage.feature.perdiem.domain.PerDiemRates
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class AddStayUiState(
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val includeLodging: Boolean = false,
    val locationLabel: String = "",
    val loaded: Boolean = false,
    val days: Int = 0,
    val travelDays: Int = 0,
    val travelDayRate: Money = Money.ZERO,
    val travelDaysTotal: Money = Money.ZERO,
    val fullDays: Int = 0,
    val fullDayRate: Money = Money.ZERO,
    val fullDaysTotal: Money = Money.ZERO,
    val nights: Int = 0,
    val lodgingTotal: Money = Money.ZERO,
    val total: Money = Money.ZERO,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

private data class StayInputs(val checkIn: LocalDate, val checkOut: LocalDate, val includeLodging: Boolean, val saving: Boolean, val saved: Boolean)

@HiltViewModel(assistedFactory = AddStayViewModel.Factory::class)
class AddStayViewModel @AssistedInject constructor(
    @Assisted private val route: AddStayRoute,
    getRates: GetRatesForLocationUseCase,
    private val estimateStay: EstimateStayUseCase,
    private val addStay: AddStayUseCase,
    analytics: Analytics,
    clock: Clock,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(route: AddStayRoute): AddStayViewModel
    }

    private val today = LocalDate.now(clock)
    private val inputs = MutableStateFlow(StayInputs(today, today.plusDays(DEFAULT_NIGHTS), includeLodging = false, saving = false, saved = false))
    private val rates: StateFlow<PerDiemRates?> = flow { emit(getRates(route.locationId)) }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<AddStayUiState> = combine(inputs, rates) { input, loaded -> toUiState(input, loaded) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), toUiState(inputs.value, rates.value))

    init {
        analytics.track(AnalyticsEvent.ScreenView(SCREEN))
    }

    fun setCheckIn(date: LocalDate) = inputs.update { it.copy(checkIn = date, checkOut = maxOf(date, it.checkOut)) }

    fun setCheckOut(date: LocalDate) = inputs.update { it.copy(checkOut = maxOf(date, it.checkIn)) }

    fun setIncludeLodging(include: Boolean) = inputs.update { it.copy(includeLodging = include) }

    fun save() {
        val loaded = rates.value ?: return
        val input = inputs.value
        if (input.saving || input.saved) return
        inputs.update { it.copy(saving = true) }
        viewModelScope.launch {
            addStay(loaded, input.checkIn, input.checkOut, input.includeLodging)
            inputs.update { it.copy(saving = false, saved = true) }
        }
    }

    private fun toUiState(input: StayInputs, loaded: PerDiemRates?): AddStayUiState {
        val base = AddStayUiState(input.checkIn, input.checkOut, input.includeLodging, saving = input.saving, saved = input.saved)
        loaded ?: return base
        val estimate = estimateStay(loaded, input.checkIn, input.checkOut, input.includeLodging)
        return base.copy(
            locationLabel = loaded.location.label,
            loaded = true,
            days = estimate.days,
            travelDays = estimate.travelDays,
            travelDayRate = loaded.mie.firstLastDay,
            travelDaysTotal = estimate.lines.filter { it.isTravelDay }.map { it.mie }.sum(),
            fullDays = estimate.fullDays,
            fullDayRate = loaded.mie.total,
            fullDaysTotal = estimate.lines.filterNot { it.isTravelDay }.map { it.mie }.sum(),
            nights = estimate.nights,
            lodgingTotal = estimate.lodgingTotal,
            total = estimate.total,
        )
    }

    private companion object {
        const val SCREEN = "add_stay"
        const val DEFAULT_NIGHTS = 2L
    }
}
