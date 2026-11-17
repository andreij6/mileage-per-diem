# core:analytics

**Purpose:** Analytics API, pure Kotlin. See `docs/ANALYTICS.md`.

**Public API:** `Analytics.track(AnalyticsEvent)`, the `AnalyticsEvent` catalog (snake_case names with typed params), `EventLog` (recent events, `HealthSummary`, NDJSON export). testFixtures: `FakeAnalytics` with `eventsOf<T>()`.

**Allowed dependencies:** Coroutines only.

**Tests:** Add new events here first and to `docs/ANALYTICS.md`. Never add personal data.

Rules: see the root `AGENTS.md` (no comments, fakes not mocks).
