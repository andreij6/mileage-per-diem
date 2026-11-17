package com.aijones.mileage.feature.trips.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.trips.domain.DeleteTripUseCase
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.FakeVehicleRepository
import com.aijones.mileage.feature.trips.domain.ObserveTripUseCase
import com.aijones.mileage.feature.trips.domain.ObserveVehiclesUseCase
import com.aijones.mileage.feature.trips.domain.TripCategory
import com.aijones.mileage.feature.trips.domain.UpdateTripDetailsUseCase
import com.aijones.mileage.feature.trips.domain.Vehicle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditTripViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val analytics = FakeAnalytics()
    private val repo = FakeTripRepository(listOf(trip(1, "2026-11-16T09:12")))
    private val vehicles = FakeVehicleRepository(listOf(Vehicle(1, "2021 Civic", "Personal", true), Vehicle(2, "Rental", "Rental", false)))

    private fun viewModel(id: Long = 1) = EditTripViewModel(
        id,
        ObserveTripUseCase(repo),
        ObserveVehiclesUseCase(vehicles),
        UpdateTripDetailsUseCase(repo, analytics),
        DeleteTripUseCase(repo, analytics),
        analytics,
    )

    @Test
    fun loadsTheTripIntoTheForm() = runTest {
        viewModel().state.test {
            assertEquals(
                EditTripUiState(
                    loading = false,
                    form = EditTripForm("Client meeting", TripCategory.BUSINESS, 1, "18.4 mi", ""),
                    vehicles = listOf(VehicleOption(1, "Personal · 2021 Civic"), VehicleOption(2, "Rental · Rental")),
                    recordedDistance = "18.4 mi",
                ),
                awaitItem(),
            )
        }
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("trip_edit")), analytics.events)
    }

    @Test
    fun countsChangesAndSavesThem() = runTest {
        val vm = viewModel()
        vm.state.test {
            awaitItem()
            vm.edit { it.copy(purpose = "Quarterly review ") }
            assertEquals(1, awaitItem().changes)
            vm.edit { it.copy(distance = "20.1") }
            awaitItem().let { s ->
                assertEquals(2, s.changes)
                assertEquals(true, s.canSave)
            }
            vm.edit { it.copy(vehicleId = 2, category = TripCategory.PERSONAL, notes = "Acme") }
            assertEquals(5, awaitItem().changes)
            vm.save()
            assertTrue(expectMostRecentItem().done)
        }
        val saved = repo.get(1)!!
        assertEquals("Quarterly review", saved.purpose)
        assertEquals(Distance.ofMiles(20.1).tenthsOfMile, saved.distance.tenthsOfMile)
        assertEquals(TripCategory.PERSONAL, saved.category)
        assertEquals(2L, saved.vehicleId)
        assertEquals("Acme", saved.notes)
        assertTrue(saved.edited)
        assertEquals(AnalyticsEvent.TripEdited(listOf("purpose", "category", "vehicle", "distance", "notes")), analytics.events.last())
    }

    @Test
    fun cannotSaveWithoutChangesOrWithAnInvalidDistance() = runTest {
        val vm = viewModel()
        vm.state.test {
            assertEquals(false, awaitItem().canSave)
            vm.edit { it.copy(distance = "") }
            assertEquals(false, awaitItem().canSave)
            vm.save()
            expectNoEvents()
        }
        assertEquals(1, analytics.events.size)
    }

    @Test
    fun deleteRemovesTheTripAndFinishes() = runTest {
        val vm = viewModel()
        vm.state.test {
            awaitItem()
            vm.delete()
            assertTrue(expectMostRecentItem().done)
        }
        assertNull(repo.get(1))
        assertEquals(AnalyticsEvent.TripDeleted, analytics.events.last())
    }
}
