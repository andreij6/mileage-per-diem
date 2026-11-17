# core:featureflags

**Purpose:** Feature flag API, pure Kotlin.

**Public API:** `FeatureFlag` (key, default, title, description), `FeatureFlags` (observe, observeAll, isEnabled, setOverride, resetOverrides), `FlagState`, `FlagSource`. testFixtures: `FakeFeatureFlags(vararg overrides)`.

**Allowed dependencies:** Coroutines only.

**Tests:** Every new flag needs a test on both paths in the feature that reads it.

Rules: see the root `AGENTS.md` (no comments, fakes not mocks).
