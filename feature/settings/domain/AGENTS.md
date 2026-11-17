# feature:settings:domain

Pure Kotlin. User settings, feature-flag overrides, app health and the custom mileage rate. Package `com.aijones.mileage.feature.settings.domain`.

## Public API
- Models: `Settings` (blank strings mean unset), `BatteryMode`, `ThemeMode`, `AppHealth(summary, recentEvents)`, `MileageRateSettings(irsRates, customRate, effectiveRate)`.
- Interfaces (implemented in `settings:data`): `SettingsRepository`, `DataResetter`.

## Use cases
`ObserveSettingsUseCase`, `UpdateSettingsUseCase`, `ObserveFeatureFlagsUseCase`, `SetFeatureFlagOverrideUseCase`, `ResetFeatureFlagOverridesUseCase`, `ObserveAppHealthUseCase(since)` (summary + 20 recent events), `ExportEventLogUseCase` (NDJSON), `ObserveMileageRateSettingsUseCase`, `SetCustomRateUseCase` (positive or null), `DeleteAllDataUseCase`, `UnlockDeveloperOptionsUseCase` (returns taps remaining; the 7th tap persists `developerUnlocked`; keep one instance per screen), `ObserveFeatureFlagUseCase(flag)` (one flag, tracks exposure), `ObserveVehicleUsageUseCase` (vehicles with this month's finished-trip distance, as `VehicleUsage`).

## Dependencies
`core:*` JVM modules and `feature:trips:domain` (api).

## Tests
Fakes in `src/testFixtures`: `FakeSettingsRepository`, `FakeDataResetter`, `FakeEventLog`. Reuse `FakeFeatureFlags` and trips fakes.
