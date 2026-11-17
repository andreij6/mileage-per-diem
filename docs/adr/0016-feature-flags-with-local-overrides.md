# 0016. Feature flags with local overrides

- Status: accepted
- Date: 2026-11-16

## Context
We want to ship features dark, test both paths, and roll back without a release.

## Decision
`core:featureflags` defines flags with defaults; `core:featureflags-impl` resolves local override (DataStore) then default, and tracks exposure once per session. Developer options toggles them. A remote-config source can slot in between later.

## Consequences
Both paths are testable with `FakeFeatureFlags`; instant kill switch on device.

## Alternatives considered
Build-time flags (no runtime toggle); a third-party SDK.
