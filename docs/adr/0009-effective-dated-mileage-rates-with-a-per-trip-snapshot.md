# 0009. Effective-dated mileage rates with a per-trip snapshot

- Status: accepted
- Date: 2026-11-16

## Context
The IRS changed the 2026 rate mid-year (72.5¢ to 76¢ from Jul 1, 2026).

## Decision
Rates are an effective-dated table bundled in the app; each trip stores the rate in effect on its date when it is saved. A company rate in Settings overrides the IRS rate.

## Consequences
Old trips never change when rates change; reports are reproducible.

## Alternatives considered
Rates keyed by calendar year; recomputing totals on the fly.
