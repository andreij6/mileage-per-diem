package com.aijones.mileage.feature.trips.domain

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveVehiclesUseCase @Inject constructor(private val vehicles: VehicleRepository) {
    operator fun invoke(): Flow<List<Vehicle>> = vehicles.observeAll()
}

sealed interface VehicleAction {
    data class Add(val name: String, val kind: String) : VehicleAction
    data class Rename(val id: Long, val name: String) : VehicleAction
    data class Delete(val id: Long) : VehicleAction
    data class SetDefault(val id: Long) : VehicleAction
}

class ManageVehiclesUseCase @Inject constructor(private val vehicles: VehicleRepository) {
    suspend operator fun invoke(action: VehicleAction) {
        when (action) {
            is VehicleAction.Add -> vehicles.add(action.name.trim().also { require(it.isNotEmpty()) }, action.kind)
            is VehicleAction.Rename -> vehicles.rename(action.id, action.name.trim().also { require(it.isNotEmpty()) })
            is VehicleAction.Delete -> vehicles.delete(action.id)
            is VehicleAction.SetDefault -> vehicles.setDefault(action.id)
        }
    }
}
