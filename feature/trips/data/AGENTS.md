# feature:trips:data

Implements the trips domain repositories on Room and DataStore. Package `com.aijones.mileage.feature.trips.data` (all classes `internal`).

## Public API (through Hilt only)
`TripsDataModule` binds:
- `TripRepository` → `RoomTripRepository` (`TripDao` + `TrackPointDao`). Instant ↔ epoch ms, `Distance` ↔ meters, `Money` ↔ micros, `GeoPoint` ↔ lat/lng columns, enums ↔ names. Lists are newest first and only include `DONE`/`MANUAL` trips. Track points are batch-inserted with one `insertAll`; deleting a trip deletes its points.
- `VehicleRepository` → `RoomVehicleRepository` (`VehicleDao`). The first vehicle added becomes the default.
- `MileageRateRepository` → `BundledMileageRateRepository`. IRS business rates are the constant `IRS_BUSINESS_RATES` (2025: 70¢ Notice 2025-5; 2026 H1: 72.5¢ Notice 2026-10; 2026 H2: 76¢ Announcement 2026-11). Add a row here when the IRS publishes a new rate. The custom rate lives in the `@SettingsStore` DataStore under `trips_custom_rate_micros` (absent = use IRS).

Flows are mapped on the injected `@IoDispatcher`.

## Owned DAO queries (`core:database`)
`TripDao`, `TrackPointDao`, `VehicleDao` (`rename`, `count` added for this module). `observeNeedingReview` is newest first, matching the domain contract.

## Dependencies
`feature:trips:domain`, `core:database`, `core:datastore`, `core:common`, `core:model`.

## Tests
Robolectric (`@Config(sdk = [35])`): `RoomRepositoriesTest` (in-memory `MileageDatabase`: every Trip field round-trips, ordering, live review list, batched points + delete, vehicle default/rename) and `BundledMileageRateRepositoryTest` (rate edges Jun 30 / Jul 1, custom rate set/clear on a temp DataStore).
