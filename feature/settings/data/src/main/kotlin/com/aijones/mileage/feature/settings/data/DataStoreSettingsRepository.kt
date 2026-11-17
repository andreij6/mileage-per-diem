package com.aijones.mileage.feature.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aijones.mileage.core.common.IoDispatcher
import com.aijones.mileage.core.datastore.SettingsStore
import com.aijones.mileage.feature.settings.domain.BatteryMode
import com.aijones.mileage.feature.settings.domain.Settings
import com.aijones.mileage.feature.settings.domain.SettingsRepository
import com.aijones.mileage.feature.settings.domain.ThemeMode
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class DataStoreSettingsRepository @Inject constructor(
    @SettingsStore private val store: DataStore<Preferences>,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SettingsRepository {
    override fun observe(): Flow<Settings> = store.data.map { it.toSettings() }.distinctUntilChanged().flowOn(io)

    override suspend fun update(transform: (Settings) -> Settings) {
        withContext(io) { store.edit { it.write(transform(it.toSettings())) } }
    }

    private fun Preferences.toSettings() = Settings(
        defaultPurpose = this[DEFAULT_PURPOSE].orEmpty(),
        batteryMode = enumOr(this[BATTERY_MODE], BatteryMode.AUTO),
        driveDetection = this[DRIVE_DETECTION] ?: false,
        reportName = this[REPORT_NAME].orEmpty(),
        company = this[COMPANY].orEmpty(),
        employeeId = this[EMPLOYEE_ID].orEmpty(),
        theme = enumOr(this[THEME], ThemeMode.SYSTEM),
        developerUnlocked = this[DEVELOPER_UNLOCKED] ?: false,
    )

    private fun MutablePreferences.write(s: Settings) {
        this[DEFAULT_PURPOSE] = s.defaultPurpose
        this[BATTERY_MODE] = s.batteryMode.name
        this[DRIVE_DETECTION] = s.driveDetection
        this[REPORT_NAME] = s.reportName
        this[COMPANY] = s.company
        this[EMPLOYEE_ID] = s.employeeId
        this[THEME] = s.theme.name
        this[DEVELOPER_UNLOCKED] = s.developerUnlocked
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback

    private companion object {
        val DEFAULT_PURPOSE = stringPreferencesKey("settings_default_purpose")
        val BATTERY_MODE = stringPreferencesKey("settings_battery_mode")
        val DRIVE_DETECTION = booleanPreferencesKey("settings_drive_detection")
        val REPORT_NAME = stringPreferencesKey("settings_report_name")
        val COMPANY = stringPreferencesKey("settings_company")
        val EMPLOYEE_ID = stringPreferencesKey("settings_employee_id")
        val THEME = stringPreferencesKey("settings_theme")
        val DEVELOPER_UNLOCKED = booleanPreferencesKey("settings_developer_unlocked")
    }
}
