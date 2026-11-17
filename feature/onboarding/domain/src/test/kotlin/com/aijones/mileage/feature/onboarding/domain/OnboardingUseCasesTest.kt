package com.aijones.mileage.feature.onboarding.domain

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnboardingUseCasesTest {
    private val repo = FakeOnboardingRepository()
    private val analytics = FakeAnalytics()
    private val complete = CompleteOnboardingStepUseCase(repo, analytics)

    @Test
    fun completingEveryStepFinishesOnboarding() = runTest {
        ObserveOnboardingStateUseCase(repo)().test {
            assertEquals(OnboardingStep.WELCOME, awaitItem().next)
            complete(OnboardingStep.WELCOME, "continue")
            assertEquals(OnboardingStep.LOCATION, awaitItem().next)
            complete(OnboardingStep.LOCATION, "continue")
            assertFalse(awaitItem().isDone)
            complete(OnboardingStep.NOTIFICATIONS, "skip")
            val done = awaitItem()
            assertTrue(done.isDone)
            assertNull(done.next)
        }
        assertEquals(
            listOf(
                AnalyticsEvent.OnboardingStep("welcome", "continue"),
                AnalyticsEvent.OnboardingStep("location", "continue"),
                AnalyticsEvent.OnboardingStep("notifications", "skip"),
            ),
            analytics.events,
        )
    }

    @Test
    fun unknownResultIsRejected() = runTest {
        assertFailsWith<IllegalArgumentException> { complete(OnboardingStep.WELCOME, "maybe") }
        assertTrue(repo.current.completed.isEmpty())
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun permissionResultsAreTracked() {
        val record = RecordPermissionResultUseCase(analytics)
        record("location", granted = true, precise = false)
        record("notifications", granted = false)
        assertEquals(
            listOf(AnalyticsEvent.PermissionResult("location", true, false), AnalyticsEvent.PermissionResult("notifications", false, null)),
            analytics.events,
        )
    }
}
