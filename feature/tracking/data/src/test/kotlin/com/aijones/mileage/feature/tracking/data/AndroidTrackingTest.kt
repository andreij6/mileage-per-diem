package com.aijones.mileage.feature.tracking.data

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Looper
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.feature.tracking.domain.DeviceState
import com.aijones.mileage.feature.tracking.domain.RecordingPolicy
import com.aijones.mileage.feature.tracking.domain.RecordingState
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidTrackingTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifications = TrackingNotifications(context)
    private val recording = RecordingState.Recording(
        1, Instant.EPOCH, Distance.ofMiles(12.6), Duration.ofMinutes(24).plusSeconds(18), 5f, 13.9f, RecordingPolicy.HIGH,
    )

    private val Notification.title get() = extras.getCharSequence(Notification.EXTRA_TITLE).toString()
    private val Notification.text get() = extras.getCharSequence(Notification.EXTRA_TEXT).toString()
    private val Notification.actionTitles get() = actions.map { it.title.toString() }

    @Test
    fun recordingNotificationShowsDistanceTimeAndControls() {
        val n = notifications.recording(recording, liveUpdate = false)
        assertEquals("Recording trip · 12.6 mi · 24 min", n.title)
        assertEquals("Location is used only while this trip records.", n.text)
        assertEquals(listOf("Pause", "Stop"), n.actionTitles)
        assertEquals(TrackingNotifications.CHANNEL_RECORDING, n.channelId)
        assertEquals(Notification.FLAG_ONGOING_EVENT, n.flags and Notification.FLAG_ONGOING_EVENT)
    }

    @Test
    fun pausedNotificationOffersResume() {
        val n = notifications.recording(RecordingState.Paused(1, Instant.EPOCH, Distance.ofMiles(8.2), Duration.ofMinutes(75)), liveUpdate = true)
        assertEquals("Trip paused · 8.2 mi · 1 h 15 min", n.title)
        assertEquals(listOf("Resume", "Stop"), n.actionTitles)
    }

    @Test
    fun driveDetectedNotificationMatchesDesignCopy() {
        val n = notifications.driveDetected()
        assertEquals("Drive detected", n.title)
        assertEquals("Looks like you're driving. Tap to log this as a business trip. Nothing records until you start it.", n.text)
        assertEquals(listOf("Start trip", "Not business"), n.actionTitles)
        assertEquals(TrackingNotifications.CHANNEL_DRIVE, n.channelId)
    }

    @Test
    fun channelsHaveExpectedImportance() {
        notifications.ensureChannels()
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        assertEquals(android.app.NotificationManager.IMPORTANCE_LOW, manager.getNotificationChannel("recording").importance)
        assertEquals(android.app.NotificationManager.IMPORTANCE_DEFAULT, manager.getNotificationChannel("drive").importance)
    }

    @Test
    fun deviceMonitorMapsBatteryAndPowerSave() = runTest {
        context.sendStickyBroadcast(battery(41, 50))
        val power = shadowOf(context.getSystemService(PowerManager::class.java))
        power.setIsPowerSaveMode(true)
        val states = mutableListOf<DeviceState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { AndroidDeviceStateMonitor(context).observe().toList(states) }
        shadowOf(Looper.getMainLooper()).idle()

        power.setIsPowerSaveMode(false)
        context.sendBroadcast(Intent(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        context.sendBroadcast(battery(15, 100))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(listOf(DeviceState(true, 82), DeviceState(false, 82), DeviceState(false, 15)), states)
        job.cancel()
    }

    @Test
    fun batteryPctHandlesMissingExtras() {
        assertEquals(100, batteryPct(Intent(Intent.ACTION_BATTERY_CHANGED)))
        assertEquals(50, batteryPct(battery(1, 2)))
    }

    private fun battery(level: Int, scale: Int) =
        Intent(Intent.ACTION_BATTERY_CHANGED).putExtra(BatteryManager.EXTRA_LEVEL, level).putExtra(BatteryManager.EXTRA_SCALE, scale)
}
