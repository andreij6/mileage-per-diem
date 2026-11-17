package com.aijones.mileage.feature.trips.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.model.formatMiles
import com.aijones.mileage.feature.trips.domain.DeleteTripUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTripUseCase
import com.aijones.mileage.feature.trips.domain.ObserveVehiclesUseCase
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripCategory
import com.aijones.mileage.feature.trips.domain.UpdateTripDetailsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class VehicleOption(val id: Long, val label: String)

@Immutable
data class EditTripForm(
    val purpose: String,
    val category: TripCategory,
    val vehicleId: Long?,
    val distance: String,
    val notes: String,
)

@Immutable
data class EditTripUiState(
    val loading: Boolean = true,
    val form: EditTripForm? = null,
    val vehicles: List<VehicleOption> = emptyList(),
    val recordedDistance: String = "",
    val changes: Int = 0,
    val canSave: Boolean = false,
    val saving: Boolean = false,
    val done: Boolean = false,
) {
    val vehicleLabel: String? get() = vehicles.firstOrNull { it.id == form?.vehicleId }?.label
}

@HiltViewModel(assistedFactory = EditTripViewModel.Factory::class)
class EditTripViewModel @AssistedInject constructor(
    @Assisted private val tripId: Long,
    private val observeTrip: ObserveTripUseCase,
    observeVehicles: ObserveVehiclesUseCase,
    private val updateTripDetails: UpdateTripDetailsUseCase,
    private val deleteTrip: DeleteTripUseCase,
    analytics: Analytics,
) : ViewModel() {
    private val original = MutableStateFlow<Trip?>(null)
    private val form = MutableStateFlow<EditTripForm?>(null)
    private val progress = MutableStateFlow(Progress())

    private data class Progress(val saving: Boolean = false, val done: Boolean = false)

    val state: StateFlow<EditTripUiState> = combine(original, form, observeVehicles(), progress) { trip, f, vehicles, p ->
        if (trip == null || f == null) return@combine EditTripUiState(loading = !p.done, done = p.done)
        val changes = changedFields(trip, f)
        EditTripUiState(
            loading = false,
            form = f,
            vehicles = vehicles.map { VehicleOption(it.id, "${it.kind} · ${it.name}") },
            recordedDistance = (trip.recordedDistance ?: trip.distance).formatMiles(),
            changes = changes,
            canSave = changes > 0 && parseMiles(f.distance) != null && !p.saving,
            saving = p.saving,
            done = p.done,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditTripUiState())

    init {
        analytics.track(AnalyticsEvent.ScreenView("trip_edit"))
        viewModelScope.launch {
            val trip = observeTrip(tripId).first()
            if (trip == null) {
                progress.value = Progress(done = true)
                return@launch
            }
            original.value = trip
            form.value = EditTripForm(trip.purpose.orEmpty(), trip.category, trip.vehicleId, trip.distance.formatMiles(), trip.notes.orEmpty())
        }
    }

    fun edit(change: (EditTripForm) -> EditTripForm) = form.update { it?.let(change) }

    fun save() {
        val trip = original.value ?: return
        val f = form.value ?: return
        val distance = parseMiles(f.distance) ?: return
        if (changedFields(trip, f) == 0 || progress.value.saving) return
        progress.value = Progress(saving = true)
        viewModelScope.launch {
            updateTripDetails(tripId, f.purpose, f.category, f.vehicleId, distance, f.notes)
            progress.value = Progress(done = true)
        }
    }

    fun delete() {
        progress.value = Progress(saving = true)
        viewModelScope.launch {
            deleteTrip(tripId)
            progress.value = Progress(done = true)
        }
    }

    private fun changedFields(trip: Trip, f: EditTripForm): Int = listOf(
        f.purpose.trim().ifEmpty { null } != trip.purpose,
        f.category != trip.category,
        f.vehicleId != trip.vehicleId,
        parseMiles(f.distance)?.tenthsOfMile != trip.distance.tenthsOfMile,
        f.notes.trim().ifEmpty { null } != trip.notes,
    ).count { it }

    @AssistedFactory
    interface Factory {
        fun create(tripId: Long): EditTripViewModel
    }
}
