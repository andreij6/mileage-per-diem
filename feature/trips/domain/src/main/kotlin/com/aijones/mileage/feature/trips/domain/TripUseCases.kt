package com.aijones.mileage.feature.trips.domain

import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.model.Distance
import com.aijones.mileage.core.model.sum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class ObserveRecentTripsUseCase @Inject constructor(private val trips: TripRepository) {
    operator fun invoke(limit: Int = 20): Flow<List<Trip>> = trips.observeRecent(limit)
}

class ObserveTripsUseCase @Inject constructor(private val trips: TripRepository, private val clock: Clock) {
    operator fun invoke(from: LocalDate, toInclusive: LocalDate): Flow<List<Trip>> =
        trips.observeBetween(from.atStartOfDay(clock.zone).toInstant(), toInclusive.plusDays(1).atStartOfDay(clock.zone).toInstant())
}

class ObserveMonthSummaryUseCase @Inject constructor(
    private val observeTrips: ObserveTripsUseCase,
    private val observeCurrentRate: ObserveCurrentRateUseCase,
) {
    operator fun invoke(month: YearMonth): Flow<MonthSummary> =
        combine(observeTrips(month.atDay(1), month.atEndOfMonth()), observeCurrentRate()) { trips, rate ->
            val business = trips.filter { it.isReimbursable }
            MonthSummary(
                month = month,
                reimbursement = business.map { it.reimbursement }.sum(),
                distance = business.fold(Distance.ZERO) { acc, t -> acc + t.distance },
                trips = business.size,
                currentRate = rate,
            )
        }
}

class ObserveTripUseCase @Inject constructor(private val trips: TripRepository) {
    operator fun invoke(id: Long): Flow<Trip?> = trips.observe(id)
}

class ObserveTrackUseCase @Inject constructor(private val trips: TripRepository) {
    operator fun invoke(tripId: Long): Flow<List<TrackPoint>> = trips.observeTrackPoints(tripId)
}

class UpdateTripDetailsUseCase @Inject constructor(private val trips: TripRepository, private val analytics: Analytics) {
    suspend operator fun invoke(
        tripId: Long,
        purpose: String?,
        category: TripCategory,
        vehicleId: Long?,
        distance: Distance,
        notes: String?,
    ): Trip? {
        val trip = trips.get(tripId) ?: return null
        val cleanPurpose = purpose?.trim()?.ifEmpty { null }
        val cleanNotes = notes?.trim()?.ifEmpty { null }
        val distanceChanged = distance.tenthsOfMile != trip.distance.tenthsOfMile
        val fields = buildList {
            if (cleanPurpose != trip.purpose) add("purpose")
            if (category != trip.category) add("category")
            if (vehicleId != trip.vehicleId) add("vehicle")
            if (distanceChanged) add("distance")
            if (cleanNotes != trip.notes) add("notes")
        }
        if (fields.isEmpty()) return trip
        val updated = trip.copy(
            purpose = cleanPurpose,
            category = category,
            vehicleId = vehicleId,
            distance = if (distanceChanged) distance else trip.distance,
            notes = cleanNotes,
            edited = trip.edited || distanceChanged,
            needsReview = false,
        )
        trips.update(updated)
        analytics.track(AnalyticsEvent.TripEdited(fields))
        return updated
    }
}

class DeleteTripUseCase @Inject constructor(private val trips: TripRepository, private val analytics: Analytics) {
    suspend operator fun invoke(tripId: Long) {
        trips.delete(tripId)
        analytics.track(AnalyticsEvent.TripDeleted)
    }
}

class AddManualTripUseCase @Inject constructor(
    private val trips: TripRepository,
    private val getMileageRate: GetMileageRateUseCase,
    private val analytics: Analytics,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        date: LocalDate,
        from: String,
        to: String,
        distance: Distance,
        roundTrip: Boolean,
        purpose: String?,
        category: TripCategory = TripCategory.BUSINESS,
        vehicleId: Long? = null,
    ): Long {
        require(distance > Distance.ZERO) { "distance must be positive" }
        val total = if (roundTrip) distance * 2 else distance
        val id = trips.insert(
            Trip(
                startedAt = date.atStartOfDay(clock.zone).toInstant(),
                status = TripStatus.MANUAL,
                distance = total,
                purpose = purpose?.trim()?.ifEmpty { null },
                category = category,
                vehicleId = vehicleId,
                ratePerMile = getMileageRate(date),
                startLabel = from.trim().ifEmpty { null },
                endLabel = to.trim().ifEmpty { null },
                roundTrip = roundTrip,
                source = "manual",
            ),
        )
        analytics.track(AnalyticsEvent.TripAddedManual(total.tenthsOfMile))
        return id
    }
}

class ObserveInterruptedTripUseCase @Inject constructor(private val trips: TripRepository) {
    operator fun invoke(isRecordingLive: Flow<Boolean>): Flow<Trip?> =
        combine(trips.observeActive(), isRecordingLive) { active, live -> active.takeUnless { live } }
}

class ObserveTripsNeedingReviewUseCase @Inject constructor(private val trips: TripRepository) {
    operator fun invoke(): Flow<List<Trip>> = trips.observeNeedingReview().map { list -> list.filter { it.isFinished } }
}

class ClassifyTripUseCase @Inject constructor(private val trips: TripRepository, private val analytics: Analytics) {
    suspend operator fun invoke(tripId: Long, category: TripCategory, purpose: String?, via: String): Trip? {
        val previous = trips.get(tripId) ?: return null
        trips.update(
            previous.copy(
                category = category,
                purpose = purpose?.trim()?.ifEmpty { null } ?: previous.purpose,
                needsReview = false,
            ),
        )
        analytics.track(AnalyticsEvent.TripClassified(category.name.lowercase(), via))
        return previous
    }
}

class UndoClassificationUseCase @Inject constructor(private val trips: TripRepository) {
    suspend operator fun invoke(previous: Trip) = trips.update(previous)
}
