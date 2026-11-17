package com.aijones.mileage.feature.reports.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.aijones.mileage.core.designsystem.R
import com.aijones.mileage.core.designsystem.theme.LightColors
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.model.formatMiles
import com.aijones.mileage.core.model.formatUsd
import com.aijones.mileage.feature.perdiem.domain.Stay
import com.aijones.mileage.feature.reports.domain.Report
import com.aijones.mileage.feature.trips.domain.Trip
import java.io.OutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal class ReportPdf(context: Context, private val zone: ZoneId) {
    private val grotesk = font(context, R.font.space_grotesk)
    private val inter = font(context, R.font.inter)
    private val ink = LightColors.text.toArgb()
    private val sub = LightColors.text2.toArgb()
    private val rule = LightColors.outline.toArgb()
    private val boxFill = LightColors.bg.toArgb()

    private val titlePaint = paint(grotesk, 22f, 700, ink)
    private val periodPaint = paint(inter, 11f, 600, ink)
    private val identityPaint = paint(inter, 11f, 400, sub)
    private val boxLabelPaint = paint(inter, 10f, 400, sub)
    private val boxValuePaint = paint(grotesk, 18f, 700, ink, tnum = true)
    private val sectionPaint = paint(inter, 12f, 600, ink)
    private val headCellPaint = paint(inter, 9.5f, 600, sub, tnum = true)
    private val cellPaint = paint(inter, 9.5f, 400, ink, tnum = true)
    private val notePaint = paint(inter, 9f, 400, sub)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun write(report: Report, out: OutputStream) {
        val doc = PdfDocument()
        try {
            var page: PdfDocument.Page? = null
            render(report) { number ->
                page?.let(doc::finishPage)
                doc.startPage(PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, number).create()).also { page = it }.canvas
            }
            page?.let(doc::finishPage)
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    fun render(report: Report, newPage: (number: Int) -> Canvas): Int {
        val pager = Pager(newPage)
        pager.header(report)
        pager.table(
            "Trips",
            listOf("Date", "Route", "Purpose", "Miles", "Rate", "Amount"),
            floatArrayOf(56f, 180f, 110f, 60f, 56f, 70f),
            report.trips.map(::tripRow).ifEmpty { listOf(listOf("", "No trips in this period", "", "", "", "")) },
        )
        pager.table(
            "Per diem (GSA ${fiscalYears(report)})",
            listOf("Dates", "Location", "Basis", "Days", "Rate", "Amount"),
            floatArrayOf(86f, 150f, 110f, 50f, 56f, 80f),
            report.stays.map(::stayRow).ifEmpty { listOf(listOf("", "No stays in this period", "", "", "", "")) },
        )
        pager.footer(report)
        return pager.pages
    }

    private inner class Pager(private val newPage: (Int) -> Canvas) {
        var pages = 1
            private set
        private var canvas = newPage(pages)
        private var y = MARGIN

        private fun breakIfNeeded(height: Float): Boolean {
            if (y + height <= HEIGHT - MARGIN) return false
            canvas = newPage(++pages)
            y = MARGIN
            return true
        }

        fun header(report: Report) {
            text(titlePaint, "Mileage & Expense Report", MARGIN, y, 28f)
            textRight(periodPaint, period(report.from, report.to), WIDTH - MARGIN, y + 2f, 16f)
            y += 30f
            val identity = listOf(report.employee, report.company, report.employeeId.takeIf { it.isNotBlank() }?.let { "Employee $it" }.orEmpty()).filter { it.isNotBlank() }.joinToString(" · ")
            if (identity.isNotEmpty()) {
                text(identityPaint, identity, MARGIN, y, 15f)
                y += 15f
            }
            y += GAP
            val boxWidth = (CONTENT - 2 * 10f) / 3
            listOf(
                "Mileage · ${report.distance.formatMiles()}" to report.mileageTotal,
                "Per diem · ${days(report.stays.sumOf { it.days })}" to report.perDiemTotal,
                "Total" to report.total,
            ).forEachIndexed { i, (label, amount) ->
                val x = MARGIN + i * (boxWidth + 10f)
                fillPaint.color = boxFill
                canvas.drawRoundRect(x, y, x + boxWidth, y + BOX_HEIGHT, 8f, 8f, fillPaint)
                text(boxLabelPaint, label, x + 12f, y + 12f, 13f)
                text(boxValuePaint, amount.formatUsd(), x + 12f, y + 27f, 23f)
            }
            y += BOX_HEIGHT + GAP
        }

        fun table(title: String, head: List<String>, widths: FloatArray, rows: List<List<String>>) {
            breakIfNeeded(TITLE_HEIGHT + GAP + 2 * ROW_HEIGHT)
            text(sectionPaint, title, MARGIN, y, TITLE_HEIGHT)
            y += TITLE_HEIGHT + GAP
            row(head, widths, headCellPaint)
            rows.forEach { cells ->
                if (breakIfNeeded(ROW_HEIGHT)) row(head, widths, headCellPaint)
                row(cells, widths, cellPaint)
            }
            y += GAP
        }

        private fun row(cells: List<String>, widths: FloatArray, paint: TextPaint) {
            var x = MARGIN
            cells.forEachIndexed { i, cell ->
                val width = widths[i]
                if (i >= cells.size - 3) {
                    textRight(paint, cell, x + width, y + 6f, 14f)
                } else {
                    val fitted = TextUtils.ellipsize(cell, paint, width - 8f, TextUtils.TruncateAt.END).toString()
                    text(paint, fitted, x, y + 6f, 14f)
                }
                x += width
            }
            fillPaint.color = rule
            canvas.drawRect(MARGIN, y + ROW_HEIGHT - 1f, WIDTH - MARGIN, y + ROW_HEIGHT, fillPaint)
            y += ROW_HEIGHT
        }

        fun footer(report: Report) {
            val rates = report.rateNote.takeIf { it.isNotBlank() }?.let { "Rates: $it; " } ?: "Rates: "
            val note = "${rates}GSA per diem ${fiscalYears(report)}. Generated on-device by Mileage & Per Diem · AI.Jones."
            val layout = StaticLayout.Builder.obtain(note, 0, note.length, notePaint, CONTENT.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.4f)
                .build()
            breakIfNeeded(layout.height.toFloat())
            canvas.save()
            canvas.translate(MARGIN, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + GAP
            breakIfNeeded(SIGNATURE_HEIGHT)
            y += 24f
            val columnWidth = (CONTENT - 24f) / 2
            listOf("Employee signature", "Approver signature").forEachIndexed { i, label ->
                val x = MARGIN + i * (columnWidth + 24f)
                fillPaint.color = ink
                canvas.drawRect(x, y, x + columnWidth, y + 1f, fillPaint)
                text(notePaint, label, x, y + 5f, 12f)
            }
            y += SIGNATURE_HEIGHT - 24f
        }

        private fun text(paint: TextPaint, value: String, x: Float, top: Float, lineHeight: Float) {
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(value, x, baseline(paint, top, lineHeight), paint)
        }

        private fun textRight(paint: TextPaint, value: String, right: Float, top: Float, lineHeight: Float) {
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(value, right, baseline(paint, top, lineHeight), paint)
            paint.textAlign = Paint.Align.LEFT
        }

        private fun baseline(paint: TextPaint, top: Float, lineHeight: Float): Float {
            val fm = paint.fontMetrics
            return top + lineHeight / 2 - (fm.ascent + fm.descent) / 2
        }
    }

    private fun tripRow(trip: Trip): List<String> = listOf(
        DAY.format(trip.startedAt.atZone(zone)),
        listOfNotNull(trip.startLabel, trip.endLabel).joinToString(" → "),
        trip.purpose.orEmpty(),
        trip.distance.formatMiles().removeSuffix(" mi"),
        trip.ratePerMile?.let(::rate).orEmpty(),
        trip.reimbursement.formatUsd(),
    )

    private fun stayRow(stay: Stay): List<String> = listOf(
        dates(stay.checkIn, stay.checkOut),
        stay.locationLabel,
        if (stay.includeLodging) "Lodging + M&IE, 75% first/last" else "M&IE, 75% first/last",
        stay.days.toString(),
        wholeDollars(stay.mieDaily),
        stay.total.formatUsd(),
    )

    private fun fiscalYears(report: Report): String {
        val years = report.stays.map { it.fiscalYear }.distinct().sorted()
            .ifEmpty { listOf(if (report.to.monthValue >= 10) report.to.year + 1 else report.to.year) }
        return years.joinToString("/") { "FY$it" }
    }

    private fun font(context: Context, res: Int): Typeface =
        runCatching { ResourcesCompat.getFont(context, res) }.getOrNull() ?: Typeface.DEFAULT

    private fun paint(typeface: Typeface, size: Float, weight: Int, color: Int, tnum: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = size
        this.color = color
        fontVariationSettings = "'wght' $weight"
        if (tnum) fontFeatureSettings = "tnum"
        if (typeface == Typeface.DEFAULT && weight >= 600) isFakeBoldText = true
    }

    private companion object {
        const val WIDTH = 612
        const val HEIGHT = 792
        const val MARGIN = 40f
        const val CONTENT = WIDTH - 2 * MARGIN
        const val GAP = 14f
        const val BOX_HEIGHT = 60f
        const val TITLE_HEIGHT = 16f
        const val ROW_HEIGHT = 26f
        const val SIGNATURE_HEIGHT = 41f
        val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
        val DAY_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

        fun period(from: LocalDate, to: LocalDate): String =
            if (from.year == to.year) "${DAY.format(from)} – ${DAY_YEAR.format(to)}" else "${DAY_YEAR.format(from)} – ${DAY_YEAR.format(to)}"

        fun dates(from: LocalDate, to: LocalDate): String = when {
            from == to -> DAY.format(from)
            from.month == to.month && from.year == to.year -> "${DAY.format(from)}–${to.dayOfMonth}"
            else -> "${DAY.format(from)}–${DAY.format(to)}"
        }

        fun days(count: Int) = if (count == 1) "1 day" else "$count days"

        fun rate(perMile: Money): String {
            val dollars = BigDecimal.valueOf(perMile.micros, 6).stripTrailingZeros()
            return "$" + (if (dollars.scale() < 2) dollars.setScale(2) else dollars).toPlainString()
        }

        fun wholeDollars(money: Money): String =
            if (money.cents % 100 == 0L) "$" + (money.cents / 100) else money.formatUsd()
    }
}
