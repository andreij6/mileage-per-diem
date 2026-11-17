package com.aijones.mileage.feature.trips.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.model.formatMiles
import com.aijones.mileage.core.model.formatUsd
import com.aijones.mileage.core.model.mileageAmount
import com.aijones.mileage.feature.trips.domain.ClassifyTripUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTrackUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTripsNeedingReviewUseCase
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripCategory
import com.aijones.mileage.feature.trips.domain.UndoClassificationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@Immutable
data class ReviewCardUi(
    val id: Long,
    val from: String?,
    val to: String?,
    val subtitle: String,
    val distance: String,
    val worth: String,
    val purposes: List<String>,
    val points: List<GeoPoint> = emptyList(),
)

@Immutable
data class ReviewUiState(
    val loading: Boolean = true,
    val cards: List<ReviewCardUi> = emptyList(),
    val reviewed: Int = 0,
    val selectedPurpose: String? = null,
) {
    val total: Int get() = reviewed + cards.size
    val canUndo: Boolean get() = reviewed > 0
    val done: Boolean get() = !loading && cards.isEmpty()
    val top: ReviewCardUi? get() = cards.firstOrNull()
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    observeTripsNeedingReview: ObserveTripsNeedingReviewUseCase,
    observeTrack: ObserveTrackUseCase,
    private val classifyTrip: ClassifyTripUseCase,
    private val undoClassification: UndoClassificationUseCase,
    analytics: Analytics,
    private val clock: Clock,
) : ViewModel() {
    private val undoStack = MutableStateFlow<List<Trip>>(emptyList())
    private val chosenPurpose = MutableStateFlow<Map<Long, String>>(emptyMap())
    private var busy = false

    private val trips = observeTripsNeedingReview()
    private val visibleTracks = trips
        .map { list -> list.take(3).map { it.id } }
        .distinctUntilChanged()
        .flatMapLatest { ids ->
            if (ids.isEmpty()) flowOf(emptyMap())
            else combine(ids.map { id -> observeTrack(id).map { pts -> id to pts.map { it.point } } }) { it.toMap() }
        }

    val state: StateFlow<ReviewUiState> = combine(trips, visibleTracks, undoStack, chosenPurpose) { trips, tracks, undo, chosen ->
        val cards = trips.map { trip -> trip.toCard(tracks[trip.id].orEmpty()) }
        val top = cards.firstOrNull()
        ReviewUiState(
            loading = false,
            cards = cards,
            reviewed = undo.size,
            selectedPurpose = top?.let { chosen[it.id] ?: it.purposes.first() },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

    init {
        analytics.track(AnalyticsEvent.ScreenView("review_trips"))
    }

    fun selectPurpose(purpose: String) {
        val top = state.value.top ?: return
        chosenPurpose.update { it + (top.id to purpose) }
    }

    fun classify(category: TripCategory, via: String) {
        val current = state.value
        val top = current.top ?: return
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                val purpose = if (category == TripCategory.BUSINESS) current.selectedPurpose else null
                classifyTrip(top.id, category, purpose, via)?.let { previous -> undoStack.update { it + previous } }
            } finally {
                busy = false
            }
        }
    }

    fun undo() {
        val previous = undoStack.value.lastOrNull() ?: return
        undoStack.update { it.dropLast(1) }
        viewModelScope.launch { undoClassification(previous) }
    }

    private fun Trip.toCard(track: List<GeoPoint>) = ReviewCardUi(
        id = id,
        from = startLabel,
        to = endLabel,
        subtitle = "${localDate(clock).format(weekdayDate)} · ${timeRange(clock)} · ${distance.formatMiles()}",
        distance = distance.formatMiles(),
        worth = mileageAmount(distance, ratePerMile ?: Money.ZERO).formatUsd(),
        purposes = (listOfNotNull(purpose) + LIKELY_PURPOSES).distinct().take(LIKELY_PURPOSES.size),
        points = track.ifEmpty { listOfNotNull(start, end) },
    )

    companion object {
        val LIKELY_PURPOSES = listOf("Site visit", "Supplies", "Client")
    }
}
