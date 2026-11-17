package com.aijones.mileage.feature.settings.domain

import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.featureflags.FeatureFlags
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.feature.trips.domain.ObserveTripsUseCase
import com.aijones.mileage.feature.trips.domain.ObserveVehiclesUseCase
import com.aijones.mileage.feature.trips.domain.Vehicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class ObserveFeatureFlagUseCase @Inject constructor(private val flags: FeatureFlags) {
    operator fun invoke(flag: FeatureFlag): Flow<Boolean> = flags.observe(flag)
}

data class VehicleUsage(val vehicle: Vehicle, val thisMonth: Distance)

class ObserveVehicleUsageUseCase @Inject constructor(
    private val observeVehicles: ObserveVehiclesUseCase,
    private val observeTrips: ObserveTripsUseCase,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<VehicleUsage>> {
        val today = LocalDate.now(clock)
        return combine(observeVehicles(), observeTrips(today.withDayOfMonth(1), today)) { vehicles, trips ->
            vehicles.map { v ->
                VehicleUsage(v, trips.filter { it.vehicleId == v.id && it.isFinished }.fold(Distance.ZERO) { acc, t -> acc + t.distance })
            }
        }
    }
}
