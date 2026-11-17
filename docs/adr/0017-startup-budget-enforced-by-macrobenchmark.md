# 0017. Startup budget enforced by Macrobenchmark

- Status: accepted
- Date: 2026-11-16

## Context
Startup speed is the first impression and regresses silently.

## Decision
`:benchmark` measures cold startup (no compilation and with the Baseline Profile); `verifyStartupBudget` fails CI when the median TTID with the profile is over 600 ms. A Baseline Profile with a startup profile (dex layout) ships with the app. WorkManager initializes on demand; Hilt bindings that aren't needed at startup are `Lazy`.

## Consequences
Regressions are caught in CI. Costs: an emulator or device in CI.

## Alternatives considered
Manual profiling only.
