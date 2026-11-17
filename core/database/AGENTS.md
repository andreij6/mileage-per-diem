# core:database

**Purpose:** The single Room database.

**Public API:** `MileageDatabase` (trip, track_point, stay, vehicle, analytics_event), one DAO per feature table, Hilt `DatabaseModule`. Schemas are exported to `schemas/`.

**Allowed dependencies:** Room, Hilt.

**Tests:** DAO behaviour is tested in the owning feature's data module with in-memory Room (Robolectric). Bump the version and add a migration test for schema changes after 1.0.

Rules: see the root `AGENTS.md` (no comments, fakes not mocks).
