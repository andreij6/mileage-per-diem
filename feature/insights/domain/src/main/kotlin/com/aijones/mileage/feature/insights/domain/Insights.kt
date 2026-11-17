package com.aijones.mileage.feature.insights.domain

import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.trips.domain.Trip
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class DayCell(val date: LocalDate, val miles: Double, val level: Int, val isFuture: Boolean, val isToday: Boolean) {
    companion object {
        fun levelFor(miles: Double): Int = when {
            miles <= 0.0 -> 0
            miles < 25 -> 1
            miles < 45 -> 2
            miles < 70 -> 3
            else -> 4
        }
    }
}

data class MonthInsights(
    val month: YearMonth,
    val days: List<DayCell>,
    val totalDistance: Distance,
    val longest: Trip?,
    val busiestDay: Pair<LocalDate, Distance>?,
    val topStop: Pair<String, Int>?,
    val weekdayTotals: Map<DayOfWeek, Distance>,
)

data class PlaceCount(val label: String, val trips: Int)

data class RatePeriodTotal(val label: String, val distance: Distance, val rate: Money, val amount: Money)

data class YearInReview(
    val year: Int,
    val distance: Distance,
    val earthPercent: Int,
    val topPlaces: List<PlaceCount>,
    val periods: List<RatePeriodTotal>,
    val totalReimbursed: Money,
    val perDiemTotal: Money,
    val cities: Int,
) {
    companion object {
        const val EARTH_CIRCUMFERENCE_MI = 24_901.0
    }
}

interface ShareCardWriter {
    suspend fun save(png: ByteArray, name: String): String
}
