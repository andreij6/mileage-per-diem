package com.aijones.mileage.feature.tracking.domain

import com.aijones.mileage.feature.settings.domain.BatteryMode
import org.junit.Test
import java.time.Duration
import kotlin.test.assertEquals

class RecordingPolicyResolverTest {
    @Test
    fun table() {
        data class Case(val device: DeviceState, val mode: BatteryMode, val still: Duration, val expected: RecordingPolicy)
        val ok = DeviceState(powerSave = false, batteryPct = 80)
        listOf(
            Case(ok, BatteryMode.AUTO, Duration.ZERO, RecordingPolicy.HIGH),
            Case(DeviceState(false, 21), BatteryMode.AUTO, Duration.ZERO, RecordingPolicy.HIGH),
            Case(DeviceState(false, 20), BatteryMode.AUTO, Duration.ZERO, RecordingPolicy.SAVER),
            Case(DeviceState(true, 90), BatteryMode.AUTO, Duration.ZERO, RecordingPolicy.SAVER),
            Case(ok, BatteryMode.SAVER, Duration.ZERO, RecordingPolicy.SAVER),
            Case(ok, BatteryMode.AUTO, Duration.ofMinutes(2), RecordingPolicy.HIGH),
            Case(ok, BatteryMode.AUTO, Duration.ofSeconds(121), RecordingPolicy.BALANCED),
            Case(DeviceState(true, 10), BatteryMode.SAVER, Duration.ofMinutes(5), RecordingPolicy.BALANCED),
            Case(DeviceState(true, 10), BatteryMode.ACCURATE, Duration.ofMinutes(5), RecordingPolicy.HIGH),
        ).forEach { c -> assertEquals(c.expected, RecordingPolicyResolver.resolve(c.device, c.mode, c.still), c.toString()) }
    }

    @Test
    fun policyParameters() {
        assertEquals(RecordingPolicy("high", true, 2_000, 5f), RecordingPolicy.HIGH)
        assertEquals(RecordingPolicy("balanced", false, 15_000, 25f), RecordingPolicy.BALANCED)
        assertEquals(RecordingPolicy("saver", false, 10_000, 20f), RecordingPolicy.SAVER)
    }
}
