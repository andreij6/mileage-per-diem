package com.aijones.mileage.feature.trips.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.trips.domain.irsRateOn
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BundledMileageRateRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun bundledTableHasTheIrsBusinessRates() = runTest {
        val repo = BundledMileageRateRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "a.preferences_pb") })
        val table = repo.irsRates()
        assertEquals(Money(700_000), irsRateOn(table, LocalDate.of(2025, 12, 31)))
        assertEquals(Money(725_000), irsRateOn(table, LocalDate.of(2026, 1, 1)))
        assertEquals(Money(725_000), irsRateOn(table, LocalDate.of(2026, 6, 30)))
        assertEquals(Money(760_000), irsRateOn(table, LocalDate.of(2026, 7, 1)))
        assertEquals(Money(760_000), irsRateOn(table, LocalDate.of(2026, 12, 31)))
    }

    @Test
    fun customRateIsStoredAndCleared() = runTest {
        val file = File(tmp.root, "b.preferences_pb")
        val repo = BundledMileageRateRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { file })
        repo.observeCustomRate().test {
            assertNull(awaitItem())
            repo.setCustomRate(Money(655_000))
            assertEquals(Money(655_000), awaitItem())
            repo.setCustomRate(null)
            assertNull(awaitItem())
        }
    }
}
