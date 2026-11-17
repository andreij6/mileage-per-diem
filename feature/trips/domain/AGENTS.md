# feature:trips:domain

Pure Kotlin. Trips, track points, vehicles and effective-dated mileage rates. Package `com.aijones.mileage.feature.trips.domain` (flat).

## Public API
- Models: `Trip` (`reimbursement`, `duration`, `isActive`, `isFinished`), `TripStatus`, `TripCategory`, `TrackPoint`, `Vehicle`, `MileageRate`, `MonthSummary`, `SpeedProfile` (`from(points, buckets)`, `sampleAt(fraction)`), `SpeedSample`, `VehicleAction`, `irsRateOn(table, date)`.
- Interfaces (implemented in `trips:data`): `TripRepository` (lists are newest first; `observeBetween` is `[from, to)`), `VehicleRepository`, `MileageRateRepository`.

## Use cases
`ObserveRecentTripsUseCase`, `ObserveTripsUseCase(from, toInclusive: LocalDate)` (local days in the clock's zone), `ObserveMonthSummaryUseCase` (finished business trips), `ObserveTripUseCase`, `ObserveTrackUseCase`, `UpdateTripDetailsUseCase` (distance change sets `edited`; tracks `TripEdited`), `DeleteTripUseCase`, `AddManualTripUseCase` (round trip doubles distance, snapshots the rate), `GetMileageRateUseCase` (custom rate beats IRS), `ObserveCurrentRateUseCase`, `ObserveInterruptedTripUseCase(isRecordingLive)`, `ObserveTripsNeedingReviewUseCase`, `ClassifyTripUseCase` (returns the previous trip), `UndoClassificationUseCase`, `ObserveVehiclesUseCase`, `ManageVehiclesUseCase(VehicleAction)`.

## Dependencies
`core:model`, `core:common`, `core:analytics`, `core:featureflags`. No feature modules.

## Tests
Fakes in `src/testFixtures`: `FakeTripRepository`, `FakeVehicleRepository`, `FakeMileageRateRepository`, `SampleRates.irs2026` (72.5¢ to Jun 30 2026, 76¢ from Jul 1 2026). Cover rate edges (Jun 30 / Jul 1, custom override), round trips, analytics events and DST-safe local-day ranges.
