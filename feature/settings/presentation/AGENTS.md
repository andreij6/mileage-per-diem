# feature:settings:presentation

Settings tab and its sub-screens. Package `com.aijones.mileage.feature.settings.presentation`.

## Public API
`settingsEntries(navigator)` registers `SettingsRoute`, `VehiclesRoute`, `MileageRateRoute`, `AboutRoute`, `DeveloperOptionsRoute`, `AppHealthRoute`. Each screen is a stateless `XScreen(state, intents…)` plus an `XRoute` with a `@HiltViewModel`.

## Screens (design frames in `design/renders/settings/`)
| Screen | Frame | ViewModel use cases |
|---|---|---|
| `SettingsScreen` (tab, no nav-bar padding) | `settings` | ObserveSettings, UpdateSettings, ObserveVehicles, ObserveMileageRateSettings, ObserveFeatureFlag(DRIVE_DETECTION), ObserveFeatureFlags. Edit dialogs (purpose, name, company), pickers (battery mode, theme; theme applies at once because the app reads `Settings.theme`). Drive detection section hidden when the flag is off. Developer options row shown when the app is debuggable or `developerUnlocked`. |
| `VehiclesScreen` | `vehicles` | ObserveVehicleUsage, ManageVehicles. Row tap opens rename / set default / delete (also as accessibility actions); Add vehicle dialog. |
| `MileageRateScreen` | `rate_settings` | ObserveMileageRateSettings, SetCustomRate. Company rate is dollars per mile (`$0.65`), saved when valid and positive. IRS table newest first, "Current" for today's period. |
| `AboutScreen` | `about` | UnlockDeveloperOptions (tap the header 7×, inline live-region countdown), DeleteAllData (confirm dialog). Privacy policy opens `https://andreij6.github.io/mileage-per-diem/privacy/`; data sources and licenses are dialogs. |
| `DeveloperOptionsScreen` | `dev_options` | ObserveFeatureFlags, SetFeatureFlagOverride, ResetFeatureFlagOverrides, ObserveAppHealth(EPOCH) for the event count. |
| `AppHealthScreen` | `app_health` | ObserveAppHealth(now − 7 days), ExportEventLog → `ACTION_SEND` text/plain (consumable `shareText`). The design's "GPS fix p90" tile is "Events logged" because `HealthSummary` has no fix accuracy. |

Every ViewModel tracks `ScreenView` (`settings`, `vehicles`, `mileage_rate`, `about`, `developer_options`, `app_health`) once in `init`.

## Dependencies
`feature:settings:domain` (which exposes `feature:trips:domain` types such as `Vehicle`, `ManageVehiclesUseCase`), core UI modules, `core:analytics`, `core:featureflags`.

## Tests
JVM ViewModel tests with real use cases over fakes (`FakeSettingsRepository`, `FakeDataResetter`, `FakeEventLog`, trips fakes, `FakeFeatureFlags`, `FakeAnalytics`, `FakeClock`), asserting `UiState` sequences with Turbine. Robolectric Compose test: `DeveloperOptionsScreenTest` (toggle → Override chip, reset).
