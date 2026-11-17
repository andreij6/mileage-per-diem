# feature:widgets:presentation

Jetpack Glance home-screen widgets and the "Start trip" Quick Settings tile. Presentation only: reads the trips, tracking and perdiem domains through use cases. Package `com.aijones.mileage.feature.widgets.presentation`.

## Design frames
`design/renders/widgets/widgets-{light,dark}.png`.

## Public API
- Widgets (receivers declared in this module's manifest, providers in `res/xml/widget_*_info.xml` with `previewLayout` (API 31+, `res/layout-v31`) and `description`):
  - `MonthWidget` / `MonthWidgetReceiver` (2×2): month name, reimbursement, odometer digits with the tenth highlighted, "Start trip" pill.
  - `PerDiemWidget` / `PerDiemWidgetReceiver` (2×2, `primaryContainer`): today's M&IE, city, lodging per night for the stay covering today, else the most recent searched city.
  - `RecordingWidget` / `RecordingWidgetReceiver` (4×1): "Recording · 12.6 mi", "24:18 · $9.58 so far", round stop button (`StopTripAction` → `StopTripUseCase`); idle shows "Not recording" and a "Start trip" pill.
- `StartTripTileService`: label "Start trip", subtitle "Mileage", `STATE_ACTIVE` while recording or paused. Click opens `MainActivity` (`ACTION_START_TRIP`, source "tile"; while recording `ACTION_OPEN_LIVE_TRIP`) with `startActivityAndCollapse(PendingIntent)` on API 34+.
- Start actions open `MainActivity` with `Deeplinks.ACTION_START_TRIP`, `EXTRA_SOURCE = "widget"` and extra `widget` = `month` / `recording`; the activity starts the FGS (user-initiated).
- `WidgetUpdater` (`@Singleton`): `start()` (idempotent) collects `ObserveWidgetSnapshotUseCase` in `@ApplicationScope`, throttled to one update per 30 s while recording (start/stop pass at once), and calls `updateAll` only for widgets whose part changed; asks the tile to refresh when recording starts or stops. **The app must call `start()` after the first frame.**
- `ObserveWidgetSnapshotUseCase`: the widget facade (month summary, current rate, recording state, stays, recent searches, rates).
- `WidgetEntryPoint` (Hilt `@EntryPoint`): `Analytics`, `StopTripUseCase`, `ObserveRecordingStateUseCase`, `WidgetUpdater` for Glance callbacks and the tile.
- Colours: `GlanceTheme` with `ColorProviders` built from `LightColors` / `DarkColors`.
- Analytics: `WidgetAction("recording", "stop")` and `WidgetAction("tile", "start_trip" | "open")`.

## Pure mappers (unit tested on the JVM)
`MonthSummary.toWidgetUi`, `PerDiemRates.toWidgetUi`, `RecordingState.toWidgetUi(rate)`, `RecordingState.tileState()`, `Duration.formatElapsed`, `Money.formatUsdShort`, `Flow<WidgetSnapshot>.throttleWhileRecording`.

## Dependencies
`feature:trips:domain`, `feature:tracking:domain`, `feature:perdiem:domain`, `core:analytics`, `core:designsystem`, `core:navigation`, `core:common`, Glance appwidget + material3.

## Tests
`WidgetMappingTest`, `ObserveWidgetSnapshotUseCaseTest` (fakes from the domain test fixtures), `ThrottleTest` (virtual time).
