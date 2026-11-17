package com.aijones.mileage.feature.settings.domain

import app.cash.turbine.test
import com.aijones.mileage.core.analytics.LoggedEvent
import com.aijones.mileage.core.featureflags.FakeFeatureFlags
import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.featureflags.FlagSource
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.feature.trips.domain.FakeMileageRateRepository
import com.aijones.mileage.feature.trips.domain.ObserveCurrentRateUseCase
import com.aijones.mileage.feature.trips.domain.SampleRates
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsUseCasesTest {
    private val repo = FakeSettingsRepository()

    @Test
    fun updateIsObserved() = runTest {
        ObserveSettingsUseCase(repo)().test {
            assertEquals(Settings(), awaitItem())
            UpdateSettingsUseCase(repo).invoke { it.copy(batteryMode = BatteryMode.SAVER, company = "Acme") }
            assertEquals(Settings(batteryMode = BatteryMode.SAVER, company = "Acme"), awaitItem())
        }
    }

    @Test
    fun featureFlagOverridesAndReset() = runTest {
        val flags = FakeFeatureFlags()
        val observe = ObserveFeatureFlagsUseCase(flags)
        SetFeatureFlagOverrideUseCase(flags)(FeatureFlag.LIVE_UPDATE, true)
        val live = observe().first().single { it.flag == FeatureFlag.LIVE_UPDATE }
        assertTrue(live.enabled)
        assertEquals(FlagSource.OVERRIDE, live.source)

        ResetFeatureFlagOverridesUseCase(flags)()
        assertTrue(observe().first().all { it.source == FlagSource.DEFAULT && it.enabled == it.flag.default })
    }

    @Test
    fun appHealthCombinesSummaryAndRecentEvents() = runTest {
        val since = Instant.parse("2026-11-01T00:00:00Z")
        val old = LoggedEvent(1, "session_start", emptyMap(), since.toEpochMilli() - 1, "s0")
        val events = listOf(old) + (2L..30L).map {
            LoggedEvent(it, if (it == 30L) "trip_recording_failed" else "session_start", if (it == 30L) mapOf("reason" to "location_off") else emptyMap(), since.toEpochMilli() + it, "s1")
        }
        val log = FakeEventLog(events)
        val health = ObserveAppHealthUseCase(log)(since).first()
        assertEquals(since.toEpochMilli(), log.lastSummarySinceMs)
        assertEquals(29, health.summary.totalEvents)
        assertEquals("location_off", health.summary.lastFailureReason)
        assertEquals(20, health.recentEvents.size)
        assertEquals(30L, health.recentEvents.first().id)
    }

    @Test
    fun exportEventLogIsNdjson() = runTest {
        val log = FakeEventLog(listOf(LoggedEvent(1, "trip_deleted", emptyMap(), 5, "s"), LoggedEvent(2, "screen_view", mapOf("screen" to "home"), 6, "s")))
        val lines = ExportEventLogUseCase(log)().lines()
        assertEquals(2, lines.size)
        assertTrue(lines.all { it.startsWith("{") && it.endsWith("}") })
    }

    @Test
    fun mileageRateSettingsShowTableCustomAndEffectiveRate() = runTest {
        val rates = FakeMileageRateRepository()
        val useCase = ObserveMileageRateSettingsUseCase(rates, ObserveCurrentRateUseCase(rates, FakeClock()))
        useCase().test {
            assertEquals(MileageRateSettings(SampleRates.irs2026, null, Money(760_000)), awaitItem())
            SetCustomRateUseCase(rates)(Money(700_000))
            val updated = expectMostRecentItem()
            assertEquals(Money(700_000), updated.customRate)
            assertEquals(Money(700_000), updated.effectiveRate)
        }
    }

    @Test
    fun customRateMustBePositiveOrCleared() = runTest {
        val rates = FakeMileageRateRepository(customRate = Money(700_000))
        assertFailsWith<IllegalArgumentException> { SetCustomRateUseCase(rates)(Money.ZERO) }
        SetCustomRateUseCase(rates)(null)
        assertNull(rates.customRate)
    }

    @Test
    fun deleteAllDataDelegatesToResetter() = runTest {
        val resetter = FakeDataResetter()
        DeleteAllDataUseCase(resetter)()
        assertEquals(1, resetter.resets)
    }

    @Test
    fun sevenTapsUnlockDeveloperOptions() = runTest {
        val unlock = UnlockDeveloperOptionsUseCase(repo)
        val remaining = (1..6).map { unlock() }
        assertEquals(listOf(6, 5, 4, 3, 2, 1), remaining)
        assertFalse(repo.settings.developerUnlocked)
        assertEquals(0, unlock())
        assertTrue(repo.settings.developerUnlocked)
        assertEquals(0, unlock())
    }

    @Test
    fun alreadyUnlockedNeedsNoTaps() = runTest {
        val unlocked = FakeSettingsRepository(Settings(developerUnlocked = true))
        assertEquals(0, UnlockDeveloperOptionsUseCase(unlocked)())
    }
}
