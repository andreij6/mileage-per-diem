# feature:perdiem:domain

Pure Kotlin. GSA per diem lookup, stay estimates and saved stays. Package `com.aijones.mileage.feature.perdiem.domain`.

## Public API
- Models: `PerDiemLocation` (`label` = "Boston, MA"), `MieBreakdown`, `PerDiemRates` (`lodgingFor(night)`), `Stay` (`total`, `days` inclusive), `StayEstimate`, `StayLine`.
- `PerDiemCalculator.estimate(rates, checkIn, checkOut, includeLodging)`: inclusive days; first and last day pay `mie.firstLastDay` (75%), a one-day trip pays it once; lodging per night at that night's month.
- Interfaces (implemented in `perdiem:data`): `PerDiemRateRepository` (`fiscalYearFor` defaults to the Oct 1 fiscal year), `StayRepository`, `RecentSearchRepository`.

## Use cases
`SearchLocationsUseCase` (blank query returns nothing; tracks `PerDiemSearched`), `GetRatesForLocationUseCase` (records non-standard locations as recent), `EstimateStayUseCase`, `AddStayUseCase` (tracks `StayAdded`), `ObserveStaysUseCase`, `ObserveRecentSearchesUseCase`, `CurrentFiscalYearUseCase`.

## Dependencies
`core:*` JVM modules only.

## Tests
Fakes in `src/testFixtures`: `FakePerDiemRateRepository`, `FakeStayRepository`, `FakeRecentSearchRepository`, `SamplePerDiem` (sample FY2027 standard $68/$113, NYC $92/$286 in Nov, Boston $86/$239 in Nov). Table-driven calculator tests: 8-day NYC = $690 M&IE + $2,002 lodging, 3-day = $802, one-day, month and year boundaries.
