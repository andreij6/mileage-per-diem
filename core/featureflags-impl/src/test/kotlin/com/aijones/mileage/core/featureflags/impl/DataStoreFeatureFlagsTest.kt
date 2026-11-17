package com.aijones.mileage.core.featureflags.impl

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.featureflags.FlagSource
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class DataStoreFeatureFlagsTest {
    private val analytics = FakeAnalytics()
    private fun TestScope.flags(): DataStoreFeatureFlags {
        val dir = Files.createTempDirectory("flags").toFile()
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(dir, "flags.preferences_pb") }
        return DataStoreFeatureFlags(store, analytics)
    }

    @Test
    fun `flags use defaults until overridden and the override can be cleared`() = runTest {
        val flags = flags()
        assertTrue(flags.isEnabled(FeatureFlag.YEAR_IN_REVIEW))
        flags.setOverride(FeatureFlag.YEAR_IN_REVIEW, false)
        assertFalse(flags.isEnabled(FeatureFlag.YEAR_IN_REVIEW))
        flags.setOverride(FeatureFlag.YEAR_IN_REVIEW, null)
        assertTrue(flags.isEnabled(FeatureFlag.YEAR_IN_REVIEW))
    }

    @Test
    fun `observing a flag emits on toggle and reports exposure once per session`() = runTest {
        val flags = flags()
        flags.observe(FeatureFlag.ROUTE_REPLAY).test {
            assertTrue(awaitItem())
            flags.setOverride(FeatureFlag.ROUTE_REPLAY, false)
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        val exposures = analytics.eventsOf<AnalyticsEvent.FeatureFlagExposed>()
        assertEquals(listOf("default", "override"), exposures.map { it.source })
    }

    @Test
    fun `observeAll reports the source of every flag and reset clears overrides`() = runTest {
        val flags = flags()
        flags.setOverride(FeatureFlag.LIVE_UPDATE, true)
        flags.observeAll().test {
            val states = awaitItem().associateBy { it.flag }
            assertEquals(FlagSource.OVERRIDE, states.getValue(FeatureFlag.LIVE_UPDATE).source)
            assertTrue(states.getValue(FeatureFlag.LIVE_UPDATE).enabled)
            assertEquals(FlagSource.DEFAULT, states.getValue(FeatureFlag.DRIVE_DETECTION).source)
            flags.resetOverrides()
            assertFalse(awaitItem().first { it.flag == FeatureFlag.LIVE_UPDATE }.enabled)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
