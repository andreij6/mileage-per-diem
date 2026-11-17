package com.aijones.mileage.feature.perdiem.domain

import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class SearchLocationsUseCase @Inject constructor(private val rates: PerDiemRateRepository, private val analytics: Analytics) {
    suspend operator fun invoke(query: String, fiscalYear: Int): List<PerDiemLocation> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return rates.search(q, fiscalYear).also { analytics.track(AnalyticsEvent.PerDiemSearched(it.size)) }
    }
}

class GetRatesForLocationUseCase @Inject constructor(
    private val rates: PerDiemRateRepository,
    private val recent: RecentSearchRepository,
) {
    suspend operator fun invoke(locationId: Long): PerDiemRates? =
        rates.rates(locationId)?.also { if (!it.location.isStandard) recent.record(it.location) }
}

class EstimateStayUseCase @Inject constructor() {
    operator fun invoke(rates: PerDiemRates, checkIn: LocalDate, checkOut: LocalDate, includeLodging: Boolean): StayEstimate =
        PerDiemCalculator.estimate(rates, checkIn, checkOut, includeLodging)
}

class AddStayUseCase @Inject constructor(private val stays: StayRepository, private val analytics: Analytics) {
    suspend operator fun invoke(rates: PerDiemRates, checkIn: LocalDate, checkOut: LocalDate, includeLodging: Boolean): Long {
        val estimate = PerDiemCalculator.estimate(rates, checkIn, checkOut, includeLodging)
        val id = stays.add(
            Stay(
                locationId = rates.location.id,
                locationLabel = rates.location.label,
                fiscalYear = rates.location.fiscalYear,
                checkIn = checkIn,
                checkOut = checkOut,
                includeLodging = includeLodging,
                mieDaily = rates.mie.total,
                mieTotal = estimate.mieTotal,
                lodgingTotal = estimate.lodgingTotal,
            ),
        )
        analytics.track(AnalyticsEvent.StayAdded(estimate.days, includeLodging))
        return id
    }
}

class ObserveStaysUseCase @Inject constructor(private val stays: StayRepository) {
    operator fun invoke(from: LocalDate, to: LocalDate): Flow<List<Stay>> = stays.observeOverlapping(from, to)
}

class ObserveRecentSearchesUseCase @Inject constructor(private val recent: RecentSearchRepository) {
    operator fun invoke(): Flow<List<PerDiemLocation>> = recent.observe()
}

class CurrentFiscalYearUseCase @Inject constructor(private val rates: PerDiemRateRepository, private val clock: Clock) {
    operator fun invoke(): Int = rates.fiscalYearFor(LocalDate.now(clock))
}
