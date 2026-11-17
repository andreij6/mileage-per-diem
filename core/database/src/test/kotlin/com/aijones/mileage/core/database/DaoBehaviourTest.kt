package com.aijones.mileage.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aijones.mileage.core.database.entity.AnalyticsEventEntity
import com.aijones.mileage.core.database.entity.TrackPointEntity
import com.aijones.mileage.core.database.entity.TripEntity
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DaoBehaviourTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MileageDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    @AfterTest
    fun close() = db.close()

    @Test
    fun `analytics ring buffer keeps only the newest rows`() = runTest {
        val dao = db.analyticsEventDao()
        dao.append((1..8).map { AnalyticsEventEntity(name = "e$it", params = "{}", ts = it.toLong(), sessionId = "s") }, keep = 5)
        assertEquals(listOf("e8", "e7", "e6", "e5", "e4"), dao.observeRecent(10).first().map { it.name })
    }

    @Test
    fun `deleting a trip cascades to its track points`() = runTest {
        val id = db.tripDao().insert(TripEntity(startedAt = 1, status = "DONE"))
        db.trackPointDao().insertAll(listOf(TrackPointEntity(tripId = id, t = 1, lat = 0.0, lng = 0.0, accuracyM = 5f)))
        db.tripDao().delete(id)
        assertEquals(emptyList(), db.trackPointDao().forTrip(id))
        assertNull(db.tripDao().get(id))
    }

    @Test
    fun `needing review lists finished unreviewed trips newest first and active excludes them`() = runTest {
        val dao = db.tripDao()
        dao.insert(TripEntity(startedAt = 10, status = "DONE", needsReview = true))
        dao.insert(TripEntity(startedAt = 20, status = "DONE", needsReview = true))
        dao.insert(TripEntity(startedAt = 30, status = "RECORDING", needsReview = true))
        assertEquals(listOf(20L, 10L), dao.observeNeedingReview().first().map { it.startedAt })
        assertEquals(30L, dao.active()?.startedAt)
    }
}
