package com.aijones.mileage.feature.trips.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.aijones.mileage.core.database.MileageDatabase
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.trips.domain.TrackPoint
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripCategory
import com.aijones.mileage.feature.trips.domain.TripStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomRepositoriesTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MileageDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val trips = RoomTripRepository(db.tripDao(), db.trackPointDao(), Dispatchers.Unconfined)
    private val vehicles = RoomVehicleRepository(db.vehicleDao(), Dispatchers.Unconfined)

    @After
    fun close() = db.close()

    private val base = Instant.parse("2026-11-16T14:12:00Z")

    private fun trip(minutesAgo: Long, status: TripStatus = TripStatus.DONE, needsReview: Boolean = false) = Trip(
        startedAt = base.minusSeconds(minutesAgo * 60),
        endedAt = base.minusSeconds(minutesAgo * 60).plusSeconds(1_740),
        status = status,
        distance = Distance.ofMiles(18.4),
        recordedDistance = Distance.ofMiles(18.3),
        purpose = "Client meeting",
        category = TripCategory.PERSONAL,
        vehicleId = 7,
        ratePerMile = Money(760_000),
        startLabel = "Brooklyn office",
        endLabel = "Acme HQ, Midtown",
        start = GeoPoint(40.7033, -73.9881),
        end = GeoPoint(40.7536, -73.9832),
        notes = "Quarterly review",
        needsReview = needsReview,
        edited = true,
        roundTrip = true,
        source = "manual",
    )

    @Test
    fun tripRoundTripsEveryField() = runTest {
        val original = trip(0)
        val id = trips.insert(original)
        assertEquals(original.copy(id = id), trips.get(id))
    }

    @Test
    fun listsAreNewestFirstAndSkipActiveTrips() = runTest {
        val old = trips.insert(trip(600))
        val new = trips.insert(trip(10))
        trips.insert(trip(0, status = TripStatus.RECORDING))
        assertEquals(listOf(new, old), trips.observeRecent(10).first().map { it.id })
        assertEquals(listOf(new), trips.observeBetween(base.minusSeconds(3_600), base).first().map { it.id })
        assertEquals(TripStatus.RECORDING, trips.getActive()?.status)
    }

    @Test
    fun needingReviewIsNewestFirstAndUpdatesLive() = runTest {
        val a = trips.insert(trip(600, needsReview = true))
        val b = trips.insert(trip(60, needsReview = true))
        trips.observeNeedingReview().test {
            assertEquals(listOf(b, a), awaitItem().map { it.id })
            trips.update(trips.get(b)!!.copy(needsReview = false))
            assertEquals(listOf(a), awaitItem().map { it.id })
        }
    }

    @Test
    fun trackPointsAreBatchInsertedInTimeOrderAndDeletedWithTheTrip() = runTest {
        val id = trips.insert(trip(0))
        val pts = (0 until 50).map { TrackPoint(base.plusSeconds(it * 2L), GeoPoint(40.7 + it / 1000.0, -73.98), 5f, 12f) }
        trips.appendTrackPoints(id, pts.reversed())
        assertEquals(pts, trips.trackPoints(id))
        assertEquals(pts, trips.observeTrackPoints(id).first())
        trips.delete(id)
        assertNull(trips.get(id))
        assertTrue(trips.trackPoints(id).isEmpty())
    }

    @Test
    fun firstVehicleBecomesDefaultAndDefaultCanMove() = runTest {
        val civic = vehicles.add("2021 Civic", "Personal")
        val rental = vehicles.add("Rental", "Rental")
        assertEquals(listOf(civic to true, rental to false), vehicles.observeAll().first().map { it.id to it.isDefault })
        vehicles.setDefault(rental)
        vehicles.rename(civic, "Civic")
        val all = vehicles.observeAll().first()
        assertEquals(rental, all.first { it.isDefault }.id)
        assertEquals("Civic", all.first { it.id == civic }.name)
        vehicles.delete(civic)
        assertEquals(listOf(rental), vehicles.observeAll().first().map { it.id })
    }
}
