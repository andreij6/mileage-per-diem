# 0007. User-initiated trips; no background location

- Status: accepted
- Date: 2026-11-16

## Context
Play treats `ACCESS_BACKGROUND_LOCATION` as a high-risk permission, needing a declaration, a video and review; automatic trip logging is classed as convenience.

## Decision
Trips always start from a visible UI: the app, a notification tap, a widget or the Quick Settings tile all open the activity. Recording runs in a `location` foreground service. Drive detection (Activity Recognition) only suggests a trip. CI fails if the merged manifest contains `ACCESS_BACKGROUND_LOCATION` or `INTERNET`.

## Consequences
Simpler Play review and a stronger privacy story. Costs: one tap to start a trip.

## Alternatives considered
Fully automatic logging with background location.
