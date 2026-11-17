# core:analytics-impl

**Purpose:** On-device analytics sink and crash recorder.

**Public API:** `LocalAnalytics` (Channel → batched Room insert every 20 events or 2 s, ring buffer of 5,000, Logcat in debuggable builds), `CrashRecorder.install()`, `FlatJson`, `HealthCalculator`. Bound to `Analytics` and `EventLog`.

**Allowed dependencies:** core:analytics, core:database, core:common.

**Tests:** `FlatJsonAndHealthTest`. `track()` must stay non-blocking and safe on the main thread.

Rules: see the root `AGENTS.md` (no comments, fakes not mocks).
