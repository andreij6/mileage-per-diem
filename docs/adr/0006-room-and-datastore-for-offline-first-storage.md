# 0006. Room and DataStore for offline-first storage

- Status: accepted
- Date: 2026-11-16

## Context
There is no backend; the phone is the source of truth.

## Decision
One `MileageDatabase` in `core:database` (entities and DAOs per feature in separate files), a read-only prepackaged `perdiem.db` owned by `feature:perdiem:data`, and Preferences DataStore for settings and flag overrides. Room is opened lazily on first query, off the main thread.

## Consequences
Fast queries, Flow-based observation, migrations tested with `MigrationTestHelper`.

## Alternatives considered
SQLDelight (fine, but Room is the team standard); files/JSON (no queries).
