package com.aijones.mileage.core.analytics.impl

import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.EventLog
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AnalyticsModule {
    @Binds fun analytics(impl: LocalAnalytics): Analytics
    @Binds fun eventLog(impl: LocalAnalytics): EventLog
}
