package com.aijones.mileage.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface TopLevelRoute : NavKey

@Serializable data object TripsRoute : TopLevelRoute
@Serializable data object PerDiemRoute : TopLevelRoute
@Serializable data object ReportsRoute : TopLevelRoute
@Serializable data object SettingsRoute : TopLevelRoute

@Serializable data object OnboardingRoute : NavKey
@Serializable data object AllTripsRoute : NavKey
@Serializable data class TripDetailRoute(val tripId: Long) : NavKey
@Serializable data class EditTripRoute(val tripId: Long) : NavKey
@Serializable data object AddTripRoute : NavKey
@Serializable data object ReviewTripsRoute : NavKey
@Serializable data class LiveTripRoute(val startSource: String? = null) : NavKey
@Serializable data class TripReceiptRoute(val tripId: Long) : NavKey
@Serializable data object InsightsRoute : NavKey
@Serializable data class YearInReviewRoute(val year: Int) : NavKey
@Serializable data object PerDiemSearchRoute : NavKey
@Serializable data class AddStayRoute(val locationId: Long) : NavKey
@Serializable data object VehiclesRoute : NavKey
@Serializable data object MileageRateRoute : NavKey
@Serializable data object AboutRoute : NavKey
@Serializable data object DeveloperOptionsRoute : NavKey
@Serializable data object AppHealthRoute : NavKey
@Serializable data object PreciseLocationOffRoute : NavKey

interface Navigator {
    fun navigate(route: NavKey)
    fun replace(route: NavKey)
    fun back()
    fun switchTab(route: TopLevelRoute)
}

object Deeplinks {
    const val ACTION_START_TRIP = "com.aijones.mileage.action.START_TRIP"
    const val ACTION_OPEN_LIVE_TRIP = "com.aijones.mileage.action.OPEN_LIVE_TRIP"
    const val EXTRA_SOURCE = "source"
}
