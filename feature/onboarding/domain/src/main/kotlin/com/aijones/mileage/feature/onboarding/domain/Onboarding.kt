package com.aijones.mileage.feature.onboarding.domain

import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

enum class OnboardingStep { WELCOME, LOCATION, NOTIFICATIONS }

data class OnboardingState(val completed: Set<OnboardingStep> = emptySet()) {
    val isDone: Boolean get() = completed.containsAll(OnboardingStep.entries)
    val next: OnboardingStep? get() = OnboardingStep.entries.firstOrNull { it !in completed }
}

interface OnboardingRepository {
    fun observe(): Flow<OnboardingState>
    suspend fun complete(step: OnboardingStep)
}

class ObserveOnboardingStateUseCase @Inject constructor(private val repository: OnboardingRepository) {
    operator fun invoke(): Flow<OnboardingState> = repository.observe()
}

class CompleteOnboardingStepUseCase @Inject constructor(private val repository: OnboardingRepository, private val analytics: Analytics) {
    suspend operator fun invoke(step: OnboardingStep, result: String) {
        require(result == CONTINUE || result == SKIP) { "result must be '$CONTINUE' or '$SKIP'" }
        repository.complete(step)
        analytics.track(AnalyticsEvent.OnboardingStep(step.name.lowercase(), result))
    }

    companion object {
        const val CONTINUE = "continue"
        const val SKIP = "skip"
    }
}

class RecordPermissionResultUseCase @Inject constructor(private val analytics: Analytics) {
    operator fun invoke(permission: String, granted: Boolean, precise: Boolean? = null) =
        analytics.track(AnalyticsEvent.PermissionResult(permission, granted, precise))
}
