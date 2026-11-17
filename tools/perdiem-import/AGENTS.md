# tools:perdiem-import

**Purpose:** Build-time JVM CLI that turns one fiscal year of GSA per diem rates into the read-only `feature/perdiem/data/src/main/assets/perdiem.db`. The app never calls the network (ADR 0012).

**How it works:**
- `openapi/perdiem.yaml` is a subset of GSA's published description (`https://open.gsa.gov/api/perdiem/v2/openapi.yaml`, same server and `X-API-KEY` scheme). GSA's file has no response schemas and omits the M&IE endpoint, so the subset adds `LodgingRate` and `MieRate` for `GET /v2/rates/conus/lodging/{year}` and `GET /v2/rates/conus/mie/{year}`. The `org.openapi.generator` plugin generates a Kotlin client (`jvm-okhttp4`, kotlinx.serialization) into `build/generated/openapi` before `compileKotlin`.
- `mapGsa` collapses the per-state "Standard Rate" rows into one `Standard rate, CONUS` location (id 1, and fails if they disagree), gives other destinations ids 2… sorted by state, city and county, converts dollars to cents and fails if a "Meals" amount has no M&IE tier.
- `writePerDiemDb` creates the schema from Room's exported JSON (`feature/perdiem/data/schemas/.../PerDiemDatabase/1.json`: `createSql`, FTS content-sync triggers, `room_master_table` identity hash, `user_version`), inserts, optimizes the FTS index and VACUUMs.

## Regenerate the asset
```
GSA_API_KEY=<api.data.gov key> ./gradlew :tools:perdiem-import:run            # FY2027 (default)
GSA_API_KEY=<key> ./gradlew :tools:perdiem-import:run --args="2028"           # another fiscal year
```
Without `GSA_API_KEY` it uses `DEMO_KEY` (rate-limited). Optional args: `<year> <room schema json> <output db>`, relative to the repo root. If `PerDiemDatabase` entities change, build `:feature:perdiem:data` first so the schema JSON is current, then rerun and commit the new `perdiem.db`.

**Dependencies (tool only):** openapi-generator plugin, okhttp, kotlinx-serialization-json, sqlite-jdbc.

**Tests:** `./gradlew :tools:perdiem-import:test` — mapping over saved GSA FY2027 responses in `src/test/resources` (no network), and a written database checked for Room's identity hash, `user_version` and FTS prefix search.

Rules: see the root `AGENTS.md` (no comments, fakes not mocks).
