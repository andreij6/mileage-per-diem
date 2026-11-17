# 0003. Use cases as the only path from ViewModel to data

- Status: accepted
- Date: 2026-11-16

## Context
ViewModels that call repositories directly accumulate business rules that cannot be tested on the JVM.

## Decision
Every ViewModel action goes through a use case: a class with an `@Inject constructor` and a single `operator fun invoke`. Use cases may combine repositories, other use cases, `Analytics`, `FeatureFlags` and `Clock`.

## Consequences
Business rules are tested without Android. Costs: more small classes.

## Alternatives considered
Fat repositories; interactor objects with many methods.
