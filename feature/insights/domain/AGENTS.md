# feature:insights:domain

Pure Kotlin. Month heatmap and highlights, year in review, share cards. Package `com.aijones.mileage.feature.insights.domain`.

## Public API
- Models: `MonthInsights`, `DayCell` (`levelFor(miles)`: 0 / < 25 / < 45 / < 70 / ≥ 70), `YearInReview` (`EARTH_CIRCUMFERENCE_MI`), `PlaceCount`, `RatePeriodTotal`.
- Interface (implemented in `insights:data`): `ShareCardWriter`.

## Use cases
`ObserveMonthInsightsUseCase(YearMonth)` (finished business trips; top stop = most frequent end label), `BuildYearInReviewUseCase(year)` (periods follow the IRS rate table, e.g. "Jan–Jun"; top 3 places; cities = distinct stay locations), `ObserveYearInReviewEnabledUseCase`, `ExportShareCardUseCase(png, story)` (tracks `ShareCardExported`). Screen-view events are tracked by the UI.

## Dependencies
`core:*` JVM modules, `feature:trips:domain`, `feature:perdiem:domain` (api).

## Tests
Fake in `src/testFixtures`: `FakeShareCardWriter`. Reuse trips and perdiem fakes.
