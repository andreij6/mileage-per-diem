package com.aijones.mileage.feature.reports.domain

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.feature.perdiem.domain.FakeStayRepository
import com.aijones.mileage.feature.perdiem.domain.Stay
import com.aijones.mileage.feature.settings.domain.FakeSettingsRepository
import com.aijones.mileage.feature.settings.domain.Settings
import com.aijones.mileage.feature.trips.domain.FakeMileageRateRepository
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.ObserveTripsUseCase
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripCategory
import com.aijones.mileage.feature.trips.domain.TripStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportUseCasesTest {
    private val clock = FakeClock()
    private val analytics = FakeAnalytics()
    private val rates = FakeMileageRateRepository()
    private val settings = FakeSettingsRepository(Settings(reportName = "Dana Jones", company = "Acme"))

    private fun trip(id: Long, at: String, miles: Double, rate: Long, category: TripCategory = TripCategory.BUSINESS, status: TripStatus = TripStatus.DONE) =
        Trip(id = id, startedAt = Instant.parse(at), status = status, distance = Distance.ofMiles(miles), ratePerMile = Money(rate), category = category)

    private val trips = FakeTripRepository(
        listOf(
            trip(1, "2026-06-30T14:00:00Z", 10.0, 725_000),
            trip(2, "2026-07-01T14:00:00Z", 10.0, 760_000),
            trip(3, "2026-07-02T14:00:00Z", 50.0, 760_000, category = TripCategory.PERSONAL),
            trip(4, "2026-07-03T14:00:00Z", 5.0, 760_000, status = TripStatus.RECORDING),
            trip(5, "2026-08-01T14:00:00Z", 99.0, 760_000),
        ),
    )
    private val stays = FakeStayRepository(
        listOf(
            Stay(1, 2, "New York City, NY", 2026, LocalDate.of(2026, 6, 29), LocalDate.of(2026, 7, 1), true, Money.ofDollars(92), Money.ofDollars(230), Money.ofDollars(572)),
        ),
    )
    private val observe = ObserveReportUseCase(ObserveTripsUseCase(trips, clock), stays, settings, rates, clock)
    private val june30 = LocalDate.of(2026, 6, 30)
    private val july31 = LocalDate.of(2026, 7, 31)

    @Test
    fun reportCombinesBusinessTripsStaysAndTotals() = runTest {
        val report = observe(june30, july31).first()
        assertEquals(listOf(1L, 2L), report.trips.map { it.id })
        assertEquals(Money.ofCents(725 + 760), report.mileageTotal)
        assertEquals(Money.ofDollars(802), report.perDiemTotal)
        assertEquals(report.mileageTotal + report.perDiemTotal, report.total)
        assertEquals(200L, report.distance.tenthsOfMile)
        assertEquals("Dana Jones", report.employee)
        assertEquals("Acme", report.company)
        assertEquals(clock.instant(), report.generatedAt)
        assertEquals("IRS 72.5¢/mi (2026-01-01 to 2026-06-30, IRS Notice 2025-5); IRS 76¢/mi (2026-07-01 to 2026-12-31, IRB 2026-29)", report.rateNote)
    }

    @Test
    fun personalTripsIncludedWhenRequestedButNotReimbursed() = runTest {
        val report = observe(june30, july31, businessOnly = false).first()
        assertEquals(listOf(1L, 2L, 3L), report.trips.map { it.id })
        assertEquals(Money.ofCents(725 + 760), report.mileageTotal)
        assertEquals(700L, report.distance.tenthsOfMile)
    }

    @Test
    fun customRateIsNotedAndReportUpdatesLive() = runTest {
        observe(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)).test {
            assertEquals("IRS 76¢/mi (2026-07-01 to 2026-12-31, IRB 2026-29)", awaitItem().rateNote)
            rates.setCustomRate(Money(700_000))
            assertEquals("Company rate 70¢/mi", awaitItem().rateNote)
            settings.update { it.copy(company = "Globex") }
            assertEquals("Globex", awaitItem().company)
        }
    }

    @Test
    fun exportMeasuresDurationAndTracksCompletion() = runTest {
        val report = observe(june30, july31).first()
        val writer = FakeReportFileWriter(onWrite = { clock.advance(Duration.ofMillis(420)) })
        val result = ExportReportUseCase(writer, analytics, clock)(report, ReportFormat.CSV)
        assertEquals("text/csv", result.getOrThrow().mimeType)
        assertEquals(listOf(report to ReportFormat.CSV), writer.written)
        assertEquals(listOf(AnalyticsEvent.ExportCompleted("csv", 2, 420)), analytics.events)
    }

    @Test
    fun exportFailureIsReturnedAndTracked() = runTest {
        val report = observe(june30, july31).first()
        val result = ExportReportUseCase(FakeReportFileWriter(failure = IOException("disk full")), analytics, clock)(report, ReportFormat.PDF)
        assertTrue(result.isFailure)
        assertEquals(listOf(AnalyticsEvent.ExportFailed("pdf", "IOException")), analytics.events)
    }
}
