package com.aijones.mileage.startup

import android.os.Process
import android.os.SystemClock
import android.view.Choreographer
import androidx.activity.ComponentActivity
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.StartType

class StartupTracker(private val analytics: Analytics) {
    fun track(activity: ComponentActivity, cold: Boolean) {
        val origin = if (cold) Process.getStartUptimeMillis() else SystemClock.uptimeMillis()
        var ttid = -1L
        Choreographer.getInstance().postFrameCallback { ttid = SystemClock.uptimeMillis() - origin }
        activity.fullyDrawnReporter.addOnReportDrawnListener {
            val ttfd = SystemClock.uptimeMillis() - origin
            analytics.track(AnalyticsEvent.AppStartup(if (cold) StartType.COLD else StartType.WARM, if (ttid < 0) ttfd else ttid, ttfd))
        }
    }
}
