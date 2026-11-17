# 0014. No code comments; context in AGENTS.md

- Status: accepted
- Date: 2026-11-16

## Context
Comments drift from code; coding agents need module context.

## Decision
Production and test code has no comments. Intent lives in names, types, tests and ADRs. Every module has an `AGENTS.md`, plus a root guide.

## Consequences
Less stale prose. Costs: names must be good.

## Alternatives considered
KDoc on every public API.
