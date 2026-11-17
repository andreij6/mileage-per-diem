# 0010. Money as Long micro-dollars

- Status: accepted
- Date: 2026-11-16

## Context
Floating point produces cent errors in totals.

## Decision
`core.model.Money` wraps a `Long` of micro-dollars; rounding is half-up to cents only when displayed or exported. Distance is `Distance` (meters) with tenth-of-a-mile rounding for reimbursement.

## Consequences
Exact arithmetic; value classes add no allocation.

## Alternatives considered
BigDecimal (allocations); Double (rounding errors).
