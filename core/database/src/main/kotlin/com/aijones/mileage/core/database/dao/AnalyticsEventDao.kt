package com.aijones.mileage.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.aijones.mileage.core.database.entity.AnalyticsEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsEventDao {
    @Insert
    suspend fun insertAll(events: List<AnalyticsEventEntity>)

    @Query("DELETE FROM analytics_event WHERE id <= (SELECT MAX(id) FROM analytics_event) - :keep")
    suspend fun trimTo(keep: Int)

    @Transaction
    suspend fun append(events: List<AnalyticsEventEntity>, keep: Int) {
        insertAll(events)
        trimTo(keep)
    }

    @Query("SELECT * FROM analytics_event ORDER BY id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AnalyticsEventEntity>>

    @Query("SELECT * FROM analytics_event WHERE ts >= :sinceMs ORDER BY id")
    fun observeSince(sinceMs: Long): Flow<List<AnalyticsEventEntity>>

    @Query("SELECT * FROM analytics_event ORDER BY id")
    suspend fun all(): List<AnalyticsEventEntity>
}
