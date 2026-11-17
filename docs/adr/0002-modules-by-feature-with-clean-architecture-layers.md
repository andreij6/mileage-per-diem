# 0002. Modules by feature with clean-architecture layers

- Status: accepted
- Date: 2026-11-16

## Context
The app has seven features plus widgets. Ramp-scale Android teams split by feature to keep builds fast and ownership clear.

## Decision
Each feature has `domain` (pure Kotlin), `data` (Android, implements domain interfaces) and `presentation` (Compose). Presentation never depends on data or on another feature's presentation. Domain-to-domain dependencies are allowed when acyclic. Shared code lives in `core:*`. A feature gets a `data` module only when it owns data.

## Consequences
Parallel Gradle builds and JVM-only domain tests; the dependency rules are explicit. Costs: more modules and convention plugins in `build-logic`.

## Alternatives considered
One `app` module (simplest, but slow to build and test); layer-first modules (no ownership boundaries).
