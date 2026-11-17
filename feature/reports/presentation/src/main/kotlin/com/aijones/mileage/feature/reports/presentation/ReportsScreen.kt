package com.aijones.mileage.feature.reports.presentation

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.designsystem.component.ButtonKind
import com.aijones.mileage.core.designsystem.component.CardList
import com.aijones.mileage.core.designsystem.component.InfoNote
import com.aijones.mileage.core.designsystem.component.MpdButton
import com.aijones.mileage.core.designsystem.component.MpdCard
import com.aijones.mileage.core.designsystem.component.MpdChip
import com.aijones.mileage.core.designsystem.component.MpdListRow
import com.aijones.mileage.core.designsystem.component.ScreenTitle
import com.aijones.mileage.core.designsystem.component.SectionLabel
import com.aijones.mileage.core.designsystem.component.StatusDot
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.designsystem.theme.tnum
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.model.formatMiles
import com.aijones.mileage.core.model.formatUsd
import com.aijones.mileage.feature.perdiem.domain.Stay
import com.aijones.mileage.feature.reports.domain.ExportedFile
import com.aijones.mileage.feature.reports.domain.Report
import com.aijones.mileage.feature.reports.domain.ReportFormat
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripStatus
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun ReportsRoute(onOpenTrips: () -> Unit, onOpenPerDiem: () -> Unit, viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.reports_share_title)
    LaunchedEffect(state.share) {
        val file = state.share ?: return@LaunchedEffect
        context.startActivity(shareIntent(context, file, shareTitle))
        viewModel.onShareHandled()
    }
    ReportsScreen(
        state = state,
        onRangeChange = viewModel::setRange,
        onToggleBusinessOnly = viewModel::toggleBusinessOnly,
        onExport = viewModel::export,
        onFailureShown = viewModel::onFailureShown,
        onOpenTrips = onOpenTrips,
        onOpenPerDiem = onOpenPerDiem,
    )
}

private fun shareIntent(context: Context, file: ExportedFile, title: String): Intent {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", File(file.path))
    val send = Intent(Intent.ACTION_SEND)
        .setType(file.mimeType)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(file.fileName, uri)
    return Intent.createChooser(send, title).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}

@Composable
fun ReportsScreen(
    state: ReportsUiState,
    onRangeChange: (LocalDate, LocalDate) -> Unit,
    onToggleBusinessOnly: () -> Unit,
    onExport: (ReportFormat) -> Unit,
    onFailureShown: () -> Unit,
    onOpenTrips: () -> Unit,
    onOpenPerDiem: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val failedMessage = stringResource(R.string.reports_export_failed)
    LaunchedEffect(state.exportFailed) {
        if (state.exportFailed) {
            scope.launch { snackbar.showSnackbar(failedMessage) }
            onFailureShown()
        }
    }
    var picking by rememberSaveable { mutableStateOf(false) }
    if (picking) {
        RangePickerDialog(state.from, state.to, onDismiss = { picking = false }) { from, to ->
            picking = false
            onRangeChange(from, to)
        }
    }
    Box(modifier.fillMaxSize().background(MpdTheme.colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScreenTitle(stringResource(R.string.reports_title))
            Filters(state, onPickRange = { picking = true }, onToggleBusinessOnly = onToggleBusinessOnly)
            val report = state.report
            if (report != null) {
                TotalsCard(report)
                SectionLabel(stringResource(R.string.reports_included))
                IncludedList(report, onOpenTrips, onOpenPerDiem)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExportButton(ReportFormat.PDF, state, onExport)
                ExportButton(ReportFormat.CSV, state, onExport)
            }
            InfoNote(stringResource(R.string.reports_privacy_note), icon = MpdIcons.ShieldCheck)
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Filters(state: ReportsUiState, onPickRange: () -> Unit, onToggleBusinessOnly: () -> Unit) {
    val c = MpdTheme.colors
    val range = rangeLabel(state.from, state.to)
    val rangeAction = stringResource(R.string.reports_range_action)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MpdChip(
            range,
            Modifier.semantics { contentDescription = "$range. $rangeAction" },
            icon = MpdIcons.Calendar,
            bg = c.primaryC,
            fg = c.onPrimaryC,
            onClick = onPickRange,
        )
        MpdChip(
            stringResource(if (state.businessOnly) R.string.reports_business_only else R.string.reports_all_trips),
            outlined = true,
            selected = state.businessOnly,
            onClick = onToggleBusinessOnly,
        )
    }
}

@Composable
private fun TotalsCard(report: Report) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    MpdCard(radius = 24.dp, padding = PaddingValues(20.dp), spacing = 14.dp) {
        Column(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            Text(stringResource(R.string.reports_total_reimbursable), style = t.bodyS.copy(fontWeight = FontWeight.Medium), color = c.text2)
            Spacer(Modifier.height(14.dp))
            Text(report.total.formatUsd(), style = t.displayL.tnum(), color = c.text)
        }
        SplitBar(report.mileageTotal, report.perDiemTotal)
        LegendRow(
            c.primary,
            stringResource(R.string.reports_mileage),
            stringResource(R.string.reports_dot_join, report.distance.formatMiles(), pluralStringResource(R.plurals.reports_trips, report.trips.size, report.trips.size)),
            report.mileageTotal,
        )
        val days = report.stays.sumOf { it.days }
        val cities = report.stays.distinctBy { it.locationId }.size
        LegendRow(
            c.accent,
            stringResource(R.string.reports_per_diem),
            stringResource(R.string.reports_dot_join, pluralStringResource(R.plurals.reports_days, days, days), pluralStringResource(R.plurals.reports_cities, cities, cities)),
            report.perDiemTotal,
        )
    }
}

@Composable
private fun SplitBar(mileage: Money, perDiem: Money) {
    val c = MpdTheme.colors
    Row(Modifier.fillMaxWidth().height(12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (mileage.micros <= 0 && perDiem.micros <= 0) Segment(c.outline, 1f)
        if (mileage.micros > 0) Segment(c.primary, mileage.micros.toFloat())
        if (perDiem.micros > 0) Segment(c.accent, perDiem.micros.toFloat())
    }
}

@Composable
private fun RowScope.Segment(color: Color, weight: Float) {
    Box(Modifier.weight(weight).fillMaxSize().clip(CircleShape).background(color))
}

@Composable
private fun LegendRow(dot: Color, title: String, subtitle: String, amount: Money) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatusDot(dot)
        Column(Modifier.weight(1f)) {
            Text(title, style = t.bodyM.copy(fontWeight = FontWeight.Medium), color = c.text)
            Text(subtitle, style = t.caption.tnum(), color = c.text2)
        }
        Text(amount.formatUsd(), style = t.amount, color = c.text)
    }
}

@Composable
private fun IncludedList(report: Report, onOpenTrips: () -> Unit, onOpenPerDiem: () -> Unit) {
    val c = MpdTheme.colors
    val tripsTitle = pluralStringResource(R.plurals.reports_trips, report.trips.size, report.trips.size)
    val tripsSubtitle = stringResource(R.string.reports_dot_join, rangeLabel(report.from, report.to, withYear = false), report.distance.formatMiles())
    val mieOnly = stringResource(R.string.reports_mie_only)
    val withLodging = stringResource(R.string.reports_with_lodging)
    val rows = buildList<@Composable () -> Unit> {
        add {
            MpdListRow(
                tripsTitle, Modifier.semantics(mergeDescendants = true) {}, subtitle = tripsSubtitle, icon = MpdIcons.CircleCheck,
                iconBg = c.primaryC, iconFg = c.onPrimaryC, amount = report.mileageTotal.formatUsd(), chevron = true, onClick = onOpenTrips,
            )
        }
        report.stays.forEach { stay ->
            add {
                MpdListRow(
                    stay.locationLabel,
                    Modifier.semantics(mergeDescendants = true) {},
                    subtitle = stringResource(R.string.reports_dot_join, stayDates(stay), if (stay.includeLodging) withLodging else mieOnly),
                    icon = MpdIcons.CircleCheck, iconBg = c.primaryC, iconFg = c.onPrimaryC, amount = stay.total.formatUsd(), chevron = true,
                    onClick = onOpenPerDiem,
                )
            }
        }
    }
    CardList(rows = rows)
}

@Composable
private fun RowScope.ExportButton(format: ReportFormat, state: ReportsUiState, onExport: (ReportFormat) -> Unit) {
    val busy = state.exporting == format
    val label = stringResource(if (format == ReportFormat.PDF) R.string.reports_pdf else R.string.reports_csv)
    val kind = if (format == ReportFormat.PDF) ButtonKind.Filled else ButtonKind.Outlined
    val exportingLabel = stringResource(R.string.reports_exporting, label)
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        MpdButton(
            text = if (busy) "" else label,
            onClick = { onExport(format) },
            modifier = Modifier.fillMaxWidth().then(if (busy) Modifier.semantics { contentDescription = exportingLabel } else Modifier),
            kind = kind,
            icon = if (busy) null else if (format == ReportFormat.PDF) MpdIcons.FileText else MpdIcons.FileSpreadsheet,
            enabled = state.report != null,
        )
        if (busy) {
            CircularProgressIndicator(
                Modifier.size(20.dp),
                color = if (kind == ButtonKind.Filled) MpdTheme.colors.onPrimary else MpdTheme.colors.primary,
                strokeWidth = 2.dp,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePickerDialog(from: LocalDate, to: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate, LocalDate) -> Unit) {
    val picker = rememberDateRangePickerState(initialSelectedStartDateMillis = from.utcMillis(), initialSelectedEndDateMillis = to.utcMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            MpdButton(
                stringResource(R.string.reports_save),
                onClick = {
                    val start = picker.selectedStartDateMillis
                    val end = picker.selectedEndDateMillis
                    if (start != null && end != null) onConfirm(start.utcDate(), end.utcDate())
                },
                kind = ButtonKind.Text,
                height = 48.dp,
                enabled = picker.selectedEndDateMillis != null,
            )
        },
        dismissButton = { MpdButton(stringResource(R.string.reports_cancel), onClick = onDismiss, kind = ButtonKind.Text, height = 48.dp) },
    ) {
        DateRangePicker(
            picker,
            Modifier.weight(1f),
            title = { Text(stringResource(R.string.reports_pick_dates), Modifier.padding(start = 24.dp, top = 16.dp), style = MpdTheme.type.labelM) },
        )
    }
}

private fun LocalDate.utcMillis(): Long = atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

private fun Long.utcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private val DAY = DateTimeFormatter.ofPattern("MMM d")
private val DAY_YEAR = DateTimeFormatter.ofPattern("MMM d, yyyy")

internal fun rangeLabel(from: LocalDate, to: LocalDate, withYear: Boolean = true): String = when {
    !withYear -> "${DAY.format(from)} – ${DAY.format(to)}"
    from.year == to.year -> "${DAY.format(from)} – ${DAY_YEAR.format(to)}"
    else -> "${DAY_YEAR.format(from)} – ${DAY_YEAR.format(to)}"
}

private fun stayDates(stay: Stay): String =
    if (stay.checkIn.month == stay.checkOut.month && stay.checkIn.year == stay.checkOut.year) {
        "${DAY.format(stay.checkIn)} – ${stay.checkOut.dayOfMonth}"
    } else {
        "${DAY.format(stay.checkIn)} – ${DAY.format(stay.checkOut)}"
    }

internal val sampleReport: Report = run {
    val from = LocalDate.of(2026, 11, 1)
    val trips = List(32) { i ->
        Trip(id = i + 1L, startedAt = Instant.parse("2026-11-02T14:00:00Z"), status = TripStatus.DONE, distance = Distance.ofMiles(26.4), ratePerMile = Money(760_000))
    }
    val stays = listOf(
        Stay(1, 3, "Boston, MA", 2027, LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 7), false, Money.ofDollars(92), Money.ofDollars(322), Money.ZERO),
        Stay(2, 2, "New York City, NY", 2027, LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 25), false, Money.ofDollars(92), Money.ofDollars(690), Money.ZERO),
    )
    Report(
        from, from.withDayOfMonth(30), "Jordan Lee", "Acme Corp", trips, stays, Money.ofCents(64_220), Money.ofDollars(1_012), Money.ofCents(165_420),
        Distance.ofMiles(845.0), Instant.parse("2026-11-16T14:41:00Z"), "IRS 76¢/mi",
    )
}

internal val sampleState = ReportsUiState(sampleReport.from, sampleReport.to, report = sampleReport)

@Composable
private fun ReportsPreview(dark: Boolean) {
    MpdTheme(darkTheme = dark) {
        ReportsScreen(sampleState, { _, _ -> }, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Report light", widthDp = 412, heightDp = 915)
@Composable
private fun ReportsLightPreview() = ReportsPreview(dark = false)

@Preview(name = "Report dark", widthDp = 412, heightDp = 915)
@Composable
private fun ReportsDarkPreview() = ReportsPreview(dark = true)
