package com.aijones.mileage

import android.app.Application
import android.os.SystemClock
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.impl.CrashRecorder
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MileageApp : Application(), Configuration.Provider {
    @Inject lateinit var crashRecorder: CrashRecorder
    @Inject lateinit var analytics: Lazy<Analytics>
    @Inject lateinit var workerFactory: Lazy<HiltWorkerFactory>

    var coldStartPending = true

    override fun onCreate() {
        super.onCreate()
        crashRecorder.install()
        ProcessLifecycleOwner.get().lifecycle.addObserver(SessionObserver())
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory.get()).build()

    private inner class SessionObserver : DefaultLifecycleObserver {
        private var startedAt = 0L
        override fun onStart(owner: LifecycleOwner) {
            startedAt = SystemClock.elapsedRealtime()
            analytics.get().track(AnalyticsEvent.SessionStart)
        }

        override fun onStop(owner: LifecycleOwner) {
            analytics.get().track(AnalyticsEvent.SessionEnd((SystemClock.elapsedRealtime() - startedAt) / 1_000))
        }
    }
}
