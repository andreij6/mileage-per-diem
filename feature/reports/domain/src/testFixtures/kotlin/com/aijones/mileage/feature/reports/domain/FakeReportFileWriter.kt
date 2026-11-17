package com.aijones.mileage.feature.reports.domain

class FakeReportFileWriter(
    private val failure: Exception? = null,
    private val onWrite: () -> Unit = {},
) : ReportFileWriter {
    val written = mutableListOf<Pair<Report, ReportFormat>>()

    override suspend fun write(report: Report, format: ReportFormat): ExportedFile {
        onWrite()
        failure?.let { throw it }
        written += report to format
        val name = "mileage-report-${report.from}-${report.to}.${format.extension}"
        return ExportedFile("/cache/reports/$name", format.mimeType, name)
    }
}
