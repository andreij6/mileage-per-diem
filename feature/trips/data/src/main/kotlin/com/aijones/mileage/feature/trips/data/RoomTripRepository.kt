package com.aijones.mileage.feature.trips.data

import com.aijones.mileage.core.common.IoDispatcher
import com.aijones.mileage.core.database.dao.TrackPointDao
import com.aijones.mileage.core.database.dao.TripDao
import com.aijones.mileage.core.database.entity.TripEntity
import com.aijones.mileage.feature.trips.domain.TrackPoint
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

internal class RoomTripRepository @Inject constructor(
    private val trips: TripDao,
    private val points: TrackPointDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : TripRepository {

    override fun observeBetween(from: Instant, to: Instant): Flow<List<Trip>> =
        trips.observeBetween(from.toEpochMilli(), to.toEpochMilli()).toTrips()

    override fun observeRecent(limit: Int): Flow<List<Trip>> = trips.observeRecent(limit).toTrips()

    override fun observe(id: Long): Flow<Trip?> = trips.observe(id).map { it?.toDomain() }.flowOn(io)

    override suspend fun get(id: Long): Trip? = trips.get(id)?.toDomain()

    override suspend fun insert(trip: Trip): Long = trips.insert(trip.toEntity())

    override suspend fun update(trip: Trip) = trips.update(trip.toEntity())

    override suspend fun delete(id: Long) {
        points.deleteForTrip(id)
        trips.delete(id)
    }

    override suspend fun getActive(): Trip? = trips.active()?.toDomain()

    override fun observeActive(): Flow<Trip?> = trips.observeActive().map { it?.toDomain() }.flowOn(io)

    override fun observeNeedingReview(): Flow<List<Trip>> = trips.observeNeedingReview().toTrips()

    override suspend fun trackPoints(id: Long): List<TrackPoint> = points.forTrip(id).map { it.toDomain() }

    override fun observeTrackPoints(id: Long): Flow<List<TrackPoint>> =
        points.observeForTrip(id).map { list -> list.map { it.toDomain() } }.flowOn(io)

    override suspend fun appendTrackPoints(tripId: Long, points: List<TrackPoint>) {
        if (points.isNotEmpty()) this.points.insertAll(points.map { it.toEntity(tripId) })
    }

    private fun Flow<List<TripEntity>>.toTrips(): Flow<List<Trip>> =
        map { list -> list.map { it.toDomain() } }.flowOn(io)
}
