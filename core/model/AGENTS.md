# core:model

**Purpose:** Shared value types, pure Kotlin.

**Public API:** `Money` (Long micro-dollars; `cents`, `percent`, `formatUsd`, `formatCentsPerMile`), `Distance` (meters; `miles`, `tenthsOfMile`, `formatMiles`), `mileageAmount(distance, rate)`, `GeoPoint` + `distanceTo` (haversine), `groupThousands`.

**Allowed dependencies:** None.

**Tests:** `MoneyAndDistanceTest`: rounding, formatting, haversine. Add a case for every new formatter.

Rules: see the root `AGENTS.md` (no comments, fakes not mocks).
