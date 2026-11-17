package com.aijones.mileage.feature.trips.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.TripsRoute
import com.aijones.mileage.core.navigation.AllTripsRoute
import com.aijones.mileage.core.navigation.TripDetailRoute
import com.aijones.mileage.core.navigation.EditTripRoute
import com.aijones.mileage.core.navigation.AddTripRoute
import com.aijones.mileage.core.navigation.ReviewTripsRoute

fun EntryProviderScope<NavKey>.tripsEntries(navigator: Navigator) {
    entry<TripsRoute> { PendingScreen("TripsRoute") }
    entry<AllTripsRoute> { PendingScreen("AllTripsRoute") }
    entry<TripDetailRoute> { PendingScreen("TripDetailRoute") }
    entry<EditTripRoute> { PendingScreen("EditTripRoute") }
    entry<AddTripRoute> { PendingScreen("AddTripRoute") }
    entry<ReviewTripsRoute> { PendingScreen("ReviewTripsRoute") }
}

@Composable
private fun PendingScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(name) }
}
