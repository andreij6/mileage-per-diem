package com.aijones.mileage.feature.perdiem.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.navigation.AddStayRoute
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.perdiem.domain.AddStayUseCase
import com.aijones.mileage.feature.perdiem.domain.EstimateStayUseCase
import com.aijones.mileage.feature.perdiem.domain.FakePerDiemRateRepository
import com.aijones.mileage.feature.perdiem.domain.FakeRecentSearchRepository
import com.aijones.mileage.feature.perdiem.domain.FakeStayRepository
import com.aijones.mileage.feature.perdiem.domain.GetRatesForLocationUseCase
import com.aijones.mileage.feature.perdiem.domain.SamplePerDiem
import com.aijones.mileage.feature.perdiem.domain.Stay
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class AddStayViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val analytics = FakeAnalytics()
    private val stays = FakeStayRepository()

    private fun viewModel() = AddStayViewModel(
        AddStayRoute(SamplePerDiem.NYC_LOCATION.id),
        GetRatesForLocationUseCase(FakePerDiemRateRepository(), FakeRecentSearchRepository()),
        EstimateStayUseCase(),
        AddStayUseCase(stays, analytics),
        analytics,
        FakeClock(),
    )

    private val nov16 = LocalDate.of(2026, 11, 16)
    private val nov18 = LocalDate.of(2026, 11, 18)
    private val nov25 = LocalDate.of(2026, 11, 25)

    private fun nyc(
        checkIn: LocalDate,
        checkOut: LocalDate,
        includeLodging: Boolean,
        days: Int,
        travelDays: Int,
        fullDays: Int,
        lodging: Long,
        saved: Boolean = false,
    ) = AddStayUiState(
        checkIn = checkIn,
        checkOut = checkOut,
        includeLodging = includeLodging,
        locationLabel = "New York City, NY",
        loaded = true,
        days = days,
        travelDays = travelDays,
        travelDayRate = Money.ofDollars(69),
        travelDaysTotal = Money.ofDollars(69L * travelDays),
        fullDays = fullDays,
        fullDayRate = Money.ofDollars(92),
        fullDaysTotal = Money.ofDollars(92L * fullDays),
        nights = days - 1,
        lodgingTotal = Money.ofDollars(lodging),
        total = Money.ofDollars(69L * travelDays + 92L * fullDays + lodging),
        saved = saved,
    )

    @Test
    fun `estimate follows dates and the lodging switch`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(nyc(nov16, nov18, false, 3, 2, 1, 0), awaitItem())
            vm.setIncludeLodging(true)
            assertEquals(nyc(nov16, nov18, true, 3, 2, 1, 286 * 2), awaitItem())
            vm.setCheckIn(nov18)
            assertEquals(nyc(nov18, nov18, true, 1, 1, 0, 0), awaitItem())
            vm.setCheckOut(nov25)
            assertEquals(nyc(nov18, nov25, true, 8, 2, 6, 286 * 7), awaitItem())
            vm.setIncludeLodging(false)
            assertEquals(nyc(nov18, nov25, false, 8, 2, 6, 0), awaitItem())
        }
    }

    @Test
    fun `check-in after check-out moves check-out, and check-out never precedes check-in`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            vm.setCheckIn(nov25)
            assertEquals(nyc(nov25, nov25, false, 1, 1, 0, 0), awaitItem())
            vm.setCheckOut(nov18)
            expectNoEvents()
        }
    }

    @Test
    fun `adding the stay saves it through the use case and tracks stay_added`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            vm.setCheckIn(nov18)
            vm.setCheckOut(nov25)
            skipItems(2)
            vm.save()
            assertEquals(nyc(nov18, nov25, false, 8, 2, 6, 0).copy(saving = true), awaitItem())
            assertEquals(nyc(nov18, nov25, false, 8, 2, 6, 0, saved = true), awaitItem())
        }
        assertEquals(
            listOf(
                Stay(1, 2, "New York City, NY", 2027, nov18, nov25, false, Money.ofDollars(92), Money.ofDollars(690), Money.ZERO),
            ),
            stays.stays,
        )
        assertEquals(listOf(AnalyticsEvent.ScreenView("add_stay"), AnalyticsEvent.StayAdded(8, false)), analytics.events)
    }
}
