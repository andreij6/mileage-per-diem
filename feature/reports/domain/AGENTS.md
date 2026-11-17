# feature:reports:domain

Pure Kotlin. Builds expense reports from trips and stays and exports them. Package `com.aijones.mileage.feature.reports.domain`.

## Public API
- Models: `Report`, `ReportFormat` (`mimeType`, `extension`), `ExportedFile`.
- `CsvFormatter.format(report, zone)`: RFC 4180 (CRLF, quotes doubled, fields with `,` `"` CR or LF quoted), header row, one row per trip and per stay, three total rows. `CsvFormatter.escape(field)`.
- Interface (implemented in `reports:data`): `ReportFileWriter`.

## Use cases
`ObserveReportUseCase(from, to, businessOnly = true)` (finished trips only, oldest first; `rateNote` names the custom rate or the IRS periods in range), `ExportReportUseCase(report, format)` (returns `Result`; tracks `ExportCompleted` with clock-measured duration or `ExportFailed` with the exception class).

## Dependencies
`core:*` JVM modules, `feature:trips:domain`, `feature:perdiem:domain`, `feature:settings:domain` (api).

## Tests
Fake in `src/testFixtures`: `FakeReportFileWriter` (optional failure, `onWrite` hook to advance `FakeClock`). Test CSV escaping thoroughly.
