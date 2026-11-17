package com.aijones.mileage.feature.onboarding.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeOnboardingRepository(initial: OnboardingState = OnboardingState()) : OnboardingRepository {
    private val state = MutableStateFlow(initial)

    val current: OnboardingState get() = state.value

    override fun observe(): Flow<OnboardingState> = state

    override suspend fun complete(step: OnboardingStep) = state.update { it.copy(completed = it.completed + step) }
}
