package com.aijones.mileage.feature.settings.data

import com.aijones.mileage.feature.settings.domain.DataResetter
import com.aijones.mileage.feature.settings.domain.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface SettingsDataModule {
    @Binds
    fun settings(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    fun resetter(impl: RoomDataResetter): DataResetter
}
