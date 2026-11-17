package com.aijones.mileage.feature.settings.presentation

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FailureReason
import com.aijones.mileage.core.analytics.HealthSummary
import com.aijones.mileage.core.analytics.LoggedEvent
import com.aijones.mileage.core.designsystem.component.BarAction
import com.aijones.mileage.core.designsystem.component.CardList
import com.aijones.mileage.core.designsystem.component.MpdCard
import com.aijones.mileage.core.designsystem.component.MpdChip
import com.aijones.mileage.core.designsystem.component.MpdTopBar
import com.aijones.mileage.core.designsystem.component.SectionLabel
import com.aijones.mileage.core.designsystem.component.StatusDot
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.designsystem.theme.SpaceGrotesk
import com.aijones.mileage.core.designsystem.theme.tnum
import com.aijones.mileage.core.model.groupThousands
import com.aijones.mileage.feature.settings.domain.ExportEventLogUseCase
import com.aijones.mileage.feature.settings.domain.ObserveAppHealthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class EventItem(val id: Long, val name: String, val params: List<Pair<String, String>>, val time: String)

@Immutable
data class AppHealthUiState(
    val summary: HealthSummary? = null,
    val lastFailureDate: String? = null,
    val events: List<EventItem> = emptyList(),
    val shareText: String? = null,
)

@HiltViewModel
class AppHealthViewModel @Inject constructor(
    observeAppHealth: ObserveAppHealthUseCase,
    private val exportEventLog: ExportEventLogUseCase,
    private val clock: Clock,
    analytics: Analytics,
) : ViewModel() {
    private val share = MutableStateFlow<String?>(null)
    private val timeFormat = DateTimeFormatter.ofPattern("h:mm").withZone(clock.zone)
    private val dateFormat = DateTimeFormatter.ofPattern("MMM d").withZone(clock.zone)

    val state: StateFlow<AppHealthUiState> = combine(observeAppHealth(Instant.now(clock).minus(RANGE)), share) { health, text ->
        AppHealthUiState(
            summary = health.summary,
            lastFailureDate = health.summary.lastFailureAtMs?.let { dateFormat.format(Instant.ofEpochMilli(it)) },
            events = health.recentEvents.map { it.toItem() },
            shareText = text,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppHealthUiState())

    init {
        analytics.track(AnalyticsEvent.ScreenView("app_health"))
    }

    fun export() {
        viewModelScope.launch { share.value = exportEventLog() }
    }

    fun onShared() {
        share.value = null
    }

    private fun LoggedEvent.toItem() = EventItem(id, name, params.toList(), timeFormat.format(Instant.ofEpochMilli(timestampMs)))

    companion object {
        val RANGE: Duration = Duration.ofDays(7)
    }
}

@Composable
fun AppHealthRoute(onBack: () -> Unit, viewModel: AppHealthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.health_share)
    LaunchedEffect(state.shareText) {
        val text = state.shareText ?: return@LaunchedEffect
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(send, chooserTitle))
        viewModel.onShared()
    }
    AppHealthScreen(state, onBack, viewModel::export)
}

@Composable
fun AppHealthScreen(state: AppHealthUiState, onBack: () -> Unit, onShare: () -> Unit, modifier: Modifier = Modifier) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    val s = state.summary
    val none = stringResource(R.string.health_none)
    val noData = stringResource(R.string.health_no_data)
    Column(modifier.fillMaxSize()) {
        MpdTopBar(stringResource(R.string.health_title), onBack = onBack) {
            BarAction(MpdIcons.Share2, stringResource(R.string.health_share), onShare)
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MpdChip(stringResource(R.string.health_last_7_days), icon = MpdIcons.Calendar, bg = c.primaryC, fg = c.onPrimaryC)
                MpdChip(stringResource(R.string.health_this_device), outlined = true)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiRow(
                    Kpi(
                        R.string.health_cold_start, s?.coldStartP50Ms?.let { stringResource(R.string.health_ms, groupThousands(it)) } ?: noData,
                        stringResource(R.string.health_budget), s?.coldStartP50Ms?.let { it <= COLD_START_BUDGET_MS } ?: true,
                    ),
                    Kpi(
                        R.string.health_crash_free, s?.crashFreeSessionsPct?.let { stringResource(R.string.health_percent, it) } ?: noData,
                        pluralStringResource(R.plurals.health_sessions, s?.sessions ?: 0, s?.sessions ?: 0), s?.crashFreeSessionsPct?.let { it >= CRASH_FREE_TARGET } ?: true,
                    ),
                )
                KpiRow(
                    Kpi(R.string.health_trips_recorded, (s?.tripsRecorded ?: 0).toString(), stringResource(R.string.health_interrupted, s?.tripsInterrupted ?: 0), (s?.tripsInterrupted ?: 0) == 0),
                    Kpi(R.string.health_events_logged, groupThousands((s?.totalEvents ?: 0).toLong()), stringResource(R.string.health_events_kept), true),
                )
                val failures = s?.recordingFailures ?: 0
                val reason = s?.lastFailureReason?.let { reasonLabel(it) }
                val exports = s?.exportsByFormat.orEmpty().entries.sortedByDescending { it.value }
                    .map { (format, n) -> stringResource(R.string.health_export_format, n, format.uppercase()) }
                KpiRow(
                    Kpi(
                        R.string.health_fgs_failures, failures.toString(),
                        when {
                            failures == 0 || reason == null -> none
                            state.lastFailureDate == null -> reason
                            else -> stringResource(R.string.health_failure_detail, reason, state.lastFailureDate)
                        },
                        failures == 0,
                    ),
                    Kpi(R.string.health_exports, (s?.exports ?: 0).toString(), exports.joinToString(" · ").ifEmpty { none }, true),
                )
            }
            SectionLabel(stringResource(R.string.health_recent_events))
            if (state.events.isEmpty()) {
                Text(stringResource(R.string.health_no_events), style = t.bodyS, color = c.text2)
            } else {
                CardList(rows = state.events.map { e -> { EventRow(e) } })
            }
        }
    }
}

@Immutable
private data class Kpi(@StringRes val label: Int, val value: String, val sub: String, val ok: Boolean)

@Composable
private fun KpiRow(a: Kpi, b: Kpi) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        KpiCard(a, Modifier.weight(1f))
        KpiCard(b, Modifier.weight(1f))
    }
}

@Composable
private fun KpiCard(k: Kpi, modifier: Modifier) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    val status = stringResource(if (k.ok) R.string.health_status_ok else R.string.health_status_warning)
    MpdCard(modifier.fillMaxHeight().semantics(mergeDescendants = true) { }, radius = 16.dp, padding = PaddingValues(14.dp), spacing = 2.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusDot(if (k.ok) c.primary else c.accent, Modifier.semantics { this.contentDescription = status }, size = 8.dp)
            Text(stringResource(k.label), style = t.label.copy(fontWeight = null), color = c.text2)
        }
        Text(k.value, style = t.valueM.tnum(), color = c.text)
        Text(k.sub, style = t.micro.copy(fontWeight = null), color = c.text2)
    }
}

@Composable
private fun EventRow(e: EventItem) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }.padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(e.name, style = t.labelM.copy(fontFamily = SpaceGrotesk), color = c.text)
            if (e.params.isNotEmpty()) {
                Text(e.params.joinToString(" · ") { (k, v) -> "$k=$v" }, style = t.label.copy(fontWeight = null), color = c.text2)
            }
        }
        Text(e.time, style = t.label.copy(fontWeight = null).tnum(), color = c.text2)
    }
}

@Composable
private fun reasonLabel(wire: String): String = when (wire) {
    FailureReason.FGS_START_FAILED.wire -> stringResource(R.string.health_reason_fgs_start_failed)
    FailureReason.PERMISSION_REVOKED.wire -> stringResource(R.string.health_reason_permission_revoked)
    FailureReason.LOCATION_OFF.wire -> stringResource(R.string.health_reason_location_off)
    else -> wire
}

private const val COLD_START_BUDGET_MS = 600L
private const val CRASH_FREE_TARGET = 99

internal val previewAppHealth = AppHealthUiState(
    summary = HealthSummary(412, 100, 96, 23, 0, 1, "location_off", 0, 4, mapOf("pdf" to 3, "csv" to 1), 1284),
    lastFailureDate = "Nov 12",
    events = listOf(
        EventItem(4, "trip_recording_stopped", listOf("distance_mi" to "18.4", "duration_s" to "1742", "fixes" to "812"), "9:41"),
        EventItem(3, "export_completed", listOf("format" to "pdf", "trips" to "32", "ms" to "184"), "9:44"),
        EventItem(2, "feature_flag_exposed", listOf("flag" to "route_replay", "value" to "false", "source" to "override"), "9:45"),
        EventItem(1, "app_startup", listOf("type" to "cold", "ttid_ms" to "398", "ttfd_ms" to "521"), "10:02"),
    ),
)

@Preview(name = "App health light", widthDp = 412, heightDp = 915)
@Composable
private fun AppHealthLightPreview() = PreviewSurface(false) { AppHealthScreen(previewAppHealth, {}, {}) }

@Preview(name = "App health dark", widthDp = 412, heightDp = 915)
@Composable
private fun AppHealthDarkPreview() = PreviewSurface(true) { AppHealthScreen(previewAppHealth, {}, {}) }
