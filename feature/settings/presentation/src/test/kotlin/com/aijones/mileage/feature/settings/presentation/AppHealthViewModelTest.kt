package com.aijones.mileage.feature.settings.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.analytics.LoggedEvent
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.settings.domain.ExportEventLogUseCase
import com.aijones.mileage.feature.settings.domain.FakeEventLog
import com.aijones.mileage.feature.settings.domain.ObserveAppHealthUseCase
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class AppHealthViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val clock = FakeClock()
    private fun at(iso: String) = Instant.parse(iso).toEpochMilli()
    private val log = FakeEventLog(
        listOf(
            LoggedEvent(1, "session_start", emptyMap(), at("2026-11-01T12:00:00Z"), "s0"),
            LoggedEvent(2, "trip_recording_stopped", mapOf("distance_mi" to "18.4", "interrupted" to "false"), at("2026-11-16T14:41:00Z"), "s1"),
            LoggedEvent(3, "trip_recording_failed", mapOf("reason" to "location_off"), at("2026-11-12T15:00:00Z"), "s1"),
            LoggedEvent(4, "export_completed", mapOf("format" to "pdf"), at("2026-11-16T14:44:00Z"), "s1"),
        ),
    )
    private val analytics = FakeAnalytics()
    private val vm = AppHealthViewModel(ObserveAppHealthUseCase(log), ExportEventLogUseCase(log), clock, analytics)

    @Test
    fun `summarises the last seven days with recent events in local time`() = runTest {
        vm.state.test {
            assertEquals(AppHealthUiState(), awaitItem())
            val state = awaitItem()
            assertEquals(3, state.summary?.totalEvents)
            assertEquals(1, state.summary?.recordingFailures)
            assertEquals("Nov 12", state.lastFailureDate)
            assertEquals(
                listOf(
                    EventItem(4, "export_completed", listOf("format" to "pdf"), "9:44"),
                    EventItem(2, "trip_recording_stopped", listOf("distance_mi" to "18.4", "interrupted" to "false"), "9:41"),
                ),
                state.events.take(2),
            )
            assertNull(state.shareText)
        }
        assertEquals(at("2026-11-09T14:41:00Z"), log.lastSummarySinceMs)
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("app_health")), analytics.events)
    }

    @Test
    fun `export produces NDJSON to share once`() = runTest {
        vm.state.test {
            skipItems(2)
            vm.export()
            val text = awaitItem().shareText!!
            assertEquals(4, text.lines().size)
            assertEquals(true, text.lines().first().startsWith("{\"event\":\"session_start\""))
            vm.onShared()
            assertNull(awaitItem().shareText)
        }
    }
}
