package com.aijones.mileage.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "trip", indices = [Index("startedAt"), Index("status")])
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long? = null,
    val status: String,
    val distanceMeters: Double = 0.0,
    val recordedDistanceMeters: Double? = null,
    val purpose: String? = null,
    val category: String = "BUSINESS",
    val vehicleId: Long? = null,
    val rateMicrosPerMile: Long? = null,
    val startLabel: String? = null,
    val endLabel: String? = null,
    val startLat: Double? = null,
    val startLng: Double? = null,
    val endLat: Double? = null,
    val endLng: Double? = null,
    val notes: String? = null,
    val needsReview: Boolean = false,
    val edited: Boolean = false,
    val roundTrip: Boolean = false,
    val source: String = "app",
)
