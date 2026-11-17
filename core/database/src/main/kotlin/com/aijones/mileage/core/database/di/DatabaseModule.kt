package com.aijones.mileage.core.database.di

import android.content.Context
import androidx.room.Room
import com.aijones.mileage.core.database.MileageDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): MileageDatabase =
        Room.databaseBuilder(context, MileageDatabase::class.java, "mileage.db").build()

    @Provides fun tripDao(db: MileageDatabase) = db.tripDao()
    @Provides fun trackPointDao(db: MileageDatabase) = db.trackPointDao()
    @Provides fun stayDao(db: MileageDatabase) = db.stayDao()
    @Provides fun vehicleDao(db: MileageDatabase) = db.vehicleDao()
    @Provides fun analyticsEventDao(db: MileageDatabase) = db.analyticsEventDao()
}
