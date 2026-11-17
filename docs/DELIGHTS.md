# Delights & Motion Craft

This document details the animations, transitions, and tactile micro-interactions implemented across **Mileage & Per Diem**.

---

## 1. Directional Page Choreography (`core/ui/Motion.kt`)

When switching between top-level tabs or opening **Insights**, individual screen sections enter from distinct directions (`Top`, `Start`, `End`, `Bottom`) with staggered delays rather than sliding as a single flat slab.

- **Implementation:** `Modifier.staggeredEnter(direction: MotionDirection, delayMillis: Int = 0)` in [`core/ui/src/main/kotlin/com/aijones/mileage/core/ui/Motion.kt`](../core/ui/src/main/kotlin/com/aijones/mileage/core/ui/Motion.kt)
- **Scope Wiring:** [`MileageAppUi.kt`](../app/src/main/kotlin/com/aijones/mileage/ui/MileageAppUi.kt) provides `LocalNavAnimatedContentScope.current` to `LocalPageAnimatedScope` via a Navigation 3 `NavEntryDecorator`. Outside `NavDisplay` (Compose Previews and Robolectric UI tests), `LocalPageAnimatedScope.current` is `null` and `staggeredEnter` is a safe no-op.
- **Timing:** `36.dp` travel distance, `320ms` (`FastOutSlowInEasing`) slide + `260ms` fade-in on enter; `200ms` slide + `160ms` fade-out on exit.

### Screen Choreography Breakdown

| Screen | `Top` | `Start` | `End` | `Bottom` |
|---|---|---|---|---|
| **Trips (`HomeScreen`)** | Title & Offline chip (`0ms`) | Reimbursable Hero + Odometer (`40ms`) | Review Trips Banner (`80ms`) | Recent Trips Header (`110ms`) & List (`140ms`) |
| **Per diem (`PerDiemScreen`)** | Title & Subtitle (`0ms`) | Lodging Card (`60ms`) & M&IE Breakdown (`120ms`) | Search Pill (`30ms`) & M&IE Card (`60ms`) | Lodging Chart (`90ms`) & Stay Estimate (`150ms`) |
| **Reports (`ReportsScreen`)** | Title (`0ms`) | Total Reimbursable Card (`70ms`) | Filter Chips (`35ms`) & Included List (`120ms`) | Section Label (`100ms`), PDF/CSV (`150ms`), Privacy Note (`175ms`) |
| **Settings (`SettingsScreen`)** | Title (`0ms`) | Trips & Vehicles Section (`40ms`) | Drive Detection (`75ms`) & Reports (`100ms`) | App & About Section (`130ms`) |
| **Insights (`InsightsScreen`)** | Mileage Heatmap (`30ms`) | Highlights Row (`75ms`) | Weekday Chart (`110ms`) | 2026 on the Road Card (`145ms`) |

---

## 2. App-Wide Navigation 3 Transitions (`MileageAppUi.kt`)

Configured directly on `NavDisplay` in [`MileageAppUi.kt`](../app/src/main/kotlin/com/aijones/mileage/ui/MileageAppUi.kt):

- **Top-Level Tab Switches** (`TripsRoute`, `PerDiemRoute`, `ReportsRoute`, `SettingsRoute`):
  Clean crossfade (`260ms` in / `180ms` out) so each screen's `staggeredEnter` directional choreography takes center stage.
- **Modal & Full-Screen Flows** (`LiveTripRoute`, `TripReceiptRoute`, `ReviewTripsRoute`, `AddTripRoute`, `EditTripRoute`, `YearInReviewRoute`, `OnboardingRoute`, `PreciseLocationOffRoute`):
  Vertical rise from `1/5` height + `0.97f → 1f` scale-in (`340ms`, `FastOutSlowInEasing`) while the underlying screen recesses to `0.96f`; reverses cleanly on pop and predictive back.
- **Detail Drill-Downs** (`TripDetailRoute`, `AllTripsRoute`, `InsightsRoute`, `PerDiemSearchRoute`, `VehiclesRoute`, `MileageRateRoute`, `AboutRoute`, `DeveloperOptionsRoute`, `AppHealthRoute`):
  Layered horizontal push (`+25%` incoming slide over `-10%` outgoing slide + `0.96f` scale-down) with matching reverse pop and predictive back scrub.
- **Bottom Navigation Bar (`MpdNavBar`)**:
  Wrapped in `AnimatedVisibility` with `slideInVertically` + `fadeIn` (`240ms`) and `slideOutVertically` + `fadeOut` (`200ms`) when entering or exiting full-screen flows.

---

## 3. Review Trips Stacked Swipe Deck (`ReviewScreen.kt` & `ReviewViewModel.kt`)

- **Preloaded Behind Cards:** [`ReviewViewModel.kt`](../feature/trips/presentation/src/main/kotlin/com/aijones/mileage/feature/trips/presentation/ReviewViewModel.kt) observes `RouteCanvas` track points for the top 3 pending trips (`trips.take(3)`), so stacked cards behind the active card are fully populated with their real route map, title, reimbursement total, and purpose chips.
- **Physical Deck Stacking & Drag Physics:** [`ReviewScreen.kt`](../feature/trips/presentation/src/main/kotlin/com/aijones/mileage/feature/trips/presentation/ReviewScreen.kt) offsets behind cards downward (`dy + 220.dp * (1 - scale)`), scales the next card from `0.94f → 1f` as the top card is dragged, rotates the active card (`offsetX / 18°`), and fades in the angled `BUSINESS` / `PERSONAL` stamp.
- **Accessibility Isolation:** Behind cards use `Modifier.clearAndSetSemantics {}` so TalkBack and UI tests interact only with the active top card.
- **Progress & Completion:** `ProgressSegments` smoothly animate bar colors with `animateColorAsState`, and `AnimatedContent` transitions the finished deck into the **All sorted** celebration state (`fadeIn + scaleIn`).
