package com.aijones.mileage.feature.widgets.presentation

import com.aijones.mileage.feature.perdiem.domain.GetRatesForLocationUseCase
import com.aijones.mileage.feature.perdiem.domain.ObserveRecentSearchesUseCase
import com.aijones.mileage.feature.perdiem.domain.ObserveStaysUseCase
import com.aijones.mileage.feature.tracking.domain.ObserveRecordingStateUseCase
import com.aijones.mileage.feature.trips.domain.ObserveCurrentRateUseCase
import com.aijones.mileage.feature.trips.domain.ObserveMonthSummaryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import javax.inject.Inject

class ObserveWidgetSnapshotUseCase @Inject constructor(
    private val observeMonthSummary: ObserveMonthSummaryUseCase,
    private val observeRecordingState: ObserveRecordingStateUseCase,
    private val observeCurrentRate: ObserveCurrentRateUseCase,
    private val observeStays: ObserveStaysUseCase,
    private val observeRecentSearches: ObserveRecentSearchesUseCase,
    private val getRates: GetRatesForLocationUseCase,
    private val clock: Clock,
) {
    operator fun invoke(locale: Locale = Locale.getDefault()): Flow<WidgetSnapshot> {
        val today = LocalDate.now(clock)
        val perDiem = combine(observeStays(today, today), observeRecentSearches()) { stays, recent ->
            stays.firstOrNull()?.locationId ?: recent.firstOrNull()?.id
        }.distinctUntilChanged().mapLatest { id -> id?.let { getRates(it) }?.toWidgetUi(today) }
        return combine(observeMonthSummary(YearMonth.from(today)), perDiem, observeRecordingState(), observeCurrentRate()) { month, rates, recording, rate ->
            WidgetSnapshot(month.toWidgetUi(locale), rates, recording.toWidgetUi(rate))
        }
    }
}
