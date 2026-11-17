# feature:onboarding:presentation

First-run flow and the precise-location help screen. Package `com.aijones.mileage.feature.onboarding.presentation`.

## Public API
`onboardingEntries(navigator)` registers:
- `OnboardingRoute`: Welcome → Location disclosure → Notifications, driven by `OnboardingState.next`. When done: `navigator.switchTab(TripsRoute)`, then `PreciseLocationOffRoute` if only approximate location was granted.
- `PreciseLocationOffRoute`: "Open settings" opens the app details settings; "Log a trip by hand" → `navigator.replace(AddTripRoute)`.

## Screens (design frames in `design/renders/onboarding/`)
`welcome`, `disclosure` (prominent disclosure copy must match the design word for word; `RouteCanvas` sample route with a navigation badge), `notifications`, `precise_off`.

## Behaviour
- Continue on the disclosure requests `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION`; "Not now" completes the step with `skip`.
- "Allow notifications" requests `POST_NOTIFICATIONS` on API 33+ (granted below 33); "Skip" completes with `skip`.
- `CompleteOnboardingStepUseCase` tracks `onboarding_step`; `RecordPermissionResultUseCase` tracks `permission_result` (`location` with `precise` = fine granted, `notifications`).
- Screen views: `onboarding_welcome`, `onboarding_location`, `onboarding_notifications` (once each), `precise_location_off`.

## Dependencies
`feature:onboarding:domain`, core UI modules, `core:analytics`, `androidx.activity:activity-compose` (permission launchers).

## Tests
`OnboardingViewModelTest` (step progression, permission outcomes, skips, screen views) and Robolectric `OnboardingScreenTest` (disclosure copy, Continue requests location, Not now skips).
