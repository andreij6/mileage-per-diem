package com.aijones.mileage.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aijones.mileage.core.database.dao.AnalyticsEventDao
import com.aijones.mileage.core.database.dao.StayDao
import com.aijones.mileage.core.database.dao.TrackPointDao
import com.aijones.mileage.core.database.dao.TripDao
import com.aijones.mileage.core.database.dao.VehicleDao
import com.aijones.mileage.core.database.entity.AnalyticsEventEntity
import com.aijones.mileage.core.database.entity.StayEntity
import com.aijones.mileage.core.database.entity.TrackPointEntity
import com.aijones.mileage.core.database.entity.TripEntity
import com.aijones.mileage.core.database.entity.VehicleEntity

@Database(
    entities = [TripEntity::class, TrackPointEntity::class, StayEntity::class, VehicleEntity::class, AnalyticsEventEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MileageDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun stayDao(): StayDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun analyticsEventDao(): AnalyticsEventDao
}
