# feature:reports:presentation

The Report tab. Package `com.aijones.mileage.feature.reports.presentation`.

## Public API
- `reportsEntries(navigator)` registers `ReportsRoute`. The trips row opens `AllTripsRoute`; stay rows switch to the `PerDiemRoute` tab.
- `ReportsRoute(onOpenTrips, onOpenPerDiem)`: wires `ReportsViewModel` and launches the share sheet (`Intent.createChooser(ACTION_SEND)` with a `FileProvider` URI, authority `packageName + ".files"`, `FLAG_GRANT_READ_URI_PERMISSION` and `ClipData`).
- `ReportsScreen(state, …)`: stateless.
- `ReportsViewModel`: `StateFlow<ReportsUiState>`; intents `setRange(from, to)` (dates are ordered), `toggleBusinessOnly()`, `export(format)` (ignored while an export runs or before the report loads), `onShareHandled()`, `onFailureShown()`. Default range is the current month from the injected `Clock`. Tracks `ScreenView("reports")`; `ExportReportUseCase` tracks `export_completed` / `export_failed`.

## Screen
`design/renders/reports/report-light.png` and `report-dark.png`: title, range chip (opens the Material 3 `DateRangePicker`), "Business only" / "All trips" toggle chip, totals card (total, split bar proportional to mileage vs per diem, legend), INCLUDED list (trips summary + one row per stay, "M&IE only" / "with lodging"), PDF (filled) and CSV (outlined) buttons with a progress indicator on the pressed one, privacy note, snackbar on failure.

## Dependencies
`feature:reports:domain`, `core:analytics`, the core UI modules, `androidx.core` (FileProvider).

## Tests
- `ReportsViewModelTest`: real use cases over the domain fakes; state sequences for the default month, range change, business-only toggle, export success (exporting → share → consumed) and failure (exporting → failed → consumed), plus tracked events.
- `ReportsScreenTest` (Robolectric Compose): totals and included text, PDF/CSV clicks call through, chip toggle, progress replaces the pressed button's label.
