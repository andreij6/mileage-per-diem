# benchmark

**Purpose:** Macrobenchmarks and Baseline Profile generation (`com.android.test`).

**Public API:** `StartupBenchmark` (cold, no compilation vs baseline profile), `BaselineProfileGenerator` (startup + open a trip), `verifyStartupBudget` task (median TTID ≤ `startupBudgetMs`, default 600).

**Allowed dependencies:** Targets `:app`.

**Tests:** Run `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest :benchmark:verifyStartupBudget`; regenerate the profile with `./gradlew :app:generateBaselineProfile`.

Rules: see the root `AGENTS.md` (no comments, fakes not mocks).
