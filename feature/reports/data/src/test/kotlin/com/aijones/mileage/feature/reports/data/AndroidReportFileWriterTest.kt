package com.aijones.mileage.feature.reports.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.model.sum
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.feature.perdiem.domain.Stay
import com.aijones.mileage.feature.reports.domain.CsvFormatter
import com.aijones.mileage.feature.reports.domain.Report
import com.aijones.mileage.feature.reports.domain.ReportFormat
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripStatus
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidReportFileWriterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = FakeClock()
    private val writer = AndroidReportFileWriter(context, UnconfinedTestDispatcher(), clock)

    private fun report(tripCount: Int): Report {
        val trips = (1..tripCount).map {
            Trip(
                id = it.toLong(), startedAt = Instant.parse("2026-11-01T14:00:00Z").plus(Duration.ofHours(it * 10L)), status = TripStatus.DONE,
                distance = Distance.ofMiles(18.4), ratePerMile = Money(760_000), purpose = "Client meeting", startLabel = "Office", endLabel = "Acme HQ",
            )
        }
        val stays = listOf(
            Stay(1, 3, "Boston, MA", 2027, LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 7), false, Money.ofDollars(92), Money.ofDollars(322), Money.ZERO),
        )
        val mileage = trips.map { it.reimbursement }.sum()
        val perDiem = stays.map { it.total }.sum()
        return Report(
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), "Jordan Lee", "Acme Corp", trips, stays, mileage, perDiem, mileage + perDiem,
            trips.fold(Distance.ZERO) { a, t -> a + t.distance }, clock.instant(), "IRS 76¢/mi (2026-07-01 to 2026-12-31, IRB 2026-29)",
        )
    }

    private fun renderPages(report: Report): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        val count = ReportPdf(context, clock.zone).render(report) {
            Canvas(Bitmap.createBitmap(612, 792, Bitmap.Config.ARGB_8888).also(bitmaps::add))
        }
        assertEquals(bitmaps.size, count)
        return bitmaps
    }

    private fun Bitmap.isBlank(): Boolean {
        val pixels = IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }
        return pixels.all { it == pixels[0] }
    }

    @Test
    fun pdfPaginatesEveryTripWithNonBlankPages() {
        listOf(0 to 1, 10 to 1, 60 to 3).forEach { (trips, pages) ->
            val rendered = renderPages(report(trips))
            assertEquals(pages, rendered.size, "pages for $trips trips")
            assertTrue(rendered.none { it.isBlank() }, "blank page for $trips trips")
        }
    }

    @Test
    fun csvMatchesFormatter() = runTest {
        val report = report(3)
        val exported = writer.write(report, ReportFormat.CSV)
        assertEquals("mileage-report-2026-11.csv", exported.fileName)
        assertEquals("text/csv", exported.mimeType)
        assertEquals(CsvFormatter.format(report, clock.zone), File(exported.path).readText())
    }

    @Test
    fun rangeAcrossMonthsUsesBothDates() = runTest {
        val exported = writer.write(report(1).copy(to = LocalDate.of(2026, 12, 15)), ReportFormat.CSV)
        assertEquals("mileage-report-2026-11-01_2026-12-15.csv", exported.fileName)
    }
}
