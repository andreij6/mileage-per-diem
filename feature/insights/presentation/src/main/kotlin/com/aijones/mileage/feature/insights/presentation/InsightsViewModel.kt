package com.aijones.mileage.feature.insights.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.feature.insights.domain.BuildYearInReviewUseCase
import com.aijones.mileage.feature.insights.domain.ObserveMonthInsightsUseCase
import com.aijones.mileage.feature.insights.domain.ObserveYearInReviewEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.YearMonth
import java.util.Locale
import javax.inject.Inject

sealed interface InsightsUiState {
    data object Loading : InsightsUiState
    data class Ready(val insights: InsightsUi, val yearCard: YearCardUi?) : InsightsUiState
}

@HiltViewModel
class InsightsViewModel @Inject constructor(
    observeMonthInsights: ObserveMonthInsightsUseCase,
    observeYearInReviewEnabled: ObserveYearInReviewEnabledUseCase,
    buildYearInReview: BuildYearInReviewUseCase,
    analytics: Analytics,
    clock: Clock,
) : ViewModel() {
    private val month = YearMonth.now(clock)

    val state: StateFlow<InsightsUiState> = combine(
        observeMonthInsights(month),
        observeYearInReviewEnabled().flatMapLatest { enabled ->
            if (enabled) flow<YearCardUi?> { emit(YearCardUi(month.year, buildYearInReview(month.year).distance.wholeMiles())) } else flowOf(null)
        },
    ) { insights, yearCard ->
        InsightsUiState.Ready(insights.toUi(Locale.getDefault(), clock.zone), yearCard)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState.Loading)

    init {
        analytics.track(AnalyticsEvent.ScreenView("insights"))
        analytics.track(AnalyticsEvent.InsightsViewed)
    }
}
