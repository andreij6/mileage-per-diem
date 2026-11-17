package com.aijones.mileage.feature.onboarding.presentation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.designsystem.component.ButtonKind
import com.aijones.mileage.core.designsystem.component.IconTile
import com.aijones.mileage.core.designsystem.component.MpdButton
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.ui.RouteCanvas
import com.aijones.mileage.feature.onboarding.domain.CompleteOnboardingStepUseCase
import com.aijones.mileage.feature.onboarding.domain.ObserveOnboardingStateUseCase
import com.aijones.mileage.feature.onboarding.domain.OnboardingStep
import com.aijones.mileage.feature.onboarding.domain.RecordPermissionResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class OnboardingUiState(val step: OnboardingStep? = null, val done: Boolean = false, val approximateOnly: Boolean = false)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    observeOnboarding: ObserveOnboardingStateUseCase,
    private val completeStep: CompleteOnboardingStepUseCase,
    private val recordPermission: RecordPermissionResultUseCase,
    private val analytics: Analytics,
) : ViewModel() {
    private val approximateOnly = MutableStateFlow(false)
    private var shownStep: OnboardingStep? = null

    val state: StateFlow<OnboardingUiState> = combine(observeOnboarding(), approximateOnly) { s, approx ->
        OnboardingUiState(s.next, s.isDone, approx)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingUiState())

    fun onStepShown(step: OnboardingStep) {
        if (step == shownStep) return
        shownStep = step
        analytics.track(AnalyticsEvent.ScreenView("onboarding_" + step.name.lowercase()))
    }

    fun getStarted() = complete(OnboardingStep.WELCOME, CompleteOnboardingStepUseCase.CONTINUE)

    fun onLocationResult(fine: Boolean, coarse: Boolean) {
        recordPermission(PERMISSION_LOCATION, granted = fine || coarse, precise = fine)
        approximateOnly.value = coarse && !fine
        complete(OnboardingStep.LOCATION, CompleteOnboardingStepUseCase.CONTINUE)
    }

    fun skipLocation() = complete(OnboardingStep.LOCATION, CompleteOnboardingStepUseCase.SKIP)

    fun onNotificationsResult(granted: Boolean) {
        recordPermission(PERMISSION_NOTIFICATIONS, granted)
        complete(OnboardingStep.NOTIFICATIONS, CompleteOnboardingStepUseCase.CONTINUE)
    }

    fun skipNotifications() = complete(OnboardingStep.NOTIFICATIONS, CompleteOnboardingStepUseCase.SKIP)

    private fun complete(step: OnboardingStep, result: String) {
        viewModelScope.launch { completeStep(step, result) }
    }

    companion object {
        const val PERMISSION_LOCATION = "location"
        const val PERMISSION_NOTIFICATIONS = "notifications"
    }
}

@Composable
fun OnboardingRoute(onDone: (approximateOnly: Boolean) -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone(state.approximateOnly) }
    val location = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        viewModel.onLocationResult(
            fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true,
            coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true,
        )
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), viewModel::onNotificationsResult)
    OnboardingScreen(
        state = state,
        onStepShown = viewModel::onStepShown,
        onGetStarted = viewModel::getStarted,
        onAllowLocation = { location.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
        onSkipLocation = viewModel::skipLocation,
        onAllowNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            else viewModel.onNotificationsResult(true)
        },
        onSkipNotifications = viewModel::skipNotifications,
    )
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onStepShown: (OnboardingStep) -> Unit,
    onGetStarted: () -> Unit,
    onAllowLocation: () -> Unit,
    onSkipLocation: () -> Unit,
    onAllowNotifications: () -> Unit,
    onSkipNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val step = state.step ?: return
    LaunchedEffect(step) { onStepShown(step) }
    when (step) {
        OnboardingStep.WELCOME -> WelcomeStep(onGetStarted, modifier)
        OnboardingStep.LOCATION -> DisclosureStep(onAllowLocation, onSkipLocation, modifier)
        OnboardingStep.NOTIFICATIONS -> NotificationsStep(onAllowNotifications, onSkipNotifications, modifier)
    }
}

@Composable
private fun WelcomeStep(onGetStarted: () -> Unit, modifier: Modifier) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    OnboardingPage(modifier) {
        Spacer(Modifier.weight(1f))
        IconTile(MpdIcons.Route, c.primary, c.onPrimary, size = 88.dp, iconSize = 44.dp, radius = 24.dp)
        Text(stringResource(R.string.onboarding_app_name), Modifier.semantics { heading() }, style = t.displayM, color = c.text)
        Text(stringResource(R.string.onboarding_welcome_body), style = t.bodyL, color = c.text2)
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Point(MpdIcons.Car, stringResource(R.string.onboarding_welcome_point_rate), Alignment.CenterVertically)
            Point(MpdIcons.Utensils, stringResource(R.string.onboarding_welcome_point_perdiem), Alignment.CenterVertically)
            Point(MpdIcons.FileText, stringResource(R.string.onboarding_welcome_point_reports), Alignment.CenterVertically)
        }
        Spacer(Modifier.weight(1f))
        MpdButton(stringResource(R.string.onboarding_get_started), onGetStarted, Modifier.fillMaxWidth())
        Text(
            stringResource(R.string.onboarding_byline),
            Modifier.fillMaxWidth(),
            style = t.caption.copy(fontWeight = t.label.fontWeight, textAlign = TextAlign.Center),
            color = c.text2,
        )
    }
}

@Composable
private fun DisclosureStep(onContinue: () -> Unit, onNotNow: () -> Unit, modifier: Modifier) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    OnboardingPage(modifier) {
        Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
            RouteCanvas(sampleRoute, stringResource(R.string.onboarding_disclosure_map), Modifier.fillMaxSize())
            IconTile(MpdIcons.Navigation, c.primary, c.onPrimary, size = 72.dp, iconSize = 32.dp, radius = 36.dp)
        }
        Text(stringResource(R.string.onboarding_disclosure_title), Modifier.semantics { heading() }, style = t.headlineM, color = c.text)
        Text(stringResource(R.string.onboarding_disclosure_body), style = t.bodyL, color = c.text2)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Point(MpdIcons.Navigation, stringResource(R.string.onboarding_disclosure_point_start))
            Point(MpdIcons.ShieldCheck, stringResource(R.string.onboarding_disclosure_point_local))
            Point(MpdIcons.BatteryMedium, stringResource(R.string.onboarding_disclosure_point_battery))
        }
        Spacer(Modifier.weight(1f))
        MpdButton(stringResource(R.string.onboarding_continue), onContinue, Modifier.fillMaxWidth())
        MpdButton(stringResource(R.string.onboarding_not_now), onNotNow, Modifier.fillMaxWidth(), kind = ButtonKind.Text, height = 48.dp)
    }
}

@Composable
private fun NotificationsStep(onAllow: () -> Unit, onSkip: () -> Unit, modifier: Modifier) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    OnboardingPage(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(c.surface2)
                .padding(horizontal = 20.dp)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(20.dp))
                    .background(c.surface, RoundedCornerShape(20.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconTile(MpdIcons.Navigation, c.primary, c.onPrimary, size = 32.dp, iconSize = 16.dp, radius = 16.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.onboarding_notification_sample_title), style = t.labelM, color = c.text)
                    Text(stringResource(R.string.onboarding_notification_sample_body), style = t.caption, color = c.text2)
                }
            }
        }
        Text(stringResource(R.string.onboarding_notifications_title), Modifier.semantics { heading() }, style = t.headlineM, color = c.text)
        Text(stringResource(R.string.onboarding_notifications_body), style = t.bodyL, color = c.text2)
        Spacer(Modifier.weight(1f))
        MpdButton(stringResource(R.string.onboarding_allow_notifications), onAllow, Modifier.fillMaxWidth(), icon = MpdIcons.Bell)
        MpdButton(stringResource(R.string.onboarding_skip), onSkip, Modifier.fillMaxWidth(), kind = ButtonKind.Text, height = 48.dp)
    }
}

@Composable
private fun Point(icon: Int, text: String, align: Alignment.Vertical = Alignment.Top) {
    val c = MpdTheme.colors
    Row(Modifier.semantics(mergeDescendants = true) { }, verticalAlignment = align, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconTile(icon, c.primaryC, c.onPrimaryC, size = 36.dp, iconSize = 18.dp, radius = 10.dp)
        Text(text, style = MpdTheme.type.bodyM, color = c.text)
    }
}

private val sampleRoute = listOf(.24 to .86, .27 to .64, .45 to .62, .5 to .58, .53 to .5, .55 to .4, .56 to .33, .6 to .2, .78 to .24)
    .map { (x, y) -> GeoPoint(40.70 + (1 - y) * 0.15, -74.00 + x * 0.28) }

@Composable
private fun OnboardingPreview(step: OnboardingStep, dark: Boolean) = PreviewSurface(dark) {
    OnboardingScreen(OnboardingUiState(step), {}, {}, {}, {}, {}, {})
}

@Preview(name = "Welcome light", widthDp = 412, heightDp = 879)
@Composable
private fun WelcomeLightPreview() = OnboardingPreview(OnboardingStep.WELCOME, false)

@Preview(name = "Welcome dark", widthDp = 412, heightDp = 879)
@Composable
private fun WelcomeDarkPreview() = OnboardingPreview(OnboardingStep.WELCOME, true)

@Preview(name = "Disclosure light", widthDp = 412, heightDp = 879)
@Composable
private fun DisclosureLightPreview() = OnboardingPreview(OnboardingStep.LOCATION, false)

@Preview(name = "Disclosure dark", widthDp = 412, heightDp = 879)
@Composable
private fun DisclosureDarkPreview() = OnboardingPreview(OnboardingStep.LOCATION, true)

@Preview(name = "Notifications light", widthDp = 412, heightDp = 879)
@Composable
private fun NotificationsLightPreview() = OnboardingPreview(OnboardingStep.NOTIFICATIONS, false)

@Preview(name = "Notifications dark", widthDp = 412, heightDp = 879)
@Composable
private fun NotificationsDarkPreview() = OnboardingPreview(OnboardingStep.NOTIFICATIONS, true)
