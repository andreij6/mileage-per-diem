package com.aijones.mileage.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicle")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String,
    val isDefault: Boolean,
)
