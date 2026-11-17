# feature:perdiem:presentation

**Purpose:** The Per diem tab: city rates, search and the Add stay sheet.

**Public API:** `perDiemEntries(navigator)` registers `PerDiemRoute`, `PerDiemSearchRoute` and `AddStayRoute(locationId)`. Screens are stateless (`PerDiemScreen`, `PerDiemSearchScreen`, `AddStayContent`); routes (`PerDiemScreenRoute`, `PerDiemSearchScreenRoute`, `AddStaySheetRoute`) wire the ViewModels.

**Screens and design frames (`design/renders/per-diem/`):**
- `per_diem-*` → `PerDiemScreen` / `PerDiemViewModel`: search pill showing the selection (most recent search, else New York City, else the standard rate), lodging this month and M&IE cards, 12-bar "Lodging by month" (Oct→Sep, current month highlighted, one semantics summary), M&IE breakdown, 3-day estimate hero (today → today+2, lodging on) with "Add stay". Tracks `screen_view(per_diem)` via `onShown()` from the route.
- `perdiem_search-*` → `PerDiemSearchScreen` / `PerDiemSearchViewModel`: autofocused field (back, clear), 150 ms debounce, "Results · FY2027", "Recent", standard CONUS note. Selecting a result or recent calls `GetRatesForLocationUseCase`, which records it as the most recent search: that is how the selection reaches the Per diem screen. Tracks `screen_view(per_diem_search)`; `per_diem_searched` comes from the use case.
- `stay_add-*` → `AddStaySheetRoute` / `AddStayViewModel` (assisted, keyed by `AddStayRoute`): a `ModalBottomSheet` drawn over the Per diem screen inside the `AddStayRoute` entry (entry transitions disabled so the swap is seamless). Check-in/out `DatePickerDialog`s (check-out ≥ check-in), "Claim lodging" switch (default off), breakdown lines and total from `EstimateStayUseCase`, "Add to <check-in month> report" → `AddStayUseCase` (tracks `stay_added`) → sheet hides → back. Defaults: today → today+2.

**Dependencies:** perdiem domain, core designsystem/ui/navigation/model/analytics, navigation3-ui (entry transition metadata).

**Tests:** `PerDiemViewModelTest`, `PerDiemSearchViewModelTest`, `AddStayViewModelTest` (real use cases over the domain fakes, Turbine state sequences, tracked events), `PerDiemScreensUiTest` (Robolectric Compose: sheet total follows the switch and saves, per diem callbacks and chart semantics, search rows and note).

**Known gaps:** search results show no M&IE amount (the domain's `PerDiemLocation` carries none; `LocationRow.mieDaily` is ready for it). The New York City default is found with `SearchLocationsUseCase`, so it tracks one `per_diem_searched` the first time the tab opens without recents.

Rules: see the root `AGENTS.md` (no comments, fakes not mocks).
