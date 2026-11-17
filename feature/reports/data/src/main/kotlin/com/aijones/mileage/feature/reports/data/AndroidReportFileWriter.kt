package com.aijones.mileage.feature.reports.data

import android.content.Context
import com.aijones.mileage.core.common.IoDispatcher
import com.aijones.mileage.feature.reports.domain.CsvFormatter
import com.aijones.mileage.feature.reports.domain.ExportedFile
import com.aijones.mileage.feature.reports.domain.Report
import com.aijones.mileage.feature.reports.domain.ReportFileWriter
import com.aijones.mileage.feature.reports.domain.ReportFormat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

class AndroidReportFileWriter @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
    private val clock: Clock,
) : ReportFileWriter {
    override suspend fun write(report: Report, format: ReportFormat): ExportedFile = withContext(io) {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val name = "${baseName(report)}.${format.extension}"
        val file = File(dir, name)
        when (format) {
            ReportFormat.PDF -> file.outputStream().use { ReportPdf(context, clock.zone).write(report, it) }
            ReportFormat.CSV -> file.writeText(CsvFormatter.format(report, clock.zone))
        }
        ExportedFile(file.absolutePath, format.mimeType, name)
    }

    private fun baseName(report: Report): String {
        val month = YearMonth.from(report.from)
        return if (month == YearMonth.from(report.to)) "mileage-report-$month" else "mileage-report-${report.from}_${report.to}"
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface ReportsDataModule {
    @Binds
    fun reportFileWriter(impl: AndroidReportFileWriter): ReportFileWriter
}
