package com.aijones.mileage.feature.perdiem.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.aijones.mileage.feature.perdiem.domain.PerDiemLocation
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DataStoreRecentSearchRepositoryTest {
    private val db = seededPerDiemDatabase(ApplicationProvider.getApplicationContext<Context>())
    private val dir: File = Files.createTempDirectory("perdiem-recent").toFile()

    @After
    fun close() {
        db.close()
        dir.deleteRecursively()
    }

    private fun location(id: Long) = PerDiemLocation(id, "x", "y", null, 2027, false)

    @Test
    fun `most recent first, deduplicated, resolved through the rates database`() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(dir, "settings.preferences_pb") }
        val repository = DataStoreRecentSearchRepository(store, { db }, Dispatchers.Unconfined)
        repository.observe().test {
            assertEquals(emptyList(), awaitItem())
            repository.record(location(3))
            assertEquals(listOf("Boston / Cambridge, MA"), awaitItem().map { it.label })
            repository.record(location(2))
            assertEquals(listOf("New York City, NY", "Boston / Cambridge, MA"), awaitItem().map { it.label })
            repository.record(location(3))
            assertEquals(listOf("Boston / Cambridge, MA", "New York City, NY"), awaitItem().map { it.label })
        }
    }

    @Test
    fun `keeps five and skips ids missing from the database`() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(dir, "settings.preferences_pb") }
        val repository = DataStoreRecentSearchRepository(store, { db }, Dispatchers.Unconfined)
        (10L..14L).forEach { repository.record(location(it)) }
        listOf(4L, 3L, 2L).forEach { repository.record(location(it)) }
        assertEquals(listOf(2L, 3L, 4L), repository.observe().first().map { it.id })
        assertEquals("2,3,4,14,13", store.data.first().asMap().values.single())
    }
}
