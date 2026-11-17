package com.aijones.mileage.feature.trips.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.aijones.mileage.core.datastore.SettingsStore
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.trips.domain.MileageRate
import com.aijones.mileage.feature.trips.domain.MileageRateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

internal val IRS_BUSINESS_RATES = listOf(
    MileageRate(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), Money(700_000), "IRS Notice 2025-5"),
    MileageRate(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), Money(725_000), "IRS Notice 2026-10"),
    MileageRate(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31), Money(760_000), "IRS Announcement 2026-11"),
)

internal val CUSTOM_RATE_KEY = longPreferencesKey("trips_custom_rate_micros")

internal class BundledMileageRateRepository @Inject constructor(
    @SettingsStore private val store: DataStore<Preferences>,
) : MileageRateRepository {

    override suspend fun irsRates(): List<MileageRate> = IRS_BUSINESS_RATES

    override fun observeCustomRate(): Flow<Money?> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[CUSTOM_RATE_KEY]?.let(::Money) }
        .distinctUntilChanged()

    override suspend fun setCustomRate(rate: Money?) {
        store.edit { prefs -> if (rate == null) prefs.remove(CUSTOM_RATE_KEY) else prefs[CUSTOM_RATE_KEY] = rate.micros }
    }
}
