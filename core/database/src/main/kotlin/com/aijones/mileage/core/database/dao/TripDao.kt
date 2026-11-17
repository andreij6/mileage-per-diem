package com.aijones.mileage.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aijones.mileage.core.database.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: TripEntity): Long

    @Update
    suspend fun update(trip: TripEntity)

    @Query("DELETE FROM trip WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM trip")
    suspend fun deleteAll()

    @Query("SELECT * FROM trip WHERE id = :id")
    suspend fun get(id: Long): TripEntity?

    @Query("SELECT * FROM trip WHERE id = :id")
    fun observe(id: Long): Flow<TripEntity?>

    @Query("SELECT * FROM trip WHERE status IN ('DONE','MANUAL') AND startedAt >= :fromMs AND startedAt < :toMs ORDER BY startedAt DESC")
    fun observeBetween(fromMs: Long, toMs: Long): Flow<List<TripEntity>>

    @Query("SELECT * FROM trip WHERE status IN ('DONE','MANUAL') ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TripEntity>>

    @Query("SELECT * FROM trip WHERE status IN ('RECORDING','PAUSED') LIMIT 1")
    suspend fun active(): TripEntity?

    @Query("SELECT * FROM trip WHERE status IN ('RECORDING','PAUSED') LIMIT 1")
    fun observeActive(): Flow<TripEntity?>

    @Query("SELECT * FROM trip WHERE needsReview = 1 AND status IN ('DONE','MANUAL') ORDER BY startedAt DESC")
    fun observeNeedingReview(): Flow<List<TripEntity>>
}
