package com.aijones.mileage.feature.insights.domain

import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.featureflags.FeatureFlags
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.sum
import com.aijones.mileage.feature.perdiem.domain.StayRepository
import com.aijones.mileage.feature.trips.domain.MileageRateRepository
import com.aijones.mileage.feature.trips.domain.ObserveTripsUseCase
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

private fun List<Trip>.businessFinished() = filter { it.isReimbursable }

private fun Iterable<Trip>.totalDistance() = fold(Distance.ZERO) { acc, t -> acc + t.distance }

class ObserveMonthInsightsUseCase @Inject constructor(private val observeTrips: ObserveTripsUseCase, private val clock: Clock) {
    operator fun invoke(month: YearMonth): Flow<MonthInsights> =
        observeTrips(month.atDay(1), month.atEndOfMonth()).map { all ->
            val trips = all.businessFinished()
            val today = LocalDate.now(clock)
            val byDay = trips.groupBy { LocalDate.ofInstant(it.startedAt, clock.zone) }.mapValues { (_, t) -> t.totalDistance() }
            val days = (1..month.lengthOfMonth()).map { d ->
                val date = month.atDay(d)
                val miles = byDay[date]?.miles ?: 0.0
                DayCell(date, miles, DayCell.levelFor(miles), date.isAfter(today), date == today)
            }
            MonthInsights(
                month = month,
                days = days,
                totalDistance = trips.totalDistance(),
                longest = trips.maxByOrNull { it.distance },
                busiestDay = byDay.maxByOrNull { it.value }?.toPair(),
                topStop = trips.mapNotNull { it.endLabel }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.toPair(),
                weekdayTotals = DayOfWeek.entries.associateWith { dow ->
                    byDay.filterKeys { it.dayOfWeek == dow }.values.fold(Distance.ZERO, Distance::plus)
                },
            )
        }
}

class BuildYearInReviewUseCase @Inject constructor(
    private val observeTrips: ObserveTripsUseCase,
    private val stays: StayRepository,
    private val rates: MileageRateRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(year: Int): YearInReview {
        val jan1 = LocalDate.of(year, 1, 1)
        val dec31 = LocalDate.of(year, 12, 31)
        val trips = observeTrips(jan1, dec31).first().businessFinished()
        val stayList = stays.observeOverlapping(jan1, dec31).first()
        val distance = trips.totalDistance()
        val periods = rates.irsRates()
            .filter { !it.to.isBefore(jan1) && !it.from.isAfter(dec31) }
            .sortedBy { it.from }
            .map { rate ->
                val inPeriod = trips.filter { LocalDate.ofInstant(it.startedAt, clock.zone) in rate }
                RatePeriodTotal(
                    label = "${maxOf(rate.from, jan1).month.short()}–${minOf(rate.to, dec31).month.short()}",
                    distance = inPeriod.totalDistance(),
                    rate = rate.perMile,
                    amount = inPeriod.map { it.reimbursement }.sum(),
                )
            }
        return YearInReview(
            year = year,
            distance = distance,
            earthPercent = (distance.miles / YearInReview.EARTH_CIRCUMFERENCE_MI * 100).roundToInt(),
            topPlaces = trips.mapNotNull { it.endLabel }.groupingBy { it }.eachCount().entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .take(TOP_PLACES)
                .map { PlaceCount(it.key, it.value) },
            periods = periods,
            totalReimbursed = trips.map { it.reimbursement }.sum(),
            perDiemTotal = stayList.map { it.total }.sum(),
            cities = stayList.map { it.locationId }.distinct().size,
        )
    }

    private fun java.time.Month.short() = getDisplayName(TextStyle.SHORT, Locale.US)

    private companion object {
        const val TOP_PLACES = 3
    }
}

class ObserveYearInReviewEnabledUseCase @Inject constructor(private val flags: FeatureFlags) {
    operator fun invoke(): Flow<Boolean> = flags.observe(FeatureFlag.YEAR_IN_REVIEW)
}

class ExportShareCardUseCase @Inject constructor(private val writer: ShareCardWriter, private val analytics: Analytics) {
    suspend operator fun invoke(png: ByteArray, story: Int): String =
        writer.save(png, "year-in-review-$story.png").also { analytics.track(AnalyticsEvent.ShareCardExported(story)) }
}
