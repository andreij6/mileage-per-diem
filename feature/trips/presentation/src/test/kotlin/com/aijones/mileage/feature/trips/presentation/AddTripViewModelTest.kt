package com.aijones.mileage.feature.trips.presentation

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.core.testing.MainDispatcherRule
import com.aijones.mileage.feature.trips.domain.AddManualTripUseCase
import com.aijones.mileage.feature.trips.domain.FakeMileageRateRepository
import com.aijones.mileage.feature.trips.domain.FakeTripRepository
import com.aijones.mileage.feature.trips.domain.GetMileageRateUseCase
import com.aijones.mileage.feature.trips.domain.TripStatus
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddTripViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val analytics = FakeAnalytics()
    private val clock = FakeClock()
    private val repo = FakeTripRepository()

    private fun viewModel(): AddTripViewModel {
        val rate = GetMileageRateUseCase(FakeMileageRateRepository())
        return AddTripViewModel(AddManualTripUseCase(repo, rate, analytics, clock), rate, analytics, clock)
    }

    @Test
    fun estimatesAtTheRateForTheChosenDate() = runTest {
        val vm = viewModel()
        vm.state.test {
            awaitItem().let { s ->
                assertEquals("Mon, Nov 16, 2026", s.dateLabel)
                assertNull(s.estimate)
                assertEquals(false, s.canAdd)
            }
            vm.edit { it.copy(distance = "18.4") }
            awaitItem().let { s ->
                assertEquals(AddTripEstimate("18.4 mi", "76¢", "$13.98"), s.estimate)
                assertEquals("Nov 16", s.rateDate)
                assertEquals(true, s.canAdd)
            }
            vm.edit { it.copy(roundTrip = true) }
            assertEquals(AddTripEstimate("36.8 mi", "76¢", "$27.97"), awaitItem().estimate)
            vm.edit { it.copy(date = LocalDate.of(2026, 6, 30)) }
            assertEquals(AddTripEstimate("36.8 mi", "72.5¢", "$26.68"), expectMostRecentItem().estimate)
            vm.edit { it.copy(date = LocalDate.of(2027, 1, 1)) }
            assertEquals(LocalDate.of(2026, 11, 16), expectMostRecentItem().form.date)
        }
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("trip_add")), analytics.events)
    }

    @Test
    fun addingStoresAManualTripAndFinishes() = runTest {
        val vm = viewModel()
        vm.state.test {
            awaitItem()
            vm.edit { it.copy(from = "Brooklyn office", to = "Acme HQ", distance = "18.4 mi", purpose = "Client meeting", roundTrip = true) }
            awaitItem()
            vm.add()
            assertTrue(expectMostRecentItem().added)
        }
        val stored = repo.trips.single()
        assertEquals(TripStatus.MANUAL, stored.status)
        assertEquals(368L, stored.distance.tenthsOfMile)
        assertEquals("Brooklyn office", stored.startLabel)
        assertEquals("Client meeting", stored.purpose)
        assertEquals(AnalyticsEvent.TripAddedManual(368), analytics.events.last())
    }
}
