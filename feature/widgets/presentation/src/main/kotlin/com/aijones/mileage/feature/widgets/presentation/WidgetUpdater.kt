package com.aijones.mileage.feature.widgets.presentation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.service.quicksettings.TileService
import androidx.glance.appwidget.updateAll
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.common.ApplicationScope
import com.aijones.mileage.core.navigation.Deeplinks
import com.aijones.mileage.feature.tracking.domain.ObserveRecordingStateUseCase
import com.aijones.mileage.feature.tracking.domain.StopTripUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

const val WIDGET_UPDATE_WINDOW_MS = 30_000L

@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val observeSnapshot: ObserveWidgetSnapshotUseCase,
) {
    private val latest = MutableStateFlow<WidgetSnapshot?>(null)
    private val started = AtomicBoolean(false)

    val snapshots: Flow<WidgetSnapshot> = latest.filterNotNull()

    suspend fun current(): WidgetSnapshot = latest.value ?: observeSnapshot().first()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            observeSnapshot().throttleWhileRecording(WIDGET_UPDATE_WINDOW_MS) { SystemClock.elapsedRealtime() }.collect { next ->
                val previous = latest.value
                latest.value = next
                if (previous?.month != next.month) MonthWidget().updateAll(context)
                if (previous?.perDiem != next.perDiem) PerDiemWidget().updateAll(context)
                if (previous?.recording != next.recording) RecordingWidget().updateAll(context)
                if ((previous?.recording is RecordingWidgetUi.Active) != (next.recording is RecordingWidgetUi.Active)) {
                    TileService.requestListeningState(context, ComponentName(context, StartTripTileService::class.java))
                }
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun analytics(): Analytics
    fun stopTrip(): StopTripUseCase
    fun observeRecordingState(): ObserveRecordingStateUseCase
    fun widgetUpdater(): WidgetUpdater
}

internal fun Context.widgetEntryPoint(): WidgetEntryPoint = EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)

internal object WidgetNames {
    const val MONTH = "month"
    const val PER_DIEM = "per_diem"
    const val RECORDING = "recording"
    const val TILE = "tile"
    const val EXTRA_WIDGET = "widget"
}

internal fun startTripIntent(context: Context, widget: String, source: String = "widget"): Intent =
    Intent(Deeplinks.ACTION_START_TRIP)
        .setPackage(context.packageName)
        .putExtra(Deeplinks.EXTRA_SOURCE, source)
        .putExtra(WidgetNames.EXTRA_WIDGET, widget)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

internal fun openLiveTripIntent(context: Context): Intent =
    Intent(Deeplinks.ACTION_OPEN_LIVE_TRIP).setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

internal fun openAppIntent(context: Context): Intent =
    context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent(Intent.ACTION_MAIN).setPackage(context.packageName)
