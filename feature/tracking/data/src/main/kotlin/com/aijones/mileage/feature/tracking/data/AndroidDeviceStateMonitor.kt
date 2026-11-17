package com.aijones.mileage.feature.tracking.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.aijones.mileage.feature.tracking.domain.DeviceState
import com.aijones.mileage.feature.tracking.domain.DeviceStateMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class AndroidDeviceStateMonitor @Inject constructor(@ApplicationContext private val context: Context) : DeviceStateMonitor {
    override fun observe(): Flow<DeviceState> = callbackFlow {
        val power = context.getSystemService(PowerManager::class.java)
        var pct = FULL
        fun emit(intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) pct = batteryPct(intent)
            trySend(DeviceState(powerSave = power?.isPowerSaveMode == true, batteryPct = pct))
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = emit(intent)
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        emit(ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED))
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    private companion object {
        const val FULL = 100
    }
}

internal fun batteryPct(intent: Intent): Int {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    return if (level < 0 || scale <= 0) 100 else (level * 100 / scale).coerceIn(0, 100)
}
