package com.aijones.mileage.ui

import android.content.Intent
import androidx.activity.compose.ReportDrawn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.aijones.mileage.core.ui.LocalPageAnimatedScope
import com.aijones.mileage.core.designsystem.component.MpdNavBar
import com.aijones.mileage.core.designsystem.component.NavItem
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.navigation.AddTripRoute
import com.aijones.mileage.core.navigation.Deeplinks
import com.aijones.mileage.core.navigation.EditTripRoute
import com.aijones.mileage.core.navigation.LiveTripRoute
import com.aijones.mileage.core.navigation.OnboardingRoute
import com.aijones.mileage.core.navigation.PerDiemRoute
import com.aijones.mileage.core.navigation.PreciseLocationOffRoute
import com.aijones.mileage.core.navigation.ReportsRoute
import com.aijones.mileage.core.navigation.ReviewTripsRoute
import com.aijones.mileage.core.navigation.SettingsRoute
import com.aijones.mileage.core.navigation.TopLevelRoute
import com.aijones.mileage.core.navigation.TripReceiptRoute
import com.aijones.mileage.core.navigation.TripsRoute
import com.aijones.mileage.core.navigation.YearInReviewRoute
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
    val pageScopeDecorator = remember {
        NavEntryDecorator<NavKey>(
            decorate = { entry ->
                CompositionLocalProvider(LocalPageAnimatedScope provides LocalNavAnimatedContentScope.current) {
                    entry.Content()
                }
            },
        )
    }
    val top = backStack.lastOrNull() as? TopLevelRoute
    Column(Modifier.fillMaxSize().background(MpdTheme.colors.bg)) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.weight(1f),
            onBack = { navigator.back() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
                pageScopeDecorator,
            ),
            transitionSpec = {
                val target = targetState.entries.lastOrNull()?.contentKey
                val initial = initialState.entries.lastOrNull()?.contentKey
                when {
                    target is TopLevelRoute && initial is TopLevelRoute -> {
                        fadeIn(tween(260)) togetherWith fadeOut(tween(180))
                    }
                    isModalKey(target) -> {
                        (slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it / 5 } +
                            scaleIn(tween(340), initialScale = 0.97f) +
                            fadeIn(tween(260))) togetherWith
                            (scaleOut(tween(340), targetScale = 0.96f) + fadeOut(tween(240)))
                    }
                    else -> {
                        (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 4 } +
                            fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 10 } +
                                scaleOut(tween(320), targetScale = 0.96f) +
                                fadeOut(tween(240)))
                    }
                }
            },
            popTransitionSpec = {
                val initial = initialState.entries.lastOrNull()?.contentKey
                if (isModalKey(initial)) {
                    (scaleIn(tween(280), initialScale = 0.96f) + fadeIn(tween(240))) togetherWith
                        (slideOutVertically(tween(280, easing = FastOutSlowInEasing)) { it / 4 } + fadeOut(tween(220)))
                } else {
                    (slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it / 10 } +
                        scaleIn(tween(280), initialScale = 0.96f) +
                        fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 4 } + fadeOut(tween(220)))
                }
            },
            predictivePopTransitionSpec = {
                val initial = initialState.entries.lastOrNull()?.contentKey
                if (isModalKey(initial)) {
                    (scaleIn(tween(280), initialScale = 0.96f) + fadeIn(tween(240))) togetherWith
                        (slideOutVertically(tween(280, easing = FastOutSlowInEasing)) { it / 4 } + fadeOut(tween(220)))
                } else {
                    (slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it / 10 } +
                        scaleIn(tween(280), initialScale = 0.96f) +
                        fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 4 } + fadeOut(tween(220)))
                }
            },
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
        AnimatedVisibility(
            visible = top != null,
            enter = slideInVertically(tween(240)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(160)),
        ) {
            if (top != null) MpdNavBar(navItems, tabs.indexOf(top), onSelect = { navigator.switchTab(tabs[it]) })
        }
    }
    TrackingOverlays(navigator)
    ReportDrawn()
}

private fun isModalKey(key: Any?): Boolean = when (key) {
    is LiveTripRoute,
    is TripReceiptRoute,
    ReviewTripsRoute,
    AddTripRoute,
    is EditTripRoute,
    is YearInReviewRoute,
    OnboardingRoute,
    PreciseLocationOffRoute,
    -> true
    else -> false
}
