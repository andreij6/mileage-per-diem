package com.aijones.mileage.feature.settings.domain

import com.aijones.mileage.core.analytics.HealthSummary
import com.aijones.mileage.core.analytics.LoggedEvent
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.feature.trips.domain.MileageRate
import kotlinx.coroutines.flow.Flow

enum class BatteryMode { AUTO, ACCURATE, SAVER }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val defaultPurpose: String = "",
    val batteryMode: BatteryMode = BatteryMode.AUTO,
    val driveDetection: Boolean = false,
    val reportName: String = "",
    val company: String = "",
    val employeeId: String = "",
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val developerUnlocked: Boolean = false,
)

data class AppHealth(val summary: HealthSummary, val recentEvents: List<LoggedEvent>)

data class MileageRateSettings(val irsRates: List<MileageRate>, val customRate: Money?, val effectiveRate: Money)

interface SettingsRepository {
    fun observe(): Flow<Settings>
    suspend fun update(transform: (Settings) -> Settings)
}

interface DataResetter {
    suspend fun deleteAllTripsAndReports()
}
