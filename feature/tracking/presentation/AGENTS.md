# feature:tracking:presentation

Live trip, trip receipt and the interrupted-trip sheet. Package `com.aijones.mileage.feature.tracking.presentation`.

## Screens and design frames
- `LiveTripRoute(key, navigator)` → `LiveTripScreen` (`design/renders/tracking/live-*`, motion `motion/motion_live-*`). With a `startSource` and no active trip it runs the permission flow (fine+coarse → POST_NOTIFICATIONS on 33+ → `SettingsClient` resolution), then `StartTripUseCase`. Approximate only → `PreciseLocationOffRoute`; denied → back; location off → `trip_recording_failed(location_off)` and back. Route canvas, odometer, money, elapsed/speed, purpose and battery-policy chips, pause/resume, `SlideToEnd`. Stats are one polite live region refreshed at most every 30 s.
- `SlideToEnd`: `AnchoredDraggable` with two anchors, settles past 85%, haptic tick at 50% and CONFIRM at the end, label fades with progress, custom action "End trip" and long-press on the thumb.
- `TripReceiptRoute(key, navigator)` → `TripReceiptScreen` (`tracking/receipt-*`): zigzag `GenericShape` paper, perforations, barcode from the trip id, prints in with a spring clip reveal (none with reduced motion). Shows once the finalize worker has marked the trip done. Edit details → `EditTripRoute`, Done → `switchTab(TripsRoute)`.
- `TrackingOverlays(navigator)` → `InterruptedTripSheet` (`trips/interrupted-*`): shown for an active trip when nothing has recorded in this process. Resume (`ResumeInterruptedTripUseCase` → `LiveTripRoute()`), Finish (`FinalizeTripUseCase(interrupted = true)` → receipt), Discard (`DeleteTripUseCase`).
- `trackingEntries(navigator)` registers `LiveTripRoute` and `TripReceiptRoute`.

## Dependencies
`feature:tracking:domain` (with trips and settings domains via api), core UI modules, activity-compose (permission launchers), play-services-location (`SettingsClient` only).

## Tests
ViewModel tests with real use cases on fakes (`TrackingTestGraph`): permission outcomes, start → record → stop → receipt, pause/resume, external stop, receipt mapping and rate label, interrupted resume/finish/discard/dismiss. Robolectric Compose tests: slide-to-end custom action and swipe, live stats, receipt content, interrupted sheet buttons.
