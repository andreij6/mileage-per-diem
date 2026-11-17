# feature:tracking:data

Android implementations of the tracking domain: GPS, the recording foreground service, battery state, place labels and drive detection. Package `com.aijones.mileage.feature.tracking.data`.

## Public API
- Bound in `TrackingDataModule`: `FusedLocationTracker` (LocationTracker, callbackFlow over `requestLocationUpdates`), `ServiceRecordingController` (RecordingController, intents to the service), `InMemoryRecordingStateStore` (@Singleton), `AndroidDeviceStateMonitor` (sticky `ACTION_BATTERY_CHANGED` + power-save broadcasts), `GeocoderPlaceLabeler` (short labels such as "Acme HQ, Midtown", null on failure/offline).
- `TripRecorderService`: `LifecycleService`, `foregroundServiceType="location"`, not exported, `START_NOT_STICKY`. Calls `startForeground(..., FOREGROUND_SERVICE_TYPE_LOCATION)` on start/resume, runs `RecordingSession`, updates the notification at most every 5 s, and on stop flushes points, enqueues `FinalizeTripWorker` (unique per trip) and stops itself at once. `SecurityException` → `RecordFailureUseCase(PERMISSION_REVOKED)`, interrupted finalize and a "Trip stopped" notification.
- `RecordingSession`: the recording pipeline as a plain class. Policy → `flatMapLatest` fixes → `DistanceFilter` → state store; raw fixes are buffered and written every 10 points or 15 s; elapsed ticks each second; resumes continue distance from stored points.
- `TrackingNotifications`: channels `recording` (low) and `drive` (default); recording, drive-detected and stopped notifications. Live Update (promoted ongoing + short critical text) on API 36+ when the `live_update` flag is on.
- `DriveDetectionRegistrar` + `DriveDetectedReceiver`: Activity Recognition IN_VEHICLE enter → "Drive detected" notification (Start trip opens the app with `Deeplinks.ACTION_START_TRIP`, source `notification`; Not business dismisses). Registration follows `ObserveDriveDetectionEnabledUseCase` and is skipped without the ACTIVITY_RECOGNITION permission.
- `TrackingInitializer.start()`: idempotent; creates channels and starts the registrar on the application scope. The app calls it after the first frame.

## Manifest
FOREGROUND_SERVICE, FOREGROUND_SERVICE_LOCATION, ACCESS_FINE/COARSE_LOCATION, POST_NOTIFICATIONS, POST_PROMOTED_NOTIFICATIONS, ACTIVITY_RECOGNITION (+ the gms permission up to API 28). Never ACCESS_BACKGROUND_LOCATION or INTERNET (ADR 0007).

## Dependencies
`feature:tracking:domain`, `core:model`, `core:common`, `core:analytics`, `core:navigation` (deeplink constants), play-services-location, lifecycle-service, WorkManager + hilt-work.

## Tests
`RecordingSessionTest` (batching, 15 s flush, resume continuity, dropped fixes, state store, short labels) with domain/trips/settings fakes; `AndroidTrackingTest` (Robolectric: notification content and channels, device monitor mapping).
