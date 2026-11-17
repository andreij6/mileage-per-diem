package com.aijones.mileage.feature.settings.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.settings.domain.ObserveMileageRateSettingsUseCase
import com.aijones.mileage.feature.settings.domain.SetCustomRateUseCase
import com.aijones.mileage.feature.trips.domain.FakeMileageRateRepository
import com.aijones.mileage.feature.trips.domain.MileageRate
import com.aijones.mileage.feature.trips.domain.ObserveCurrentRateUseCase
import com.aijones.mileage.feature.trips.domain.SampleRates
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule

class MileageRateViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val clock = FakeClock()
    private val rates = FakeMileageRateRepository(
        SampleRates.irs2026 + MileageRate(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), Money(700_000), "IRS Notice 2025-5"),
    )
    private val analytics = FakeAnalytics()
    private val vm = MileageRateViewModel(
        ObserveMileageRateSettingsUseCase(rates, ObserveCurrentRateUseCase(rates, clock)),
        SetCustomRateUseCase(rates),
        clock,
        analytics,
    )
    private val table = listOf(
        RateRow("Jul 1 – Dec 31, 2026", "76¢", true),
        RateRow("Jan 1 – Jun 30, 2026", "72.5¢", false),
        RateRow("2025", "70¢", false),
    )

    @Test
    fun `shows the IRS table newest first with the current period marked`() = runTest {
        vm.state.test {
            assertEquals(MileageRateUiState(), awaitItem())
            assertEquals(MileageRateUiState(false, "", table), awaitItem())
        }
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("mileage_rate")), analytics.events)
    }

    @Test
    fun `typing a company rate selects and saves it in dollars per mile`() = runTest {
        vm.state.test {
            assertEquals(MileageRateUiState(), awaitItem())
            assertEquals(MileageRateUiState(false, "", table), awaitItem())
            vm.changeRate("0.6")
            assertEquals(MileageRateUiState(true, "0.6", table), awaitItem())
            vm.changeRate("0.655")
            assertEquals(MileageRateUiState(true, "0.655", table), awaitItem())
        }
        assertEquals(Money(655_000), rates.customRate)
    }

    @Test
    fun `an invalid rate is flagged and not saved`() = runTest {
        vm.state.test {
            awaitItem()
            awaitItem()
            vm.selectCompany()
            val state = awaitItem()
            assertTrue(state.companySelected)
            assertTrue(state.rateInvalid)
            vm.changeRate("abc")
            assertTrue(awaitItem().rateInvalid)
        }
        assertNull(rates.customRate)
    }

    @Test
    fun `an existing company rate is shown and choosing IRS clears it`() = runTest {
        rates.setCustomRate(Money.ofCents(65))
        vm.state.test {
            assertEquals(MileageRateUiState(), awaitItem())
            assertEquals(MileageRateUiState(true, "$0.65", table), awaitItem())
            vm.selectIrs()
            val state = awaitItem()
            assertFalse(state.companySelected)
            assertEquals("$0.65", state.rateText)
        }
        assertNull(rates.customRate)
    }

    @Test
    fun `dollar input parsing`() {
        assertEquals(Money(650_000), parseDollarsPerMile("$0.65"))
        assertEquals(Money(725_000), parseDollarsPerMile(" 0.725 "))
        assertNull(parseDollarsPerMile("0"))
        assertNull(parseDollarsPerMile("-1"))
        assertNull(parseDollarsPerMile(""))
        assertEquals("$0.70", formatDollarsPerMile(Money(700_000)))
        assertEquals("$0.655", formatDollarsPerMile(Money(655_000)))
    }
}
