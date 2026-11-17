package com.aijones.mileage.feature.tracking.presentation

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource

internal fun formatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

internal fun formatTenths(tenths: Long): String = "${tenths / 10}.${tenths % 10}"

@Composable
internal fun durationText(minutes: Long): String =
    if (minutes < 60) {
        stringResource(R.string.duration_minutes, minutes.toInt())
    } else {
        stringResource(R.string.duration_hours_minutes, (minutes / 60).toInt(), (minutes % 60).toInt())
    }

@Composable
internal fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    val preview = LocalInspectionMode.current
    return remember(context) {
        !preview && Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
