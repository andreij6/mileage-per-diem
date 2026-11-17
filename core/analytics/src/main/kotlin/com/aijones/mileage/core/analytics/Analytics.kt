package com.aijones.mileage.core.analytics

import kotlinx.coroutines.flow.Flow

fun interface Analytics {
    fun track(event: AnalyticsEvent)
}

data class LoggedEvent(val id: Long, val name: String, val params: Map<String, String>, val timestampMs: Long, val sessionId: String)

data class HealthSummary(
    val coldStartP50Ms: Long?,
    val crashFreeSessionsPct: Int?,
    val sessions: Int,
    val tripsRecorded: Int,
    val tripsInterrupted: Int,
    val recordingFailures: Int,
    val lastFailureReason: String?,
    val lastFailureAtMs: Long?,
    val exports: Int,
    val exportsByFormat: Map<String, Int>,
    val totalEvents: Int,
)

interface EventLog {
    fun observeRecent(limit: Int): Flow<List<LoggedEvent>>
    fun observeSummary(sinceMs: Long): Flow<HealthSummary>
    suspend fun exportNdjson(): String
}
