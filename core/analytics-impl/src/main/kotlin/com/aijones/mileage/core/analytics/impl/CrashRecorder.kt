package com.aijones.mileage.core.analytics.impl

import javax.inject.Inject

class CrashRecorder @Inject internal constructor(private val analytics: LocalAnalytics) {
    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            analytics.recordCrashSync(throwable, thread)
            previous?.uncaughtException(thread, throwable)
        }
    }
}
