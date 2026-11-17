# feature:trips:presentation

Trips UI: home tab, review deck, all trips, trip detail with route replay, edit trip and add a past trip. Package `com.aijones.mileage.feature.trips.presentation`.

## Public API
`EntryProviderScope<NavKey>.tripsEntries(navigator)` registers `TripsRoute`, `AllTripsRoute`, `TripDetailRoute`, `EditTripRoute`, `AddTripRoute`, `ReviewTripsRoute`. Stateless screens (`HomeScreen`, `ReviewScreen`, `TripListScreen`, `TripDetailScreen`, `EditTripScreen`, `AddTripScreen`) are public for previews and tests.

## Screens → design frames (`design/renders/trips/`)
| Key | Route composable / ViewModel | Frames | Notes |
|---|---|---|---|
| `TripsRoute` | `HomeRoute` / `HomeViewModel` | `home-*`, `home_empty-*` | Hero (odometer of month business miles), review banner, 4 recent trips. `ReportDrawnWhen { !loading }`. Start trip → `LiveTripRoute("app")`, Insights → `InsightsRoute`. |
| `ReviewTripsRoute` | `ReviewRoute` / `ReviewViewModel` | `review-*` | Swipe deck (rotation = offset dp / 18, stamp alpha follows drag, 40% or fling classifies via `"swipe"`), buttons via `"button"`, custom a11y actions "Mark business"/"Mark personal", undo stack, done state. Progress is a polite live region. |
| `AllTripsRoute` | `TripListRoute` / `TripListViewModel` | `trips_all-*` | Current month grouped by local day; Business toggle, vehicle chip cycles vehicles, search filters place/purpose; FAB → `AddTripRoute`. |
| `TripDetailRoute` | `TripDetailEntry` / `TripDetailViewModel` (assisted) | `detail-*` | `RouteCanvas` 200 dp. With `ROUTE_REPLAY` on: speed sparkline (`SpeedProfile`, slider semantics), play at 60×, marker placed by time (`routeFractionAt`), time/speed chip. Off: no replay row or chip; map shows start/end labels. Overflow: Edit, Delete. "Add to report" → `switchTab(ReportsRoute)`. |
| `EditTripRoute` | `EditTripEntry` / `EditTripViewModel` (assisted) | `trip_edit-*` | Purpose, category `SegmentedControl`, vehicle menu, distance with helper, notes, delete with confirm. SaveBar shows the change count. |
| `AddTripRoute` | `AddTripEntry` / `AddTripViewModel` | `trip_add-*` | Material date picker (no future dates), live estimate at the rate for the chosen date, round trip doubles. |

## Rules followed
ViewModels call trips use cases only (plus `Analytics` for `ScreenView` and the injected `Clock`). One `StateFlow` per screen via `stateIn(WhileSubscribed(5_000))`. Navigation/finish events are state (`missing`, `done`, `added`) consumed by the route. Edit/delete/classify/add analytics are tracked by the use cases, not here. Copy is in `res/values/strings.xml`.

## Dependencies
`feature:trips:domain`, `core:designsystem`, `core:ui`, `core:navigation`, `core:model`, `core:analytics`, `core:featureflags` (flag read through `ObserveRouteReplayEnabledUseCase`), `androidx.activity:activity-compose` (`ReportDrawnWhen`).

## Tests
JVM ViewModel tests with real use cases on the domain fakes, `FakeAnalytics`, `FakeFeatureFlags`, `FakeClock` (Turbine): `HomeViewModelTest`, `ReviewViewModelTest`, `TripListViewModelTest`, `TripDetailViewModelTest` (flag on/off/live toggle, scrub, delete, `routeFractionAt`), `EditTripViewModelTest`, `AddTripViewModelTest`. Robolectric Compose: `TripsScreensUiTest` (home values + navigation, empty state, review button + custom action + undo, detail with replay flag on/off). Routes take an optional `viewModel` so tests can pass one built from fakes.
