# Analytics, app health and feature flags

The app has no backend and no `INTERNET` permission, so events are stored **on the device**. The tracking layer is still built as if a pipeline existed: events are typed, named in snake_case with a fixed parameter schema, and kept in a local store that exports NDJSON (one JSON event per line). A remote sink (Firebase, Amplitude, a warehouse) could be added later as one more `AnalyticsSink`, with no call-site changes.

## Architecture
```
feature code ──track(AnalyticsEvent)──▶ Analytics (core:analytics, pure Kotlin)
                                           │
                    core:analytics-impl ───┤ LocalAnalytics: non-blocking Channel → batched Room insert
                                           │   (every 20 events or 2 s), ring buffer of 5,000 rows
                                           ├ LogcatSink (debuggable builds only)
                                           └ CrashRecorder: UncaughtExceptionHandler writes `app_crash` synchronously
EventLog (read side) ─▶ App health screen (Settings › Developer options) · NDJSON export via share sheet
```
- `track()` never blocks the caller and never throws.
- Every event carries `ts` (epoch ms), `session_id`, `app_version` and `os_api` automatically.
- No personal data: no coordinates, place names or free text in events. Distances are rounded to 0.1 mi.
- Tests use `FakeAnalytics` (from `core:analytics` test fixtures), which records events so tests can check behaviour: "stopping a trip tracks `trip_recording_stopped` with the right distance".

## Event catalog

| Event | Parameters | Fired when |
|---|---|---|
| `app_startup` | `type` (cold/warm), `ttid_ms`, `ttfd_ms` | First frame / `reportFullyDrawn` |
| `session_start` / `session_end` | `duration_s` | Process lifecycle start/stop |
| `screen_view` | `screen` | Navigation 3 entry shown |
| `onboarding_step` | `step`, `result` (continue/skip) | Each onboarding screen |
| `permission_result` | `permission`, `granted`, `precise` | Runtime permission callback |
| `trip_recording_started` | `source` (app/notification/widget/tile) | FGS started |
| `trip_recording_stopped` | `distance_mi`, `duration_s`, `fixes`, `dropped_fixes`, `interrupted` | Trip finalised |
| `trip_recording_failed` | `reason` (fgs_start_failed/permission_revoked/location_off) | Recording could not start or continue |
| `location_policy_changed` | `policy` (high/balanced/saver) | Battery policy switch |
| `trip_classified` | `category`, `via` (swipe/button) | Review deck |
| `trip_edited` / `trip_added_manual` / `trip_deleted` | `fields` / `distance_mi` / — | Trip changes |
| `per_diem_searched` | `result_count` | Search committed |
| `stay_added` | `days`, `lodging` | Add stay |
| `export_completed` / `export_failed` | `format`, `trips`, `duration_ms` / `format`, `reason` | Report export |
| `insights_viewed` / `year_in_review_viewed` | — / `story` | Insights screens |
| `share_card_exported` | `story` | Share card saved |
| `widget_action` | `widget`, `action` | Glance or Quick Settings tile tap |
| `feature_flag_exposed` | `flag`, `value`, `source` (default/override) | First read of a flag in a session |
| `app_crash` | `exception`, `thread` | Uncaught exception |

## Dashboards these events support
| Dashboard | Panels (metric ← events) | Alert |
|---|---|---|
| **Startup** | p50/p90 `ttid_ms`, `ttfd_ms` by `type` and `app_version` ← `app_startup` | p50 cold TTID > 600 ms (the Macrobenchmark budget) |
| **Stability** | crash-free sessions ← `app_crash` / `session_start`; top exceptions | < 99.5% crash-free |
| **Trip recording reliability** | start success rate ← `trip_recording_started` ÷ (started + failed); interrupted %; failure reasons; dropped-fix ratio | failure rate > 2% |
| **Permissions funnel** | onboarding → location granted → precise → notifications ← `onboarding_step`, `permission_result` | precise-grant drop > 10% week over week |
| **Battery** | time share by policy ← `location_policy_changed` | — |
| **Exports** | success rate, p90 `duration_ms` by `format` ← `export_*` | p90 > 2 s |
| **Engagement** | trips per active day, review-deck usage, widget/tile starts ← `trip_*`, `widget_action` | — |
| **Experiments** | exposure counts and outcome deltas by flag value ← `feature_flag_exposed` joined to the events above | — |

The on-device **App health** screen (Settings › Developer options) shows the same KPIs for this phone, computed by `EventLog.summary(since)`.

## Feature flags
`core:featureflags` (pure Kotlin) defines the flags and the `FeatureFlags` interface. `core:featureflags-impl` resolves them with this order of precedence: **local override (DataStore) → built-in default**. A remote-config source could slot in between later.

| Flag | Default | Gates |
|---|---|---|
| `year_in_review` | on | The "Your 2026 on the road" card and stories on Insights |
| `drive_detection` | on | The drive-detection setting and the Activity Recognition receiver |
| `route_replay` | on | The scrubber and speed graph on trip detail (the map stays) |
| `live_update` | off | Android 16 promoted Live Update notification while recording |

- UI reads flags as `Flow<Boolean>` through use cases, so toggling one in Developer options updates open screens immediately.
- Each flag's first read in a session tracks `feature_flag_exposed`.
- Developer options shows every flag with its source (Default or Override) and a "Reset all overrides" button. It is always visible in debuggable builds. In release builds it unlocks by tapping the version number on About & privacy 7 times.
- Tests use `FakeFeatureFlags`, which lets each test set a flag and assert behaviour both on and off.
