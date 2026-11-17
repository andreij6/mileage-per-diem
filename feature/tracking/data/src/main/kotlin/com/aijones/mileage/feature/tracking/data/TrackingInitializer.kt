package com.aijones.mileage.feature.tracking.data

import com.aijones.mileage.core.common.ApplicationScope
import com.aijones.mileage.core.common.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingInitializer @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
    private val notifications: TrackingNotifications,
    private val driveDetection: DriveDetectionRegistrar,
) {
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch(io) {
            notifications.ensureChannels()
            driveDetection.run()
        }
    }
}
