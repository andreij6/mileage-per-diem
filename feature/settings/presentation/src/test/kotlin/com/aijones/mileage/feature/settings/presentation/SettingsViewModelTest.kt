package com.aijones.mileage.feature.settings.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.featureflags.FakeFeatureFlags
import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.settings.domain.BatteryMode
import com.aijones.mileage.feature.settings.domain.FakeSettingsRepository
import com.aijones.mileage.feature.settings.domain.ObserveFeatureFlagUseCase
import com.aijones.mileage.feature.settings.domain.ObserveFeatureFlagsUseCase
import com.aijones.mileage.feature.settings.domain.ObserveMileageRateSettingsUseCase
import com.aijones.mileage.feature.settings.domain.ObserveSettingsUseCase
import com.aijones.mileage.feature.settings.domain.Settings
import com.aijones.mileage.feature.settings.domain.ThemeMode
import com.aijones.mileage.feature.settings.domain.UpdateSettingsUseCase
import com.aijones.mileage.feature.trips.domain.FakeMileageRateRepository
import com.aijones.mileage.feature.trips.domain.FakeVehicleRepository
import com.aijones.mileage.feature.trips.domain.ObserveCurrentRateUseCase
import com.aijones.mileage.feature.trips.domain.ObserveVehiclesUseCase
import com.aijones.mileage.feature.trips.domain.Vehicle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class SettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val settings = FakeSettingsRepository(Settings(defaultPurpose = "Client meeting", reportName = "Jordan Lee", company = "Acme Corp"))
    private val vehicles = FakeVehicleRepository(listOf(Vehicle(1, "2021 Civic", "Personal", true), Vehicle(2, "Rental", "", false)))
    private val rates = FakeMileageRateRepository()
    private val analytics = FakeAnalytics()

    private fun viewModel(flags: FakeFeatureFlags = FakeFeatureFlags()) = SettingsViewModel(
        ObserveSettingsUseCase(settings),
        ObserveVehiclesUseCase(vehicles),
        ObserveMileageRateSettingsUseCase(rates, ObserveCurrentRateUseCase(rates, FakeClock())),
        ObserveFeatureFlagUseCase(flags),
        ObserveFeatureFlagsUseCase(flags),
        UpdateSettingsUseCase(settings),
        analytics,
    )

    private val loaded = SettingsUiState(
        settings = Settings(defaultPurpose = "Client meeting", reportName = "Jordan Lee", company = "Acme Corp"),
        vehicleCount = 2,
        customRate = false,
        rate = "76¢",
        driveDetectionAvailable = true,
        flagCount = 4,
    )

    @Test
    fun `loads settings, vehicle count, IRS rate and flags and tracks the screen view`() = runTest {
        viewModel().state.test {
            assertEquals(loaded, expectMostRecentItem())
        }
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("settings")), analytics.events)
    }

    @Test
    fun `theme change is persisted and closes the picker`() = runTest {
        val vm = viewModel()
        vm.state.test {
            assertEquals(loaded, expectMostRecentItem())
            vm.edit(SettingsEdit.THEME)
            assertEquals(loaded.copy(editing = SettingsEdit.THEME), awaitItem())
            vm.setTheme(ThemeMode.DARK)
            assertEquals(loaded.copy(settings = loaded.settings.copy(theme = ThemeMode.DARK)), expectMostRecentItem())
        }
        assertEquals(ThemeMode.DARK, settings.settings.theme)
    }

    @Test
    fun `text and choice edits are saved`() = runTest {
        val vm = viewModel()
        vm.state.test {
            expectMostRecentItem()
            vm.setDefaultPurpose("Site visit")
            vm.setReportName("Sam Rivera")
            vm.setCompany("Globex")
            vm.setBatteryMode(BatteryMode.SAVER)
            vm.setDriveDetection(true)
            assertEquals(
                Settings("Site visit", BatteryMode.SAVER, true, "Sam Rivera", "Globex"),
                expectMostRecentItem().settings,
            )
        }
    }

    @Test
    fun `drive detection row is hidden when the flag is off`() = runTest {
        val flags = FakeFeatureFlags(FeatureFlag.DRIVE_DETECTION to false)
        val vm = viewModel(flags)
        vm.state.test {
            assertEquals(loaded.copy(driveDetectionAvailable = false), expectMostRecentItem())
            flags.setOverride(FeatureFlag.DRIVE_DETECTION, true)
            assertEquals(loaded, awaitItem())
        }
    }

    @Test
    fun `a company rate shows as the company rate`() = runTest {
        rates.setCustomRate(Money.ofCents(65))
        viewModel().state.test {
            assertEquals(loaded.copy(customRate = true, rate = "65¢"), expectMostRecentItem())
        }
    }
}
