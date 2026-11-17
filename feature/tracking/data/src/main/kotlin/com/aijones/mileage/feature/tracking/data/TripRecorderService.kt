package com.aijones.mileage.feature.tracking.data

import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.aijones.mileage.core.analytics.FailureReason
import com.aijones.mileage.core.common.ApplicationScope
import com.aijones.mileage.feature.tracking.domain.ObserveLiveUpdateEnabledUseCase
import com.aijones.mileage.feature.tracking.domain.PauseTripUseCase
import com.aijones.mileage.feature.tracking.domain.RecordFailureUseCase
import com.aijones.mileage.feature.tracking.domain.RecordingState
import com.aijones.mileage.feature.tracking.domain.RecordingStateStore
import com.aijones.mileage.feature.tracking.domain.ResumeTripUseCase
import com.aijones.mileage.feature.tracking.domain.StopTripUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TripRecorderService : LifecycleService() {
    @Inject lateinit var session: RecordingSession
    @Inject lateinit var store: RecordingStateStore
    @Inject lateinit var notifications: TrackingNotifications
    @Inject lateinit var observeLiveUpdate: ObserveLiveUpdateEnabledUseCase
    @Inject lateinit var pauseTrip: PauseTripUseCase
    @Inject lateinit var resumeTrip: ResumeTripUseCase
    @Inject lateinit var stopTrip: StopTripUseCase
    @Inject lateinit var recordFailure: RecordFailureUseCase

    @Inject @ApplicationScope
    lateinit var appScope: CoroutineScope

    private var recording: Job? = null
    private var notifier: Job? = null
    private var tripId: Long? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START, ACTION_RESUME -> if (goForeground()) record()
            ACTION_PAUSE -> pause()
            ACTION_STOP -> finish(interrupted = false)
            ACTION_USER_PAUSE -> appScope.launch { pauseTrip() }
            ACTION_USER_RESUME -> appScope.launch { resumeTrip() }
            ACTION_USER_STOP -> appScope.launch { stopTrip() }
            else -> if (recording == null) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun goForeground(): Boolean = try {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        ServiceCompat.startForeground(this, TrackingNotifications.ID_RECORDING, notifications.recording(store.state.value, liveUpdate = false), type)
        startNotifier()
        true
    } catch (e: SecurityException) {
        fail(FailureReason.PERMISSION_REVOKED)
        false
    } catch (e: IllegalStateException) {
        fail(FailureReason.FGS_START_FAILED)
        false
    }

    private fun record() {
        val state = store.state.value as? RecordingState.Recording ?: return stopIfIdle()
        if (recording?.isActive == true) return
        tripId = state.tripId
        recording = lifecycleScope.launch {
            try {
                session.record(state.tripId)
            } catch (e: SecurityException) {
                fail(FailureReason.PERMISSION_REVOKED)
            }
        }
    }

    private fun pause() {
        recording?.cancel()
        recording = null
    }

    private fun finish(interrupted: Boolean) {
        val id = tripId ?: store.state.value.tripIdOrNull()
        val job = recording
        recording = null
        job?.cancel()
        appScope.launch {
            job?.join()
            id?.let { FinalizeTripWorker.enqueue(applicationContext, it, interrupted) }
        }
        stopService()
    }

    private fun fail(reason: FailureReason) {
        val id = tripId ?: store.state.value.tripIdOrNull()
        val job = recording
        recording = null
        job?.cancel()
        appScope.launch {
            job?.join()
            recordFailure(reason)
            if (id != null && reason == FailureReason.PERMISSION_REVOKED) {
                FinalizeTripWorker.enqueue(applicationContext, id, interrupted = true)
                notifications.notify(TrackingNotifications.ID_STOPPED, notifications.recordingStopped())
            }
        }
        stopService()
    }

    private fun stopIfIdle() {
        if (store.state.value is RecordingState.Idle) stopService()
    }

    private fun stopService() {
        notifier?.cancel()
        notifier = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startNotifier() {
        if (notifier != null) return
        notifier = lifecycleScope.launch {
            combine(store.state, observeLiveUpdate()) { state, live -> state to live }
                .conflate()
                .collect { (state, live) ->
                    if (state !is RecordingState.Idle) {
                        notifications.notify(TrackingNotifications.ID_RECORDING, notifications.recording(state, live))
                    }
                    delay(NOTIFY_INTERVAL_MS)
                }
        }
    }

    companion object {
        const val ACTION_START = "com.aijones.mileage.tracking.START"
        const val ACTION_PAUSE = "com.aijones.mileage.tracking.PAUSE"
        const val ACTION_RESUME = "com.aijones.mileage.tracking.RESUME"
        const val ACTION_STOP = "com.aijones.mileage.tracking.STOP"
        const val ACTION_USER_PAUSE = "com.aijones.mileage.tracking.USER_PAUSE"
        const val ACTION_USER_RESUME = "com.aijones.mileage.tracking.USER_RESUME"
        const val ACTION_USER_STOP = "com.aijones.mileage.tracking.USER_STOP"
        private const val NOTIFY_INTERVAL_MS = 5_000L
    }
}

internal fun RecordingState.tripIdOrNull(): Long? = when (this) {
    is RecordingState.Recording -> tripId
    is RecordingState.Paused -> tripId
    RecordingState.Idle -> null
}
