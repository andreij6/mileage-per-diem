package com.aijones.mileage.feature.trips.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.designsystem.component.ButtonKind
import com.aijones.mileage.core.designsystem.component.MpdButton
import com.aijones.mileage.core.designsystem.component.MpdCard
import com.aijones.mileage.core.designsystem.component.MpdTopBar
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.designsystem.theme.tnum
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.navigation.EditTripRoute
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.ReportsRoute
import com.aijones.mileage.core.ui.RouteCanvas
import com.aijones.mileage.feature.trips.domain.TripCategory

@Composable
internal fun TripDetailEntry(
    tripId: Long,
    navigator: Navigator,
    viewModel: TripDetailViewModel = hiltViewModel<TripDetailViewModel, TripDetailViewModel.Factory> { it.create(tripId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.missing) { if (state.missing) navigator.back() }
    TripDetailScreen(
        state = state,
        onBack = navigator::back,
        onEdit = { navigator.navigate(EditTripRoute(tripId)) },
        onAddToReport = { navigator.switchTab(ReportsRoute) },
        onDelete = viewModel::delete,
        onScrub = viewModel::scrub,
        onPlaying = viewModel::setPlaying,
    )
}

@Composable
fun TripDetailScreen(
    state: TripDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddToReport: () -> Unit,
    onDelete: () -> Unit,
    onScrub: (Float) -> Unit,
    onPlaying: (Boolean) -> Unit,
) {
    val c = MpdTheme.colors
    var menu by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(c.bg)) {
        MpdTopBar(stringResource(R.string.trips_detail_title), onBack = onBack) {
            Box {
                IconButton(onClick = { menu = true }) { MpdIcon(MpdIcons.EllipsisVertical, c.text2, contentDescription = stringResource(R.string.trips_more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.trips_edit)) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.trips_delete_trip), color = c.danger) }, onClick = { menu = false; confirmDelete = true })
                }
            }
        }
        if (state.loading || state.missing) return@Column
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MapCard(state)
            state.replay?.let { ReplayRow(it, onScrub, onPlaying) }
            StopsCard(state)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(stringResource(R.string.trips_distance), state.distance, false, Modifier.weight(1f))
                StatCard(stringResource(R.string.trips_duration), durationText(state.durationMinutes), false, Modifier.weight(1f))
                StatCard(stringResource(R.string.trips_at_rate, state.rate), state.amount, true, Modifier.weight(1f))
            }
            MpdCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), spacing = 0.dp) {
                DetailRow(stringResource(R.string.trips_purpose), state.purpose ?: stringResource(R.string.trips_no_purpose))
                DetailRow(stringResource(R.string.trips_category), state.category.label())
                DetailRow(stringResource(R.string.trips_vehicle), state.vehicle ?: stringResource(R.string.trips_none))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MpdButton(stringResource(R.string.trips_edit), onEdit, Modifier.weight(1f), kind = ButtonKind.Outlined, icon = MpdIcons.Pencil, height = 48.dp)
                MpdButton(stringResource(R.string.trips_add_to_report), onAddToReport, Modifier.weight(1f), kind = ButtonKind.Tonal, icon = MpdIcons.Plus, height = 48.dp)
            }
        }
    }
    if (confirmDelete) DeleteTripDialog(onConfirm = { confirmDelete = false; onDelete() }, onDismiss = { confirmDelete = false })
}

@Composable
private fun durationText(minutes: Int?): String = when {
    minutes == null -> stringResource(R.string.trips_manual_entry)
    minutes < 60 -> stringResource(R.string.trips_minutes, minutes)
    else -> stringResource(R.string.trips_hours_minutes, minutes / 60, minutes % 60)
}

@Composable
private fun MapCard(state: TripDetailUiState) {
    val c = MpdTheme.colors
    val unknown = stringResource(R.string.trips_unknown_place)
    val replay = state.replay
    Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp))) {
        RouteCanvas(
            points = state.points,
            description = stringResource(R.string.trips_route_description, state.distance, state.from ?: unknown, state.to ?: unknown),
            modifier = Modifier.fillMaxSize(),
            replayAt = replay?.routeFraction,
            startLabel = if (replay == null) state.from else null,
            endLabel = if (replay == null) state.to else null,
        )
        if (replay != null) {
            Text(
                stringResource(R.string.trips_speed_chip, replay.time, replay.mph),
                Modifier.padding(12.dp).clip(CircleShape).background(c.surface).padding(horizontal = 10.dp, vertical = 6.dp),
                style = MpdTheme.type.label.copy(fontWeight = MpdTheme.type.labelM.fontWeight).tnum(),
                color = c.text,
            )
        }
    }
}

@Composable
private fun ReplayRow(replay: ReplayUi, onScrub: (Float) -> Unit, onPlaying: (Boolean) -> Unit) {
    val c = MpdTheme.colors
    val fraction by rememberUpdatedState(replay.fraction)
    LaunchedEffect(replay.playing) {
        if (!replay.playing) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (fraction < 1f) {
            val now = withFrameMillis { it }
            onScrub(fraction + (now - last) * REPLAY_SPEED / replay.durationMs.coerceAtLeast(1))
            last = now
        }
        onPlaying(false)
    }
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.outline, shape).padding(start = 10.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconButton(
            onClick = { onPlaying(!replay.playing) },
            modifier = Modifier.size(44.dp).clip(CircleShape).background(c.primary),
        ) {
            MpdIcon(
                if (replay.playing) MpdIcons.Square else MpdIcons.Play,
                c.onPrimary,
                size = 20.dp,
                contentDescription = stringResource(if (replay.playing) R.string.trips_replay_pause else R.string.trips_replay_play),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SpeedSparkline(replay, onScrub, Modifier.fillMaxWidth().height(34.dp))
            Row(Modifier.fillMaxWidth()) {
                val style = MpdTheme.type.micro.copy(fontWeight = MpdTheme.type.bodyS.fontWeight).tnum()
                Text(replay.start, style = style, color = c.text2)
                Text(stringResource(R.string.trips_replay_hint), Modifier.weight(1f), style = style, color = c.text2, textAlign = TextAlign.Center)
                Text(replay.end, style = style, color = c.text2)
            }
        }
    }
}

@Composable
private fun SpeedSparkline(replay: ReplayUi, onScrub: (Float) -> Unit, modifier: Modifier) {
    val c = MpdTheme.colors
    val state = stringResource(R.string.trips_replay_state, replay.time, replay.mph)
    val fraction by rememberUpdatedState(replay.fraction)
    val speeds = replay.speeds
    val top = replay.maxMph.coerceAtLeast(1f)
    Box(
        modifier
            .pointerInput(Unit) { detectTapGestures { onScrub(it.x / size.width) } }
            .pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> change.consume(); onScrub(change.position.x / size.width) } }
            .semantics {
                role = Role.Image
                contentDescription = state
                stateDescription = state
                liveRegion = LiveRegionMode.Polite
                progressBarRangeInfo = ProgressBarRangeInfo(replay.fraction, 0f..1f)
                setProgress { onScrub(it); true }
            }
            .drawWithCache {
                val w = size.width
                val h = size.height
                val pts = speeds.mapIndexed { i, v -> Offset(i * w / (speeds.size - 1).coerceAtLeast(1), h - 4.dp.toPx() - v / top * (h - 8.dp.toPx())) }
                val line = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
                val area = Path().apply { addPath(line); lineTo(w, h); lineTo(0f, h); close() }
                val lineStroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val knob = 7.dp.toPx()
                onDrawBehind {
                    drawPath(area, c.primary.copy(alpha = 0.15f))
                    drawPath(line, c.primary, style = lineStroke)
                    val x = w * fraction
                    drawLine(c.text, Offset(x, -4.dp.toPx()), Offset(x, h + 4.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                    drawCircle(c.surface, knob + 2.dp.toPx(), Offset(x, -3.dp.toPx()))
                    drawCircle(c.text, knob, Offset(x, -3.dp.toPx()))
                }
            },
    )
}

@Composable
private fun StopsCard(state: TripDetailUiState) {
    val c = MpdTheme.colors
    val unknown = stringResource(R.string.trips_unknown_place)
    MpdCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), spacing = 0.dp) {
        StopRow(MpdIcons.Circle, c.text2, state.from ?: unknown, state.startTime)
        StopRow(MpdIcons.MapPin, c.primary, state.to ?: unknown, state.endTime ?: state.startTime)
    }
}

@Composable
private fun StopRow(icon: Int, tint: Color, place: String, time: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MpdIcon(icon, tint, size = 20.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(place, style = MpdTheme.type.bodyLStrong, color = MpdTheme.colors.text)
            Text(time, style = MpdTheme.type.caption, color = MpdTheme.colors.text2)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, accent: Boolean, modifier: Modifier) {
    MpdCard(modifier.semantics(mergeDescendants = true) {}, radius = 16.dp, padding = PaddingValues(14.dp), spacing = 2.dp) {
        Text(label, style = MpdTheme.type.label.copy(fontWeight = MpdTheme.type.bodyS.fontWeight), color = MpdTheme.colors.text2, maxLines = 1)
        Text(value, style = MpdTheme.type.valueS.tnum(), color = if (accent) MpdTheme.colors.primary else MpdTheme.colors.text, maxLines = 1)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp).semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MpdTheme.type.bodyM, color = MpdTheme.colors.text2)
        Text(value, Modifier.weight(1f), style = MpdTheme.type.bodyM.copy(fontWeight = MpdTheme.type.bodyLStrong.fontWeight), color = MpdTheme.colors.text, textAlign = TextAlign.End)
    }
}

@Composable
internal fun DeleteTripDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MpdTheme.colors.surface,
        title = { Text(stringResource(R.string.trips_delete_confirm_title), style = MpdTheme.type.headlineS, color = MpdTheme.colors.text) },
        text = { Text(stringResource(R.string.trips_delete_confirm_body), style = MpdTheme.type.bodyM, color = MpdTheme.colors.text2) },
        confirmButton = { MpdButton(stringResource(R.string.trips_delete), onConfirm, kind = ButtonKind.Text, height = 48.dp, content = MpdTheme.colors.danger) },
        dismissButton = { MpdButton(stringResource(R.string.trips_cancel), onDismiss, kind = ButtonKind.Text, height = 48.dp) },
    )
}

private const val REPLAY_SPEED = 60f

private val sampleDetailRoute = listOf(
    GeoPoint(40.7033, -73.9881), GeoPoint(40.7180, -73.9860), GeoPoint(40.7195, -73.9700), GeoPoint(40.7230, -73.9660),
    GeoPoint(40.7300, -73.9640), GeoPoint(40.7390, -73.9625), GeoPoint(40.7450, -73.9615), GeoPoint(40.7560, -73.9580), GeoPoint(40.7530, -73.9420),
)

internal val sampleDetail = TripDetailUiState(
    loading = false,
    points = sampleDetailRoute,
    from = "Brooklyn office",
    to = "Acme HQ, Midtown",
    startTime = "9:12 AM",
    endTime = "9:41 AM",
    distance = "18.4 mi",
    durationMinutes = 29,
    rate = "76¢",
    amount = "$13.98",
    purpose = "Client meeting",
    category = TripCategory.BUSINESS,
    vehicle = "Personal · 2021 Civic",
    replay = ReplayUi(
        speeds = listOf(0f, 8f, 22f, 31f, 28f, 12f, 4f, 18f, 34f, 41f, 38f, 35f, 22f, 9f, 17f, 29f, 33f, 26f, 14f, 3f),
        maxMph = 45f,
        fraction = 0.55f,
        routeFraction = 0.55f,
        time = "9:26 AM",
        mph = 34,
        start = "9:12",
        end = "9:41",
        durationMs = 1_740_000,
        playing = false,
    ),
)

@Preview(widthDp = 412, heightDp = 879)
@Composable
private fun TripDetailLightPreview() = MpdTheme(darkTheme = false) { TripDetailScreen(sampleDetail, {}, {}, {}, {}, {}, {}) }

@Preview(widthDp = 412, heightDp = 879)
@Composable
private fun TripDetailDarkPreview() = MpdTheme(darkTheme = true) { TripDetailScreen(sampleDetail, {}, {}, {}, {}, {}, {}) }
