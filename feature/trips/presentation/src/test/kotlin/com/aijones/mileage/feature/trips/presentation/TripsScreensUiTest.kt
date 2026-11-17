package com.aijones.mileage.feature.trips.presentation

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.featureflags.FakeFeatureFlags
import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.navigation.AddTripRoute
import com.aijones.mileage.core.navigation.AllTripsRoute
import com.aijones.mileage.core.navigation.LiveTripRoute
import com.aijones.mileage.core.navigation.ReportsRoute
import com.aijones.mileage.core.navigation.ReviewTripsRoute
import com.aijones.mileage.core.navigation.TripDetailRoute
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.feature.trips.domain.ClassifyTripUseCase
import com.aijones.mileage.feature.trips.domain.DeleteTripUseCase
import com.aijones.mileage.feature.trips.domain.FakeMileageRateRepository
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.FakeVehicleRepository
import com.aijones.mileage.feature.trips.domain.ObserveCurrentRateUseCase
import com.aijones.mileage.feature.trips.domain.ObserveMonthSummaryUseCase
import com.aijones.mileage.feature.trips.domain.ObserveRecentTripsUseCase
import com.aijones.mileage.feature.trips.domain.ObserveRouteReplayEnabledUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTrackUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTripUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTripsNeedingReviewUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTripsUseCase
import com.aijones.mileage.feature.trips.domain.ObserveVehiclesUseCase
import com.aijones.mileage.feature.trips.domain.TripCategory
import com.aijones.mileage.feature.trips.domain.UndoClassificationUseCase
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-xxhdpi")
class TripsScreensUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val clock = FakeClock()
    private val analytics = FakeAnalytics()
    private val navigator = RecordingNavigator()

    @Test
    fun homeShowsTheMonthAndStartTripNavigatesToTheLiveTrip() {
        val repo = FakeTripRepository(listOf(trip(1, "2026-11-16T09:12"), trip(2, "2026-11-13T11:05", miles = 24.8, to = "Newark Supply", needsReview = true)))
        val vm = HomeViewModel(
            ObserveMonthSummaryUseCase(ObserveTripsUseCase(repo, clock), ObserveCurrentRateUseCase(FakeMileageRateRepository(), clock)),
            ObserveRecentTripsUseCase(repo),
            ObserveTripsNeedingReviewUseCase(repo),
            analytics,
            clock,
        )
        compose.setContent { MpdTheme(darkTheme = false) { HomeRoute(navigator, vm) } }

        compose.onAllNodesWithText("$13.98").assertCountEquals(2)
        compose.onNodeWithContentDescription("18.4 miles").assertIsDisplayed()
        compose.onNodeWithText("1 trip needs a purpose").assertIsDisplayed()
        compose.onNodeWithText("Today · 9:12 – 9:41 · 18.4 mi", useUnmergedTree = true).assertIsDisplayed()

        compose.onNodeWithText("Start trip").performClick()
        compose.onNodeWithText("Review").performClick()
        compose.onNodeWithText("See all").performClick()
        compose.onNodeWithText("Office → Acme HQ", useUnmergedTree = true).performClick()
        assertEquals(listOf<Any>(LiveTripRoute("app"), ReviewTripsRoute, AllTripsRoute, TripDetailRoute(1)), navigator.events)
    }

    @Test
    fun homeEmptyStateOffersAPastTrip() {
        val repo = FakeTripRepository()
        val vm = HomeViewModel(
            ObserveMonthSummaryUseCase(ObserveTripsUseCase(repo, clock), ObserveCurrentRateUseCase(FakeMileageRateRepository(), clock)),
            ObserveRecentTripsUseCase(repo),
            ObserveTripsNeedingReviewUseCase(repo),
            analytics,
            clock,
        )
        compose.setContent { MpdTheme { HomeRoute(navigator, vm) } }
        compose.onNodeWithText("No trips yet").assertIsDisplayed()
        compose.onNodeWithText("IRS rate · 76¢ per mile").assertIsDisplayed()
        compose.onNodeWithText("Add a past trip").performClick()
        assertEquals(listOf<Any>(AddTripRoute), navigator.events)
    }

    private fun reviewViewModel(repo: FakeTripRepository) = ReviewViewModel(
        ObserveTripsNeedingReviewUseCase(repo),
        ObserveTrackUseCase(repo),
        ClassifyTripUseCase(repo, analytics),
        UndoClassificationUseCase(repo),
        analytics,
        clock,
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun reviewButtonsAndAccessibilityActionsClassifyTrips() {
        val repo = FakeTripRepository(
            listOf(
                trip(1, "2026-11-13T11:05", from = "Home", to = "Newark Supply", purpose = null, needsReview = true),
                trip(2, "2026-11-11T17:30", purpose = null, needsReview = true),
            ),
        )
        compose.setContent { MpdTheme { ReviewRoute(navigator, reviewViewModel(repo)) } }

        compose.onNodeWithText("Home → Newark Supply").assertIsDisplayed()
        compose.onNodeWithContentDescription("0 of 2 reviewed").assertExists()
        compose.onNodeWithText("Supplies").performClick()
        compose.onNodeWithText("Business").performClick()
        compose.waitForIdle()
        assertEquals(TripCategory.BUSINESS, runBlocking { repo.get(1) }!!.category)
        assertEquals("Supplies", runBlocking { repo.get(1) }!!.purpose)
        compose.onNodeWithContentDescription("1 of 2 reviewed").assertExists()

        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions)).performCustomAccessibilityActionWithLabel("Mark personal")
        compose.waitForIdle()
        assertEquals(TripCategory.PERSONAL, runBlocking { repo.get(2) }!!.category)
        compose.onNodeWithText("All sorted").assertIsDisplayed()
        assertEquals(
            listOf(AnalyticsEvent.TripClassified("business", "button"), AnalyticsEvent.TripClassified("personal", "button")),
            analytics.eventsOf<AnalyticsEvent.TripClassified>(),
        )

        compose.onNodeWithText("Undo").performClick()
        compose.onNodeWithText("Office → Acme HQ").assertIsDisplayed()
    }

    private fun detail(flags: FakeFeatureFlags): TripDetailViewModel {
        val repo = FakeTripRepository(listOf(trip(1, "2026-11-16T09:12", from = "Brooklyn office", to = "Acme HQ, Midtown")))
        runBlocking { repo.appendTrackPoints(1, track("2026-11-16T09:12")) }
        return TripDetailViewModel(
            1,
            ObserveTripUseCase(repo),
            ObserveTrackUseCase(repo),
            ObserveVehiclesUseCase(FakeVehicleRepository()),
            ObserveRouteReplayEnabledUseCase(flags),
            DeleteTripUseCase(repo, analytics),
            analytics,
            clock,
        )
    }

    @Test
    fun detailShowsReplayWhenTheFlagIsOn() {
        compose.setContent { MpdTheme { TripDetailEntry(1, navigator, detail(FakeFeatureFlags(FeatureFlag.ROUTE_REPLAY to true))) } }
        compose.onNodeWithText("Replay · drag to scrub").assertIsDisplayed()
        compose.onNodeWithContentDescription("Play route replay").assertIsDisplayed()
        compose.onNodeWithText("9:41 AM · ", substring = true).assertIsDisplayed()
    }

    @Test
    fun detailHidesReplayWhenTheFlagIsOff() {
        compose.setContent { MpdTheme { TripDetailEntry(1, navigator, detail(FakeFeatureFlags(FeatureFlag.ROUTE_REPLAY to false))) } }
        compose.onNodeWithText("$13.98").assertIsDisplayed()
        compose.onNodeWithText("Replay · drag to scrub").assertDoesNotExist()
        compose.onNodeWithContentDescription("Play route replay").assertDoesNotExist()
        compose.onNodeWithText("9:12 AM · 0 mph").assertDoesNotExist()
        compose.onNodeWithText("Add to report").performClick()
        assertEquals(listOf<Any>(ReportsRoute), navigator.events)
    }
}
