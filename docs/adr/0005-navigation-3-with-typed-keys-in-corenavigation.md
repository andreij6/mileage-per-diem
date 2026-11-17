# 0005. Navigation 3 with typed keys in core:navigation

- Status: accepted
- Date: 2026-11-16

## Context
Features must navigate to each other without depending on each other's presentation modules.

## Decision
Destinations are `@Serializable` `NavKey`s in `core:navigation`. Each presentation module contributes `EntryProviderScope.xEntries(navigator)`. The app owns one back stack and a `Navigator` implementation.

## Consequences
Typed arguments, predictive back, adaptive scenes later; features stay decoupled.

## Alternatives considered
Navigation-Compose string routes; a custom router.
