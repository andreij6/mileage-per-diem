package com.aijones.mileage.feature.tracking.data

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.aijones.mileage.core.analytics.TripSource
import com.aijones.mileage.feature.tracking.domain.RecordingController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ServiceRecordingController @Inject constructor(@ApplicationContext private val context: Context) : RecordingController {
    override fun start(source: TripSource) {
        ContextCompat.startForegroundService(context, intent(TripRecorderService.ACTION_START))
    }

    override fun pause() = send(TripRecorderService.ACTION_PAUSE)

    override fun resume() = send(TripRecorderService.ACTION_RESUME)

    override fun stop() = send(TripRecorderService.ACTION_STOP)

    private fun send(action: String) {
        runCatching { context.startService(intent(action)) }
    }

    private fun intent(action: String) = Intent(context, TripRecorderService::class.java).setAction(action)
}
