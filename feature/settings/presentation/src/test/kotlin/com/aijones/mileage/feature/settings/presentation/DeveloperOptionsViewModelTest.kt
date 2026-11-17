package com.aijones.mileage.feature.settings.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.analytics.LoggedEvent
import com.aijones.mileage.core.featureflags.FakeFeatureFlags
import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.featureflags.FlagSource
import com.aijones.mileage.core.featureflags.FlagState
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.settings.domain.FakeEventLog
import com.aijones.mileage.feature.settings.domain.ObserveAppHealthUseCase
import com.aijones.mileage.feature.settings.domain.ObserveFeatureFlagsUseCase
import com.aijones.mileage.feature.settings.domain.ResetFeatureFlagOverridesUseCase
import com.aijones.mileage.feature.settings.domain.SetFeatureFlagOverrideUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class DeveloperOptionsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val flags = FakeFeatureFlags()
    private val log = FakeEventLog((1L..3L).map { LoggedEvent(it, "session_start", emptyMap(), it, "s") })
    private val analytics = FakeAnalytics()
    private val vm = DeveloperOptionsViewModel(
        ObserveFeatureFlagsUseCase(flags),
        ObserveAppHealthUseCase(log),
        SetFeatureFlagOverrideUseCase(flags),
        ResetFeatureFlagOverridesUseCase(flags),
        analytics,
    )
    private val defaults = FeatureFlag.entries.map { FlagState(it, it.default, FlagSource.DEFAULT) }

    @Test
    fun `lists every flag with its source and the event count`() = runTest {
        vm.state.test {
            assertEquals(DeveloperOptionsUiState(), awaitItem())
            val state = awaitItem()
            assertEquals(DeveloperOptionsUiState(defaults, 3), state)
            assertFalse(state.hasOverrides)
        }
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("developer_options")), analytics.events)
    }

    @Test
    fun `toggling a flag overrides it and reset restores defaults`() = runTest {
        vm.state.test {
            skipItems(2)
            vm.toggle(FeatureFlag.ROUTE_REPLAY, false)
            val overridden = awaitItem()
            assertEquals(FlagState(FeatureFlag.ROUTE_REPLAY, false, FlagSource.OVERRIDE), overridden.flags.single { it.flag == FeatureFlag.ROUTE_REPLAY })
            assertTrue(overridden.hasOverrides)
            assertFalse(flags.isEnabled(FeatureFlag.ROUTE_REPLAY))
            vm.resetAll()
            assertEquals(DeveloperOptionsUiState(defaults, 3), awaitItem())
        }
        assertTrue(flags.isEnabled(FeatureFlag.ROUTE_REPLAY))
    }
}
