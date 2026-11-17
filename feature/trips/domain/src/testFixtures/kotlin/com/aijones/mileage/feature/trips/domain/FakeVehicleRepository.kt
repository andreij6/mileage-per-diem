package com.aijones.mileage.feature.trips.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeVehicleRepository(initial: List<Vehicle> = emptyList()) : VehicleRepository {
    private val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    val vehicles: List<Vehicle> get() = state.value

    override fun observeAll(): Flow<List<Vehicle>> = state

    override suspend fun add(name: String, kind: String): Long {
        val id = nextId++
        state.update { it + Vehicle(id, name, kind, isDefault = it.isEmpty()) }
        return id
    }

    override suspend fun rename(id: Long, name: String) {
        state.update { list -> list.map { if (it.id == id) it.copy(name = name) else it } }
    }

    override suspend fun delete(id: Long) {
        state.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun setDefault(id: Long) {
        state.update { list -> list.map { it.copy(isDefault = it.id == id) } }
    }
}
