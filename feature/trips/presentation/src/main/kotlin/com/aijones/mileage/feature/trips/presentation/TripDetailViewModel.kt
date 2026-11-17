package com.aijones.mileage.feature.trips.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.model.distanceTo
import com.aijones.mileage.core.model.formatCentsPerMile
import com.aijones.mileage.core.model.formatMiles
import com.aijones.mileage.core.model.formatUsd
import com.aijones.mileage.core.model.mileageAmount
import com.aijones.mileage.feature.trips.domain.DeleteTripUseCase
import com.aijones.mileage.feature.trips.domain.ObserveRouteReplayEnabledUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTrackUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTripUseCase
import com.aijones.mileage.feature.trips.domain.ObserveVehiclesUseCase
import com.aijones.mileage.feature.trips.domain.SpeedProfile
import com.aijones.mileage.feature.trips.domain.TrackPoint
import com.aijones.mileage.feature.trips.domain.TripCategory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import kotlin.math.roundToInt

@Immutable
data class ReplayUi(
    val speeds: List<Float>,
    val maxMph: Float,
    val fraction: Float,
    val routeFraction: Float,
    val time: String,
    val mph: Int,
    val start: String,
    val end: String,
    val durationMs: Long,
    val playing: Boolean,
)

@Immutable
data class TripDetailUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val points: List<GeoPoint> = emptyList(),
    val from: String? = null,
    val to: String? = null,
    val startTime: String = "",
    val endTime: String? = null,
    val distance: String = "",
    val durationMinutes: Int? = null,
    val rate: String = "",
    val amount: String = "",
    val purpose: String? = null,
    val category: TripCategory = TripCategory.BUSINESS,
    val vehicle: String? = null,
    val replay: ReplayUi? = null,
)

private data class Replay(val fraction: Float = 0f, val playing: Boolean = false)

@HiltViewModel(assistedFactory = TripDetailViewModel.Factory::class)
class TripDetailViewModel @AssistedInject constructor(
    @Assisted private val tripId: Long,
    observeTrip: ObserveTripUseCase,
    observeTrack: ObserveTrackUseCase,
    observeVehicles: ObserveVehiclesUseCase,
    observeRouteReplayEnabled: ObserveRouteReplayEnabledUseCase,
    private val deleteTrip: DeleteTripUseCase,
    analytics: Analytics,
    private val clock: Clock,
) : ViewModel() {
    private val replay = MutableStateFlow(Replay())

    val state: StateFlow<TripDetailUiState> = combine(
        observeTrip(tripId),
        observeTrack(tripId).map { it to SpeedProfile.from(it) },
        observeVehicles(),
        observeRouteReplayEnabled(),
        replay,
    ) { trip, (track, profile), vehicles, replayEnabled, r ->
        if (trip == null) return@combine TripDetailUiState(loading = false, missing = true)
        val rate = trip.ratePerMile
        TripDetailUiState(
            loading = false,
            points = track.map { it.point }.ifEmpty { listOfNotNull(trip.start, trip.end) },
            from = trip.startLabel,
            to = trip.endLabel,
            startTime = trip.startedAt.atZone(clock.zone).format(meridiemTime),
            endTime = trip.endedAt?.atZone(clock.zone)?.format(meridiemTime),
            distance = trip.distance.formatMiles(),
            durationMinutes = trip.duration?.toMinutes()?.toInt(),
            rate = rate?.formatCentsPerMile().orEmpty(),
            amount = (rate?.let { mileageAmount(trip.distance, it) })?.formatUsd().orEmpty(),
            purpose = trip.purpose,
            category = trip.category,
            vehicle = vehicles.firstOrNull { it.id == trip.vehicleId }?.let { "${it.kind} · ${it.name}" },
            replay = if (replayEnabled && profile.samples.isNotEmpty()) replayUi(profile, track, r) else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripDetailUiState())

    init {
        analytics.track(AnalyticsEvent.ScreenView("trip_detail"))
    }

    fun scrub(fraction: Float) = replay.update { it.copy(fraction = fraction.coerceIn(0f, 1f)) }

    fun setPlaying(playing: Boolean) = replay.update { r ->
        r.copy(playing = playing, fraction = if (playing && r.fraction >= 1f) 0f else r.fraction)
    }

    fun delete() {
        viewModelScope.launch { deleteTrip(tripId) }
    }

    private fun replayUi(profile: SpeedProfile, track: List<TrackPoint>, r: Replay): ReplayUi {
        val sample = profile.sampleAt(r.fraction.toDouble())
        return ReplayUi(
            speeds = profile.samples,
            maxMph = profile.maxMph,
            fraction = r.fraction,
            routeFraction = routeFractionAt(track, sample.t),
            time = sample.t.atZone(clock.zone).format(meridiemTime),
            mph = sample.mph.roundToInt(),
            start = profile.start.atZone(clock.zone).format(clockTime),
            end = profile.end.atZone(clock.zone).format(clockTime),
            durationMs = profile.duration.toMillis(),
            playing = r.playing,
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(tripId: Long): TripDetailViewModel
    }
}

internal fun routeFractionAt(track: List<TrackPoint>, t: Instant): Float {
    if (track.size < 2) return 0f
    val cumulative = track.zipWithNext { a, b -> a.point.distanceTo(b.point).meters }.runningFold(0.0) { acc, d -> acc + d }
    val total = cumulative.last().takeIf { it > 0 } ?: return 0f
    val i = track.indexOfLast { !it.t.isAfter(t) }.coerceIn(0, track.lastIndex - 1)
    val a = track[i]
    val b = track[i + 1]
    val span = (b.t.toEpochMilli() - a.t.toEpochMilli()).coerceAtLeast(1)
    val k = ((t.toEpochMilli() - a.t.toEpochMilli()).toDouble() / span).coerceIn(0.0, 1.0)
    return ((cumulative[i] + (cumulative[i + 1] - cumulative[i]) * k) / total).toFloat()
}
