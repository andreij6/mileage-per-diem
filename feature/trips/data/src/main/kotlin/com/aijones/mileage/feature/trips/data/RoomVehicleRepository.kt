package com.aijones.mileage.feature.trips.data

import com.aijones.mileage.core.common.IoDispatcher
import com.aijones.mileage.core.database.dao.VehicleDao
import com.aijones.mileage.core.database.entity.VehicleEntity
import com.aijones.mileage.feature.trips.domain.Vehicle
import com.aijones.mileage.feature.trips.domain.VehicleRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class RoomVehicleRepository @Inject constructor(
    private val vehicles: VehicleDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : VehicleRepository {

    override fun observeAll(): Flow<List<Vehicle>> = vehicles.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(io)

    override suspend fun add(name: String, kind: String): Long =
        vehicles.insert(VehicleEntity(name = name, kind = kind, isDefault = vehicles.count() == 0))

    override suspend fun rename(id: Long, name: String) = vehicles.rename(id, name)

    override suspend fun delete(id: Long) = vehicles.delete(id)

    override suspend fun setDefault(id: Long) = vehicles.setDefault(id)
}
