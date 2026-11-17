package com.aijones.mileage.core.featureflags.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.datastore.FlagsStore
import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.featureflags.FeatureFlags
import com.aijones.mileage.core.featureflags.FlagSource
import com.aijones.mileage.core.featureflags.FlagState
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@Singleton
internal class DataStoreFeatureFlags @Inject constructor(
    @FlagsStore private val store: DataStore<Preferences>,
    private val analytics: Analytics,
) : FeatureFlags {
    private val exposed = ConcurrentHashMap.newKeySet<FeatureFlag>()

    override fun observe(flag: FeatureFlag): Flow<Boolean> =
        store.data.map { it.state(flag) }.distinctUntilChanged().onEach(::expose).map { it.enabled }

    override fun observeAll(): Flow<List<FlagState>> = store.data.map { p -> FeatureFlag.entries.map { p.state(it) } }

    override suspend fun isEnabled(flag: FeatureFlag): Boolean = store.data.first().state(flag).also(::expose).enabled

    override suspend fun setOverride(flag: FeatureFlag, enabled: Boolean?) {
        exposed -= flag
        store.edit { if (enabled == null) it.remove(flag.prefKey) else it[flag.prefKey] = enabled }
    }

    override suspend fun resetOverrides() {
        exposed.clear()
        store.edit { it.clear() }
    }

    private fun expose(state: FlagState) {
        if (exposed.add(state.flag)) {
            analytics.track(AnalyticsEvent.FeatureFlagExposed(state.flag.key, state.enabled, state.source.name.lowercase()))
        }
    }

    private fun Preferences.state(flag: FeatureFlag): FlagState {
        val override = this[flag.prefKey]
        return FlagState(flag, override ?: flag.default, if (override != null) FlagSource.OVERRIDE else FlagSource.DEFAULT)
    }

    private val FeatureFlag.prefKey get() = booleanPreferencesKey(key)
}
