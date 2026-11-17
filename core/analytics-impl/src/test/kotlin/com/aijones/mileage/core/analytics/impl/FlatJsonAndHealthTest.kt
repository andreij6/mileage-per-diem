package com.aijones.mileage.core.analytics.impl

import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FailureReason
import com.aijones.mileage.core.analytics.LoggedEvent
import com.aijones.mileage.core.analytics.StartType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FlatJsonAndHealthTest {
    @Test
    fun `params round trip through flat json including escapes`() {
        val params = mapOf("format" to "pdf", "trips" to 32, "ok" to true, "note" to "a \"quoted\"\nline")
        val decoded = FlatJson.decode(FlatJson.encode(params))
        assertEquals(mapOf("format" to "pdf", "trips" to "32", "ok" to "true", "note" to "a \"quoted\"\nline"), decoded)
    }

    @Test
    fun `health summary computes median cold start, crash free rate and failures`() {
        var id = 0L
        fun ev(e: AnalyticsEvent, ts: Long = 0) = LoggedEvent(id++, e.name, FlatJson.decode(FlatJson.encode(e.params)), ts, "s")
        val events = listOf(
            ev(AnalyticsEvent.SessionStart), ev(AnalyticsEvent.SessionStart), ev(AnalyticsEvent.SessionStart), ev(AnalyticsEvent.SessionStart),
            ev(AnalyticsEvent.AppStartup(StartType.COLD, 400, 520)),
            ev(AnalyticsEvent.AppStartup(StartType.COLD, 380, null)),
            ev(AnalyticsEvent.AppStartup(StartType.COLD, 900, null)),
            ev(AnalyticsEvent.AppStartup(StartType.WARM, 90, null)),
            ev(AnalyticsEvent.AppCrash("java.lang.IllegalStateException", "main")),
            ev(AnalyticsEvent.TripRecordingStopped(184, 1742, 812, 3, false)),
            ev(AnalyticsEvent.TripRecordingStopped(82, 600, 200, 0, true)),
            ev(AnalyticsEvent.TripRecordingFailed(FailureReason.LOCATION_OFF), ts = 77),
            ev(AnalyticsEvent.ExportCompleted("pdf", 32, 184)),
            ev(AnalyticsEvent.ExportCompleted("csv", 32, 20)),
            ev(AnalyticsEvent.ExportCompleted("pdf", 10, 90)),
        )
        val s = HealthCalculator.summarize(events)
        assertEquals(400, s.coldStartP50Ms)
        assertEquals(75, s.crashFreeSessionsPct)
        assertEquals(2, s.tripsRecorded)
        assertEquals(1, s.tripsInterrupted)
        assertEquals("location_off", s.lastFailureReason)
        assertEquals(77, s.lastFailureAtMs)
        assertEquals(mapOf("pdf" to 2, "csv" to 1), s.exportsByFormat)
    }

    @Test
    fun `empty log has no p50 and no crash free rate`() {
        val s = HealthCalculator.summarize(emptyList())
        assertNull(s.coldStartP50Ms)
        assertNull(s.crashFreeSessionsPct)
    }
}
