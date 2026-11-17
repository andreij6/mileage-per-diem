package com.aijones.mileage.feature.trips.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.model.formatCentsPerMile
import com.aijones.mileage.core.model.formatUsd
import com.aijones.mileage.feature.trips.domain.ObserveMonthSummaryUseCase
import com.aijones.mileage.feature.trips.domain.ObserveRecentTripsUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTripsNeedingReviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@Immutable
data class HomeUiState(
    val loading: Boolean = true,
    val month: String = "",
    val reimbursement: String = "",
    val odometerTenths: Long = 0,
    val tripCount: Int = 0,
    val rate: String = "",
    val needsReview: Int = 0,
    val recent: List<TripRowUi> = emptyList(),
) {
    val empty: Boolean get() = !loading && recent.isEmpty()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeMonthSummary: ObserveMonthSummaryUseCase,
    observeRecentTrips: ObserveRecentTripsUseCase,
    observeTripsNeedingReview: ObserveTripsNeedingReviewUseCase,
    analytics: Analytics,
    clock: Clock,
) : ViewModel() {
    private val month = YearMonth.now(clock)
    private val monthLabel = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))

    val state: StateFlow<HomeUiState> = combine(
        observeMonthSummary(month),
        observeRecentTrips(RECENT_LIMIT),
        observeTripsNeedingReview(),
    ) { summary, recent, review ->
        HomeUiState(
            loading = false,
            month = monthLabel,
            reimbursement = summary.reimbursement.formatUsd(),
            odometerTenths = summary.distance.tenthsOfMile,
            tripCount = summary.trips,
            rate = summary.currentRate.formatCentsPerMile(),
            needsReview = review.size,
            recent = recent.map { it.toRowUi(clock) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(month = monthLabel))

    init {
        analytics.track(AnalyticsEvent.ScreenView("trips"))
    }

    private companion object {
        const val RECENT_LIMIT = 4
    }
}
