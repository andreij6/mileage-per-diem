package com.aijones.mileage.core.analytics.impl

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.util.Log
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.EventLog
import com.aijones.mileage.core.analytics.HealthSummary
import com.aijones.mileage.core.analytics.LoggedEvent
import com.aijones.mileage.core.common.ApplicationScope
import com.aijones.mileage.core.common.IoDispatcher
import com.aijones.mileage.core.database.dao.AnalyticsEventDao
import com.aijones.mileage.core.database.entity.AnalyticsEventEntity
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
internal class LocalAnalytics @Inject constructor(
    private val dao: Lazy<AnalyticsEventDao>,
    private val clock: Clock,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) : Analytics, EventLog {
    private val sessionId = Random.nextLong().toULong().toString(16).take(8)
    private val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    private val queue = Channel<Pending>(capacity = QUEUE, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val base by lazy { mapOf("app_version" to versionName(), "os_api" to Build.VERSION.SDK_INT) }

    private class Pending(val event: AnalyticsEvent, val ts: Long)

    init {
        scope.launch(io) {
            ingestPendingCrash()
            while (true) {
                val batch = mutableListOf(queue.receive())
                withTimeoutOrNull(FLUSH_MS) {
                    while (batch.size < BATCH) batch += queue.receive()
                }
                dao.get().append(batch.map { it.toEntity() }, KEEP)
            }
        }
    }

    override fun track(event: AnalyticsEvent) {
        queue.trySend(Pending(event, clock.millis()))
    }

    private fun Pending.toEntity(): AnalyticsEventEntity {
        val params = event.params + base
        if (debuggable) Log.d(TAG, "${event.name} $params")
        return AnalyticsEventEntity(name = event.name, params = FlatJson.encode(params), ts = ts, sessionId = sessionId)
    }

    fun recordCrashSync(throwable: Throwable, thread: Thread) {
        val event = AnalyticsEvent.AppCrash(throwable.javaClass.name, thread.name)
        runCatching {
            crashFile().writeText("${clock.millis()}\n${FlatJson.encode(event.params + base)}\n$sessionId")
        }
    }

    override fun observeRecent(limit: Int): Flow<List<LoggedEvent>> =
        dao.get().observeRecent(limit).map { rows -> rows.map { it.toLogged() } }.flowOn(io)

    override fun observeSummary(sinceMs: Long): Flow<HealthSummary> =
        dao.get().observeSince(sinceMs).map { rows -> HealthCalculator.summarize(rows.map { it.toLogged() }) }.flowOn(io)

    override suspend fun exportNdjson(): String = dao.get().all().joinToString("\n") { e ->
        FlatJson.encode(linkedMapOf<String, Any>("event" to e.name, "ts" to e.ts, "session_id" to e.sessionId) + FlatJson.decode(e.params))
    }

    private suspend fun ingestPendingCrash() {
        val f = crashFile()
        if (!f.exists()) return
        val lines = f.readLines()
        f.delete()
        if (lines.size >= 3) {
            dao.get().append(listOf(AnalyticsEventEntity(name = "app_crash", params = lines[1], ts = lines[0].toLong(), sessionId = lines[2])), KEEP)
        }
    }

    private fun crashFile() = File(context.noBackupFilesDir, "pending_crash")

    private fun versionName(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrDefault("")

    private fun AnalyticsEventEntity.toLogged() = LoggedEvent(id, name, FlatJson.decode(params), ts, sessionId)

    private companion object {
        const val TAG = "Analytics"
        const val QUEUE = 512
        const val BATCH = 20
        const val FLUSH_MS = 2_000L
        const val KEEP = 5_000
    }
}
