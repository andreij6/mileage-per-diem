package com.aijones.mileage.feature.tracking.domain

import com.aijones.mileage.feature.settings.domain.BatteryMode
import java.time.Duration

object RecordingPolicyResolver {
    val STATIONARY_AFTER: Duration = Duration.ofMinutes(2)
    const val LOW_BATTERY_PCT = 20

    fun resolve(device: DeviceState, mode: BatteryMode, stationaryFor: Duration): RecordingPolicy = when {
        mode == BatteryMode.ACCURATE -> RecordingPolicy.HIGH
        stationaryFor > STATIONARY_AFTER -> RecordingPolicy.BALANCED
        mode == BatteryMode.SAVER || device.powerSave || device.batteryPct <= LOW_BATTERY_PCT -> RecordingPolicy.SAVER
        else -> RecordingPolicy.HIGH
    }
}
