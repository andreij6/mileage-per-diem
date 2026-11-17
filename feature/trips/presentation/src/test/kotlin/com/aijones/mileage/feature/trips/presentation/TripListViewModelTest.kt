package com.aijones.mileage.feature.trips.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.FakeVehicleRepository
import com.aijones.mileage.feature.trips.domain.ObserveTripsUseCase
import com.aijones.mileage.feature.trips.domain.ObserveVehiclesUseCase
import com.aijones.mileage.feature.trips.domain.TripCategory
import com.aijones.mileage.feature.trips.domain.Vehicle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class TripListViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val analytics = FakeAnalytics()
    private val clock = FakeClock()
    private val repo = FakeTripRepository(
        listOf(
            trip(1, "2026-11-16T09:12"),
            trip(2, "2026-11-16T15:02", miles = 19.1, from = "Acme HQ", to = "Office"),
            trip(3, "2026-11-15T06:40", miles = 16.2, to = "JFK Airport", purpose = "Travel", vehicleId = 2),
            trip(4, "2026-11-13T11:05", miles = 24.8, from = "Home", to = "Newark Supply", category = TripCategory.PERSONAL),
            trip(5, "2026-10-30T11:05", miles = 50.0),
        ),
    )
    private val vehicles = FakeVehicleRepository(listOf(Vehicle(1, "2021 Civic", "Personal", true), Vehicle(2, "Rental", "Rental", false)))

    private fun viewModel() = TripListViewModel(ObserveTripsUseCase(repo, clock), ObserveVehiclesUseCase(vehicles), analytics, clock)

    @Test
    fun groupsThisMonthsTripsByDayWithTotals() = runTest {
        viewModel().state.test {
            val s = awaitItem()
            assertEquals("November 2026", s.month)
            assertEquals(4, s.count)
            assertEquals("78.5 mi", s.distance)
            assertEquals("$40.81", s.total)
            assertEquals(listOf(DayLabel.Today, DayLabel.Yesterday, DayLabel.On("Fri, Nov 13")), s.groups.map { it.day })
            assertEquals(listOf(listOf(2L, 1L), listOf(3L), listOf(4L)), s.groups.map { g -> g.trips.map { it.id } })
        }
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("all_trips")), analytics.events)
    }

    @Test
    fun filtersByBusinessVehicleAndSearch() = runTest {
        val vm = viewModel()
        vm.state.test {
            awaitItem()
            vm.toggleBusinessOnly()
            assertEquals(listOf(2L, 1L, 3L), awaitItem().groups.flatMap { g -> g.trips.map { it.id } })
            vm.cycleVehicle()
            awaitItem().let { s ->
                assertEquals("2021 Civic", s.vehicle)
                assertEquals(listOf(2L, 1L), s.groups.flatMap { g -> g.trips.map { it.id } })
            }
            vm.cycleVehicle()
            assertEquals("Rental", awaitItem().vehicle)
            vm.cycleVehicle()
            assertEquals(null, awaitItem().vehicle)
            vm.toggleBusinessOnly()
            awaitItem()
            vm.toggleSearch()
            assertEquals(true, awaitItem().searching)
            vm.search("newark")
            assertEquals(listOf(4L), awaitItem().groups.flatMap { g -> g.trips.map { it.id } })
            vm.toggleSearch()
            awaitItem().let { s ->
                assertEquals(false, s.searching)
                assertEquals("", s.query)
                assertEquals(4, s.count)
            }
        }
    }
}
