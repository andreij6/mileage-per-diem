# feature:insights:presentation

Compose UI for monthly insights and the "2026 on the road" stories. Package `com.aijones.mileage.feature.insights.presentation`.

## Screens and design frames
| Key | Route / Screen | ViewModel | Design |
|---|---|---|---|
| `InsightsRoute` | `InsightsRoute` / `InsightsScreen` | `InsightsViewModel` | `design/renders/insights/insights-{light,dark}.png` |
| `YearInReviewRoute(year)` | `YearInReviewRoute` / `YearInReviewScreen` | `YearInReviewViewModel` (assisted, key) | `story_miles`, `story_places`, `story_money` |

Both are registered in `insightsEntries(navigator)` (`Navigation.kt`).

- Insights: heatmap (Sun-first, 40 dp cells radius 10, primary alpha 0/25/50/75/100%, level 0 = `surface2`, today outlined in `text`, future outlined only; each cell reads "November 10, 86 miles", today has state "today"), Longest / Busiest / Top stop cards, weekday bars, and the year card shown only when `FeatureFlag.YEAR_IN_REVIEW` is on (opens `YearInReviewRoute(year)`).
- Year in review: `HorizontalPager` of 3 stories on the `storyTop → storyBottom` gradient, white status-bar icons while shown, segmented progress auto-advancing every 5 s (paused while pressed; off when TalkBack touch exploration is on or animators are disabled), tap left third = back, elsewhere = forward, custom accessibility actions "Next story" / "Previous story", close X. The money story records its card into a `rememberGraphicsLayer()`; "Share this card" turns it into PNG bytes on `Dispatchers.Default`, `YearInReviewViewModel.share` exports it through `ExportShareCardUseCase`, and the route opens an `ACTION_SEND image/png` chooser with the returned URI.

## State and events
- `InsightsUiState` (`Loading`, `Ready(insights, yearCard?)`); mapper `MonthInsights.toUi(locale, zone)`.
- `YearInReviewUiState` (`Loading`, `Ready(stories, current, sharing, shareUri)`); intents `showStory(index)` (clamped), `share(png)`, `onShareHandled()`; mapper `YearInReview.toUi()`.
- Analytics: `ScreenView("insights")` + `InsightsViewed` once; `ScreenView("year_in_review")` and `YearInReviewViewed(story 1..3)` once per story shown; `ShareCardExported(3)` comes from the use case.

## Dependencies
`feature:insights:domain`, `core:designsystem` (`MpdCard`, `MpdTopBar`, `Odometer`, `MpdIcons`), `core:navigation`, `core:analytics`, `core:featureflags`.

## Tests
- `InsightsViewModelTest` (flag on/off and toggling, full mapped state, analytics), `YearInReviewViewModelTest` (load, story progression and bounds, once-per-story tracking, share export) with real use cases over trips/perdiem/insights fakes.
- `InsightsScreenTest` (Robolectric Compose): heatmap cell semantics, year card click and flag-off hiding, story close and custom actions.
- Previews: light and dark for every screen (`android preview render "gradle://.?:feature:insights:presentation" --fqn …`).
