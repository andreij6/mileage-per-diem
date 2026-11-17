# app

**Purpose:** Application shell.

**Public API:** `MileageApp` (Hilt, crash recorder, sessions, WorkManager on demand), `MainActivity` (splash, edge-to-edge, startup tracking, intents), `MileageAppUi` (NavDisplay + bottom nav + `TrackingOverlays`), `AppNavigator`, `AppModule` (dispatchers, Clock, app scope).

**Allowed dependencies:** Everything (the composition root).

**Tests:** Keep `onCreate` work minimal; the startup budget is enforced by `:benchmark`.

Rules: see the root `AGENTS.md` (no comments, fakes not mocks).
