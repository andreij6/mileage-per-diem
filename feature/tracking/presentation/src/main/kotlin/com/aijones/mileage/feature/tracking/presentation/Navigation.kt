package com.aijones.mileage.feature.tracking.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.aijones.mileage.core.navigation.LiveTripRoute
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.TripReceiptRoute

fun EntryProviderScope<NavKey>.trackingEntries(navigator: Navigator) {
    entry<LiveTripRoute> { LiveTripRoute(it, navigator) }
    entry<TripReceiptRoute> { TripReceiptRoute(it, navigator) }
}

@Composable
fun TrackingOverlays(navigator: Navigator) {
    val vm = hiltViewModel<InterruptedTripViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.nav) {
        navigator.navigate(state.nav ?: return@LaunchedEffect)
        vm.onNavigated()
    }
    InterruptedTripSheet(state, onResume = vm::resume, onFinish = vm::finish, onDiscard = vm::discard, onDismiss = vm::dismiss)
}
