package com.aijones.mileage.feature.reports.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.perdiem.domain.FakeStayRepository
import com.aijones.mileage.feature.reports.domain.ExportReportUseCase
import com.aijones.mileage.feature.reports.domain.ExportedFile
import com.aijones.mileage.feature.reports.domain.FakeReportFileWriter
import com.aijones.mileage.feature.reports.domain.ObserveReportUseCase
import com.aijones.mileage.feature.reports.domain.ReportFormat
import com.aijones.mileage.feature.settings.domain.FakeSettingsRepository
import com.aijones.mileage.feature.settings.domain.Settings
import com.aijones.mileage.feature.trips.domain.FakeMileageRateRepository
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.ObserveTripsUseCase
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripCategory
import com.aijones.mileage.feature.trips.domain.TripStatus
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReportsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val clock = FakeClock()
    private val analytics = FakeAnalytics()
    private val trips = FakeTripRepository(
        listOf(
            trip(1, "2026-11-03T14:00:00Z", TripCategory.BUSINESS),
            trip(2, "2026-11-04T14:00:00Z", TripCategory.PERSONAL),
            trip(3, "2026-10-15T14:00:00Z", TripCategory.BUSINESS),
        ),
    )
    private val nov1 = LocalDate.of(2026, 11, 1)
    private val nov30 = LocalDate.of(2026, 11, 30)

    private fun trip(id: Long, at: String, category: TripCategory) =
        Trip(id = id, startedAt = Instant.parse(at), status = TripStatus.DONE, distance = Distance.ofMiles(10.0), ratePerMile = Money(760_000), category = category)

    private fun viewModel(writer: FakeReportFileWriter = FakeReportFileWriter()) = ReportsViewModel(
        ObserveReportUseCase(
            ObserveTripsUseCase(trips, clock),
            FakeStayRepository(),
            FakeSettingsRepository(Settings(reportName = "Jordan Lee", company = "Acme Corp")),
            FakeMileageRateRepository(),
            clock,
        ),
        ExportReportUseCase(writer, analytics, clock),
        analytics,
        clock,
    )

    @Test
    fun defaultsToCurrentMonthBusinessOnlyAndTracksScreenView() = runTest {
        val vm = viewModel()
        vm.state.test {
            val loaded = awaitItem()
            assertEquals(nov1 to nov30, loaded.from to loaded.to)
            assertEquals(true, loaded.businessOnly)
            assertEquals(listOf(1L), loaded.report?.trips?.map { it.id })
            assertEquals(Money.ofCents(760), loaded.report?.total)
            assertEquals("Jordan Lee", loaded.report?.employee)
        }
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("reports")), analytics.events)
    }

    @Test
    fun rangeChangeRequeriesAndOrdersDates() = runTest {
        val vm = viewModel()
        vm.state.test {
            skipItems(1)
            vm.setRange(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1))
            val october = awaitItem()
            assertEquals(LocalDate.of(2026, 10, 1) to LocalDate.of(2026, 10, 31), october.from to october.to)
            assertEquals(listOf(3L), october.report?.trips?.map { it.id })
        }
    }

    @Test
    fun businessOnlyToggleIncludesPersonalTrips() = runTest {
        val vm = viewModel()
        vm.state.test {
            skipItems(1)
            vm.toggleBusinessOnly()
            val all = awaitItem()
            assertEquals(false, all.businessOnly)
            assertEquals(listOf(1L, 2L), all.report?.trips?.map { it.id })
            assertEquals(Money.ofCents(760), all.report?.total)
            vm.toggleBusinessOnly()
            assertEquals(listOf(1L), awaitItem().report?.trips?.map { it.id })
        }
    }

    @Test
    fun exportSuccessExposesShareUntilHandled() = runTest {
        val writer = FakeReportFileWriter()
        val vm = viewModel(writer)
        vm.state.test {
            val loaded = awaitItem()
            vm.export(ReportFormat.PDF)
            assertEquals(loaded.copy(exporting = ReportFormat.PDF), awaitItem())
            val file = ExportedFile("/cache/reports/mileage-report-2026-11-01-2026-11-30.pdf", "application/pdf", "mileage-report-2026-11-01-2026-11-30.pdf")
            assertEquals(loaded.copy(share = file), awaitItem())
            vm.onShareHandled()
            assertEquals(loaded, awaitItem())
        }
        assertEquals(listOf(ReportFormat.PDF), writer.written.map { it.second })
        assertEquals(AnalyticsEvent.ExportCompleted("pdf", 1, 0), analytics.events.last())
    }

    @Test
    fun exportFailureShowsOnceAndIsTracked() = runTest {
        val vm = viewModel(FakeReportFileWriter(failure = IOException("disk full")))
        vm.state.test {
            val loaded = awaitItem()
            vm.export(ReportFormat.CSV)
            assertEquals(loaded.copy(exporting = ReportFormat.CSV), awaitItem())
            assertEquals(loaded.copy(exportFailed = true), awaitItem())
            vm.onFailureShown()
            assertEquals(loaded, awaitItem())
            assertNull(loaded.share)
        }
        assertEquals(AnalyticsEvent.ExportFailed("csv", "IOException"), analytics.events.last())
    }
}
