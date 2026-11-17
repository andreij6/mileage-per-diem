package com.aijones.mileage.feature.settings.presentation

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.featureflags.FakeFeatureFlags
import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.settings.domain.FakeEventLog
import com.aijones.mileage.feature.settings.domain.ObserveAppHealthUseCase
import com.aijones.mileage.feature.settings.domain.ObserveFeatureFlagsUseCase
import com.aijones.mileage.feature.settings.domain.ResetFeatureFlagOverridesUseCase
import com.aijones.mileage.feature.settings.domain.SetFeatureFlagOverrideUseCase
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h915dp")
class DeveloperOptionsScreenTest {
    @get:Rule val compose = createComposeRule()

    private val flags = FakeFeatureFlags()
    private val vm = DeveloperOptionsViewModel(
        ObserveFeatureFlagsUseCase(flags),
        ObserveAppHealthUseCase(FakeEventLog()),
        SetFeatureFlagOverrideUseCase(flags),
        ResetFeatureFlagOverridesUseCase(flags),
        FakeAnalytics(),
    )

    @Test
    fun togglingAFlagShowsOverrideAndResetRestoresDefault() = runTest {
        compose.setContent {
            MpdTheme(darkTheme = false) {
                val state by vm.state.collectAsStateWithLifecycle()
                DeveloperOptionsScreen(state, {}, vm::toggle, vm::resetAll, {})
            }
        }
        compose.onNodeWithText("Route replay").assertExists()
        compose.onNodeWithText("route_replay · Scrubber and speed graph on trip detail").assertExists()
        compose.onNodeWithText("Reset all overrides").assertIsNotEnabled()

        val row = compose.onNodeWithText("Route replay", useUnmergedTree = false)
        row.assertIsOn()
        row.performClick()
        compose.waitForIdle()

        row.assertIsOff()
        compose.onNodeWithText("Override").assertExists()
        assertFalse(flags.isEnabled(FeatureFlag.ROUTE_REPLAY))

        compose.onNodeWithText("Reset all overrides").performScrollTo().assertIsEnabled().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Override").assertDoesNotExist()
        compose.onNodeWithText("Route replay").assertIsOn()
    }
}
