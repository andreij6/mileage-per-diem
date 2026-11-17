package com.aijones.mileage.feature.perdiem.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.PerDiemRoute
import com.aijones.mileage.core.navigation.PerDiemSearchRoute
import com.aijones.mileage.core.navigation.AddStayRoute

fun EntryProviderScope<NavKey>.perDiemEntries(navigator: Navigator) {
    entry<PerDiemRoute> { PendingScreen("PerDiemRoute") }
    entry<PerDiemSearchRoute> { PendingScreen("PerDiemSearchRoute") }
    entry<AddStayRoute> { PendingScreen("AddStayRoute") }
}

@Composable
private fun PendingScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(name) }
}
