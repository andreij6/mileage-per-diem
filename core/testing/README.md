# Testing behaviour with fakes, not mocks

> **TL;DR:** **279 unit tests**, no mocking library. Every interface ships an in-memory fake in its module's `testFixtures`, and tests check what users and dashboards would actually see.

| Layer | Tests | Runs on | Example |
|---|---|---|---|
| Domain (use cases, calculators) | 99 | plain JVM | Jun 30 vs Jul 1 IRS rate; 75% first/last day per diem; distance filter on GPS jitter |
| Presentation (ViewModels + Compose UI) | 115 | JVM + Robolectric | full `UiState` sequences with Turbine; the slide-to-end accessibility action; flag on/off |
| Data (Room, DataStore, PDF, CSV) | 40 | Robolectric | the bundled GSA database opened via `createFromAsset`; PDF page counts |
| Core + tools | 25 | JVM / Robolectric | analytics ring buffer; flag exposure; GSA JSON mapping |
| **Startup** | Macrobenchmark | emulator / device | median cold TTID ≤ 600 ms ([`benchmark/`](../../benchmark/README.md)) |

## What a test looks like
```kotlin
@Test
fun startIsRefusedWhileATripIsActive() = runTest {
    val first = start(TripSource.APP)
    analytics.clear(); controller.calls.clear()

    assertEquals(first, start(TripSource.TILE))      // the same trip comes back
    assertEquals(1, trips.trips.size)                 // nothing new is stored
    assertTrue(controller.calls.isEmpty())            // the service isn't started twice
    assertTrue(analytics.events.isEmpty())            // and no duplicate analytics event
}
```
`trips` is a [`FakeTripRepository`](../../feature/trips/domain/src/testFixtures/kotlin/com/aijones/mileage/feature/trips/domain/FakeTripRepository.kt) backed by a `MutableStateFlow`; `analytics` is `FakeAnalytics`; time comes from `FakeClock` (this module). Real use cases run on top of fake repositories, so the tests survive refactoring.

## This module
`MainDispatcherRule`, `FakeClock` (2026-11-16, America/New_York), plus JUnit, kotlin-test, coroutines-test and Turbine re-exported as one dependency.

```bash
./gradlew test testDebugUnitTest      # everything
./gradlew :feature:tracking:domain:test   # one module, seconds
```

**Decision:** [ADR 0013 fakes over mocks](../../docs/adr/0013-behaviour-tests-with-fakes-not-mocks.md)
