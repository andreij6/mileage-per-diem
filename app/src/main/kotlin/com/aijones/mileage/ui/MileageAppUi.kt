package com.aijones.mileage.ui

import android.content.Intent
import androidx.activity.compose.ReportDrawn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.aijones.mileage.core.designsystem.component.MpdNavBar
import com.aijones.mileage.core.designsystem.component.NavItem
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.navigation.Deeplinks
import com.aijones.mileage.core.navigation.LiveTripRoute
import com.aijones.mileage.core.navigation.OnboardingRoute
import com.aijones.mileage.core.navigation.PerDiemRoute
import com.aijones.mileage.core.navigation.ReportsRoute
import com.aijones.mileage.core.navigation.SettingsRoute
import com.aijones.mileage.core.navigation.TopLevelRoute
import com.aijones.mileage.core.navigation.TripsRoute
import com.aijones.mileage.feature.insights.presentation.insightsEntries
import com.aijones.mileage.feature.onboarding.presentation.onboardingEntries
import com.aijones.mileage.feature.perdiem.presentation.perDiemEntries
import com.aijones.mileage.feature.reports.presentation.reportsEntries
import com.aijones.mileage.feature.settings.presentation.settingsEntries
import com.aijones.mileage.feature.tracking.presentation.TrackingOverlays
import com.aijones.mileage.feature.tracking.presentation.trackingEntries
import com.aijones.mileage.feature.trips.presentation.tripsEntries
import kotlinx.coroutines.flow.StateFlow

private val tabs = listOf(TripsRoute, PerDiemRoute, ReportsRoute, SettingsRoute)
private val navItems = listOf(
    NavItem(MpdIcons.Car, "Trips"),
    NavItem(MpdIcons.Utensils, "Per diem"),
    NavItem(MpdIcons.FileText, "Reports"),
    NavItem(MpdIcons.Settings, "Settings"),
)

@Composable
fun MileageAppUi(onboarded: Boolean, intents: StateFlow<Intent?>, onIntentHandled: () -> Unit) {
    val backStack = rememberNavBackStack(if (onboarded) TripsRoute else OnboardingRoute)
    val navigator = remember(backStack) { AppNavigator(backStack) }
    val intent by intents.collectAsStateWithLifecycle()
    LaunchedEffect(intent) {
        val i = intent ?: return@LaunchedEffect
        when (i.action) {
            Deeplinks.ACTION_START_TRIP -> navigator.navigate(LiveTripRoute(i.getStringExtra(Deeplinks.EXTRA_SOURCE) ?: "app"))
            Deeplinks.ACTION_OPEN_LIVE_TRIP -> navigator.navigate(LiveTripRoute())
        }
        onIntentHandled()
    }
    val top = backStack.lastOrNull() as? TopLevelRoute
    Column(Modifier.fillMaxSize().background(MpdTheme.colors.bg)) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.weight(1f),
            onBack = { navigator.back() },
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
            entryProvider = entryProvider {
                onboardingEntries(navigator)
                tripsEntries(navigator)
                trackingEntries(navigator)
                perDiemEntries(navigator)
                reportsEntries(navigator)
                settingsEntries(navigator)
                insightsEntries(navigator)
            },
        )
        if (top != null) MpdNavBar(navItems, tabs.indexOf(top), onSelect = { navigator.switchTab(tabs[it]) })
    }
    TrackingOverlays(navigator)
    ReportDrawn()
}
