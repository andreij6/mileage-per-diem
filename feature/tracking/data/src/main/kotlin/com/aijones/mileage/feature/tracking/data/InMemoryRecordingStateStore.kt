package com.aijones.mileage.feature.tracking.data

import com.aijones.mileage.feature.tracking.domain.RecordingState
import com.aijones.mileage.feature.tracking.domain.RecordingStateStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryRecordingStateStore @Inject constructor() : RecordingStateStore {
    private val mutable = MutableStateFlow<RecordingState>(RecordingState.Idle)
    override val state: StateFlow<RecordingState> = mutable.asStateFlow()

    override fun update(transform: (RecordingState) -> RecordingState) = mutable.update(transform)
}
