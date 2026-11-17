package com.aijones.mileage.feature.settings.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aijones.mileage.core.database.MileageDatabase
import com.aijones.mileage.core.database.entity.StayEntity
import com.aijones.mileage.core.database.entity.TrackPointEntity
import com.aijones.mileage.core.database.entity.TripEntity
import com.aijones.mileage.core.database.entity.VehicleEntity
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomDataResetterTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MileageDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    @AfterTest
    fun close() = db.close()

    @Test
    fun `deletes trips with their track points and stays but keeps vehicles`() = runTest {
        val tripId = db.tripDao().insert(TripEntity(startedAt = 0, status = "DONE"))
        db.trackPointDao().insertAll(listOf(TrackPointEntity(tripId = tripId, t = 1, lat = 40.0, lng = -74.0, accuracyM = 5f)))
        db.stayDao().insert(StayEntity(0, 1, "Boston, MA", 2027, 20_000, 20_002, true, 6_800, 13_600, 22_600))
        db.vehicleDao().insert(VehicleEntity(name = "2021 Civic", kind = "Personal", isDefault = true))

        RoomDataResetter(db.tripDao(), db.stayDao(), StandardTestDispatcher(testScheduler)).deleteAllTripsAndReports()

        assertTrue(db.tripDao().observeRecent(10).first().isEmpty())
        assertTrue(db.trackPointDao().forTrip(tripId).isEmpty())
        assertTrue(db.stayDao().observeOverlapping(0, Long.MAX_VALUE).first().isEmpty())
        assertEquals(1, db.vehicleDao().observeAll().first().size)
    }
}
