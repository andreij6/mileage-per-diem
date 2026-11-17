package com.aijones.mileage.feature.trips.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VehicleUseCasesTest {
    private val repo = FakeVehicleRepository()
    private val manage = ManageVehiclesUseCase(repo)

    @Test
    fun addRenameSetDefaultDelete() = runTest {
        manage(VehicleAction.Add(" Civic ", "car"))
        manage(VehicleAction.Add("Model 3", "ev"))
        assertEquals(listOf("Civic", "Model 3"), ObserveVehiclesUseCase(repo)().first().map { it.name })
        assertEquals(1L, repo.vehicles.single { it.isDefault }.id)

        manage(VehicleAction.Rename(2, "Tesla"))
        manage(VehicleAction.SetDefault(2))
        assertEquals(Vehicle(2, "Tesla", "ev", true), repo.vehicles.single { it.isDefault })

        manage(VehicleAction.Delete(1))
        assertEquals(listOf(2L), repo.vehicles.map { it.id })
    }

    @Test
    fun blankNamesAreRejected() = runTest {
        assertFailsWith<IllegalArgumentException> { manage(VehicleAction.Add("  ", "car")) }
        assertFailsWith<IllegalArgumentException> { manage(VehicleAction.Rename(1, "")) }
    }
}
