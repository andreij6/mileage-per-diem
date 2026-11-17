# 0013. Behaviour tests with fakes, not mocks

- Status: accepted
- Date: 2026-11-16

## Context
Mock-heavy tests verify call order and break on refactors.

## Decision
Every interface has an in-memory fake in its module's `testFixtures`. Tests assert observable behaviour: state sequences, stored data, tracked analytics events, and flag on/off paths. No mocking library is on the classpath.

## Consequences
Refactor-safe tests and shared fakes. Costs: fakes must be maintained.

## Alternatives considered
MockK/Mockito.
