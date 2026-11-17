package com.aijones.mileage.feature.reports.domain

import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.perdiem.domain.Stay
import com.aijones.mileage.feature.trips.domain.Trip
import java.time.Instant
import java.time.LocalDate

enum class ReportFormat(val mimeType: String, val extension: String) {
    PDF("application/pdf", "pdf"),
    CSV("text/csv", "csv"),
}

data class Report(
    val from: LocalDate,
    val to: LocalDate,
    val employee: String,
    val employeeId: String = "",
    val company: String,
    val trips: List<Trip>,
    val stays: List<Stay>,
    val mileageTotal: Money,
    val perDiemTotal: Money,
    val total: Money,
    val distance: Distance,
    val generatedAt: Instant,
    val rateNote: String,
)

data class ExportedFile(val path: String, val mimeType: String, val fileName: String)

interface ReportFileWriter {
    suspend fun write(report: Report, format: ReportFormat): ExportedFile
}
