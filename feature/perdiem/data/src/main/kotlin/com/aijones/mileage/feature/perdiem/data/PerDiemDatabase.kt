package com.aijones.mileage.feature.perdiem.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "location")
data class LocationEntity(
    @PrimaryKey val id: Long,
    val fy: Int,
    val city: String,
    val state: String,
    val county: String?,
    val isStandard: Boolean,
    val mieTotalCents: Long,
)

@Entity(tableName = "lodging", primaryKeys = ["locationId", "month"])
data class LodgingEntity(val locationId: Long, val month: Int, val cents: Long)

@Entity(tableName = "mie", primaryKeys = ["fy", "totalCents"])
data class MieEntity(
    val fy: Int,
    val totalCents: Long,
    val breakfastCents: Long,
    val lunchCents: Long,
    val dinnerCents: Long,
    val incidentalsCents: Long,
    val firstLastCents: Long,
)

@Fts4(contentEntity = LocationEntity::class)
@Entity(tableName = "location_fts")
data class LocationFtsEntity(val city: String, val state: String, val county: String?)

@Dao
interface PerDiemDao {
    @Query(
        "SELECT location.* FROM location JOIN location_fts ON location.id = location_fts.rowid " +
            "WHERE location_fts MATCH :match AND location.fy = :fy AND location.isStandard = 0 " +
            "ORDER BY location.city, location.state LIMIT :limit",
    )
    suspend fun search(match: String, fy: Int, limit: Int): List<LocationEntity>

    @Query("SELECT * FROM location WHERE id = :id")
    suspend fun location(id: Long): LocationEntity?

    @Query("SELECT * FROM location WHERE id IN (:ids)")
    suspend fun locations(ids: List<Long>): List<LocationEntity>

    @Query("SELECT * FROM location WHERE isStandard = 1 AND fy = :fy LIMIT 1")
    suspend fun standard(fy: Int): LocationEntity?

    @Query("SELECT * FROM location WHERE isStandard = 1 ORDER BY fy DESC LIMIT 1")
    suspend fun latestStandard(): LocationEntity?

    @Query("SELECT * FROM lodging WHERE locationId = :locationId")
    suspend fun lodging(locationId: Long): List<LodgingEntity>

    @Query("SELECT * FROM mie WHERE fy = :fy AND totalCents = :totalCents")
    suspend fun mie(fy: Int, totalCents: Long): MieEntity?
}

@Database(
    entities = [LocationEntity::class, LodgingEntity::class, MieEntity::class, LocationFtsEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PerDiemDatabase : RoomDatabase() {
    abstract fun dao(): PerDiemDao
}
