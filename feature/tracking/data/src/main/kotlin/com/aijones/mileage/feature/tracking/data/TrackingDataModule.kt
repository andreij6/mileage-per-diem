package com.aijones.mileage.feature.tracking.data

import com.aijones.mileage.feature.tracking.domain.DeviceStateMonitor
import com.aijones.mileage.feature.tracking.domain.LocationTracker
import com.aijones.mileage.feature.tracking.domain.PlaceLabeler
import com.aijones.mileage.feature.tracking.domain.RecordingController
import com.aijones.mileage.feature.tracking.domain.RecordingStateStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface TrackingDataModule {
    @Binds fun locationTracker(impl: FusedLocationTracker): LocationTracker

    @Binds fun recordingController(impl: ServiceRecordingController): RecordingController

    @Binds fun recordingStateStore(impl: InMemoryRecordingStateStore): RecordingStateStore

    @Binds fun deviceStateMonitor(impl: AndroidDeviceStateMonitor): DeviceStateMonitor

    @Binds fun placeLabeler(impl: GeocoderPlaceLabeler): PlaceLabeler
}
