package com.aijones.mileage.feature.trips.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.trips.domain.ClassifyTripUseCase
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.ObserveTrackUseCase
import com.aijones.mileage.feature.trips.domain.ObserveTripsNeedingReviewUseCase
import com.aijones.mileage.feature.trips.domain.TripCategory
import com.aijones.mileage.feature.trips.domain.UndoClassificationUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReviewViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val analytics = FakeAnalytics()
    private val repo = FakeTripRepository(
        listOf(
            trip(1, "2026-11-13T11:05", minutes = 46, miles = 24.8, from = "Home", to = "Newark Supply", purpose = null, needsReview = true),
            trip(2, "2026-11-11T17:30", miles = 17.9, purpose = "Client", needsReview = true),
            trip(3, "2026-11-08T07:15", miles = 15.6, purpose = null, needsReview = true),
            trip(4, "2026-11-16T09:12"),
        ),
    )

    private fun viewModel() = ReviewViewModel(
        ObserveTripsNeedingReviewUseCase(repo),
        ObserveTrackUseCase(repo),
        ClassifyTripUseCase(repo, analytics),
        UndoClassificationUseCase(repo),
        analytics,
        FakeClock(),
    )

    @Test
    fun showsTheDeckNewestFirstWithTheTopCardsRoute() = runTest {
        val points = track("2026-11-13T11:05")
        repo.appendTrackPoints(1, points)
        viewModel().state.test {
            val s = awaitItem()
            assertEquals(listOf(1L, 2L, 3L), s.cards.map { it.id })
            assertEquals(
                ReviewCardUi(1, "Home", "Newark Supply", "Fri, Nov 13 · 11:05 – 11:51 · 24.8 mi", "24.8 mi", "$18.85", listOf("Site visit", "Supplies", "Client"), points.map { it.point }),
                s.top,
            )
            assertEquals(listOf("Client", "Site visit", "Supplies"), s.cards[1].purposes)
            assertEquals(0, s.reviewed)
            assertEquals(3, s.total)
            assertEquals("Site visit", s.selectedPurpose)
        }
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("review_trips")), analytics.events)
    }

    @Test
    fun businessButtonStoresTheChosenPurposeAndAdvances() = runTest {
        val vm = viewModel()
        vm.state.test {
            awaitItem()
            vm.selectPurpose("Supplies")
            assertEquals("Supplies", awaitItem().selectedPurpose)
            vm.classify(TripCategory.BUSINESS, "button")
            val s = expectMostRecentItem()
            assertEquals(listOf(2L, 3L), s.cards.map { it.id })
            assertEquals(1, s.reviewed)
            assertEquals(3, s.total)
            assertEquals("Client", s.selectedPurpose)
        }
        val stored = repo.get(1)!!
        assertEquals(TripCategory.BUSINESS, stored.category)
        assertEquals("Supplies", stored.purpose)
        assertEquals(false, stored.needsReview)
        assertEquals(AnalyticsEvent.TripClassified("business", "button"), analytics.events.last())
    }

    @Test
    fun swipePersonalThenUndoRestoresTheCard() = runTest {
        val vm = viewModel()
        vm.state.test {
            awaitItem()
            vm.classify(TripCategory.PERSONAL, "swipe")
            assertEquals(listOf(2L, 3L), expectMostRecentItem().cards.map { it.id })
            assertEquals(TripCategory.PERSONAL, repo.get(1)!!.category)
            assertEquals(null, repo.get(1)!!.purpose)
            vm.undo()
            val s = expectMostRecentItem()
            assertEquals(listOf(1L, 2L, 3L), s.cards.map { it.id })
            assertEquals(0, s.reviewed)
            assertEquals(false, s.canUndo)
        }
        assertEquals(trip(1, "2026-11-13T11:05", minutes = 46, miles = 24.8, from = "Home", to = "Newark Supply", purpose = null, needsReview = true), repo.get(1))
        assertEquals(AnalyticsEvent.TripClassified("personal", "swipe"), analytics.eventsOf<AnalyticsEvent.TripClassified>().single())
    }

    @Test
    fun sortingEveryTripReachesTheDoneState() = runTest {
        val vm = viewModel()
        vm.state.test {
            var s = awaitItem()
            repeat(3) {
                vm.classify(TripCategory.BUSINESS, "button")
                s = expectMostRecentItem()
            }
            assertTrue(s.done)
            assertEquals(3, s.reviewed)
            assertTrue(s.canUndo)
        }
        assertEquals(3, analytics.eventsOf<AnalyticsEvent.TripClassified>().size)
    }
}
