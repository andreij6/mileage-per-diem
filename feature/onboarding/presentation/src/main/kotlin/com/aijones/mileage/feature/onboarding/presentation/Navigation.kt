package com.aijones.mileage.feature.onboarding.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.OnboardingRoute
import com.aijones.mileage.core.navigation.PreciseLocationOffRoute

fun EntryProviderScope<NavKey>.onboardingEntries(navigator: Navigator) {
    entry<OnboardingRoute> { PendingScreen("OnboardingRoute") }
    entry<PreciseLocationOffRoute> { PendingScreen("PreciseLocationOffRoute") }
}

@Composable
private fun PendingScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(name) }
}
