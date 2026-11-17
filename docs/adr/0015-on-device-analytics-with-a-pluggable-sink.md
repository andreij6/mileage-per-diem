# 0015. On-device analytics with a pluggable sink

- Status: accepted
- Date: 2026-11-16

## Context
We want app-health dashboards (startup, crashes, recording reliability, funnels) but have no backend and no INTERNET permission.

## Decision
Typed `AnalyticsEvent`s go to `Analytics`; `LocalAnalytics` batches them into a Room ring buffer (5,000 rows), logs to Logcat in debug, and records crashes. `EventLog` powers the App health screen and NDJSON export. See `docs/ANALYTICS.md`.

## Consequences
Dashboard-ready schema today; a remote sink is one new class later. Costs: data stays on one device.

## Alternatives considered
Firebase Analytics (needs network and a Data-safety declaration).
