package com.aijones.mileage.feature.trips.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant

class FakeTripRepository(initial: List<Trip> = emptyList()) : TripRepository {
    private val tripsState = MutableStateFlow(initial.associateBy { it.id })
    private val pointsState = MutableStateFlow<Map<Long, List<TrackPoint>>>(emptyMap())
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    val trips: List<Trip> get() = tripsState.value.values.sortedByDescending { it.startedAt }

    private fun newestFirst(trips: Collection<Trip>) = trips.sortedByDescending { it.startedAt }

    override fun observeBetween(from: Instant, to: Instant): Flow<List<Trip>> =
        tripsState.map { m -> newestFirst(m.values.filter { !it.startedAt.isBefore(from) && it.startedAt.isBefore(to) }) }

    override fun observeRecent(limit: Int): Flow<List<Trip>> = tripsState.map { m -> newestFirst(m.values).take(limit) }

    override fun observe(id: Long): Flow<Trip?> = tripsState.map { it[id] }

    override suspend fun get(id: Long): Trip? = tripsState.value[id]

    override suspend fun insert(trip: Trip): Long {
        val id = if (trip.id == 0L) nextId++ else trip.id
        tripsState.update { it + (id to trip.copy(id = id)) }
        return id
    }

    override suspend fun update(trip: Trip) {
        tripsState.update { if (trip.id in it) it + (trip.id to trip) else it }
    }

    override suspend fun delete(id: Long) {
        tripsState.update { it - id }
        pointsState.update { it - id }
    }

    override suspend fun getActive(): Trip? = tripsState.value.values.firstOrNull { it.isActive }

    override fun observeActive(): Flow<Trip?> = tripsState.map { m -> m.values.firstOrNull { it.isActive } }

    override fun observeNeedingReview(): Flow<List<Trip>> = tripsState.map { m -> newestFirst(m.values.filter { it.needsReview }) }

    override suspend fun trackPoints(id: Long): List<TrackPoint> = pointsState.value[id].orEmpty()

    override fun observeTrackPoints(id: Long): Flow<List<TrackPoint>> = pointsState.map { it[id].orEmpty() }

    override suspend fun appendTrackPoints(tripId: Long, points: List<TrackPoint>) {
        pointsState.update { it + (tripId to (it[tripId].orEmpty() + points).sortedBy { p -> p.t }) }
    }
}
