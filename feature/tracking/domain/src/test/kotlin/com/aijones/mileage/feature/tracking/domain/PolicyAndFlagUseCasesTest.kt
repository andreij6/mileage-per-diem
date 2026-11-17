package com.aijones.mileage.feature.tracking.domain

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.featureflags.FakeFeatureFlags
import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.feature.settings.domain.BatteryMode
import com.aijones.mileage.feature.settings.domain.FakeSettingsRepository
import com.aijones.mileage.feature.settings.domain.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolicyAndFlagUseCasesTest {
    private val clock = FakeClock()
    private val device = FakeDeviceStateMonitor()
    private val settings = FakeSettingsRepository()
    private val store = FakeRecordingStateStore()
    private val analytics = FakeAnalytics()
    private val observePolicy = ObserveRecordingPolicyUseCase(device, settings, store, analytics, clock)

    private fun recording(speed: Float?) =
        RecordingState.Recording(1, clock.instant(), Distance.ZERO, Duration.ZERO, 5f, speed, RecordingPolicy.HIGH)

    @Test
    fun policyFollowsDeviceSettingsAndStillnessAndTracksChanges() = runTest {
        observePolicy().test {
            assertEquals(RecordingPolicy.HIGH, awaitItem())

            device.state.value = DeviceState(powerSave = false, batteryPct = 15)
            assertEquals(RecordingPolicy.SAVER, awaitItem())
            device.state.value = DeviceState(powerSave = false, batteryPct = 14)
            expectNoEvents()

            settings.update { it.copy(batteryMode = BatteryMode.ACCURATE) }
            assertEquals(RecordingPolicy.HIGH, awaitItem())
            settings.update { it.copy(batteryMode = BatteryMode.AUTO) }
            assertEquals(RecordingPolicy.SAVER, awaitItem())

            device.state.value = DeviceState(powerSave = false, batteryPct = 90)
            assertEquals(RecordingPolicy.HIGH, awaitItem())
            store.update { recording(0f) }
            clock.advance(Duration.ofMinutes(3))
            store.update { recording(0.1f) }
            assertEquals(RecordingPolicy.BALANCED, awaitItem())
            store.update { recording(12f) }
            assertEquals(RecordingPolicy.HIGH, awaitItem())
        }
        assertEquals(
            listOf("high", "saver", "high", "saver", "high", "balanced", "high"),
            analytics.eventsOf<AnalyticsEvent.LocationPolicyChanged>().map { it.policy },
        )
    }

    @Test
    fun briefStopKeepsHighAccuracy() = runTest {
        observePolicy().test {
            assertEquals(RecordingPolicy.HIGH, awaitItem())
            store.update { recording(0f) }
            clock.advance(Duration.ofSeconds(60))
            store.update { recording(0f) }
            expectNoEvents()
        }
    }

    @Test
    fun driveDetectionNeedsFlagAndSetting() = runTest {
        data class Case(val flag: Boolean, val setting: Boolean, val expected: Boolean)
        listOf(Case(true, true, true), Case(true, false, false), Case(false, true, false), Case(false, false, false)).forEach { c ->
            val useCase = ObserveDriveDetectionEnabledUseCase(
                FakeFeatureFlags(FeatureFlag.DRIVE_DETECTION to c.flag),
                FakeSettingsRepository(Settings(driveDetection = c.setting)),
            )
            assertEquals(c.expected, useCase().first(), c.toString())
        }
    }

    @Test
    fun liveUpdateFollowsFlag() = runTest {
        val flags = FakeFeatureFlags()
        ObserveLiveUpdateEnabledUseCase(flags)().test {
            assertFalse(awaitItem())
            flags.setOverride(FeatureFlag.LIVE_UPDATE, true)
            assertTrue(awaitItem())
        }
    }
}
