# Mileage & Per Diem: agent guide (repo-wide)

An on-device Android app (no backend, no `INTERNET` permission) that records business drives with GPS, costs them at the effective-dated IRS rate, looks up bundled GSA per diems, and exports PDF/CSV reports. By AI.Jones.

## Read first
- `docs/ARCHITECTURE.md`: modules, use cases, data model, UI craft
- `docs/ANALYTICS.md`: the event catalog, dashboards and feature flags
- `docs/adr/`: decisions and why
- `design/mileage-per-diem.figcraft` and `design/renders/<page>/<screen>-<light|dark>.png`: **every screen must match its design in both themes**

## Hard rules
1. **No code comments.** Names, types and tests carry intent. No KDoc, no `//`, no `/* */`.
2. **Layers.**
   - `feature:x:domain` is pure Kotlin (JVM). It can use `core:model`, `core:common`, `core:analytics`, `core:featureflags` and other features' **domain** modules, with no cycles.
   - `feature:x:data` implements its own domain's interfaces and binds them with Hilt `@Binds`.
   - `feature:x:presentation` depends only on its own domain plus the `core` UI modules. It never depends on `data` or on another feature's `presentation`.
3. **ViewModels call use cases only**, never repositories. A use case is a class with a single `operator fun invoke` and an `@Inject constructor`.
4. **UDF.** A ViewModel exposes one `StateFlow<UiState>` (`stateIn(viewModelScope, WhileSubscribed(5_000), initial)`) and public intent functions. Screens are a stateless `XScreen(state, onIntent…)` plus an `XRoute` that wires up the ViewModel. Collect with `collectAsStateWithLifecycle()`.
5. **Tests use fakes, never mocks.** Fakes live in the owning module's `src/testFixtures` (for JVM modules) and are reused through `testFixtures(project(...))`. Test behaviour: state sequences, stored data, tracked `AnalyticsEvent`s (`FakeAnalytics`), and behaviour with flags on and off (`FakeFeatureFlags`). Time comes from an injected `java.time.Clock` (`FakeClock` in `core:testing`).
6. **Money** is `core.model.Money` (Long micro-dollars); **distance** is `core.model.Distance`. Never use `Double` for currency.
7. **Performance.** No work on the main thread at startup. Inject `dagger.Lazy` for anything heavy that isn't needed at once. No reflection-based libraries. No new dependencies without a reason in an ADR.
8. **Analytics.** Track events from `core.analytics.AnalyticsEvent` only. Never put personal data (coordinates, place names, free text) in events.
9. **Accessibility.** 48 dp touch targets; content descriptions on icon buttons; one merged semantics node for composite values (odometer, route canvas); `liveRegion` for live stats.
10. Use design-system components (`core:designsystem`): `MpdTheme`, `MpdButton`, `MpdCard`, `MpdListRow`, `CardList`, `ValueRow`, `MpdChip`, `SelectChip`, `MpdSwitch`, `MpdRadio`, `SegmentedControl`, `MpdField`, `MpdTopBar`, `BarAction`, `ScreenTitle`, `MpdNavBar`, `SaveBar`, `Banner`, `InfoNote`, `SectionLabel`, `IconTile`, `Odometer`, `StatusDot`, `Grabber`, and icons from `MpdIcons`. Use `RouteCanvas` from `core:ui`. Colours come from `MpdTheme.colors` and text styles from `MpdTheme.type`. Never hard-code hex values in features.

## Commands
- JVM module tests: `./gradlew :feature:trips:domain:test`
- Android module tests: `./gradlew :feature:trips:presentation:testDebugUnitTest`
- App: `./gradlew :app:installDebug` (emulator: `android emulator start Medium_Phone_API_36.1`)
- Startup benchmark: `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest` (the startup budget check runs after it)
- Compose previews to PNG without a device: `android preview`

## Module AGENTS.md
Every module has its own `AGENTS.md` with: purpose, public API, allowed dependencies, use cases or screens, test expectations, and the design frames it implements. Update it when the module changes.
