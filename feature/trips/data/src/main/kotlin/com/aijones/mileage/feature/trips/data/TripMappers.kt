package com.aijones.mileage.feature.trips.data

import com.aijones.mileage.core.database.entity.TrackPointEntity
import com.aijones.mileage.core.database.entity.TripEntity
import com.aijones.mileage.core.database.entity.VehicleEntity
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.trips.domain.TrackPoint
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripCategory
import com.aijones.mileage.feature.trips.domain.TripStatus
import com.aijones.mileage.feature.trips.domain.Vehicle
import java.time.Instant

internal fun TripEntity.toDomain() = Trip(
    id = id,
    startedAt = Instant.ofEpochMilli(startedAt),
    endedAt = endedAt?.let(Instant::ofEpochMilli),
    status = TripStatus.valueOf(status),
    distance = Distance(distanceMeters),
    recordedDistance = recordedDistanceMeters?.let(::Distance),
    purpose = purpose,
    category = TripCategory.valueOf(category),
    vehicleId = vehicleId,
    ratePerMile = rateMicrosPerMile?.let(::Money),
    startLabel = startLabel,
    endLabel = endLabel,
    start = geoPoint(startLat, startLng),
    end = geoPoint(endLat, endLng),
    notes = notes,
    needsReview = needsReview,
    edited = edited,
    roundTrip = roundTrip,
    source = source,
)

internal fun Trip.toEntity() = TripEntity(
    id = id,
    startedAt = startedAt.toEpochMilli(),
    endedAt = endedAt?.toEpochMilli(),
    status = status.name,
    distanceMeters = distance.meters,
    recordedDistanceMeters = recordedDistance?.meters,
    purpose = purpose,
    category = category.name,
    vehicleId = vehicleId,
    rateMicrosPerMile = ratePerMile?.micros,
    startLabel = startLabel,
    endLabel = endLabel,
    startLat = start?.lat,
    startLng = start?.lng,
    endLat = end?.lat,
    endLng = end?.lng,
    notes = notes,
    needsReview = needsReview,
    edited = edited,
    roundTrip = roundTrip,
    source = source,
)

internal fun TrackPointEntity.toDomain() = TrackPoint(Instant.ofEpochMilli(t), GeoPoint(lat, lng), accuracyM, speedMps)

internal fun TrackPoint.toEntity(tripId: Long) =
    TrackPointEntity(tripId = tripId, t = t.toEpochMilli(), lat = point.lat, lng = point.lng, accuracyM = accuracyM, speedMps = speedMps)

internal fun VehicleEntity.toDomain() = Vehicle(id, name, kind, isDefault)

private fun geoPoint(lat: Double?, lng: Double?) = if (lat != null && lng != null) GeoPoint(lat, lng) else null
