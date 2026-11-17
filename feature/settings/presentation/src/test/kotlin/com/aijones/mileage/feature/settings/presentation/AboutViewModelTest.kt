package com.aijones.mileage.feature.settings.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.settings.domain.DeleteAllDataUseCase
import com.aijones.mileage.feature.settings.domain.FakeDataResetter
import com.aijones.mileage.feature.settings.domain.FakeSettingsRepository
import com.aijones.mileage.feature.settings.domain.UnlockDeveloperOptionsUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class AboutViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val settings = FakeSettingsRepository()
    private val resetter = FakeDataResetter()
    private val analytics = FakeAnalytics()
    private val vm = AboutViewModel(UnlockDeveloperOptionsUseCase(settings), DeleteAllDataUseCase(resetter), analytics)

    @Test
    fun `seven version taps count down and unlock developer options`() = runTest {
        vm.state.test {
            assertEquals(AboutUiState(), awaitItem())
            repeat(6) { i ->
                vm.tapVersion()
                assertEquals(AboutUiState(tapsRemaining = 6 - i), awaitItem())
                assertFalse(settings.settings.developerUnlocked)
            }
            vm.tapVersion()
            assertEquals(AboutUiState(tapsRemaining = 0), awaitItem())
        }
        assertTrue(settings.settings.developerUnlocked)
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("about")), analytics.events)
    }

    @Test
    fun `deleting all data asks first and then resets`() = runTest {
        vm.state.test {
            assertEquals(AboutUiState(), awaitItem())
            vm.show(AboutDialog.CONFIRM_DELETE)
            assertEquals(AboutUiState(dialog = AboutDialog.CONFIRM_DELETE), awaitItem())
            assertEquals(0, resetter.resets)
            vm.confirmDelete()
            assertEquals(AboutUiState(), awaitItem())
            assertEquals(AboutUiState(deleted = true), awaitItem())
        }
        assertEquals(1, resetter.resets)
    }

    @Test
    fun `info dialogs open and close`() = runTest {
        vm.state.test {
            awaitItem()
            vm.show(AboutDialog.LICENSES)
            assertEquals(AboutDialog.LICENSES, awaitItem().dialog)
            vm.dismiss()
            assertEquals(AboutUiState(), awaitItem())
        }
        assertEquals(0, resetter.resets)
    }
}
