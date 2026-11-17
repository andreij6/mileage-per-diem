package com.aijones.mileage.feature.reports.domain

import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId

object CsvFormatter {
    val HEADER = listOf("Type", "Date", "End date", "Description", "From", "To", "Miles", "Rate per mile", "Amount")
    private const val EOL = "\r\n"

    fun format(report: Report, zone: ZoneId): String {
        val rows = buildList {
            add(HEADER)
            report.trips.forEach { t ->
                add(
                    listOf(
                        "Mileage", LocalDate.ofInstant(t.startedAt, zone).toString(), "", t.purpose.orEmpty(),
                        t.startLabel.orEmpty(), t.endLabel.orEmpty(), miles(t.distance),
                        t.ratePerMile?.let { decimal(it, 3) }.orEmpty(), decimal(t.reimbursement, 2),
                    ),
                )
            }
            report.stays.forEach { s ->
                add(listOf("Per diem", s.checkIn.toString(), s.checkOut.toString(), s.locationLabel, "", "", "", "", decimal(s.total, 2)))
            }
            add(listOf("Total", "", "", "Mileage", "", "", miles(report.distance), "", decimal(report.mileageTotal, 2)))
            add(listOf("Total", "", "", "Per diem", "", "", "", "", decimal(report.perDiemTotal, 2)))
            add(listOf("Total", "", "", "All", "", "", "", "", decimal(report.total, 2)))
        }
        return rows.joinToString(EOL, postfix = EOL) { row -> row.joinToString(",") { escape(it) } }
    }

    fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + field.replace("\"", "\"\"") + "\"" else field

    private fun miles(distance: Distance): String = BigDecimal.valueOf(distance.tenthsOfMile, 1).toPlainString()

    private fun decimal(money: Money, scale: Int): String =
        BigDecimal.valueOf(money.micros, 6).setScale(scale, RoundingMode.HALF_UP).toPlainString()
}
