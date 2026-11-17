package com.aijones.mileage.di

import com.aijones.mileage.core.common.ApplicationScope
import com.aijones.mileage.core.common.DefaultDispatcher
import com.aijones.mileage.core.common.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @ApplicationScope
    fun appScope(@DefaultDispatcher dispatcher: CoroutineDispatcher): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

    @Provides
    @Singleton
    fun clock(): Clock = Clock.systemDefaultZone()
}
