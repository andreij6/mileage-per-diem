package com.aijones.mileage.feature.trips.presentation

import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.TopLevelRoute
import com.aijones.mileage.feature.trips.domain.TrackPoint
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripCategory
import com.aijones.mileage.feature.trips.domain.TripStatus
import androidx.navigation3.runtime.NavKey
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

val NY: ZoneId = ZoneId.of("America/New_York")

fun at(local: String): Instant = LocalDateTime.parse(local).atZone(NY).toInstant()

fun trip(
    id: Long,
    start: String,
    minutes: Long = 29,
    miles: Double = 18.4,
    from: String = "Office",
    to: String = "Acme HQ",
    purpose: String? = "Client meeting",
    category: TripCategory = TripCategory.BUSINESS,
    needsReview: Boolean = false,
    vehicleId: Long? = 1,
) = Trip(
    id = id,
    startedAt = at(start),
    endedAt = at(start).plusSeconds(minutes * 60),
    status = TripStatus.DONE,
    distance = Distance.ofMiles(miles),
    recordedDistance = Distance.ofMiles(miles),
    purpose = purpose,
    category = category,
    vehicleId = vehicleId,
    ratePerMile = Money(760_000),
    startLabel = from,
    endLabel = to,
    start = GeoPoint(40.70, -73.99),
    end = GeoPoint(40.75, -73.98),
    needsReview = needsReview,
)

fun track(start: String, minutes: Int = 29, mps: List<Float> = listOf(0f, 10f, 15f, 15f, 10f, 0f)): List<TrackPoint> =
    mps.mapIndexed { i, v ->
        val f = i / (mps.size - 1.0)
        TrackPoint(at(start).plusMillis((minutes * 60_000L * f).toLong()), GeoPoint(40.70 + 0.05 * f, -73.99 + 0.01 * f), 5f, v)
    }

class RecordingNavigator : Navigator {
    val events = mutableListOf<Any>()
    override fun navigate(route: NavKey) { events += route }
    override fun replace(route: NavKey) { events += route }
    override fun back() { events += "back" }
    override fun switchTab(route: TopLevelRoute) { events += route }
}
