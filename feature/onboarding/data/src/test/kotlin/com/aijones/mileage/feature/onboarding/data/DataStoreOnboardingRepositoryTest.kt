package com.aijones.mileage.feature.onboarding.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.aijones.mileage.feature.onboarding.domain.OnboardingState
import com.aijones.mileage.feature.onboarding.domain.OnboardingStep
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class DataStoreOnboardingRepositoryTest {
    private val dir = Files.createTempDirectory("onboarding").toFile()

    private fun TestScope.repository() = DataStoreOnboardingRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(dir, "settings.preferences_pb") },
        StandardTestDispatcher(testScheduler),
    )

    @Test
    fun `starts with no completed steps`() = runTest {
        val state = repository().observe().first()
        assertEquals(OnboardingState(), state)
        assertEquals(OnboardingStep.WELCOME, state.next)
    }

    @Test
    fun `completing steps accumulates until onboarding is done`() = runTest {
        val repo = repository()
        repo.observe().test {
            assertEquals(OnboardingState(), awaitItem())
            repo.complete(OnboardingStep.WELCOME)
            assertEquals(setOf(OnboardingStep.WELCOME), awaitItem().completed)
            repo.complete(OnboardingStep.LOCATION)
            assertEquals(OnboardingStep.NOTIFICATIONS, awaitItem().next)
            repo.complete(OnboardingStep.NOTIFICATIONS)
            assertTrue(awaitItem().isDone)
        }
    }

    @Test
    fun `completing a step twice is idempotent`() = runTest {
        val repo = repository()
        repo.complete(OnboardingStep.WELCOME)
        repo.complete(OnboardingStep.WELCOME)
        assertEquals(setOf(OnboardingStep.WELCOME), repo.observe().first().completed)
    }
}
