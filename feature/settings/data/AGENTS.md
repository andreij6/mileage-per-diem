# feature:settings:data

Implements the settings domain's storage. Package `com.aijones.mileage.feature.settings.data`.

## Public API
Nothing public. Hilt `SettingsDataModule` binds:
- `DataStoreSettingsRepository` → `SettingsRepository`: the `@SettingsStore` Preferences DataStore, keys prefixed `settings_` (`settings_default_purpose`, `settings_battery_mode`, `settings_drive_detection`, `settings_report_name`, `settings_company`, `settings_employee_id`, `settings_theme`, `settings_developer_unlocked`). Unknown enum names fall back to defaults. IO on `@IoDispatcher`.
- `RoomDataResetter` → `DataResetter`: `TripDao.deleteAll()` (track points cascade) and `StayDao.deleteAll()`. Vehicles and settings are kept.

## Dependencies
`feature:settings:domain`, `core:database`, `core:datastore`, `core:common`.

## Tests
Robolectric/JVM: `DataStoreSettingsRepositoryTest` (temporary DataStore: defaults, round trip of every field, transforms), `RoomDataResetterTest` (in-memory Room: trips, points and stays gone, vehicles kept).
