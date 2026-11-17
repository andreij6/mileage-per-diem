package com.aijones.mileage.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aijones.mileage.core.database.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Insert
    suspend fun insert(vehicle: VehicleEntity): Long

    @Update
    suspend fun update(vehicle: VehicleEntity)

    @Query("DELETE FROM vehicle WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM vehicle ORDER BY isDefault DESC, name")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("UPDATE vehicle SET isDefault = (id = :id)")
    suspend fun setDefault(id: Long)

    @Query("UPDATE vehicle SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("SELECT COUNT(*) FROM vehicle")
    suspend fun count(): Int
}
