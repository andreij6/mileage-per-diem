package com.aijones.mileage.feature.insights.domain

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.featureflags.FakeFeatureFlags
import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.feature.perdiem.domain.FakeStayRepository
import com.aijones.mileage.feature.perdiem.domain.Stay
import com.aijones.mileage.feature.trips.domain.FakeMileageRateRepository
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.ObserveTripsUseCase
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripCategory
import com.aijones.mileage.feature.trips.domain.TripStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InsightsUseCasesTest {
    private val clock = FakeClock()

    private fun trip(id: Long, at: String, miles: Double, to: String? = null, rate: Long = 760_000, category: TripCategory = TripCategory.BUSINESS) =
        Trip(id = id, startedAt = Instant.parse(at), status = TripStatus.DONE, distance = Distance.ofMiles(miles), ratePerMile = Money(rate), endLabel = to, category = category)

    @Test
    fun levelTable() {
        listOf(0.0 to 0, 0.1 to 1, 24.9 to 1, 25.0 to 2, 44.9 to 2, 45.0 to 3, 69.9 to 3, 70.0 to 4, 300.0 to 4)
            .forEach { (miles, level) -> assertEquals(level, DayCell.levelFor(miles), "$miles mi") }
    }

    @Test
    fun monthInsightsBuildHeatmapAndHighlights() = runTest {
        val repo = FakeTripRepository(
            listOf(
                trip(1, "2026-11-10T14:00:00Z", 50.0, "Acme HQ"),
                trip(2, "2026-11-10T20:00:00Z", 36.0, "Depot"),
                trip(3, "2026-11-12T14:00:00Z", 60.0, "Acme HQ"),
                trip(4, "2026-11-13T14:00:00Z", 200.0, "Beach", category = TripCategory.PERSONAL),
                trip(5, "2026-12-01T14:00:00Z", 10.0, "Acme HQ"),
            ),
        )
        val insights = ObserveMonthInsightsUseCase(ObserveTripsUseCase(repo, clock), clock)(YearMonth.of(2026, 11)).first()
        assertEquals(30, insights.days.size)
        val nov10 = insights.days[9]
        assertEquals(86.0, nov10.miles, 0.01)
        assertEquals(4, nov10.level)
        assertEquals(0, insights.days[12].level)
        assertTrue(insights.days[15].isToday)
        assertTrue(insights.days[16].isFuture)
        assertFalse(insights.days[15].isFuture)
        assertEquals(146.0, insights.totalDistance.miles, 0.01)
        assertEquals(3L, insights.longest?.id)
        assertEquals(LocalDate.of(2026, 11, 10), insights.busiestDay?.first)
        assertEquals("Acme HQ" to 2, insights.topStop)
        assertEquals(86.0, insights.weekdayTotals.getValue(DayOfWeek.TUESDAY).miles, 0.01)
        assertEquals(Distance.ZERO, insights.weekdayTotals.getValue(DayOfWeek.MONDAY))
        assertEquals(7, insights.weekdayTotals.size)
    }

    @Test
    fun emptyMonthHasNoHighlights() = runTest {
        val insights = ObserveMonthInsightsUseCase(ObserveTripsUseCase(FakeTripRepository(), clock), clock)(YearMonth.of(2027, 2)).first()
        assertEquals(28, insights.days.size)
        assertTrue(insights.days.all { it.level == 0 && it.isFuture })
        assertNull(insights.longest)
        assertNull(insights.busiestDay)
        assertNull(insights.topStop)
    }

    @Test
    fun yearInReviewSplitsRatePeriods() = runTest {
        val repo = FakeTripRepository(
            listOf(
                trip(1, "2026-03-10T14:00:00Z", 100.0, "Acme HQ", rate = 725_000),
                trip(2, "2026-06-30T14:00:00Z", 100.0, "Depot", rate = 725_000),
                trip(3, "2026-07-01T14:00:00Z", 2_290.0, "Acme HQ"),
                trip(4, "2026-09-01T14:00:00Z", 10.0, "Clinic"),
                trip(5, "2026-09-02T14:00:00Z", 999.0, "Lake", category = TripCategory.PERSONAL),
                trip(6, "2025-12-31T14:00:00Z", 10.0, "Acme HQ"),
            ),
        )
        val stays = FakeStayRepository(
            listOf(
                Stay(1, 2, "New York City, NY", 2027, LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 20), true, Money.ofDollars(92), Money.ofDollars(230), Money.ofDollars(572)),
                Stay(2, 2, "New York City, NY", 2026, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1), false, Money.ofDollars(92), Money.ofDollars(69), Money.ZERO),
                Stay(3, 3, "Boston, MA", 2026, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2), false, Money.ofDollars(86), Money.ofCents(12_900), Money.ZERO),
            ),
        )
        val review = BuildYearInReviewUseCase(ObserveTripsUseCase(repo, clock), stays, FakeMileageRateRepository(), clock)(2026)
        assertEquals(2_500.0, review.distance.miles, 0.01)
        assertEquals(10, review.earthPercent)
        assertEquals(listOf(PlaceCount("Acme HQ", 2), PlaceCount("Clinic", 1), PlaceCount("Depot", 1)), review.topPlaces)
        assertEquals(listOf("Jan–Jun", "Jul–Dec"), review.periods.map { it.label })
        assertEquals(Money.ofDollars(145), review.periods[0].amount)
        assertEquals(Money(725_000), review.periods[0].rate)
        assertEquals(2_300.0, review.periods[1].distance.miles, 0.01)
        assertEquals(Money.ofDollars(1_748), review.periods[1].amount)
        assertEquals(Money.ofDollars(145 + 1_748), review.totalReimbursed)
        assertEquals(Money.ofCents(80_200 + 6_900 + 12_900), review.perDiemTotal)
        assertEquals(2, review.cities)
    }

    @Test
    fun yearInReviewFollowsFlag() = runTest {
        val flags = FakeFeatureFlags()
        ObserveYearInReviewEnabledUseCase(flags)().test {
            assertTrue(awaitItem())
            flags.setOverride(FeatureFlag.YEAR_IN_REVIEW, false)
            assertFalse(awaitItem())
        }
    }

    @Test
    fun exportShareCardSavesAndTracks() = runTest {
        val writer = FakeShareCardWriter()
        val analytics = FakeAnalytics()
        val png = byteArrayOf(1, 2, 3)
        val path = ExportShareCardUseCase(writer, analytics)(png, story = 2)
        assertEquals("/cache/share/year-in-review-2.png", path)
        assertContentEquals(png, writer.saved.getValue("year-in-review-2.png"))
        assertEquals(listOf(AnalyticsEvent.ShareCardExported(2)), analytics.events)
    }
}
