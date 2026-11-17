package com.aijones.mileage.feature.reports.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.feature.reports.domain.ExportReportUseCase
import com.aijones.mileage.feature.reports.domain.ExportedFile
import com.aijones.mileage.feature.reports.domain.ObserveReportUseCase
import com.aijones.mileage.feature.reports.domain.Report
import com.aijones.mileage.feature.reports.domain.ReportFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@Immutable
data class ReportsUiState(
    val from: LocalDate,
    val to: LocalDate,
    val businessOnly: Boolean = true,
    val report: Report? = null,
    val exporting: ReportFormat? = null,
    val share: ExportedFile? = null,
    val exportFailed: Boolean = false,
)

private data class Query(val from: LocalDate, val to: LocalDate, val businessOnly: Boolean)

private data class ExportState(val exporting: ReportFormat? = null, val share: ExportedFile? = null, val failed: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val observeReport: ObserveReportUseCase,
    private val exportReport: ExportReportUseCase,
    analytics: Analytics,
    clock: Clock,
) : ViewModel() {
    private val month = YearMonth.now(clock)
    private val query = MutableStateFlow(Query(month.atDay(1), month.atEndOfMonth(), businessOnly = true))
    private val export = MutableStateFlow(ExportState())

    val state: StateFlow<ReportsUiState> = combine(
        query.flatMapLatest { q -> observeReport(q.from, q.to, q.businessOnly).map { q to it } },
        export,
    ) { (q, report), e ->
        ReportsUiState(q.from, q.to, q.businessOnly, report, e.exporting, e.share, e.failed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState(query.value.from, query.value.to))

    init {
        analytics.track(AnalyticsEvent.ScreenView("reports"))
    }

    fun setRange(from: LocalDate, to: LocalDate) {
        query.update { it.copy(from = minOf(from, to), to = maxOf(from, to)) }
    }

    fun toggleBusinessOnly() {
        query.update { it.copy(businessOnly = !it.businessOnly) }
    }

    fun export(format: ReportFormat) {
        val report = state.value.report ?: return
        if (export.value.exporting != null) return
        export.value = ExportState(exporting = format)
        viewModelScope.launch {
            export.value = exportReport(report, format).fold(
                onSuccess = { ExportState(share = it) },
                onFailure = { ExportState(failed = true) },
            )
        }
    }

    fun onShareHandled() {
        export.update { it.copy(share = null) }
    }

    fun onFailureShown() {
        export.update { it.copy(failed = false) }
    }
}
