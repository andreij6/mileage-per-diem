package com.aijones.mileage.feature.insights.presentation

import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripCategory
import com.aijones.mileage.feature.trips.domain.TripStatus
import java.time.LocalDateTime
import java.time.ZoneId

private val NY = ZoneId.of("America/New_York")

fun trip(id: Long, at: String, miles: Double, end: String? = "Acme HQ", category: TripCategory = TripCategory.BUSINESS, rateMicros: Long = 760_000) = Trip(
    id = id,
    startedAt = LocalDateTime.parse(at).atZone(NY).toInstant(),
    endedAt = LocalDateTime.parse(at).plusHours(1).atZone(NY).toInstant(),
    status = TripStatus.DONE,
    distance = Distance.ofMiles(miles),
    category = category,
    ratePerMile = Money(rateMicros),
    startLabel = "JFK",
    endLabel = end,
)
