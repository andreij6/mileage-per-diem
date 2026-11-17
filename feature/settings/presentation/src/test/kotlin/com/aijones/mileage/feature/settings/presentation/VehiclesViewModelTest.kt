package com.aijones.mileage.feature.settings.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.settings.domain.ObserveVehicleUsageUseCase
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.FakeVehicleRepository
import com.aijones.mileage.feature.trips.domain.ManageVehiclesUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTripsUseCase
import com.aijones.mileage.feature.trips.domain.ObserveVehiclesUseCase
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripStatus
import com.aijones.mileage.feature.trips.domain.Vehicle
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class VehiclesViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val clock = FakeClock()
    private val vehicles = FakeVehicleRepository(listOf(Vehicle(1, "2021 Civic", "Personal", true), Vehicle(2, "Rental", "", false)))
    private val trips = FakeTripRepository(
        listOf(
            Trip(1, Instant.parse("2026-11-03T14:00:00Z"), status = TripStatus.DONE, distance = Distance.ofMiles(612.0), vehicleId = 1),
            Trip(2, Instant.parse("2026-11-04T14:00:00Z"), status = TripStatus.DONE, distance = Distance.ofMiles(233.0), vehicleId = 2),
        ),
    )
    private val analytics = FakeAnalytics()
    private val vm = VehiclesViewModel(
        ObserveVehicleUsageUseCase(ObserveVehiclesUseCase(vehicles), ObserveTripsUseCase(trips, clock), clock),
        ManageVehiclesUseCase(vehicles),
        analytics,
    )
    private val civic = VehicleItem(1, "2021 Civic", "Personal", "612", true)
    private val rental = VehicleItem(2, "Rental", "", "233", false)

    @Test
    fun `lists vehicles with this month's miles and tracks the screen view`() = runTest {
        vm.state.test {
            assertEquals(VehiclesUiState(), awaitItem())
            assertEquals(VehiclesUiState(listOf(civic, rental)), awaitItem())
        }
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("vehicles")), analytics.events)
    }

    @Test
    fun `adding a vehicle closes the dialog and lists it`() = runTest {
        vm.state.test {
            skipItems(2)
            vm.showAdd()
            assertEquals(VehiclesUiState(listOf(civic, rental), VehicleDialog.Add), awaitItem())
            vm.add("Van", "Company")
            assertEquals(VehiclesUiState(listOf(civic, rental)), awaitItem())
            assertEquals(VehiclesUiState(listOf(civic, rental, VehicleItem(3, "Van", "Company", "0", false))), awaitItem())
        }
    }

    @Test
    fun `a blank name is not saved`() = runTest {
        vm.state.test {
            skipItems(2)
            vm.showAdd()
            awaitItem()
            vm.add("  ", "Personal")
            expectNoEvents()
        }
        assertEquals(2, vehicles.vehicles.size)
    }

    @Test
    fun `rename, set default and delete update the list`() = runTest {
        vm.state.test {
            skipItems(2)
            vm.showRename(2, "Rental")
            assertEquals(VehicleDialog.Rename(2, "Rental"), awaitItem().dialog)
            vm.rename(2, "Rental Camry")
            assertEquals(VehiclesUiState(listOf(civic, rental)), awaitItem())
            assertEquals(VehiclesUiState(listOf(civic, rental.copy(name = "Rental Camry"))), awaitItem())
            vm.setDefault(2)
            assertEquals(
                listOf(rental.copy(name = "Rental Camry", isDefault = true), civic.copy(isDefault = false)).sortedBy { it.id },
                awaitItem().vehicles,
            )
            vm.delete(1)
            assertEquals(listOf(rental.copy(name = "Rental Camry", isDefault = true)), awaitItem().vehicles)
        }
    }
}
