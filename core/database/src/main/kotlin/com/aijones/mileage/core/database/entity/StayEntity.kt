package com.aijones.mileage.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "stay", indices = [Index("checkInEpochDay")])
data class StayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val locationId: Long,
    val locationLabel: String,
    val fiscalYear: Int,
    val checkInEpochDay: Long,
    val checkOutEpochDay: Long,
    val includeLodging: Boolean,
    val mieDailyCents: Long,
    val mieTotalCents: Long,
    val lodgingTotalCents: Long,
)
