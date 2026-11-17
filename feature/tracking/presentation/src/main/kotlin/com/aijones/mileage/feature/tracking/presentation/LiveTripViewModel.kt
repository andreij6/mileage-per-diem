package com.aijones.mileage.feature.tracking.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FailureReason
import com.aijones.mileage.core.analytics.TripSource
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.model.mileageAmount
import com.aijones.mileage.core.navigation.LiveTripRoute
import com.aijones.mileage.feature.tracking.domain.ObserveDeviceStateUseCase
import com.aijones.mileage.feature.tracking.domain.ObserveRecordingStateUseCase
import com.aijones.mileage.feature.tracking.domain.PauseTripUseCase
import com.aijones.mileage.feature.tracking.domain.RecordFailureUseCase
import com.aijones.mileage.feature.tracking.domain.RecordTrackingPermissionUseCase
import com.aijones.mileage.feature.tracking.domain.RecordingState
import com.aijones.mileage.feature.tracking.domain.ResumeTripUseCase
import com.aijones.mileage.feature.tracking.domain.StartTripUseCase
import com.aijones.mileage.feature.tracking.domain.StopTripUseCase
import com.aijones.mileage.feature.trips.domain.ObserveCurrentRateUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTrackUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTripUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LiveStatus { Starting, Recording, Paused }

enum class PermissionStep { Location, Notifications, LocationSettings }

sealed interface LiveTripNav {
    data class Receipt(val tripId: Long) : LiveTripNav
    data object PreciseLocationOff : LiveTripNav
    data object Back : LiveTripNav
}

@Immutable
data class LiveTripUiState(
    val status: LiveStatus = LiveStatus.Starting,
    val tenths: Long = 0,
    val amount: Money = Money.ZERO,
    val elapsedSeconds: Long = 0,
    val speedMph: Int = 0,
    val accuracyM: Int? = null,
    val purpose: String? = null,
    val policy: String = "high",
    val batteryPct: Int? = null,
    val route: List<GeoPoint> = emptyList(),
    val permissionStep: PermissionStep? = null,
    val nav: LiveTripNav? = null,
)

@HiltViewModel(assistedFactory = LiveTripViewModel.Factory::class)
class LiveTripViewModel @AssistedInject constructor(
    @Assisted private val key: LiveTripRoute,
    observeRecordingState: ObserveRecordingStateUseCase,
    observeTrip: ObserveTripUseCase,
    observeTrack: ObserveTrackUseCase,
    observeCurrentRate: ObserveCurrentRateUseCase,
    observeDeviceState: ObserveDeviceStateUseCase,
    private val startTrip: StartTripUseCase,
    private val pauseTrip: PauseTripUseCase,
    private val resumeTrip: ResumeTripUseCase,
    private val stopTrip: StopTripUseCase,
    private val recordFailure: RecordFailureUseCase,
    private val recordPermission: RecordTrackingPermissionUseCase,
    analytics: Analytics,
) : ViewModel() {
    private data class Control(val step: PermissionStep? = null, val nav: LiveTripNav? = null, val ending: Boolean = false)

    private val control = MutableStateFlow(Control())
    private val recording = observeRecordingState()

    private val trip = recording.map { it.tripIdOrNull() }.distinctUntilChanged().flatMapLatest { id ->
        if (id == null) flowOf(null to emptyList()) else combine(observeTrip(id), observeTrack(id)) { t, points -> t to points.map { it.point } }
    }

    val state: StateFlow<LiveTripUiState> = combine(
        control,
        recording,
        trip,
        observeCurrentRate(),
        observeDeviceState().map<_, Int?> { it.batteryPct }.onStart { emit(null) },
    ) { control, rec, (trip, route), rate, battery ->
        val base = LiveTripUiState(
            purpose = trip?.purpose,
            batteryPct = battery,
            route = route,
            permissionStep = control.step,
            nav = control.nav,
        )
        when (rec) {
            RecordingState.Idle -> base
            is RecordingState.Recording -> base.copy(
                status = LiveStatus.Recording,
                tenths = rec.distance.tenthsOfMile,
                amount = mileageAmount(rec.distance, rate),
                elapsedSeconds = rec.elapsed.seconds,
                speedMph = Math.round((rec.speedMps ?: 0f) * MPS_TO_MPH),
                accuracyM = rec.accuracyM?.let { Math.round(it) },
                policy = rec.policy.name,
            )
            is RecordingState.Paused -> base.copy(
                status = LiveStatus.Paused,
                tenths = rec.distance.tenthsOfMile,
                amount = mileageAmount(rec.distance, rate),
                elapsedSeconds = rec.elapsed.seconds,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveTripUiState())

    init {
        analytics.track(AnalyticsEvent.ScreenView("live_trip"))
        val idle = recording.value is RecordingState.Idle
        when {
            !idle -> Unit
            key.startSource != null -> control.update { it.copy(step = PermissionStep.Location) }
            else -> control.update { it.copy(nav = LiveTripNav.Back) }
        }
        viewModelScope.launch {
            recording.dropWhile { it is RecordingState.Idle }.first { it is RecordingState.Idle }
            control.update { if (it.ending || it.nav != null) it else it.copy(nav = LiveTripNav.Back) }
        }
    }

    fun onLocationPermission(fine: Boolean, coarse: Boolean, asked: Boolean) {
        if (asked) recordPermission(PERMISSION_LOCATION, fine || coarse, fine)
        control.update {
            when {
                fine -> it.copy(step = PermissionStep.Notifications)
                coarse -> it.copy(step = null, nav = LiveTripNav.PreciseLocationOff)
                else -> it.copy(step = null, nav = LiveTripNav.Back)
            }
        }
    }

    fun onNotificationPermission(granted: Boolean, asked: Boolean) {
        if (asked) recordPermission(PERMISSION_NOTIFICATIONS, granted)
        control.update { it.copy(step = PermissionStep.LocationSettings) }
    }

    fun onLocationSettings(enabled: Boolean) {
        control.update { it.copy(step = null) }
        viewModelScope.launch {
            if (!enabled) {
                recordFailure(FailureReason.LOCATION_OFF)
                return@launch control.update { it.copy(nav = LiveTripNav.Back) }
            }
            val started = try {
                startTrip(tripSource(key.startSource))
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                recordFailure(FailureReason.FGS_START_FAILED)
                false
            }
            if (!started || recording.value is RecordingState.Idle) control.update { it.copy(nav = LiveTripNav.Back) }
        }
    }

    fun togglePause() {
        viewModelScope.launch {
            when (recording.value) {
                is RecordingState.Recording -> pauseTrip()
                is RecordingState.Paused -> resumeTrip()
                RecordingState.Idle -> Unit
            }
        }
    }

    fun endTrip() {
        if (control.value.ending) return
        control.update { it.copy(ending = true) }
        viewModelScope.launch {
            val id = stopTrip()
            control.update { it.copy(nav = if (id != null) LiveTripNav.Receipt(id) else LiveTripNav.Back) }
        }
    }

    fun onNavigated() = control.update { it.copy(nav = null) }

    @AssistedFactory
    interface Factory {
        fun create(key: LiveTripRoute): LiveTripViewModel
    }

    private companion object {
        const val MPS_TO_MPH = 2.2369363f
        const val PERMISSION_LOCATION = "location"
        const val PERMISSION_NOTIFICATIONS = "notifications"
    }
}

internal fun tripSource(wire: String?): TripSource = TripSource.entries.firstOrNull { it.wire == wire } ?: TripSource.APP

internal fun RecordingState.tripIdOrNull(): Long? = when (this) {
    is RecordingState.Recording -> tripId
    is RecordingState.Paused -> tripId
    RecordingState.Idle -> null
}
