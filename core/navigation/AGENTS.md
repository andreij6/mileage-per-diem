# core:navigation

**Purpose:** Navigation 3 keys and the `Navigator` contract shared by every feature.

**Public API:** `TopLevelRoute` tabs (Trips, PerDiem, Reports, Settings), destination keys (`TripDetailRoute(tripId)`, `LiveTripRoute(startSource)`, …), `Navigator` (navigate, replace, back, switchTab), `Deeplinks` actions.

**Allowed dependencies:** navigation3-runtime, kotlinx-serialization.

**Tests:** Add keys here; features register them in their `xEntries(navigator)`.

Rules: see the root `AGENTS.md` (no comments, fakes not mocks).
