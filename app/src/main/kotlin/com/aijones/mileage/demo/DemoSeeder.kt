package com.aijones.mileage.demo

import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.onboarding.domain.OnboardingRepository
import com.aijones.mileage.feature.onboarding.domain.OnboardingStep
import com.aijones.mileage.feature.perdiem.domain.Stay
import com.aijones.mileage.feature.perdiem.domain.StayRepository
import com.aijones.mileage.feature.settings.domain.SettingsRepository
import com.aijones.mileage.feature.trips.domain.GetMileageRateUseCase
import com.aijones.mileage.feature.trips.domain.TrackPoint
import com.aijones.mileage.feature.trips.domain.Trip
import com.aijones.mileage.feature.trips.domain.TripCategory
import com.aijones.mileage.feature.trips.domain.TripRepository
import com.aijones.mileage.feature.trips.domain.TripStatus
import com.aijones.mileage.feature.trips.domain.VehicleRepository
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class DemoSeeder @Inject constructor(
    private val trips: TripRepository,
    private val vehicles: VehicleRepository,
    private val stays: StayRepository,
    private val settings: SettingsRepository,
    private val onboarding: OnboardingRepository,
    private val rate: GetMileageRateUseCase,
) {
    suspend fun seedIfEmpty() {
        OnboardingStep.entries.forEach { onboarding.complete(it) }
        if (trips.observeRecent(1).first().isNotEmpty()) return
        settings.update {
            it.copy(defaultPurpose = "Client meeting", reportName = "Jordan Lee", company = "Acme Corp", employeeId = "4471", driveDetection = true)
        }
        val civic = vehicles.add("2021 Civic", "Personal")
        vehicles.add("Rental", "Rental")
        vehicles.setDefault(civic)
        (DemoTrips.earlierThisYear() + DemoTrips.november()).forEach { demo ->
            val track = DemoTrips.track(demo)
            val start = demo.start.toInstant()
            val id = trips.insert(
                Trip(
                    startedAt = start,
                    endedAt = start.plusSeconds(demo.minutes * 60),
                    status = TripStatus.DONE,
                    distance = DemoTrips.distance(demo),
                    recordedDistance = DemoTrips.distance(demo),
                    purpose = demo.purpose,
                    category = TripCategory.BUSINESS,
                    vehicleId = civic,
                    ratePerMile = rate(demo.start.toLocalDate()),
                    startLabel = demo.from.label,
                    endLabel = demo.to.label,
                    start = demo.from.point,
                    end = demo.to.point,
                    needsReview = demo.needsReview,
                ),
            )
            trips.appendTrackPoints(id, track.mapIndexed { i, (p, t) -> TrackPoint(Instant.ofEpochMilli(t), p, 5f, speedFor(i)) })
        }
        stays.add(stay(114, "Boston / Cambridge, MA", "2026-11-04", "2026-11-07", 32_200))
        stays.add(stay(202, "New York City, NY", "2026-11-18", "2026-11-25", 69_000))
    }

    private fun speedFor(i: Int) = listOf(0f, 4f, 10f, 14f, 12f, 5f, 2f, 8f, 15f, 18f, 17f, 16f, 10f, 4f, 8f, 13f, 15f, 12f, 6f, 1f)[i % 20]

    private fun stay(locationId: Long, label: String, checkIn: String, checkOut: String, mieCents: Long) = Stay(
        locationId = locationId,
        locationLabel = label,
        fiscalYear = 2027,
        checkIn = LocalDate.parse(checkIn),
        checkOut = LocalDate.parse(checkOut),
        includeLodging = false,
        mieDaily = Money.ofDollars(92),
        mieTotal = Money.ofCents(mieCents),
        lodgingTotal = Money.ZERO,
    )
}
