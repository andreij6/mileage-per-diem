package com.aijones.mileage.feature.perdiem.domain

import com.aijones.mileage.core.model.Money
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

data class PerDiemLocation(
    val id: Long,
    val city: String,
    val state: String,
    val county: String?,
    val fiscalYear: Int,
    val isStandard: Boolean,
) {
    val label: String get() = "$city, $state"
}

data class MieBreakdown(
    val total: Money,
    val breakfast: Money,
    val lunch: Money,
    val dinner: Money,
    val incidentals: Money,
    val firstLastDay: Money,
)

data class PerDiemRates(val location: PerDiemLocation, val lodgingByMonth: Map<Month, Money>, val mie: MieBreakdown) {
    fun lodgingFor(night: LocalDate): Money = lodgingByMonth[night.month] ?: Money.ZERO
}

data class Stay(
    val id: Long = 0,
    val locationId: Long,
    val locationLabel: String,
    val fiscalYear: Int,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val includeLodging: Boolean,
    val mieDaily: Money,
    val mieTotal: Money,
    val lodgingTotal: Money,
) {
    val total: Money get() = mieTotal + lodgingTotal
    val days: Int get() = ChronoUnit.DAYS.between(checkIn, checkOut).toInt() + 1
}

interface PerDiemRateRepository {
    suspend fun search(query: String, fiscalYear: Int): List<PerDiemLocation>
    suspend fun rates(locationId: Long): PerDiemRates?
    suspend fun standardRates(fiscalYear: Int): PerDiemRates
    fun fiscalYearFor(date: LocalDate): Int = if (date.monthValue >= 10) date.year + 1 else date.year
}

interface StayRepository {
    fun observeOverlapping(from: LocalDate, to: LocalDate): Flow<List<Stay>>
    suspend fun add(stay: Stay): Long
    suspend fun delete(id: Long)
}

interface RecentSearchRepository {
    fun observe(): Flow<List<PerDiemLocation>>
    suspend fun record(location: PerDiemLocation)
}
