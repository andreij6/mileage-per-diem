package com.aijones.mileage.feature.trips.domain

import app.cash.turbine.test
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.testing.FakeClock
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals

class RateUseCasesTest {
    private val repo = FakeMileageRateRepository()
    private val getRate = GetMileageRateUseCase(repo)

    @Test
    fun irsRateIsEffectiveDated() = runTest {
        listOf(
            LocalDate.of(2026, 1, 1) to Money(725_000),
            LocalDate.of(2026, 6, 30) to Money(725_000),
            LocalDate.of(2026, 7, 1) to Money(760_000),
            LocalDate.of(2026, 11, 16) to Money(760_000),
            LocalDate.of(2027, 2, 1) to Money(760_000),
            LocalDate.of(2025, 12, 31) to Money(725_000),
        ).forEach { (date, expected) -> assertEquals(expected, getRate(date), "rate on $date") }
    }

    @Test
    fun customRateOverridesIrs() = runTest {
        repo.setCustomRate(Money(700_000))
        assertEquals(Money(700_000), getRate(LocalDate.of(2026, 6, 30)))
        assertEquals(Money(700_000), getRate(LocalDate.of(2026, 7, 1)))
    }

    @Test
    fun currentRateFollowsClockAndCustomRate() = runTest {
        val clock = FakeClock(Instant.parse("2026-06-30T12:00:00Z"))
        ObserveCurrentRateUseCase(repo, clock)().test {
            assertEquals(Money(725_000), awaitItem())
            repo.setCustomRate(Money(650_000))
            assertEquals(Money(650_000), awaitItem())
            repo.setCustomRate(null)
            assertEquals(Money(725_000), awaitItem())
        }
    }
}
