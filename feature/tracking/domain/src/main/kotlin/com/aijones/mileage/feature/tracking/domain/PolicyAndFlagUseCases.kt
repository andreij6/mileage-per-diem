package com.aijones.mileage.feature.tracking.domain

import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.featureflags.FeatureFlags
import com.aijones.mileage.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class ObserveRecordingPolicyUseCase @Inject constructor(
    private val device: DeviceStateMonitor,
    private val settings: SettingsRepository,
    private val store: RecordingStateStore,
    private val analytics: Analytics,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<RecordingPolicy> =
        combine(device.observe(), settings.observe().map { it.batteryMode }.distinctUntilChanged(), stationaryFor()) { d, mode, still ->
            RecordingPolicyResolver.resolve(d, mode, still)
        }
            .distinctUntilChanged()
            .onEach { analytics.track(AnalyticsEvent.LocationPolicyChanged(it.name)) }

    private fun stationaryFor(): Flow<Duration> =
        store.state
            .scan(null as Instant?) { since, state ->
                val still = state is RecordingState.Recording && state.speedMps != null && state.speedMps < STILL_MPS
                if (still) since ?: clock.instant() else null
            }
            .map { since -> since?.let { Duration.between(it, clock.instant()) } ?: Duration.ZERO }

    private companion object {
        const val STILL_MPS = 0.5f
    }
}

class ObserveDriveDetectionEnabledUseCase @Inject constructor(private val flags: FeatureFlags, private val settings: SettingsRepository) {
    operator fun invoke(): Flow<Boolean> =
        combine(flags.observe(FeatureFlag.DRIVE_DETECTION), settings.observe().map { it.driveDetection }) { flag, setting -> flag && setting }
            .distinctUntilChanged()
}

class ObserveLiveUpdateEnabledUseCase @Inject constructor(private val flags: FeatureFlags) {
    operator fun invoke(): Flow<Boolean> = flags.observe(FeatureFlag.LIVE_UPDATE)
}
