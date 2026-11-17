package com.aijones.mileage.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.aijones.mileage.core.common.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FlagsStore

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {
    @Provides
    @Singleton
    @SettingsStore
    fun settings(@ApplicationContext context: Context, @IoDispatcher io: CoroutineDispatcher): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = CoroutineScope(io + SupervisorJob())) { context.preferencesDataStoreFile("settings") }

    @Provides
    @Singleton
    @FlagsStore
    fun flags(@ApplicationContext context: Context, @IoDispatcher io: CoroutineDispatcher): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = CoroutineScope(io + SupervisorJob())) { context.preferencesDataStoreFile("flags") }
}
