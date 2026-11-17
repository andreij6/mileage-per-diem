package com.aijones.mileage.feature.tracking.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.aijones.mileage.core.analytics.FailureReason
import com.aijones.mileage.core.model.formatMiles
import com.aijones.mileage.core.navigation.LiveTripRoute
import com.aijones.mileage.core.navigation.TripReceiptRoute
import com.aijones.mileage.feature.tracking.domain.DistanceFilter
import com.aijones.mileage.feature.tracking.domain.FinalizeTripUseCase
import com.aijones.mileage.feature.tracking.domain.ObserveRecordingStateUseCase
import com.aijones.mileage.feature.tracking.domain.RecordFailureUseCase
import com.aijones.mileage.feature.tracking.domain.RecordingState
import com.aijones.mileage.feature.tracking.domain.ResumeInterruptedTripUseCase
import com.aijones.mileage.feature.tracking.domain.toFix
import com.aijones.mileage.feature.trips.domain.DeleteTripUseCase
import com.aijones.mileage.feature.trips.domain.ObserveInterruptedTripUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTrackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@Immutable
data class InterruptedTripUi(val tripId: Long, val stoppedAt: String, val distance: String)

@Immutable
data class InterruptedTripUiState(val trip: InterruptedTripUi? = null, val busy: Boolean = false, val nav: NavKey? = null)

@HiltViewModel
class InterruptedTripViewModel @Inject constructor(
    observeRecordingState: ObserveRecordingStateUseCase,
    observeInterruptedTrip: ObserveInterruptedTripUseCase,
    observeTrack: ObserveTrackUseCase,
    private val resumeInterrupted: ResumeInterruptedTripUseCase,
    private val finalizeTrip: FinalizeTripUseCase,
    private val deleteTrip: DeleteTripUseCase,
    private val recordFailure: RecordFailureUseCase,
    private val clock: Clock,
) : ViewModel() {
    private data class Control(val dismissed: Long? = null, val busy: Boolean = false, val nav: NavKey? = null)

    private val control = MutableStateFlow(Control())

    private val handledInThisProcess = observeRecordingState()
        .scan(false) { seen, state -> seen || state !is RecordingState.Idle }
        .distinctUntilChanged()

    private val interrupted = observeInterruptedTrip(handledInThisProcess).flatMapLatest { trip ->
        if (trip == null) {
            flowOf(null)
        } else {
            observeTrack(trip.id).map { points ->
                val filter = DistanceFilter().apply { points.forEach { accept(it.toFix()) } }
                val stopped = (points.lastOrNull()?.t ?: trip.startedAt).atZone(clock.zone)
                InterruptedTripUi(trip.id, stopped.format(TIME), filter.total.formatMiles())
            }
        }
    }

    val state: StateFlow<InterruptedTripUiState> = combine(interrupted, control) { trip, control ->
        InterruptedTripUiState(trip = trip?.takeUnless { it.tripId == control.dismissed }, busy = control.busy, nav = control.nav)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InterruptedTripUiState())

    fun resume() = act { _ ->
        val resumed = try {
            resumeInterrupted() != null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            recordFailure(FailureReason.FGS_START_FAILED)
            false
        }
        if (resumed) LiveTripRoute() else null
    }

    fun finish() = act { id ->
        finalizeTrip(id, interrupted = true)
        TripReceiptRoute(id)
    }

    fun discard() = act { id ->
        deleteTrip(id)
        null
    }

    fun dismiss() {
        val id = state.value.trip?.tripId ?: return
        control.update { it.copy(dismissed = id) }
    }

    fun onNavigated() = control.update { it.copy(nav = null) }

    private fun act(block: suspend (Long) -> NavKey?) {
        val id = state.value.trip?.tripId ?: return
        if (control.value.busy) return
        control.update { it.copy(busy = true) }
        viewModelScope.launch {
            val nav = try {
                block(id)
            } finally {
                control.update { it.copy(busy = false) }
            }
            control.update { it.copy(nav = nav, dismissed = id) }
        }
    }

    private companion object {
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    }
}
