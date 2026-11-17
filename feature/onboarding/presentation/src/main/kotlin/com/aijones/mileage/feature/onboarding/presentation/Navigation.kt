package com.aijones.mileage.feature.onboarding.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.aijones.mileage.core.navigation.AddTripRoute
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.OnboardingRoute
import com.aijones.mileage.core.navigation.PreciseLocationOffRoute
import com.aijones.mileage.core.navigation.TripsRoute

fun EntryProviderScope<NavKey>.onboardingEntries(navigator: Navigator) {
    entry<OnboardingRoute> {
        OnboardingRoute(
            onDone = { approximateOnly ->
                navigator.switchTab(TripsRoute)
                if (approximateOnly) navigator.navigate(PreciseLocationOffRoute)
            },
        )
    }
    entry<PreciseLocationOffRoute> { PreciseLocationOffRoute(onLogByHand = { navigator.replace(AddTripRoute) }) }
}
