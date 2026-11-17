package com.aijones.mileage.feature.onboarding.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.onboarding.domain.CompleteOnboardingStepUseCase
import com.aijones.mileage.feature.onboarding.domain.FakeOnboardingRepository
import com.aijones.mileage.feature.onboarding.domain.ObserveOnboardingStateUseCase
import com.aijones.mileage.feature.onboarding.domain.OnboardingStep
import com.aijones.mileage.feature.onboarding.domain.RecordPermissionResultUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class OnboardingViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val repository = FakeOnboardingRepository()
    private val analytics = FakeAnalytics()
    private val vm = OnboardingViewModel(
        ObserveOnboardingStateUseCase(repository),
        CompleteOnboardingStepUseCase(repository, analytics),
        RecordPermissionResultUseCase(analytics),
        analytics,
    )

    @Test
    fun `granting precise location and notifications walks every step to done`() = runTest {
        vm.state.test {
            assertEquals(OnboardingUiState(), awaitItem())
            assertEquals(OnboardingUiState(OnboardingStep.WELCOME), awaitItem())
            vm.onStepShown(OnboardingStep.WELCOME)
            vm.getStarted()
            assertEquals(OnboardingUiState(OnboardingStep.LOCATION), awaitItem())
            vm.onStepShown(OnboardingStep.LOCATION)
            vm.onLocationResult(fine = true, coarse = true)
            assertEquals(OnboardingUiState(OnboardingStep.NOTIFICATIONS), awaitItem())
            vm.onStepShown(OnboardingStep.NOTIFICATIONS)
            vm.onNotificationsResult(true)
            assertEquals(OnboardingUiState(null, done = true), awaitItem())
        }
        assertTrue(repository.current.isDone)
        assertEquals(
            listOf(
                AnalyticsEvent.ScreenView("onboarding_welcome"),
                AnalyticsEvent.OnboardingStep("welcome", "continue"),
                AnalyticsEvent.ScreenView("onboarding_location"),
                AnalyticsEvent.PermissionResult("location", granted = true, precise = true),
                AnalyticsEvent.OnboardingStep("location", "continue"),
                AnalyticsEvent.ScreenView("onboarding_notifications"),
                AnalyticsEvent.PermissionResult("notifications", granted = true),
                AnalyticsEvent.OnboardingStep("notifications", "continue"),
            ),
            analytics.events,
        )
    }

    @Test
    fun `approximate location is tracked as not precise and flagged for the precise screen`() = runTest {
        vm.state.test {
            skipItems(2)
            vm.getStarted()
            awaitItem()
            vm.onLocationResult(fine = false, coarse = true)
            var item = awaitItem()
            if (item.step == OnboardingStep.LOCATION) item = awaitItem()
            assertEquals(OnboardingUiState(OnboardingStep.NOTIFICATIONS, approximateOnly = true), item)
            vm.onNotificationsResult(false)
            assertEquals(OnboardingUiState(null, done = true, approximateOnly = true), awaitItem())
        }
        assertEquals(
            listOf(AnalyticsEvent.PermissionResult("location", granted = true, precise = false), AnalyticsEvent.PermissionResult("notifications", granted = false)),
            analytics.eventsOf<AnalyticsEvent.PermissionResult>(),
        )
    }

    @Test
    fun `skipping location and notifications records skips without permission results`() = runTest {
        vm.state.test {
            skipItems(2)
            vm.getStarted()
            awaitItem()
            vm.skipLocation()
            assertEquals(OnboardingStep.NOTIFICATIONS, awaitItem().step)
            vm.skipNotifications()
            assertEquals(OnboardingUiState(null, done = true), awaitItem())
        }
        assertEquals(emptyList(), analytics.eventsOf<AnalyticsEvent.PermissionResult>())
        assertEquals(
            listOf(
                AnalyticsEvent.OnboardingStep("welcome", "continue"),
                AnalyticsEvent.OnboardingStep("location", "skip"),
                AnalyticsEvent.OnboardingStep("notifications", "skip"),
            ),
            analytics.eventsOf<AnalyticsEvent.OnboardingStep>(),
        )
    }

    @Test
    fun `a step's screen view is tracked once even if shown again`() {
        vm.onStepShown(OnboardingStep.WELCOME)
        vm.onStepShown(OnboardingStep.WELCOME)
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("onboarding_welcome")), analytics.events)
    }

    @Test
    fun `precise location screen tracks its view`() {
        PreciseLocationOffViewModel(analytics)
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("precise_location_off")), analytics.events)
    }
}
