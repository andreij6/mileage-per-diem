package com.aijones.mileage.feature.settings.domain

import com.aijones.mileage.core.analytics.EventLog
import com.aijones.mileage.core.analytics.HealthSummary
import com.aijones.mileage.core.analytics.LoggedEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeEventLog(events: List<LoggedEvent> = emptyList()) : EventLog {
    val events = MutableStateFlow(events)
    var lastSummarySinceMs: Long? = null
        private set

    override fun observeRecent(limit: Int): Flow<List<LoggedEvent>> = events.map { it.sortedByDescending { e -> e.timestampMs }.take(limit) }

    override fun observeSummary(sinceMs: Long): Flow<HealthSummary> {
        lastSummarySinceMs = sinceMs
        return events.map { all -> summaryOf(all.filter { it.timestampMs >= sinceMs }) }
    }

    override suspend fun exportNdjson(): String = events.value.joinToString("\n") { e ->
        val params = e.params.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }
        "{\"event\":\"${e.name}\",\"ts\":${e.timestampMs},\"session_id\":\"${e.sessionId}\"${if (params.isEmpty()) "" else ",$params"}}"
    }

    private fun summaryOf(events: List<LoggedEvent>): HealthSummary {
        val stopped = events.filter { it.name == "trip_recording_stopped" }
        val failures = events.filter { it.name == "trip_recording_failed" }
        val exports = events.filter { it.name == "export_completed" }
        return HealthSummary(
            coldStartP50Ms = null,
            crashFreeSessionsPct = null,
            sessions = events.count { it.name == "session_start" },
            tripsRecorded = stopped.size,
            tripsInterrupted = stopped.count { it.params["interrupted"] == "true" },
            recordingFailures = failures.size,
            lastFailureReason = failures.maxByOrNull { it.timestampMs }?.params?.get("reason"),
            lastFailureAtMs = failures.maxOfOrNull { it.timestampMs },
            exports = exports.size,
            exportsByFormat = exports.groupingBy { it.params["format"].orEmpty() }.eachCount(),
            totalEvents = events.size,
        )
    }
}
