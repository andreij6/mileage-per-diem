package com.aijones.mileage.feature.perdiem.presentation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.aijones.mileage.core.navigation.AddStayRoute
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.PerDiemRoute
import com.aijones.mileage.core.navigation.PerDiemSearchRoute

private val instant = NavDisplay.transitionSpec { EnterTransition.None togetherWith ExitTransition.None } +
    NavDisplay.popTransitionSpec { EnterTransition.None togetherWith ExitTransition.None } +
    NavDisplay.predictivePopTransitionSpec { EnterTransition.None togetherWith ExitTransition.None }

fun EntryProviderScope<NavKey>.perDiemEntries(navigator: Navigator) {
    entry<PerDiemRoute> {
        PerDiemScreenRoute(onSearch = { navigator.navigate(PerDiemSearchRoute) }, onAddStay = { navigator.navigate(AddStayRoute(it)) })
    }
    entry<PerDiemSearchRoute> { PerDiemSearchScreenRoute(onBack = navigator::back) }
    entry<AddStayRoute>(metadata = instant) { key ->
        Box {
            PerDiemScreenRoute(onSearch = {}, onAddStay = {}, trackScreenView = false)
            AddStaySheetRoute(key, onDismiss = navigator::back)
        }
    }
}
