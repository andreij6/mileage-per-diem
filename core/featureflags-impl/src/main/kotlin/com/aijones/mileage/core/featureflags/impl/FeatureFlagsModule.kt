package com.aijones.mileage.core.featureflags.impl

import com.aijones.mileage.core.featureflags.FeatureFlags
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface FeatureFlagsModule {
    @Binds fun flags(impl: DataStoreFeatureFlags): FeatureFlags
}
