package com.aijones.mileage.feature.widgets.presentation

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.os.Build
import android.service.quicksettings.TileService
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.feature.tracking.domain.RecordingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class StartTripTileService : TileService() {
    private var listening: CoroutineScope? = null
    private var state: RecordingState = RecordingState.Idle

    override fun onStartListening() {
        val recording = widgetEntryPoint().observeRecordingState()()
        listening = MainScope().apply { launch { recording.collect { render(it) } } }
    }

    override fun onStopListening() {
        listening?.cancel()
        listening = null
    }

    override fun onClick() {
        if (isLocked) unlockAndRun(::launchApp) else launchApp()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchApp() {
        val active = state != RecordingState.Idle
        widgetEntryPoint().analytics().track(AnalyticsEvent.WidgetAction(WidgetNames.TILE, if (active) "open" else "start_trip"))
        val intent = if (active) openLiveTripIntent(this) else startTripIntent(this, WidgetNames.TILE, source = WidgetNames.TILE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun render(recording: RecordingState) {
        state = recording
        val tile = qsTile ?: return
        tile.state = recording.tileState()
        tile.label = getString(R.string.tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = getString(R.string.tile_subtitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = getString(if (recording == RecordingState.Idle) R.string.tile_idle else R.string.tile_recording)
        }
        tile.updateTile()
    }
}
