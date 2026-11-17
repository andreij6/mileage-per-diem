package com.aijones.mileage.feature.reports.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.aijones.mileage.core.navigation.AllTripsRoute
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.PerDiemRoute
import com.aijones.mileage.core.navigation.ReportsRoute

fun EntryProviderScope<NavKey>.reportsEntries(navigator: Navigator) {
    entry<ReportsRoute> {
        ReportsRoute(
            onOpenTrips = { navigator.navigate(AllTripsRoute) },
            onOpenPerDiem = { navigator.switchTab(PerDiemRoute) },
        )
    }
}
