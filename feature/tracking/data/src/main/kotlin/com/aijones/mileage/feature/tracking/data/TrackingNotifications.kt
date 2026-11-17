package com.aijones.mileage.feature.tracking.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.formatMiles
import com.aijones.mileage.core.navigation.Deeplinks
import com.aijones.mileage.feature.tracking.domain.RecordingState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingNotifications @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager: NotificationManager? get() = context.getSystemService(NotificationManager::class.java)

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val res = context.resources
        manager?.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_RECORDING, res.getString(R.string.tracking_channel_recording), NotificationManager.IMPORTANCE_LOW)
                    .apply { description = res.getString(R.string.tracking_channel_recording_description); setShowBadge(false) },
                NotificationChannel(CHANNEL_DRIVE, res.getString(R.string.tracking_channel_drive), NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = res.getString(R.string.tracking_channel_drive_description) },
            ),
        )
    }

    fun recording(state: RecordingState, liveUpdate: Boolean): Notification {
        ensureChannels()
        val res = context.resources
        val paused = state is RecordingState.Paused
        val builder = NotificationCompat.Builder(context, CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_stat_recording)
            .setContentTitle(recordingTitle(res, state))
            .setContentText(res.getString(if (paused) R.string.tracking_paused_text else R.string.tracking_recording_text))
            .setContentIntent(activity(Deeplinks.ACTION_OPEN_LIVE_TRIP, REQUEST_OPEN))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                0,
                res.getString(if (paused) R.string.tracking_action_resume else R.string.tracking_action_pause),
                service(if (paused) TripRecorderService.ACTION_USER_RESUME else TripRecorderService.ACTION_USER_PAUSE),
            )
            .addAction(0, res.getString(R.string.tracking_action_stop), service(TripRecorderService.ACTION_USER_STOP))
        if (liveUpdate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            runCatching {
                builder.setRequestPromotedOngoing(true).setShortCriticalText(state.distanceOrNull()?.formatMiles())
            }
        }
        return builder.build()
    }

    fun driveDetected(): Notification {
        ensureChannels()
        val res = context.resources
        val start = activity(Deeplinks.ACTION_START_TRIP, REQUEST_START) { putExtra(Deeplinks.EXTRA_SOURCE, SOURCE_NOTIFICATION) }
        val dismiss = PendingIntent.getBroadcast(
            context,
            REQUEST_DISMISS,
            Intent(context, DriveDetectedReceiver::class.java).setAction(DriveDetectedReceiver.ACTION_DISMISS),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_DRIVE)
            .setSmallIcon(R.drawable.ic_stat_drive)
            .setContentTitle(res.getString(R.string.tracking_drive_title))
            .setContentText(res.getString(R.string.tracking_drive_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(res.getString(R.string.tracking_drive_text)))
            .setContentIntent(start)
            .setAutoCancel(true)
            .addAction(0, res.getString(R.string.tracking_action_start_trip), start)
            .addAction(0, res.getString(R.string.tracking_action_not_business), dismiss)
            .build()
    }

    fun recordingStopped(): Notification {
        ensureChannels()
        val res = context.resources
        return NotificationCompat.Builder(context, CHANNEL_DRIVE)
            .setSmallIcon(R.drawable.ic_stat_recording)
            .setContentTitle(res.getString(R.string.tracking_stopped_title))
            .setContentText(res.getString(R.string.tracking_stopped_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(res.getString(R.string.tracking_stopped_text)))
            .setAutoCancel(true)
            .build()
    }

    fun notify(id: Int, notification: Notification) {
        runCatching { manager?.notify(id, notification) }
    }

    fun cancel(id: Int) {
        manager?.cancel(id)
    }

    private fun activity(action: String, request: Int, extras: Intent.() -> Unit = {}): PendingIntent = PendingIntent.getActivity(
        context,
        request,
        Intent(action).setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).apply(extras),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun service(action: String): PendingIntent = PendingIntent.getService(
        context,
        action.hashCode(),
        Intent(context, TripRecorderService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val CHANNEL_RECORDING = "recording"
        const val CHANNEL_DRIVE = "drive"
        const val ID_RECORDING = 1
        const val ID_DRIVE = 2
        const val ID_STOPPED = 3
        private const val REQUEST_OPEN = 10
        private const val REQUEST_START = 11
        private const val REQUEST_DISMISS = 12
        private const val SOURCE_NOTIFICATION = "notification"
    }
}

internal fun RecordingState.distanceOrNull() = when (this) {
    is RecordingState.Recording -> distance
    is RecordingState.Paused -> distance
    RecordingState.Idle -> null
}

internal fun RecordingState.elapsedOrZero(): Duration = when (this) {
    is RecordingState.Recording -> elapsed
    is RecordingState.Paused -> elapsed
    RecordingState.Idle -> Duration.ZERO
}

internal fun recordingTitle(res: Resources, state: RecordingState): String {
    val minutes = state.elapsedOrZero().toMinutes()
    val time = if (minutes < 60) {
        res.getString(R.string.tracking_minutes, minutes.toInt())
    } else {
        res.getString(R.string.tracking_hours_minutes, (minutes / 60).toInt(), (minutes % 60).toInt())
    }
    val title = if (state is RecordingState.Paused) R.string.tracking_paused_title else R.string.tracking_recording_title
    return res.getString(title, (state.distanceOrNull() ?: Distance.ZERO).formatMiles(), time)
}
