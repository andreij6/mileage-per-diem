package com.aijones.mileage.feature.onboarding.data

import com.aijones.mileage.feature.onboarding.domain.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface OnboardingDataModule {
    @Binds
    fun onboarding(impl: DataStoreOnboardingRepository): OnboardingRepository
}
