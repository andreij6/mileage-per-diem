# Mileage & Per Diem: architecture

By **AI.Jones**. Status: **in development**. No code is written yet. Designs: `design/mileage-per-diem.figcraft` (renders in `design/renders/`).

Logs business drives with GPS and costs them at the IRS standard mileage rate. Looks up GSA meal and lodging per diems, bundled in the app. Exports a PDF or CSV expense report. There is no login, no backend, no analytics, no map SDK and no AI, and the app **does not request `INTERNET`**: everything stays on the phone.

Changes in v4: on-device analytics and app-health dashboards, a feature-flag system with Developer options, and behaviour tests with fakes. See `ANALYTICS.md`.

Changes in v3: a UI-craft pass (§6a): odometer readouts, slide to end a trip, a printed trip receipt, route replay, a swipe deck to sort trips, monthly insights, a shareable "2026 on the road" story, home-screen widgets and a Quick Settings tile, plus a motion spec page. Card Controls Studio is paused.

Changes in v2: feature modules split into clean-architecture layers, use cases, Hilt confirmed, the map SDK replaced by a route canvas drawn in-app, on-device AI removed, and the conventions in §11 added.

## 1. Constraints that shape the design

| Constraint | Consequence |
|---|---|
| No backend, no account | Room + DataStore are the source of truth. Reference data (IRS and GSA rates) ships inside the APK and updates with releases. |
| Play location policy (`../docs/PLAY_STORE_PUBLISHING.md`, steps 6–7) | **No `ACCESS_BACKGROUND_LOCATION`.** The user always starts a trip. Drive detection only *suggests* one with a notification, and the trip starts from the visible app. Recording runs in a `foregroundServiceType="location"` service. |
| Rates change, including mid-year | IRS: 72.5¢ from Jan 1, 2026, then **76¢ from Jul 1, 2026** ([IRB 2026-29](https://www.irs.gov/irb/2026-29_irb)). Rates are keyed by effective date, and each trip **snapshots** its rate when saved. GSA rates are keyed by fiscal year (FY2027 started Oct 1, 2026). |
| No map SDK | Routes are drawn by the app's own `RouteCanvas` composable (projected track on a dot grid, with start/end pins, labels and a scale bar). No API key, no tiles, works fully offline, and nothing for the Data safety form. |
| Battery | Location accuracy adapts to motion, battery level and power-save mode. Points are batched into Room. Nothing runs while no trip is recording. |

## 2. Tech stack (latest stable at implementation time, pinned in `libs.versions.toml`)

| Concern | Choice |
|---|---|
| Language / build | Kotlin 2.x (K2), AGP 9 with built-in Kotlin, Gradle Kotlin DSL, version catalog, `build-logic` convention plugins (included build), KSP2, configuration cache + build cache |
| UI | Jetpack Compose (BOM), Material 3, edge-to-edge, predictive back, splash screen API, adaptive layouts (`NavigationSuiteScaffold`, Material 3 adaptive) |
| Navigation | **Navigation 3**: `@Serializable` `NavKey`s in `core:navigation`. Each feature contributes `entryProvider` entries, and the app owns the back stack. |
| Presentation | MVVM + UDF: `@HiltViewModel` exposes one `StateFlow<UiState>` (`stateIn(WhileSubscribed(5_000))`), takes intents as functions, and keeps one-off effects as consumable state. The UI collects with `collectAsStateWithLifecycle`. |
| Domain | **Use cases** (`operator fun invoke`), pure Kotlin. ViewModels call use cases only, never repositories. |
| Async | Coroutines + Flow; dispatchers injected with qualifiers (`@IoDispatcher`), and time through `java.time.Clock` |
| DI | **Hilt** everywhere: `@HiltViewModel`, `@AndroidEntryPoint` service and receiver, `@HiltWorker`. Domain modules use only `javax.inject.Inject`, so they stay plain JVM. |
| Persistence | Room (one `MileageDatabase`, plus a read-only prepackaged `perdiem.db`), Preferences DataStore |
| Location | `FusedLocationProviderClient`; Activity Recognition Transition API for optional drive detection |
| Background | Foreground service (type `location`) for recording; WorkManager for finishing a trip |
| Export | `android.graphics.pdf.PdfDocument`; an RFC 4180 CSV writer; `FileProvider` share sheet and SAF "Save to Files" |
| SDK levels | `minSdk 26`, `targetSdk 36`, `compileSdk` latest; R8 full mode, Baseline Profiles, StrictMode and LeakCanary in debug |

## 3. Modules: by feature, layered inside each feature

```
mileage-per-diem/
├─ app/                              Application (@HiltAndroidApp), MainActivity, NavDisplay + back stack, manifest
├─ build-logic/convention/           aijones.android.application · .android.library · .compose · .hilt · .room
│                                    .jvm.domain · .feature.presentation · .feature.data · .test
├─ core/
│  ├─ model/          JVM            Money, Distance, Miles, LocalDateRange, shared value types
│  ├─ common/         JVM            dispatcher qualifiers, Clock binding, Result helpers
│  ├─ database/       Android        MileageDatabase, entities, DAOs, migrations, schema JSON
│  ├─ datastore/      Android        settings DataStore + serializer
│  ├─ designsystem/   Android        theme (light/dark), type, tokens, components
│  ├─ ui/             Android        shared composables: RouteCanvas, MoneyText, StatCard
│  ├─ navigation/     Android        NavKey definitions for every destination
│  ├─ analytics/      JVM            AnalyticsEvent catalog, Analytics + EventLog interfaces, FakeAnalytics (testFixtures)
│  ├─ analytics-impl/ Android        LocalAnalytics (batched Room ring buffer), Logcat sink, CrashRecorder
│  ├─ featureflags/   JVM            FeatureFlag enum, FeatureFlags interface, FakeFeatureFlags (testFixtures)
│  ├─ featureflags-impl/ Android     DataStore overrides → defaults, exposure tracking
│  └─ testing/        JVM            MainDispatcherRule, Turbine, coroutines-test
├─ feature/
│  ├─ onboarding/   domain · data · presentation     disclosure + permission flow
│  ├─ trips/        domain · data · presentation     home, trip list, trip detail, rates, manual trips
│  ├─ tracking/     domain · data · presentation     recording service, location, drive detection, live trip
│  ├─ perdiem/      domain · data · presentation     city search, rates, stays, estimates
│  ├─ reports/      domain · data · presentation     build report, PDF/CSV export
│  ├─ insights/     domain · data · presentation     month heatmap, highlights, year in review, share-card image
│  ├─ widgets/      presentation                     Glance widgets + Quick Settings TileService
│  └─ settings/     domain · data · presentation     vehicles, custom rate, battery mode, report details
└─ tools/perdiem-import/             JVM CLI, build time only: GSA API (OpenAPI client) → perdiem.db asset
```

### Dependency rules (enforced in CI with a module-graph assertion plugin)
- `feature:x:presentation` → `feature:x:domain`, `core:designsystem`, `core:ui`, `core:navigation`, `core:model`. It never depends on any `data` module or on another feature's `presentation`.
- `feature:x:domain` is **pure Kotlin**. It depends on `core:model` and `core:common`, and may use other features' `domain` modules **without cycles**: `settings → trips`, `tracking → trips + settings`, `reports → trips + perdiem + settings`, `insights → trips + perdiem`. `widgets:presentation` uses the `trips` and `tracking` domains through its own small use-case facade, because a widget has no ViewModel.
- `feature:x:data` → `feature:x:domain`, `core:database` or `core:datastore`, `core:common`. It implements the domain's repository interfaces and binds them with Hilt `@Binds`.
- `app` → every `presentation` and `data` module (to assemble the Hilt graph) plus `core:navigation`.
- A feature gets a `data` module only if it owns data; no empty layers.

### Use cases and repositories per feature

| Feature | Use cases | Interfaces in `domain` (implemented in `data`) |
|---|---|---|
| onboarding | `ObserveOnboardingStateUseCase`, `CompleteOnboardingStepUseCase` | `OnboardingRepository` |
| trips | `ObserveTripsUseCase`, `ObserveMonthSummaryUseCase`, `GetTripUseCase`, `UpdateTripDetailsUseCase`, `DeleteTripUseCase`, `AddManualTripUseCase`, `GetMileageRateUseCase`, `CalculateReimbursementUseCase`, `ObserveInterruptedTripUseCase` | `TripRepository`, `MileageRateRepository` |
| tracking | `StartTripUseCase`, `PauseTripUseCase`, `ResumeTripUseCase`, `StopTripUseCase`, `ObserveRecordingStateUseCase`, `ObserveRecordingPolicyUseCase`, `AcceptFixUseCase`, `FinalizeTripUseCase`, `SetDriveDetectionUseCase` | `TrackRepository`, `LocationTracker`, `RecordingController`, `DeviceStateMonitor`, `PlaceLabeler` |
| perdiem | `SearchLocationsUseCase`, `GetRatesForLocationUseCase`, `EstimateStayUseCase`, `AddStayUseCase`, `ObserveStaysUseCase` | `PerDiemRateRepository`, `StayRepository` |
| reports | `BuildReportUseCase`, `ExportReportUseCase(format)` | `ReportFileWriter` |
| trips (review) | `ObserveTripsNeedingReviewUseCase`, `ClassifyTripUseCase`, `UndoClassificationUseCase` | `TripRepository` |
| insights | `ObserveMonthInsightsUseCase` (heatmap, highlights, weekday totals), `BuildYearInReviewUseCase`, `ExportShareCardUseCase` | `ShareCardWriter` |
| widgets | `ObserveWidgetSnapshotUseCase` (month total, odometer, today's per diem, recording state) | uses `trips`, `perdiem` and `tracking` domains |
| settings | `ObserveSettingsUseCase`, `UpdateSettingsUseCase`, `ManageVehiclesUseCase` | `SettingsRepository`, `VehicleRepository` |

Pure domain services (used by the use cases, no Android): `ReimbursementCalculator`, `PerDiemCalculator` (75% first and last day), `DistanceFilter`, `TrackSimplifier` (Douglas–Peucker), `RecordingPolicyResolver`, `ReportBuilder`.

## 3a. Screen inventory (every screen has a light and dark design)

| FigCraft page | Screens | Owning module |
|---|---|---|
| Onboarding | Welcome · Location disclosure · Notifications rationale · Precise location off | `feature:onboarding:presentation` |
| Trips | Home (empty) · Home · Review trips (swipe deck) · Interrupted-trip sheet · All trips · Trip detail · Edit trip · Add a past trip | `feature:trips:presentation` |
| Tracking | Drive detected (notification) · Live trip with recording notification · Trip receipt | `feature:tracking:presentation` |
| Insights | November insights · Year in review: miles, places, money | `feature:insights:presentation` |
| Widgets | Home-screen widgets (month total, today's per diem, recording) · Quick Settings tile | `feature:widgets:presentation` |
| Motion | Recording motion (odometer roll, slide to end, receipt print) · Transitions (route draw-on, shared element, swipe deck) | spec for `core:designsystem` + features |
| Per diem | City rates · Search · Add stay sheet | `feature:perdiem:presentation` |
| Reports | Report · PDF export (US Letter) | `feature:reports:presentation` + `data` |
| Settings | Settings · Vehicles · Mileage rate · About & privacy · Developer options (feature flags) · App health (analytics) | `feature:settings:presentation` |
| Foundations | Colour roles, type, components | `core:designsystem` |

System UI (runtime permission dialogs, share sheet, date pickers) uses the stock Android and Material components and is not redrawn. A new screen gets a FigCraft design before it is built.

## 4. Data model

```kotlin
@Entity trip(id, startedAt: Instant, endedAt: Instant?, status: RECORDING|PAUSED|DONE|MANUAL,
             distanceMeters: Double, purpose, category: BUSINESS|PERSONAL, vehicleId?,
             rateMicrosPerMile: Long, startLabel?, endLabel?, notes?, needsReview: Boolean)
@Entity track_point(tripId, t: Instant, lat, lng, accuracyM, speedMps?)   index (tripId, t)
@Entity stay(id, perDiemLocationId, checkIn: LocalDate, checkOut: LocalDate, includeLodging: Boolean)
@Entity vehicle(id, name, isDefault)
```
- Money is a `Long` in micro-dollars, never `Double`. It is rounded half-up to cents only when displayed or exported.
- Database entities are mapped to domain models in `data`. Domain and presentation never see entities.
- `perdiem.db` (prepackaged, read-only, `createFromAsset`): `location`, `lodging(month)`, `mie`, `mie_breakdown`, plus FTS4 on city and county.
- `irs_rates.json` (asset): an effective-dated list, e.g. `{"from":"2026-07-01","to":"2026-12-31","businessMicros":760000}`. A custom company rate in Settings overrides it.

## 5. Trip recording

### Flow and permissions
```
Onboarding disclosure → ACCESS_FINE + ACCESS_COARSE → POST_NOTIFICATIONS (13+)
"Start trip" in the visible app → StartTripUseCase → RecordingController.start()
  → startForegroundService(TripRecorderService) → startForeground(…, FOREGROUND_SERVICE_TYPE_LOCATION) within 5 s
Stop (app or notification) → StopTripUseCase → FinalizeTripWorker → stopSelf
```
Manifest: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `POST_NOTIFICATIONS`, and `ACTIVITY_RECOGNITION` (only for drive detection). A CI check fails the build if the merged manifest contains `ACCESS_BACKGROUND_LOCATION` or `INTERNET`.

**Drive detection** (optional, off by default): an `IN_VEHICLE` ENTER transition reaches `DriveDetectionReceiver`, which posts a "Drive detected: tap to log" notification. Tapping it opens the app with `START_TRIP`, and the trip starts from the visible activity.

### Components (`feature:tracking`)
```
domain:  LocationTracker (Flow<Fix>) · RecordingController · TrackRepository · DeviceStateMonitor
         AcceptFixUseCase(DistanceFilter) · ObserveRecordingPolicyUseCase(RecordingPolicyResolver)
data:    FusedLocationTracker (callbackFlow) · ServiceRecordingController · RoomTrackRepository
         TripRecorderService (@AndroidEntryPoint, LifecycleService)
           ├─ collects LocationTracker under the current RecordingPolicy
           ├─ AcceptFixUseCase drops fixes with accuracy > 35 m, speed jumps > 70 m/s, and stationary jitter
           ├─ batches Room writes every 10 points or 15 s (the only writer)
           └─ updates the notification at most once per 5 s
         RecordingSession (@Singleton StateFlow) · FinalizeTripWorker (@HiltWorker) · DriveDetectionReceiver
presentation: LiveTripViewModel ← ObserveRecordingStateUseCase + observe track points
```

### Battery policy (`RecordingPolicyResolver`, pure)
| State | Priority | Interval | Min distance |
|---|---|---|---|
| Moving, battery > 20% | HIGH_ACCURACY | 2 s | 5 m |
| Stationary > 2 min | BALANCED | 15 s | 25 m |
| Power-save, battery ≤ 20%, or Saver mode | BALANCED | 10 s | 20 m |

### Edge cases
- If the process dies mid-trip, Room still holds a `RECORDING` row. On the next launch, Home offers "Resume or finish". The service is `START_NOT_STICKY` because restarting a location FGS from the background is not allowed.
- If the permission is revoked mid-trip, the trip is finalized with the points so far, and a notification explains what happened.
- If location services are off, `SettingsClient` shows its resolution prompt before the trip starts.
- Only one trip can be active at a time.
- `FinalizeTripWorker` is unique per trip id. It simplifies the track, then snapshots the rate and labels the start and end places with the platform `Geocoder`. Geocoding is best-effort: offline, the labels fall back to coordinates.

## 6. Per diem and reports
- `PerDiemCalculator` adds lodging per night (using each night's month) plus M&IE per day. First and last travel days are paid at 75%.
- `BuildReportUseCase` combines trips and stays for a date range into an immutable `Report`. `ExportReportUseCase` writes it through `ReportFileWriter`, which has PDF and CSV implementations in `reports:data`. The PDF footer cites the IRS and GSA tables and their effective dates. Exports run on the IO dispatcher and are shared through `FileProvider`.

## 6a. UI craft

| Moment | Implementation | Accessibility / reduced motion |
|---|---|---|
| **Odometer readout** (home hero, live trip, widget, year in review) | `Odometer` composable in `core:designsystem`: one `AnimatedContent` per digit with vertical slide (260 ms), so only changed digits roll. The tenths drum uses the accent colour. Drum shading is a vertical gradient. | One semantics node: "12.6 miles". With reduced motion, digits swap without animating. |
| **Slide to end trip** | `AnchoredDraggable` with two anchors. A haptic tick at 50% and `CONFIRM` at the end. It springs back below 85%. The label fades with drag progress. | Custom action "End trip" and long-press on the thumb, so nobody has to drag. |
| **Trip receipt** | A zigzag `GenericShape` (paper), a `Canvas` perforation line and barcode, and a height clip-reveal (spring 0.75, 520 ms) that "prints" the receipt after Stop. | The receipt is read as a list of label/value pairs. With reduced motion it appears without printing. |
| **Route canvas** | `RouteCanvas` in `core:ui`: projects track points to the canvas, draws a halo and a line, and draws on with `PathMeasure.getSegment` (700 ms). Draws pins, labels and a scale bar. No map SDK. | Semantics summary: "18.4 mile route from Brooklyn office to Acme HQ". |
| **Route replay** | A speed sparkline (`Canvas`) with a draggable scrubber. A marker interpolates along the stored track points by time; play animates the scrubber at 60× speed. | The scrubber is a `Slider` with semantics: "9:26 AM, 34 miles per hour". |
| **Swipe deck** (review trips) | `graphicsLayer` with rotation = offsetX / 18°, a BUSINESS or PERSONAL stamp whose alpha follows the drag, a velocity fling past 40%, and the next card scaling 0.94 → 1. Undo restores the last card. | The Personal, Undo and Business buttons do the same thing as the swipe. Each result is announced. |
| **Month heatmap** | A `LazyVerticalGrid` of day cells in 5 colour levels, with today outlined and future days outlined only. | Each cell reads "November 10, 86 miles". Levels are also shown as numbers on tap. |
| **Year in review** | `HorizontalPager` stories with auto-advance progress bars (paused on touch). The globe arc uses `drawArc`. The share card is captured with `rememberGraphicsLayer().toImageBitmap()`, written to `cacheDir` and shared through `FileProvider`, all on-device. | Auto-advance is off when TalkBack or reduced motion is on. Each story has a text alternative. |
| **Widgets and tile** | Jetpack Glance: a 2×2 month widget (Start trip), a 2×2 per diem widget, and a 4×1 recording widget (Stop). A `TileService` "Start trip" tile. Start actions open the app's activity, which starts the FGS, so Play's "user-initiated" rule holds. | Glance content descriptions; the tile has a state description. |
| **Live trip notification** | `NotificationCompat` foreground notification. On Android 16+, optionally a promoted Live Update (`ProgressStyle`) showing distance. ⚠ verify eligibility rules at build time. | — |
| **Transitions** | `SharedTransitionLayout` with Navigation 3: trip row thumbnail → map card (`sharedBounds`), predictive back scrubs in reverse; the Material 3 motion scheme everywhere else. | Respects the system animator scale. |

All of these are specified on the **Motion** page of the FigCraft file. They're covered by Roborazzi screenshots of key frames (e.g. the slider at 0%, 62% and done), and by Macrobenchmark `FrameTimingMetric` runs for the live trip and the swipe deck.

## 7. OpenAPI: build-time GSA import
`tools/perdiem-import` generates a Kotlin client from GSA's Per Diem API ([open.gsa.gov/api/perdiem](https://open.gsa.gov/api/perdiem/)) with `openapi-generator`. It fetches one fiscal year using an api.data.gov key (a local/CI secret) and writes `perdiem.db`. It runs once a year (FY2027 standard CONUS: $113 lodging / $68 M&IE). ⚠ verify: if GSA publishes no OpenAPI file, we write a minimal spec for the three endpoints we use. The app itself makes no network calls.

## 8. Testing strategy
**Behaviour over implementation.** Tests drive public APIs and assert what the user or system would see: emitted `UiState`, stored rows, tracked analytics events, and flag-dependent behaviour. Every collaborator is a hand-written **fake** with real in-memory behaviour, published from the owning module's `testFixtures`: `FakeTripRepository`, `FakeLocationTracker` (replays GPX), `FakeAnalytics`, `FakeFeatureFlags`, `FakeClock`. There are no mocking libraries.

**Every use case, ViewModel, repository implementation, mapper and domain service gets unit tests.** Tests use hand-written fakes, not mocks. Each domain module publishes fakes of its interfaces through `testFixtures`, so other modules reuse the same fakes.

| Layer | Tooling | Examples |
|---|---|---|
| Domain (JVM) | kotlin.test + JUnit 4 runner, table-driven and property tests | Rate effective date (Jun 30 vs Jul 1), rounding, 75% rule across month boundaries, `DistanceFilter` on GPX fixtures (city, highway, tunnel, parking-lot jitter), simplifying keeps distance within 0.5%, policy resolution |
| Use cases | fakes + `kotlinx-coroutines-test` + Turbine | `StartTripUseCase` refuses a second active trip; `FinalizeTripUseCase` snapshots the right rate |
| ViewModels | Turbine, `MainDispatcherRule`, fake use-case inputs | Full `UiState` sequences: start → record → stop, interrupted-trip recovery, report export |
| Data | Robolectric + in-memory Room, DataStore tests, `MigrationTestHelper` | DAOs, FTS search, prepackaged DB schema, entity↔domain mappers |
| Service | `ServiceTestRule` + a fake tracker replaying GPX | FGS started within 5 s, batched writes, behaviour on permission revocation |
| UI | Compose UI tests with accessibility checks on | Onboarding → permission → start trip; per diem search |
| Screenshot | Roborazzi | Each screen in light, dark, font scale 2.0 and RTL |
| Coverage | Kover | Build fails below 90% line coverage on `domain` modules and 70% overall |
| Performance | Macrobenchmark + Baseline Profile | Cold start, trip list scroll |

## 9. CI/CD (Buildkite)
1. `spotlessCheck detekt lint` (Compose lint rules on, warnings are errors), plus the module-graph assertion, dependency analysis, and a merged-manifest permission check
2. `test koverVerify verifyRoborazziDebug`, in parallel with `:tools:perdiem-import:test`
3. Instrumented tests on Gradle Managed Devices (API 34 and 36)
4. On a `v*` tag: `bundleRelease`, sign with the upload key from Buildkite secrets, then Gradle Play Publisher to the **internal** track, with `versionCode` taken from `BUILDKITE_BUILD_NUMBER`

A GitHub Actions mirror runs steps 1–2 on pull requests for the public repo badge.

## 10. Accessibility and offline
- Touch targets ≥ 48 dp; text contrast ≥ 4.5:1 in both themes; layouts reflow at font scale 2.0.
- Live trip stats use `liveRegion = Polite`, announced at most every 30 s. Money is spoken as "13 dollars 98 cents".
- `RouteCanvas` and the lodging chart carry text summaries in their semantics ("18.4 mile route from Brooklyn office to Acme HQ"). Colour is never the only signal.
- Every feature works in airplane mode. Only the place labels degrade to coordinates.

## 11. Engineering conventions (apply when coding starts)
- **No code comments.** Intent goes in names, types and tests.
- **An `AGENTS.md` in every module** (written when the module is created) gives coding agents its purpose, public API, allowed dependencies, use cases, test expectations and design-frame references. A root `AGENTS.md` holds the repo-wide rules.
- **ADRs** in `docs/adr/` (MADR format), written as each decision is implemented: feature modularization, clean architecture and use cases, Hilt, Navigation 3, Room + DataStore, user-initiated trips with no background location, the location FGS battery policy, effective-dated rates, Money as micros, no map SDK, the build-time GSA import, fakes over mocks, no code comments, Buildkite CI, no on-device AI.
- **Unit tests** ship in the same change as the code they cover (§8).

## 12. Open decisions
1. Package name: `com.aijones.mileage`. It is permanent once published.
2. Drive detection in v1, off by default (my default), or wait for v1.1?
