# 0012. Bundled GSA per diem data via a build-time OpenAPI import

- Status: accepted
- Date: 2026-11-16

## Context
Per diem rates change each fiscal year; the app must work offline.

## Decision
`tools:perdiem-import` (JVM) generates a client from the GSA Per Diem API description, fetches one fiscal year, and writes the `perdiem.db` asset. The app never calls the network.

## Consequences
Contract-first tooling; reproducible data releases. Costs: an app release per fiscal year.

## Alternatives considered
Calling the API at runtime (needs INTERNET and a key).
