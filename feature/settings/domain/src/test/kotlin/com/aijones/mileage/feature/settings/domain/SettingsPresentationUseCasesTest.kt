package com.aijones.mileage.feature.settings.domain

import app.cash.turbine.test
import com.aijones.mileage.core.featureflags.FakeFeatureFlags
import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.FakeVehicleRepository
import com.aijones.mileage.feature.trips.domain.ObserveTripsUseCase
import com.aijones.mileage.feature.trips.domain.ObserveVehiclesUseCase
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripStatus
import com.aijones.mileage.feature.trips.domain.Vehicle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsPresentationUseCasesTest {
    @Test
    fun observedFlagFollowsOverrides() = runTest {
        val flags = FakeFeatureFlags()
        ObserveFeatureFlagUseCase(flags)(FeatureFlag.DRIVE_DETECTION).test {
            assertTrue(awaitItem())
            flags.setOverride(FeatureFlag.DRIVE_DETECTION, false)
            assertFalse(awaitItem())
        }
    }

    @Test
    fun vehicleUsageSumsFinishedTripsThisMonthPerVehicle() = runTest {
        val clock = FakeClock()
        val trip = { vehicleId: Long, miles: Double, at: String, status: TripStatus ->
            Trip(startedAt = Instant.parse(at), status = status, distance = Distance.ofMiles(miles), vehicleId = vehicleId)
        }
        val trips = FakeTripRepository(
            listOf(
                trip(1, 10.0, "2026-11-03T14:00:00Z", TripStatus.DONE),
                trip(1, 2.5, "2026-11-10T14:00:00Z", TripStatus.MANUAL),
                trip(1, 99.0, "2026-10-30T14:00:00Z", TripStatus.DONE),
                trip(2, 4.0, "2026-11-12T14:00:00Z", TripStatus.DONE),
            ).mapIndexed { i, t -> t.copy(id = i + 1L) },
        )
        val vehicles = FakeVehicleRepository(listOf(Vehicle(1, "2021 Civic", "Personal", true), Vehicle(2, "Rental", "", false), Vehicle(3, "Van", "", false)))
        val usage = ObserveVehicleUsageUseCase(ObserveVehiclesUseCase(vehicles), ObserveTripsUseCase(trips, clock), clock)().first()
        assertEquals(listOf(125L, 40L, 0L), usage.map { it.thisMonth.tenthsOfMile })
    }
}
