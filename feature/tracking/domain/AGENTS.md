# feature:tracking:domain

Pure Kotlin. Trip recording: start/pause/resume/stop, fix filtering, track simplification, battery policy and finalizing a trip. Package `com.aijones.mileage.feature.tracking.domain`.

## Public API
- Models: `Fix` (`toFix()`/`toTrackPoint()`), `RecordingPolicy` (`HIGH` 2 s/5 m, `BALANCED` 15 s/25 m, `SAVER` 10 s/20 m), `DeviceState`, `RecordingState` (`Idle`, `Recording`, `Paused`).
- Interfaces (implemented in `tracking:data`): `LocationTracker`, `RecordingController` (the foreground service), `RecordingStateStore` (singleton), `DeviceStateMonitor`, `PlaceLabeler`.
- Pure: `DistanceFilter` (drops accuracy > 35 m, implied speed > 70 m/s, stationary jitter inside the accuracy radius; exposes `total`, `accepted`, `dropped`), `TrackSimplifier` (Douglas–Peucker, 5 m), `RecordingPolicyResolver` (ACCURATE → high; still > 2 min → balanced; SAVER/power-save/≤ 20% → saver; else high).

## Use cases
`StartTripUseCase` (returns the active trip id if one exists), `PauseTripUseCase`, `ResumeTripUseCase`, `StopTripUseCase` (returns the trip id to finalize), `FinalizeTripUseCase(tripId, interrupted)` (idempotent; interrupted trips end at their last point), `RecordFailureUseCase` (drops an active trip that has no points), `ObserveRecordingStateUseCase`, `ObserveRecordingPolicyUseCase`, `ObserveDriveDetectionEnabledUseCase`, `ObserveLiveUpdateEnabledUseCase`.

## Dependencies
`core:*` JVM modules, `feature:trips:domain`, `feature:settings:domain` (api).

## Tests
Fakes in `src/testFixtures`: `FakeLocationTracker`, `FakeRecordingController`, `FakeRecordingStateStore`, `FakeDeviceStateMonitor`, `FakePlaceLabeler`. Cover jitter/teleport/low accuracy, simplifier distance within 0.5%, refused second start, zero-point finalize, labeler failure, policy changes tracked once each.
