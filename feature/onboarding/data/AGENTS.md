# feature:onboarding:data

Implements onboarding progress storage. Package `com.aijones.mileage.feature.onboarding.data`.

## Public API
Nothing public. Hilt `OnboardingDataModule` binds `DataStoreOnboardingRepository` → `OnboardingRepository`: the `@SettingsStore` Preferences DataStore, key `onboarding_completed` (string set of `OnboardingStep` names). IO on `@IoDispatcher`.

## Dependencies
`feature:onboarding:domain`, `core:datastore`, `core:common`.

## Tests
`DataStoreOnboardingRepositoryTest` (temporary DataStore): empty start, steps accumulate to done, idempotent completion.
