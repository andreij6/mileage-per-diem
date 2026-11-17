package com.aijones.mileage.feature.tracking.domain

import com.aijones.mileage.core.analytics.TripSource
import com.aijones.mileage.core.model.GeoPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.update

class FakeLocationTracker(private val fixes: List<Fix> = emptyList()) : LocationTracker {
    val requestedPolicies = mutableListOf<RecordingPolicy>()

    override fun fixes(policy: RecordingPolicy): Flow<Fix> {
        requestedPolicies += policy
        return fixes.asFlow()
    }
}

class FakeRecordingController : RecordingController {
    val calls = mutableListOf<String>()
    var failOnStart: Throwable? = null

    override fun start(source: TripSource) {
        failOnStart?.let { throw it }
        calls += "start:${source.wire}"
    }

    override fun pause() {
        calls += "pause"
    }

    override fun resume() {
        calls += "resume"
    }

    override fun stop() {
        calls += "stop"
    }
}

class FakeRecordingStateStore(initial: RecordingState = RecordingState.Idle) : RecordingStateStore {
    private val mutable = MutableStateFlow(initial)
    override val state: StateFlow<RecordingState> = mutable

    override fun update(transform: (RecordingState) -> RecordingState) = mutable.update(transform)
}

class FakeDeviceStateMonitor(initial: DeviceState = DeviceState(powerSave = false, batteryPct = 80)) : DeviceStateMonitor {
    val state = MutableStateFlow(initial)

    override fun observe(): Flow<DeviceState> = state
}

class FakePlaceLabeler(private val labels: Map<GeoPoint, String> = emptyMap(), private val failure: Throwable? = null) : PlaceLabeler {
    val requested = mutableListOf<GeoPoint>()

    override suspend fun label(point: GeoPoint): String? {
        requested += point
        failure?.let { throw it }
        return labels[point]
    }
}
