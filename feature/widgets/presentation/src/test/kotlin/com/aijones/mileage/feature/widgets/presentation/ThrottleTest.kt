package com.aijones.mileage.feature.widgets.presentation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ThrottleTest {
    private val month = MonthWidgetUi("November", "$642.20", "0845", "0")

    private fun snapshot(recording: RecordingWidgetUi) = WidgetSnapshot(month, null, recording)

    private fun active(distance: String) = snapshot(RecordingWidgetUi.Active(false, distance, "00:01", "$0.01"))

    @Test
    fun recordingUpdatesAreLimitedToOneEveryWindowWhileStartAndStopPassAtOnce() = runTest {
        val source = MutableSharedFlow<WidgetSnapshot>()
        val seen = mutableListOf<Pair<Long, String>>()
        backgroundScope.launch {
            source.throttleWhileRecording(30_000) { testScheduler.currentTime }.collect { s ->
                seen += testScheduler.currentTime to ((s.recording as? RecordingWidgetUi.Active)?.distance ?: "idle")
            }
        }
        runCurrent()

        source.emit(snapshot(RecordingWidgetUi.Idle))
        source.emit(active("0.1 mi"))
        repeat(20) { i ->
            advanceTimeBy(2_000)
            source.emit(active("${i + 2}.0 mi"))
        }
        runCurrent()
        source.emit(snapshot(RecordingWidgetUi.Idle))
        runCurrent()
        advanceTimeBy(60_000)

        assertEquals(
            listOf(0L to "idle", 0L to "0.1 mi", 30_000L to "15.0 mi", 40_000L to "idle"),
            seen,
        )
    }
}
