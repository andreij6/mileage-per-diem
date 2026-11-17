package com.aijones.mileage.feature.trips.domain

import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.model.mileageAmount
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

enum class TripStatus { RECORDING, PAUSED, DONE, MANUAL }

enum class TripCategory { BUSINESS, PERSONAL }

data class Trip(
    val id: Long = 0,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val status: TripStatus,
    val distance: Distance = Distance.ZERO,
    val recordedDistance: Distance? = null,
    val purpose: String? = null,
    val category: TripCategory = TripCategory.BUSINESS,
    val vehicleId: Long? = null,
    val ratePerMile: Money? = null,
    val startLabel: String? = null,
    val endLabel: String? = null,
    val start: GeoPoint? = null,
    val end: GeoPoint? = null,
    val notes: String? = null,
    val needsReview: Boolean = false,
    val edited: Boolean = false,
    val roundTrip: Boolean = false,
    val source: String = "app",
) {
    val reimbursement: Money
        get() = if (category == TripCategory.BUSINESS && ratePerMile != null) mileageAmount(distance, ratePerMile) else Money.ZERO

    val duration: Duration? get() = endedAt?.let { Duration.between(startedAt, it) }

    val isActive: Boolean get() = status == TripStatus.RECORDING || status == TripStatus.PAUSED

    val isFinished: Boolean get() = status == TripStatus.DONE || status == TripStatus.MANUAL
}

data class TrackPoint(val t: Instant, val point: GeoPoint, val accuracyM: Float, val speedMps: Float? = null)

data class Vehicle(val id: Long, val name: String, val kind: String, val isDefault: Boolean)

data class MileageRate(val from: LocalDate, val to: LocalDate, val perMile: Money, val source: String) {
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(from) && !date.isAfter(to)
}

data class MonthSummary(
    val month: YearMonth,
    val reimbursement: Money,
    val distance: Distance,
    val trips: Int,
    val currentRate: Money,
)
