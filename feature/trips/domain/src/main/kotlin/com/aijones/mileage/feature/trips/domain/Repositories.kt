package com.aijones.mileage.feature.trips.domain

import com.aijones.mileage.core.model.Money
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface TripRepository {
    fun observeBetween(from: Instant, to: Instant): Flow<List<Trip>>
    fun observeRecent(limit: Int): Flow<List<Trip>>
    fun observe(id: Long): Flow<Trip?>
    suspend fun get(id: Long): Trip?
    suspend fun insert(trip: Trip): Long
    suspend fun update(trip: Trip)
    suspend fun delete(id: Long)
    suspend fun getActive(): Trip?
    fun observeActive(): Flow<Trip?>
    fun observeNeedingReview(): Flow<List<Trip>>
    suspend fun trackPoints(id: Long): List<TrackPoint>
    fun observeTrackPoints(id: Long): Flow<List<TrackPoint>>
    suspend fun appendTrackPoints(tripId: Long, points: List<TrackPoint>)
}

interface VehicleRepository {
    fun observeAll(): Flow<List<Vehicle>>
    suspend fun add(name: String, kind: String): Long
    suspend fun rename(id: Long, name: String)
    suspend fun delete(id: Long)
    suspend fun setDefault(id: Long)
}

interface MileageRateRepository {
    suspend fun irsRates(): List<MileageRate>
    fun observeCustomRate(): Flow<Money?>
    suspend fun setCustomRate(rate: Money?)
}
