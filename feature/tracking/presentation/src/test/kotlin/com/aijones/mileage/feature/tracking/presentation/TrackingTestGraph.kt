package com.aijones.mileage.feature.tracking.presentation

import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.navigation.LiveTripRoute
import com.aijones.mileage.core.navigation.TripReceiptRoute
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.feature.settings.domain.FakeSettingsRepository
import com.aijones.mileage.feature.settings.domain.ObserveMileageRateSettingsUseCase
import com.aijones.mileage.feature.settings.domain.Settings
import com.aijones.mileage.feature.tracking.domain.FakeDeviceStateMonitor
import com.aijones.mileage.feature.tracking.domain.FakePlaceLabeler
import com.aijones.mileage.feature.tracking.domain.FakeRecordingController
import com.aijones.mileage.feature.tracking.domain.FakeRecordingStateStore
import com.aijones.mileage.feature.tracking.domain.FinalizeTripUseCase
import com.aijones.mileage.feature.tracking.domain.ObserveDeviceStateUseCase
import com.aijones.mileage.feature.tracking.domain.ObserveRecordingStateUseCase
import com.aijones.mileage.feature.tracking.domain.PauseTripUseCase
import com.aijones.mileage.feature.tracking.domain.RecordFailureUseCase
import com.aijones.mileage.feature.tracking.domain.RecordTrackingPermissionUseCase
import com.aijones.mileage.feature.tracking.domain.ResumeInterruptedTripUseCase
import com.aijones.mileage.feature.tracking.domain.ResumeTripUseCase
import com.aijones.mileage.feature.tracking.domain.StartTripUseCase
import com.aijones.mileage.feature.tracking.domain.StopTripUseCase
import com.aijones.mileage.feature.trips.domain.DeleteTripUseCase
import com.aijones.mileage.feature.trips.domain.FakeMileageRateRepository
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.GetMileageRateUseCase
import com.aijones.mileage.feature.trips.domain.ObserveCurrentRateUseCase
import com.aijones.mileage.feature.trips.domain.ObserveInterruptedTripUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTrackUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTripUseCase

class TrackingTestGraph {
    val clock = FakeClock()
    val trips = FakeTripRepository()
    val settings = FakeSettingsRepository(Settings(defaultPurpose = "Client meeting"))
    val store = FakeRecordingStateStore()
    val controller = FakeRecordingController()
    val analytics = FakeAnalytics()
    val rates = FakeMileageRateRepository()
    val device = FakeDeviceStateMonitor()

    private val stateUseCase get() = ObserveRecordingStateUseCase(store)
    private val failure get() = RecordFailureUseCase(trips, store, analytics)
    private val currentRate get() = ObserveCurrentRateUseCase(rates, clock)

    fun finalize() = FinalizeTripUseCase(trips, GetMileageRateUseCase(rates), FakePlaceLabeler(), analytics, clock)

    fun liveTrip(source: String?) = LiveTripViewModel(
        LiveTripRoute(source),
        stateUseCase,
        ObserveTripUseCase(trips),
        ObserveTrackUseCase(trips),
        currentRate,
        ObserveDeviceStateUseCase(device),
        StartTripUseCase(trips, settings, store, controller, analytics, clock),
        PauseTripUseCase(trips, store, controller),
        ResumeTripUseCase(trips, store, controller),
        StopTripUseCase(trips, store, controller),
        failure,
        RecordTrackingPermissionUseCase(analytics),
        analytics,
    )

    fun receipt(tripId: Long) = TripReceiptViewModel(
        TripReceiptRoute(tripId),
        ObserveTripUseCase(trips),
        ObserveTrackUseCase(trips),
        ObserveMileageRateSettingsUseCase(rates, currentRate),
        clock,
        analytics,
    )

    fun interrupted() = InterruptedTripViewModel(
        stateUseCase,
        ObserveInterruptedTripUseCase(trips),
        ObserveTrackUseCase(trips),
        ResumeInterruptedTripUseCase(trips, store, controller),
        finalize(),
        DeleteTripUseCase(trips, analytics),
        failure,
        clock,
    )
}
