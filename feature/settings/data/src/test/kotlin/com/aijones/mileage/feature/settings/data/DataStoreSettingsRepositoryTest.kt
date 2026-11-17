package com.aijones.mileage.feature.settings.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.aijones.mileage.feature.settings.domain.BatteryMode
import com.aijones.mileage.feature.settings.domain.Settings
import com.aijones.mileage.feature.settings.domain.ThemeMode
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class DataStoreSettingsRepositoryTest {
    private val dir = Files.createTempDirectory("settings").toFile()

    private fun TestScope.repository() = DataStoreSettingsRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(dir, "settings.preferences_pb") },
        StandardTestDispatcher(testScheduler),
    )

    @Test
    fun `an empty store reads as default settings`() = runTest {
        assertEquals(Settings(), repository().observe().first())
    }

    @Test
    fun `updates persist every field and emit the new settings`() = runTest {
        val repo = repository()
        val updated = Settings("Client meeting", BatteryMode.SAVER, true, "Jordan Lee", "Acme Corp", "E-42", ThemeMode.DARK, true)
        repo.observe().test {
            assertEquals(Settings(), awaitItem())
            repo.update { updated }
            assertEquals(updated, awaitItem())
        }
    }

    @Test
    fun `update transforms the current value`() = runTest {
        val repo = repository()
        repo.update { it.copy(company = "Acme Corp") }
        repo.update { it.copy(theme = ThemeMode.LIGHT) }
        assertEquals(Settings(company = "Acme Corp", theme = ThemeMode.LIGHT), repo.observe().first())
    }
}
