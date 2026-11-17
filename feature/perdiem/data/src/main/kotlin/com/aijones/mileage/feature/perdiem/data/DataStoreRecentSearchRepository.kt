package com.aijones.mileage.feature.perdiem.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aijones.mileage.core.common.IoDispatcher
import com.aijones.mileage.core.datastore.SettingsStore
import com.aijones.mileage.feature.perdiem.domain.PerDiemLocation
import com.aijones.mileage.feature.perdiem.domain.RecentSearchRepository
import dagger.Lazy
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

internal class DataStoreRecentSearchRepository @Inject constructor(
    @SettingsStore private val store: DataStore<Preferences>,
    private val database: Lazy<PerDiemDatabase>,
    @IoDispatcher private val io: CoroutineDispatcher,
) : RecentSearchRepository {
    override fun observe(): Flow<List<PerDiemLocation>> = store.data
        .map { decode(it[KEY]) }
        .distinctUntilChanged()
        .mapLatest { ids ->
            if (ids.isEmpty()) {
                emptyList()
            } else {
                val byId = database.get().dao().locations(ids).associateBy { it.id }
                ids.mapNotNull { byId[it]?.toDomain() }
            }
        }
        .flowOn(io)

    override suspend fun record(location: PerDiemLocation) {
        store.edit { prefs ->
            val ids = listOf(location.id) + decode(prefs[KEY]).filterNot { it == location.id }
            prefs[KEY] = ids.take(LIMIT).joinToString(",")
        }
    }

    private fun decode(raw: String?): List<Long> = raw.orEmpty().split(',').mapNotNull { it.toLongOrNull() }

    private companion object {
        val KEY = stringPreferencesKey("perdiem_recent_location_ids")
        const val LIMIT = 5
    }
}
