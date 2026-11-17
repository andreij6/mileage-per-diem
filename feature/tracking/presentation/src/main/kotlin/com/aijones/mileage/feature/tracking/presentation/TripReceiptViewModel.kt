package com.aijones.mileage.feature.tracking.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.model.formatCentsPerMile
import com.aijones.mileage.core.model.formatMiles
import com.aijones.mileage.core.model.formatUsd
import com.aijones.mileage.core.navigation.TripReceiptRoute
import com.aijones.mileage.feature.settings.domain.MileageRateSettings
import com.aijones.mileage.feature.settings.domain.ObserveMileageRateSettingsUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTrackUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTripUseCase
import com.aijones.mileage.feature.trips.domain.TrackPoint
import com.aijones.mileage.feature.trips.domain.Trip
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Immutable
data class ReceiptUi(
    val tripId: Long,
    val number: String,
    val dateLine: String,
    val route: List<GeoPoint>,
    val places: String?,
    val distance: String,
    val minutes: Long,
    val rateFrom: String?,
    val rate: String,
    val purpose: String?,
    val total: String,
)

@Immutable
data class TripReceiptUiState(val receipt: ReceiptUi? = null, val missing: Boolean = false)

@HiltViewModel(assistedFactory = TripReceiptViewModel.Factory::class)
class TripReceiptViewModel @AssistedInject constructor(
    @Assisted key: TripReceiptRoute,
    observeTrip: ObserveTripUseCase,
    observeTrack: ObserveTrackUseCase,
    observeRates: ObserveMileageRateSettingsUseCase,
    private val clock: Clock,
    analytics: Analytics,
) : ViewModel() {
    val state: StateFlow<TripReceiptUiState> =
        combine(observeTrip(key.tripId), observeTrack(key.tripId), observeRates()) { trip, points, rates ->
            when {
                trip == null -> TripReceiptUiState(missing = true)
                !trip.isFinished -> TripReceiptUiState()
                else -> TripReceiptUiState(receipt = trip.toReceipt(points, rates))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripReceiptUiState())

    init {
        analytics.track(AnalyticsEvent.ScreenView("trip_receipt"))
    }

    private fun Trip.toReceipt(points: List<TrackPoint>, rates: MileageRateSettings): ReceiptUi {
        val zone = clock.zone
        val start = startedAt.atZone(zone)
        val end = (endedAt ?: startedAt).atZone(zone)
        val day = LocalDate.ofInstant(startedAt, zone)
        val rate = ratePerMile ?: rates.effectiveRate
        val irs = rates.irsRates.firstOrNull { day in it && it.perMile == rate }
        return ReceiptUi(
            tripId = id,
            number = "#" + id.toString().padStart(4, '0'),
            dateLine = "${start.format(DAY)} · ${start.format(if (start.hour / 12 == end.hour / 12) TIME else TIME_AMPM)} – ${end.format(TIME_AMPM)}",
            route = points.map { it.point },
            places = listOfNotNull(startLabel, endLabel).joinToString(" → ").ifEmpty { null },
            distance = distance.formatMiles(),
            minutes = duration?.toMinutes() ?: 0,
            rateFrom = irs?.from?.format(RATE_FROM),
            rate = rate.formatCentsPerMile(),
            purpose = purpose,
            total = reimbursement.formatUsd(),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(key: TripReceiptRoute): TripReceiptViewModel
    }

    private companion object {
        val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm", Locale.US)
        val TIME_AMPM: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        val RATE_FROM: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    }
}
