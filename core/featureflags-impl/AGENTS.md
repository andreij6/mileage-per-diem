# core:featureflags-impl

**Purpose:** Resolves flags: DataStore override, then default. Tracks `feature_flag_exposed` once per session per flag.

**Public API:** `DataStoreFeatureFlags` bound to `FeatureFlags`.

**Allowed dependencies:** core:featureflags, core:datastore, core:analytics.

**Tests:** `DataStoreFeatureFlagsTest`: defaults, overrides, reset, exposure.

Rules: see the root `AGENTS.md` (no comments, fakes not mocks).
