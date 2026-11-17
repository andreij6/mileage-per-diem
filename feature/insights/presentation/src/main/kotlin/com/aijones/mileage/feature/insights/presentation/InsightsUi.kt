package com.aijones.mileage.feature.insights.presentation

import androidx.compose.runtime.Immutable
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.model.formatCentsPerMile
import com.aijones.mileage.core.model.formatMiles
import com.aijones.mileage.core.model.formatUsd
import com.aijones.mileage.core.model.groupThousands
import com.aijones.mileage.feature.insights.domain.MonthInsights
import com.aijones.mileage.feature.insights.domain.YearInReview
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToLong

@Immutable
data class DayCellUi(
    val day: Int,
    val dateLabel: String,
    val miles: String,
    val isOneMile: Boolean,
    val level: Int,
    val isToday: Boolean,
    val isFuture: Boolean,
)

@Immutable
data class HighlightUi(val value: String, val detail: String)

@Immutable
data class WeekdayBarUi(val letter: String, val name: String, val miles: String, val fraction: Float, val isMax: Boolean)

@Immutable
data class YearCardUi(val year: Int, val miles: String)

@Immutable
data class InsightsUi(
    val monthName: String,
    val total: String,
    val leadingBlanks: Int,
    val days: List<DayCellUi>,
    val longest: HighlightUi?,
    val busiest: HighlightUi?,
    val topStopCount: Int?,
    val topStop: String?,
    val weekdays: List<WeekdayBarUi>,
)

@Immutable
data class PlaceUi(val rank: Int, val label: String, val trips: Int, val fraction: Float)

@Immutable
data class PeriodUi(val label: String, val miles: String, val rate: String, val amount: String)

@Immutable
data class StoriesUi(
    val year: Int,
    val tenths: Long,
    val earthPercent: Int,
    val places: List<PlaceUi>,
    val total: String,
    val periods: List<PeriodUi>,
    val perDiem: String?,
    val cities: Int,
)

private val SUNDAY_FIRST = listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.dropLast(1)

fun Distance.wholeMiles(): String = groupThousands(miles.roundToLong())

private fun Distance.spokenMiles(): String = if (tenthsOfMile % 10 == 0L) wholeMiles() else formatMiles().removeSuffix(" mi")

fun Money.formatUsdShort(): String = if (cents % 100 == 0L) "$" + groupThousands(cents / 100) else formatUsd()

fun MonthInsights.toUi(locale: Locale, zone: ZoneId): InsightsUi {
    val dayLabel = DateTimeFormatter.ofPattern("MMMM d", locale)
    val busiestLabel = DateTimeFormatter.ofPattern("EEE, MMM d", locale)
    val maxWeekday = weekdayTotals.values.maxOrNull()?.takeIf { it.meters > 0 }
    return InsightsUi(
        monthName = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale),
        total = totalDistance.formatMiles(),
        leadingBlanks = month.atDay(1).dayOfWeek.value % 7,
        days = days.map { d ->
            val distance = Distance.ofMiles(d.miles)
            DayCellUi(
                day = d.date.dayOfMonth,
                dateLabel = d.date.format(dayLabel),
                miles = distance.spokenMiles(),
                isOneMile = distance.tenthsOfMile == 10L,
                level = d.level,
                isToday = d.isToday,
                isFuture = d.isFuture,
            )
        },
        longest = longest?.let { t ->
            val route = listOfNotNull(t.startLabel, t.endLabel).joinToString(" → ")
            HighlightUi(t.distance.formatMiles(), route.ifEmpty { t.startedAt.atZone(zone).format(busiestLabel) })
        },
        busiest = busiestDay?.let { (date, distance) -> HighlightUi(distance.wholeMiles() + " mi", date.format(busiestLabel)) },
        topStopCount = topStop?.second,
        topStop = topStop?.first,
        weekdays = SUNDAY_FIRST.map { dow ->
            val distance = weekdayTotals[dow] ?: Distance.ZERO
            WeekdayBarUi(
                letter = dow.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                name = dow.getDisplayName(TextStyle.FULL_STANDALONE, locale),
                miles = distance.formatMiles(),
                fraction = if (maxWeekday == null) 0f else (distance.meters / maxWeekday.meters).toFloat(),
                isMax = maxWeekday != null && distance == maxWeekday,
            )
        },
    )
}

fun YearInReview.toUi(): StoriesUi {
    val top = topPlaces.firstOrNull()?.trips ?: 0
    return StoriesUi(
        year = year,
        tenths = distance.tenthsOfMile,
        earthPercent = earthPercent,
        places = topPlaces.mapIndexed { i, p -> PlaceUi(i + 1, p.label, p.trips, if (top == 0) 0f else p.trips / top.toFloat()) },
        total = totalReimbursed.formatUsd(),
        periods = periods.map { PeriodUi(it.label.replace("–", " – "), it.distance.wholeMiles(), it.rate.formatCentsPerMile(), it.amount.formatUsd()) },
        perDiem = perDiemTotal.takeIf { it > Money.ZERO }?.formatUsdShort(),
        cities = cities,
    )
}
