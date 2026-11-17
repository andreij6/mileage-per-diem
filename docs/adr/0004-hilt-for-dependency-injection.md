# 0004. Hilt for dependency injection

- Status: accepted
- Date: 2026-11-16

## Context
The app injects into a foreground Service, a BroadcastReceiver, WorkManager workers, Glance widgets and a TileService.

## Decision
Use Hilt everywhere. Domain modules use only `javax.inject` annotations so they stay pure Kotlin. Use `dagger.Lazy` for objects that are heavy or not needed at startup.

## Consequences
Standard, well-documented Android entry points; compile-time graph validation. Costs: KSP time and a small startup cost.

## Alternatives considered
Manual DI (Service and Worker injection gets awkward); Koin (runtime resolution and reflection).
