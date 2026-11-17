package com.aijones.mileage.feature.settings.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.SettingsRoute
import com.aijones.mileage.core.navigation.VehiclesRoute
import com.aijones.mileage.core.navigation.MileageRateRoute
import com.aijones.mileage.core.navigation.AboutRoute
import com.aijones.mileage.core.navigation.DeveloperOptionsRoute
import com.aijones.mileage.core.navigation.AppHealthRoute

fun EntryProviderScope<NavKey>.settingsEntries(navigator: Navigator) {
    entry<SettingsRoute> { PendingScreen("SettingsRoute") }
    entry<VehiclesRoute> { PendingScreen("VehiclesRoute") }
    entry<MileageRateRoute> { PendingScreen("MileageRateRoute") }
    entry<AboutRoute> { PendingScreen("AboutRoute") }
    entry<DeveloperOptionsRoute> { PendingScreen("DeveloperOptionsRoute") }
    entry<AppHealthRoute> { PendingScreen("AppHealthRoute") }
}

@Composable
private fun PendingScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(name) }
}
