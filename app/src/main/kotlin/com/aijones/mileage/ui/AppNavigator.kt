package com.aijones.mileage.ui

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.TopLevelRoute
import com.aijones.mileage.core.navigation.TripsRoute

class AppNavigator(private val backStack: NavBackStack<NavKey>) : Navigator {
    override fun navigate(route: NavKey) {
        backStack.add(route)
    }

    override fun replace(route: NavKey) {
        backStack.removeLastOrNull()
        backStack.add(route)
    }

    override fun back() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    override fun switchTab(route: TopLevelRoute) {
        backStack.clear()
        backStack.add(TripsRoute)
        if (route != TripsRoute) backStack.add(route)
    }
}
