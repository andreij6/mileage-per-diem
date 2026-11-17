package com.aijones.mileage.feature.settings.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.aijones.mileage.core.navigation.AboutRoute
import com.aijones.mileage.core.navigation.AppHealthRoute
import com.aijones.mileage.core.navigation.DeveloperOptionsRoute
import com.aijones.mileage.core.navigation.MileageRateRoute
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.SettingsRoute
import com.aijones.mileage.core.navigation.VehiclesRoute

fun EntryProviderScope<NavKey>.settingsEntries(navigator: Navigator) {
    entry<SettingsRoute> {
        SettingsRoute(
            onVehicles = { navigator.navigate(VehiclesRoute) },
            onMileageRate = { navigator.navigate(MileageRateRoute) },
            onAbout = { navigator.navigate(AboutRoute) },
            onDeveloperOptions = { navigator.navigate(DeveloperOptionsRoute) },
        )
    }
    entry<VehiclesRoute> { VehiclesRoute(onBack = navigator::back) }
    entry<MileageRateRoute> { MileageRateRoute(onBack = navigator::back) }
    entry<AboutRoute> { AboutRoute(onBack = navigator::back) }
    entry<DeveloperOptionsRoute> { DeveloperOptionsRoute(onBack = navigator::back, onAppHealth = { navigator.navigate(AppHealthRoute) }) }
    entry<AppHealthRoute> { AppHealthRoute(onBack = navigator::back) }
}
