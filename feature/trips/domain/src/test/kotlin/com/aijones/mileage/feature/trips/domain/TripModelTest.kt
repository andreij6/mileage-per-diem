package com.aijones.mileage.feature.trips.domain

import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.GeoPoint
import com.aijones.mileage.core.model.Money
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TripModelTest {
    private val start = Instant.parse("2026-11-16T14:00:00Z")

    @Test
    fun businessTripIsReimbursedAtItsSnapshotRate() {
        val trip = Trip(startedAt = start, status = TripStatus.DONE, distance = Distance.ofMiles(18.4), ratePerMile = Money(760_000))
        assertEquals(Money(13_984_000), trip.reimbursement)
        assertEquals(1398L, trip.reimbursement.cents)
    }

    @Test
    fun personalOrUnratedTripIsNotReimbursed() {
        val base = Trip(startedAt = start, status = TripStatus.DONE, distance = Distance.ofMiles(10.0), ratePerMile = Money(760_000))
        assertEquals(Money.ZERO, base.copy(category = TripCategory.PERSONAL).reimbursement)
        assertEquals(Money.ZERO, base.copy(ratePerMile = null).reimbursement)
    }

    @Test
    fun durationIsKnownOnlyOnceEnded() {
        val trip = Trip(startedAt = start, status = TripStatus.RECORDING)
        assertNull(trip.duration)
        assertEquals(Duration.ofMinutes(42), trip.copy(endedAt = start.plusSeconds(42 * 60)).duration)
    }

    @Test
    fun speedProfileBucketsSpeedsInMph() {
        val points = (0..9).map { i ->
            TrackPoint(start.plusSeconds(i * 10L), GeoPoint(40.0, -74.0), 5f, speedMps = if (i < 5) 10f else 20f)
        }
        val profile = SpeedProfile.from(points, buckets = 2)
        assertEquals(2, profile.samples.size)
        assertEquals(22.37f, profile.samples[0], 0.01f)
        assertEquals(44.74f, profile.samples[1], 0.01f)
        assertEquals(start, profile.start)
        assertEquals(start.plusSeconds(90), profile.end)
    }

    @Test
    fun speedProfileDerivesSpeedFromPositionsWhenMissing() {
        val a = GeoPoint(40.0, -74.0)
        val b = GeoPoint(40.0009, -74.0)
        val points = listOf(TrackPoint(start, a, 5f), TrackPoint(start.plusSeconds(10), b, 5f))
        val profile = SpeedProfile.from(points, buckets = 1)
        assertEquals(10.0f * 2.2369363f / 2, profile.samples.single(), 0.2f)
    }

    @Test
    fun speedProfileSampleAtFraction() {
        val points = (0..3).map { TrackPoint(start.plusSeconds(it * 60L), GeoPoint(40.0, -74.0), 5f, speedMps = it.toFloat()) }
        val profile = SpeedProfile.from(points, buckets = 4)
        val mid = profile.sampleAt(0.5)
        assertEquals(start.plusSeconds(90), mid.t)
        assertEquals(profile.samples[2], mid.mph)
        assertEquals(profile.samples.last(), profile.sampleAt(1.0).mph)
        assertEquals(profile.end, profile.sampleAt(2.0).t)
    }

    @Test
    fun speedProfileOfTooFewPointsIsEmpty() {
        assertEquals(emptyList(), SpeedProfile.from(emptyList()).samples)
        assertEquals(0f, SpeedProfile.from(emptyList()).sampleAt(0.5).mph)
    }
}
