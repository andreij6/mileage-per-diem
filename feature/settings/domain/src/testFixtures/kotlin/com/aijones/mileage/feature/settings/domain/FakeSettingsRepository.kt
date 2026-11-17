package com.aijones.mileage.feature.settings.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(initial: Settings = Settings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    val settings: Settings get() = state.value

    override fun observe(): Flow<Settings> = state

    override suspend fun update(transform: (Settings) -> Settings) = state.update(transform)
}

class FakeDataResetter : DataResetter {
    var resets = 0
        private set

    override suspend fun deleteAllTripsAndReports() {
        resets++
    }
}
