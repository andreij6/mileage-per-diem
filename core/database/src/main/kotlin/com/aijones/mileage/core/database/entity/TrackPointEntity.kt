package com.aijones.mileage.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track_point",
    indices = [Index(value = ["tripId", "t"])],
    foreignKeys = [ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE)],
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val t: Long,
    val lat: Double,
    val lng: Double,
    val accuracyM: Float,
    val speedMps: Float? = null,
)
