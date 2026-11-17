package com.aijones.mileage.feature.onboarding.presentation

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.feature.onboarding.domain.CompleteOnboardingStepUseCase
import com.aijones.mileage.feature.onboarding.domain.FakeOnboardingRepository
import com.aijones.mileage.feature.onboarding.domain.ObserveOnboardingStateUseCase
import com.aijones.mileage.feature.onboarding.domain.OnboardingState
import com.aijones.mileage.feature.onboarding.domain.OnboardingStep
import com.aijones.mileage.feature.onboarding.domain.RecordPermissionResultUseCase
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h915dp")
class OnboardingScreenTest {
    @get:Rule val compose = createComposeRule()

    private val repository = FakeOnboardingRepository(OnboardingState(setOf(OnboardingStep.WELCOME)))
    private val analytics = FakeAnalytics()
    private val vm = OnboardingViewModel(
        ObserveOnboardingStateUseCase(repository),
        CompleteOnboardingStepUseCase(repository, analytics),
        RecordPermissionResultUseCase(analytics),
        analytics,
    )
    private var locationRequests = 0

    private fun show() = compose.setContent {
        MpdTheme(darkTheme = false) {
            val state by vm.state.collectAsStateWithLifecycle()
            OnboardingScreen(state, vm::onStepShown, vm::getStarted, { locationRequests++ }, vm::skipLocation, {}, vm::skipNotifications)
        }
    }

    @Test
    fun disclosureShowsTheProminentDisclosureCopy() {
        show()
        compose.onNodeWithText("Log business miles in one tap").assertExists()
        compose.onNodeWithText(
            "Mileage & Per Diem uses your precise location to record the route and distance of trips you start. " +
                "Location is used only while a trip is recording and its notification is showing.",
        ).assertExists()
        compose.onNodeWithText("You start every trip. Drive detection can suggest one; it never records on its own.").assertExists()
        compose.onNodeWithText("Trips stay on this phone. No account, no servers, no ads.").assertExists()
        compose.onNodeWithText("Battery-aware: lower accuracy when you're stopped or the battery is low.").assertExists()
        compose.onNodeWithContentDescription("Sample trip route drawn on a map").assertExists()
        compose.onNodeWithText("Not now — log trips by hand").assertExists()
    }

    @Test
    fun continueRequestsLocationAndNotNowSkipsToNotifications() {
        show()
        compose.onNodeWithText("Continue").performScrollTo().performClick()
        assertEquals(1, locationRequests)

        compose.onNodeWithText("Not now — log trips by hand").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("See every trip while it records").assertExists()
        compose.onNodeWithText("Allow notifications").assertExists()
        assertEquals(AnalyticsEvent.OnboardingStep("location", "skip"), analytics.eventsOf<AnalyticsEvent.OnboardingStep>().single())
    }
}
