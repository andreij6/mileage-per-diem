package com.aijones.mileage.feature.tracking.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.LiveTripRoute
import com.aijones.mileage.core.navigation.TripReceiptRoute

fun EntryProviderScope<NavKey>.trackingEntries(navigator: Navigator) {
    entry<LiveTripRoute> { PendingScreen("LiveTripRoute") }
    entry<TripReceiptRoute> { PendingScreen("TripReceiptRoute") }
}

@Composable
private fun PendingScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(name) }
}

@Composable
fun TrackingOverlays(navigator: Navigator) = Unit
