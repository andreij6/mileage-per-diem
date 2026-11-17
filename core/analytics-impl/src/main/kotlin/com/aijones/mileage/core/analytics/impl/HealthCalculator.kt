package com.aijones.mileage.core.analytics.impl

import com.aijones.mileage.core.analytics.HealthSummary
import com.aijones.mileage.core.analytics.LoggedEvent

internal object HealthCalculator {
    fun summarize(events: List<LoggedEvent>): HealthSummary {
        val byName = events.groupBy { it.name }
        val sessions = byName["session_start"].orEmpty().size
        val crashes = byName["app_crash"].orEmpty().size
        val coldStarts = byName["app_startup"].orEmpty()
            .filter { it.params["type"] == "cold" }
            .mapNotNull { it.params["ttid_ms"]?.toLongOrNull() }
            .sorted()
        val stopped = byName["trip_recording_stopped"].orEmpty()
        val failures = byName["trip_recording_failed"].orEmpty()
        val exports = byName["export_completed"].orEmpty()
        return HealthSummary(
            coldStartP50Ms = coldStarts.takeIf { it.isNotEmpty() }?.let { it[(it.size - 1) / 2] },
            crashFreeSessionsPct = if (sessions == 0) null else ((sessions - crashes).coerceAtLeast(0) * 100 / sessions),
            sessions = sessions,
            tripsRecorded = stopped.size,
            tripsInterrupted = stopped.count { it.params["interrupted"] == "true" },
            recordingFailures = failures.size,
            lastFailureReason = failures.lastOrNull()?.params?.get("reason"),
            lastFailureAtMs = failures.lastOrNull()?.timestampMs,
            exports = exports.size,
            exportsByFormat = exports.groupingBy { it.params["format"].orEmpty() }.eachCount(),
            totalEvents = events.size,
        )
    }
}
