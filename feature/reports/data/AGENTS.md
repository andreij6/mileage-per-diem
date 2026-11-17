# feature:reports:data

Writes expense reports to files. Package `com.aijones.mileage.feature.reports.data`.

## Public API
- `AndroidReportFileWriter` implements `ReportFileWriter` (bound in `ReportsDataModule`). Runs on `@IoDispatcher`, writes to `cacheDir/reports/` as `mileage-report-2026-11.pdf|csv` (or `mileage-report-<from>_<to>` when the range spans months) and returns `ExportedFile(path, mimeType, fileName)`. The app's FileProvider (`${applicationId}.files`, `cache/reports/`) shares these files.
- CSV: the domain `CsvFormatter` with the clock's zone.
- PDF (`ReportPdf`, internal): `android.graphics.pdf.PdfDocument`, US Letter 612×792 pt, 40 pt margins. Header, employee/company line, period, three totals boxes, Trips table (Date, Route, Purpose, Miles, Rate, Amount), Per diem table (GSA FY), rates footnote from `Report.rateNote`, signature lines. Every trip is listed; rows that do not fit go to a new page with the column header repeated. Fonts come from `core:designsystem` (`R.font.space_grotesk`, `R.font.inter`, variable weight via `fontVariationSettings`), with the default typeface as fallback. Colours come from `LightColors`. `render(report, newPage)` draws onto any canvas per page and returns the page count; `write(report, out)` backs it with `PdfDocument`.

## Design frames
`design/renders/reports/report_pdf-light.png` (Reports / PDF export (US Letter)).

## Dependencies
`feature:reports:domain`, `core:common`, `core:model`, `core:designsystem` (fonts and colours), `androidx.core`.

## Tests
Robolectric with `@GraphicsMode(NATIVE)`. Robolectric has no `PdfDocument` natives, so the PDF test renders pages onto bitmaps through `render` and checks the page count for 0/10/60 trips (1/1/3) and that no page is blank. CSV tests check the written file equals `CsvFormatter` output and the file naming.
