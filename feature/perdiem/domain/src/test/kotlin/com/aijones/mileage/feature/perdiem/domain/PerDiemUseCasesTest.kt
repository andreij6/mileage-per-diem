package com.aijones.mileage.feature.perdiem.domain

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.testing.FakeClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PerDiemUseCasesTest {
    private val analytics = FakeAnalytics()
    private val rates = FakePerDiemRateRepository()
    private val stays = FakeStayRepository()
    private val recent = FakeRecentSearchRepository()

    @Test
    fun searchTracksResultCount() = runTest {
        val search = SearchLocationsUseCase(rates, analytics)
        assertEquals(listOf("Boston, MA"), search(" bos ", 2027).map { it.label })
        assertTrue(search("nowhere", 2027).isEmpty())
        assertTrue(search("  ", 2027).isEmpty())
        assertEquals(listOf(AnalyticsEvent.PerDiemSearched(1), AnalyticsEvent.PerDiemSearched(0)), analytics.events)
    }

    @Test
    fun ratesForLocationAreRecordedAsRecent() = runTest {
        val get = GetRatesForLocationUseCase(rates, recent)
        ObserveRecentSearchesUseCase(recent)().test {
            assertEquals(emptyList(), awaitItem())
            assertEquals(SamplePerDiem.NYC, get(2))
            assertEquals(listOf(2L), awaitItem().map { it.id })
            get(3)
            assertEquals(listOf(3L, 2L), awaitItem().map { it.id })
            get(2)
            assertEquals(listOf(2L, 3L), awaitItem().map { it.id })
            get(1)
            assertNull(get(99))
            expectNoEvents()
        }
    }

    @Test
    fun estimateMatchesCalculator() {
        val e = EstimateStayUseCase()(SamplePerDiem.NYC, LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 25), true)
        assertEquals(Money.ofDollars(2692), e.total)
    }

    @Test
    fun addStayStoresTotalsAndTracks() = runTest {
        val id = AddStayUseCase(stays, analytics)(SamplePerDiem.NYC, LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 20), false)
        val stay = stays.stays.single()
        assertEquals(id, stay.id)
        assertEquals("New York City, NY", stay.locationLabel)
        assertEquals(2027, stay.fiscalYear)
        assertEquals(Money.ofDollars(92), stay.mieDaily)
        assertEquals(Money.ofDollars(230), stay.total)
        assertEquals(Money.ZERO, stay.lodgingTotal)
        assertEquals(3, stay.days)
        assertEquals(listOf(AnalyticsEvent.StayAdded(3, false)), analytics.events)
    }

    @Test
    fun observeStaysReturnsOverlappingOnly() = runTest {
        val add = AddStayUseCase(stays, analytics)
        add(SamplePerDiem.NYC, LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 2), true)
        add(SamplePerDiem.BOSTON, LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 19), true)
        add(SamplePerDiem.BOSTON, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2), true)
        val nov = ObserveStaysUseCase(stays)(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)).first()
        assertEquals(listOf(1L, 2L), nov.map { it.id })
    }

    @Test
    fun fiscalYearStartsOctoberFirst() {
        listOf(
            "2026-11-16T14:41:00Z" to 2027,
            "2026-10-01T12:00:00Z" to 2027,
            "2026-09-30T12:00:00Z" to 2026,
            "2027-01-15T12:00:00Z" to 2027,
        ).forEach { (now, fy) -> assertEquals(fy, CurrentFiscalYearUseCase(rates, FakeClock(Instant.parse(now)))(), now) }
    }
}
