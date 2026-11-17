package com.aijones.mileage.feature.trips.data

import com.aijones.mileage.feature.trips.domain.MileageRateRepository
import com.aijones.mileage.feature.trips.domain.TripRepository
import com.aijones.mileage.feature.trips.domain.VehicleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface TripsDataModule {
    @Binds fun trips(impl: RoomTripRepository): TripRepository

    @Binds fun vehicles(impl: RoomVehicleRepository): VehicleRepository

    @Binds fun rates(impl: BundledMileageRateRepository): MileageRateRepository
}
