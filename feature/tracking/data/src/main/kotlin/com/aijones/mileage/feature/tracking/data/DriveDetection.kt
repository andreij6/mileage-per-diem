package com.aijones.mileage.feature.tracking.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.aijones.mileage.feature.tracking.domain.ObserveDriveDetectionEnabledUseCase
import com.aijones.mileage.feature.tracking.domain.RecordingState
import com.aijones.mileage.feature.tracking.domain.RecordingStateStore
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DriveDetectionRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observeEnabled: ObserveDriveDetectionEnabledUseCase,
) {
    suspend fun run() = observeEnabled().collect { enabled -> if (enabled && granted()) register() else unregister() }

    private fun granted(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACTIVITY_RECOGNITION else GMS_PERMISSION
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private suspend fun register() = quietly {
        val transition = ActivityTransition.Builder()
            .setActivityType(DetectedActivity.IN_VEHICLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build()
        ActivityRecognition.getClient(context).requestActivityTransitionUpdates(ActivityTransitionRequest(listOf(transition)), pendingIntent()).await()
    }

    @SuppressLint("MissingPermission")
    private suspend fun unregister() = quietly {
        if (granted()) ActivityRecognition.getClient(context).removeActivityTransitionUpdates(pendingIntent()).await()
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_TRANSITIONS,
        Intent(context, DriveDetectedReceiver::class.java).setAction(DriveDetectedReceiver.ACTION_TRANSITION),
        PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0,
    )

    private suspend fun quietly(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Unit
        }
    }

    private companion object {
        const val GMS_PERMISSION = "com.google.android.gms.permission.ACTIVITY_RECOGNITION"
        const val REQUEST_TRANSITIONS = 20
    }
}

@AndroidEntryPoint
class DriveDetectedReceiver : BroadcastReceiver() {
    @Inject lateinit var store: RecordingStateStore
    @Inject lateinit var notifications: TrackingNotifications

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DISMISS -> notifications.cancel(TrackingNotifications.ID_DRIVE)
            ACTION_TRANSITION -> if (isDriveStart(intent) && store.state.value is RecordingState.Idle) {
                notifications.notify(TrackingNotifications.ID_DRIVE, notifications.driveDetected())
            }
        }
    }

    private fun isDriveStart(intent: Intent): Boolean {
        if (!ActivityTransitionResult.hasResult(intent)) return false
        return ActivityTransitionResult.extractResult(intent)?.transitionEvents.orEmpty().any {
            it.activityType == DetectedActivity.IN_VEHICLE && it.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
        }
    }

    companion object {
        const val ACTION_TRANSITION = "com.aijones.mileage.tracking.DRIVE_TRANSITION"
        const val ACTION_DISMISS = "com.aijones.mileage.tracking.DRIVE_DISMISS"
    }
}
