package com.aijones.mileage.core.featureflags

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class FakeFeatureFlags(vararg overrides: Pair<FeatureFlag, Boolean>) : FeatureFlags {
    private val state = MutableStateFlow(overrides.toMap())

    override fun observe(flag: FeatureFlag): Flow<Boolean> = state.map { it[flag] ?: flag.default }.distinctUntilChanged()

    override fun observeAll(): Flow<List<FlagState>> = state.map { o ->
        FeatureFlag.entries.map { f -> FlagState(f, o[f] ?: f.default, if (f in o) FlagSource.OVERRIDE else FlagSource.DEFAULT) }
    }

    override suspend fun isEnabled(flag: FeatureFlag) = state.value[flag] ?: flag.default

    override suspend fun setOverride(flag: FeatureFlag, enabled: Boolean?) {
        state.value = if (enabled == null) state.value - flag else state.value + (flag to enabled)
    }

    override suspend fun resetOverrides() {
        state.value = emptyMap()
    }
}
