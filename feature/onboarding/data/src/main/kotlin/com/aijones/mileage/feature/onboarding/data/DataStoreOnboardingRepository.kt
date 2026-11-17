package com.aijones.mileage.feature.onboarding.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.aijones.mileage.core.common.IoDispatcher
import com.aijones.mileage.core.datastore.SettingsStore
import com.aijones.mileage.feature.onboarding.domain.OnboardingRepository
import com.aijones.mileage.feature.onboarding.domain.OnboardingState
import com.aijones.mileage.feature.onboarding.domain.OnboardingStep
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class DataStoreOnboardingRepository @Inject constructor(
    @SettingsStore private val store: DataStore<Preferences>,
    @IoDispatcher private val io: CoroutineDispatcher,
) : OnboardingRepository {
    override fun observe(): Flow<OnboardingState> = store.data
        .map { p -> OnboardingState(OnboardingStep.entries.filterTo(mutableSetOf()) { it.name in p[COMPLETED].orEmpty() }) }
        .distinctUntilChanged()
        .flowOn(io)

    override suspend fun complete(step: OnboardingStep) {
        withContext(io) { store.edit { it[COMPLETED] = it[COMPLETED].orEmpty() + step.name } }
    }

    private companion object {
        val COMPLETED = stringSetPreferencesKey("onboarding_completed")
    }
}
