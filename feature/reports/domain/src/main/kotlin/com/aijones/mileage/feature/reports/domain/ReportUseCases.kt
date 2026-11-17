package com.aijones.mileage.feature.reports.domain

import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.model.formatCentsPerMile
import com.aijones.mileage.core.model.sum
import com.aijones.mileage.feature.perdiem.domain.StayRepository
import com.aijones.mileage.feature.settings.domain.SettingsRepository
import com.aijones.mileage.feature.trips.domain.MileageRate
import com.aijones.mileage.feature.trips.domain.MileageRateRepository
import com.aijones.mileage.feature.trips.domain.ObserveTripsUseCase
import com.aijones.mileage.feature.trips.domain.TripCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class ObserveReportUseCase @Inject constructor(
    private val observeTrips: ObserveTripsUseCase,
    private val stays: StayRepository,
    private val settings: SettingsRepository,
    private val rates: MileageRateRepository,
    private val clock: Clock,
) {
    operator fun invoke(from: LocalDate, to: LocalDate, businessOnly: Boolean = true): Flow<Report> = combine(
        observeTrips(from, to),
        stays.observeOverlapping(from, to),
        settings.observe(),
        rates.observeCustomRate(),
        flow { emit(rates.irsRates()) },
    ) { allTrips, stayList, s, custom, irs ->
        val trips = allTrips
            .filter { it.isFinished && !it.needsReview && (!businessOnly || it.category == TripCategory.BUSINESS) }
            .sortedBy { it.startedAt }
        val mileage = trips.map { it.reimbursement }.sum()
        val perDiem = stayList.map { it.total }.sum()
        Report(
            from = from,
            to = to,
            employee = s.reportName,
            employeeId = s.employeeId,
            company = s.company,
            trips = trips,
            stays = stayList.sortedBy { it.checkIn },
            mileageTotal = mileage,
            perDiemTotal = perDiem,
            total = mileage + perDiem,
            distance = trips.fold(Distance.ZERO) { acc, t -> acc + t.distance },
            generatedAt = clock.instant(),
            rateNote = rateNote(custom, irs, from, to),
        )
    }

    private fun rateNote(custom: Money?, irs: List<MileageRate>, from: LocalDate, to: LocalDate): String =
        if (custom != null) {
            "Company rate ${custom.formatCentsPerMile()}/mi"
        } else {
            irs.filter { !it.to.isBefore(from) && !it.from.isAfter(to) }
                .sortedBy { it.from }
                .joinToString("; ") { "IRS ${it.perMile.formatCentsPerMile()}/mi (${it.from} to ${it.to}, ${it.source})" }
        }
}

class ExportReportUseCase @Inject constructor(
    private val writer: ReportFileWriter,
    private val analytics: Analytics,
    private val clock: Clock,
) {
    suspend operator fun invoke(report: Report, format: ReportFormat): Result<ExportedFile> {
        val startMs = clock.millis()
        val wire = format.name.lowercase()
        return try {
            val file = writer.write(report, format)
            analytics.track(AnalyticsEvent.ExportCompleted(wire, report.trips.size, clock.millis() - startMs))
            Result.success(file)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            analytics.track(AnalyticsEvent.ExportFailed(wire, e::class.simpleName ?: "unknown"))
            Result.failure(e)
        }
    }
}
