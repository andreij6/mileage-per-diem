package com.aijones.mileage.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "analytics_event", indices = [Index("name"), Index("ts")])
data class AnalyticsEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val params: String,
    val ts: Long,
    val sessionId: String,
)
