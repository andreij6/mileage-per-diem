package com.aijones.mileage.feature.tracking.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.designsystem.component.Grabber
import com.aijones.mileage.core.designsystem.component.MpdChip
import com.aijones.mileage.core.designsystem.component.Odometer
import com.aijones.mileage.core.designsystem.component.OdometerColors
import com.aijones.mileage.core.designsystem.component.StatusDot
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.designsystem.theme.tnum
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.model.formatUsd
import com.aijones.mileage.core.navigation.LiveTripRoute
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.PreciseLocationOffRoute
import com.aijones.mileage.core.navigation.TripReceiptRoute
import com.aijones.mileage.core.ui.RouteCanvas
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@Composable
fun LiveTripRoute(key: LiveTripRoute, navigator: Navigator) {
    val vm = hiltViewModel<LiveTripViewModel, LiveTripViewModel.Factory>(creationCallback = { it.create(key) })
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val location = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        vm.onLocationPermission(r[Manifest.permission.ACCESS_FINE_LOCATION] == true, r[Manifest.permission.ACCESS_COARSE_LOCATION] == true, asked = true)
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.onNotificationPermission(it, asked = true) }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        vm.onLocationSettings(it.resultCode == Activity.RESULT_OK || context.deviceLocationOn())
    }
    var launched by rememberSaveable { mutableStateOf<PermissionStep?>(null) }
    LaunchedEffect(state.permissionStep) {
        val step = state.permissionStep
        if (step == null || step == launched) return@LaunchedEffect
        launched = step
        when (step) {
            PermissionStep.Location ->
                if (context.granted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    vm.onLocationPermission(fine = true, coarse = true, asked = false)
                } else {
                    location.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            PermissionStep.Notifications ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || context.granted(Manifest.permission.POST_NOTIFICATIONS)) {
                    vm.onNotificationPermission(granted = true, asked = false)
                } else {
                    notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            PermissionStep.LocationSettings -> checkLocationSettings(context, settings, vm::onLocationSettings)
        }
    }
    LaunchedEffect(state.nav) {
        when (val nav = state.nav ?: return@LaunchedEffect) {
            is LiveTripNav.Receipt -> navigator.replace(TripReceiptRoute(nav.tripId))
            LiveTripNav.PreciseLocationOff -> navigator.replace(PreciseLocationOffRoute)
            LiveTripNav.Back -> navigator.back()
        }
        vm.onNavigated()
    }
    LiveTripScreen(state, onTogglePause = vm::togglePause, onEnd = vm::endTrip)
}

private fun Context.granted(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private suspend fun checkLocationSettings(
    context: Context,
    launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
    onResult: (Boolean) -> Unit,
) {
    if (context.deviceLocationOn()) {
        onResult(true)
        return
    }
    val request = LocationSettingsRequest.Builder()
        .addLocationRequest(LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000).build())
        .build()
    try {
        LocationServices.getSettingsClient(context).checkLocationSettings(request).await()
        onResult(true)
    } catch (e: ResolvableApiException) {
        launcher.launch(IntentSenderRequest.Builder(e.resolution).build())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onResult(context.deviceLocationOn())
    }
}

private fun Context.deviceLocationOn(): Boolean =
    getSystemService(LocationManager::class.java)?.let(LocationManagerCompat::isLocationEnabled) == true

@Composable
fun LiveTripScreen(state: LiveTripUiState, onTogglePause: () -> Unit, onEnd: () -> Unit, modifier: Modifier = Modifier) {
    val c = MpdTheme.colors
    BoxWithConstraints(modifier.fillMaxSize().background(c.bg)) {
        val sheetMax = maxHeight * 0.75f
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy((-SHEET_OVERLAP).dp)) {
            RouteCanvas(
                points = state.route,
                description = stringResource(R.string.live_route_description, formatTenths(state.tenths)),
                modifier = Modifier.fillMaxWidth().weight(1f),
                live = true,
                showScale = false,
                padding = ROUTE_PADDING,
            )
            LiveSheet(state, onTogglePause, onEnd, Modifier.heightIn(max = sheetMax))
        }
    }
}

@Composable
private fun LiveSheet(state: LiveTripUiState, onTogglePause: () -> Unit, onEnd: () -> Unit, modifier: Modifier = Modifier) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(c.surface)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Grabber()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusDot(if (state.status == LiveStatus.Paused) c.accent else c.danger)
            Text(
                stringResource(
                    when (state.status) {
                        LiveStatus.Starting -> R.string.live_starting
                        LiveStatus.Recording -> R.string.live_recording
                        LiveStatus.Paused -> R.string.live_paused
                    },
                ),
                Modifier.weight(1f),
                style = t.labelM,
                color = c.text,
            )
            MpdChip(
                state.accuracyM?.let { stringResource(R.string.live_gps, it) } ?: stringResource(R.string.live_gps_searching),
                icon = MpdIcons.Navigation,
            )
        }
        Readout(state)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MpdChip(state.purpose ?: stringResource(R.string.live_no_purpose), icon = MpdIcons.Pencil, bg = c.primaryC, fg = c.onPrimaryC)
            MpdChip(policyLabel(state), icon = MpdIcons.BatteryMedium, outlined = true)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PauseButton(paused = state.status == LiveStatus.Paused, enabled = state.status != LiveStatus.Starting, onClick = onTogglePause)
            SlideToEnd(onEnd = onEnd, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun policyLabel(state: LiveTripUiState): String {
    val name = stringResource(
        when (state.policy) {
            "balanced" -> R.string.live_policy_balanced
            "saver" -> R.string.live_policy_saver
            else -> R.string.live_policy_high
        },
    )
    return state.batteryPct?.let { stringResource(R.string.live_policy_battery, name, it) } ?: name
}

@Composable
private fun Readout(state: LiveTripUiState) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    val elapsed = formatElapsed(state.elapsedSeconds)
    val amount = state.amount.formatUsd()
    val summary = stringResource(R.string.live_stats_spoken, formatTenths(state.tenths), amount, elapsed)
    val latest by rememberUpdatedState(summary)
    var spoken by remember { mutableStateOf(summary) }
    LaunchedEffect(Unit) {
        while (true) {
            spoken = latest
            delay(ANNOUNCE_EVERY_MS)
        }
    }
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = spoken
            liveRegion = LiveRegionMode.Polite
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Odometer(
            tenths = state.tenths,
            colors = OdometerColors(digit = c.text, drum = c.surface2, tenthDrum = c.primary, tenthDigit = c.onPrimary),
            size = 30,
            animate = !rememberReducedMotion(),
        )
        Spacer(Modifier.weight(1f).width(16.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(amount, style = t.headlineS.tnum(), color = c.text)
            Text(stringResource(R.string.live_elapsed_speed, elapsed, state.speedMph), style = t.caption.tnum(), color = c.text2)
        }
    }
}

@Composable
private fun PauseButton(paused: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = MpdTheme.colors
    val description = stringResource(if (paused) R.string.live_resume else R.string.live_pause)
    Box(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .border(1.dp, c.outline2, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (paused) {
            MpdIcon(MpdIcons.Play, c.text, size = 22.dp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(2) { Box(Modifier.size(width = 5.dp, height = 18.dp).clip(RoundedCornerShape(2.dp)).background(c.text)) }
            }
        }
    }
}

private const val SHEET_OVERLAP = 40
private const val ROUTE_PADDING = 72f
private const val ANNOUNCE_EVERY_MS = 30_000L

internal val previewRoute = listOf(
    GeoPoint(40.6782, -73.9442), GeoPoint(40.6890, -73.9400), GeoPoint(40.6900, -73.9300), GeoPoint(40.6925, -73.9270),
    GeoPoint(40.6965, -73.9255), GeoPoint(40.7020, -73.9245), GeoPoint(40.7055, -73.9240),
)

private val previewState = LiveTripUiState(
    status = LiveStatus.Recording,
    tenths = 126,
    amount = Money(9_580_000),
    elapsedSeconds = 24 * 60 + 18,
    speedMph = 31,
    accuracyM = 5,
    purpose = "Client meeting",
    policy = "balanced",
    batteryPct = 82,
    route = previewRoute,
)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun LiveTripLightPreview() = MpdTheme(darkTheme = false) { LiveTripScreen(previewState, {}, {}) }

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun LiveTripDarkPreview() = MpdTheme(darkTheme = true) { LiveTripScreen(previewState, {}, {}) }
