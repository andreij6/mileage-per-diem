# feature:onboarding:domain

Pure Kotlin. Onboarding progress and permission analytics. Package `com.aijones.mileage.feature.onboarding.domain`.

## Public API
- Models: `OnboardingStep` (`WELCOME`, `LOCATION`, `NOTIFICATIONS`), `OnboardingState` (`isDone`, `next`).
- Interface (implemented in `onboarding:data`): `OnboardingRepository`.

## Use cases
`ObserveOnboardingStateUseCase`, `CompleteOnboardingStepUseCase(step, result)` (`"continue"` or `"skip"`; tracks `OnboardingStep`), `RecordPermissionResultUseCase(permission, granted, precise)` (tracks `PermissionResult`).

## Dependencies
`core:*` JVM modules only.

## Tests
Fake in `src/testFixtures`: `FakeOnboardingRepository`.
