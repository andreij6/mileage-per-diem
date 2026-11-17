package com.aijones.mileage.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.aijones.mileage.core.database.entity.TrackPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {
    @Insert
    suspend fun insertAll(points: List<TrackPointEntity>)

    @Query("SELECT * FROM track_point WHERE tripId = :tripId ORDER BY t")
    suspend fun forTrip(tripId: Long): List<TrackPointEntity>

    @Query("SELECT * FROM track_point WHERE tripId = :tripId ORDER BY t")
    fun observeForTrip(tripId: Long): Flow<List<TrackPointEntity>>

    @Query("DELETE FROM track_point WHERE tripId = :tripId")
    suspend fun deleteForTrip(tripId: Long)
}
