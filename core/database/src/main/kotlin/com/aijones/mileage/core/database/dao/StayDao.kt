package com.aijones.mileage.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.aijones.mileage.core.database.entity.StayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StayDao {
    @Insert
    suspend fun insert(stay: StayEntity): Long

    @Query("DELETE FROM stay WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM stay")
    suspend fun deleteAll()

    @Query("SELECT * FROM stay WHERE checkOutEpochDay >= :fromEpochDay AND checkInEpochDay <= :toEpochDay ORDER BY checkInEpochDay")
    fun observeOverlapping(fromEpochDay: Long, toEpochDay: Long): Flow<List<StayEntity>>
}
