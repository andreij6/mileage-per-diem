package com.aijones.mileage.feature.perdiem.data

import android.content.Context
import androidx.room.Room
import com.aijones.mileage.feature.perdiem.domain.PerDiemRateRepository
import com.aijones.mileage.feature.perdiem.domain.RecentSearchRepository
import com.aijones.mileage.feature.perdiem.domain.StayRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PerDiemDataModule {
    @Binds fun rates(impl: RoomPerDiemRateRepository): PerDiemRateRepository

    @Binds fun stays(impl: RoomStayRepository): StayRepository

    @Binds fun recent(impl: DataStoreRecentSearchRepository): RecentSearchRepository

    companion object {
        @Provides
        @Singleton
        fun database(@ApplicationContext context: Context): PerDiemDatabase =
            Room.databaseBuilder(context, PerDiemDatabase::class.java, "perdiem.db")
                .createFromAsset("perdiem.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
