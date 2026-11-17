package com.aijones.mileage.feature.trips.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.aijones.mileage.core.navigation.AddTripRoute
import com.aijones.mileage.core.navigation.AllTripsRoute
import com.aijones.mileage.core.navigation.EditTripRoute
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.ReviewTripsRoute
import com.aijones.mileage.core.navigation.TripDetailRoute
import com.aijones.mileage.core.navigation.TripsRoute

fun EntryProviderScope<NavKey>.tripsEntries(navigator: Navigator) {
    entry<TripsRoute> { HomeRoute(navigator) }
    entry<AllTripsRoute> { TripListRoute(navigator) }
    entry<TripDetailRoute> { key -> TripDetailEntry(key.tripId, navigator) }
    entry<EditTripRoute> { key -> EditTripEntry(key.tripId, navigator) }
    entry<AddTripRoute> { AddTripEntry(navigator) }
    entry<ReviewTripsRoute> { ReviewRoute(navigator) }
}
