package com.aijones.mileage.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.feature.onboarding.domain.ObserveOnboardingStateUseCase
import com.aijones.mileage.feature.settings.domain.ThemeMode
import com.aijones.mileage.feature.settings.domain.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface AppUiState {
    data object Loading : AppUiState
    data class Ready(val onboarded: Boolean, val theme: ThemeMode) : AppUiState
}

@HiltViewModel
class AppViewModel @Inject constructor(
    observeOnboarding: ObserveOnboardingStateUseCase,
    observeSettings: ObserveSettingsUseCase,
) : ViewModel() {
    val state: StateFlow<AppUiState> = combine(observeOnboarding(), observeSettings()) { onboarding, settings ->
        AppUiState.Ready(onboarding.isDone, settings.theme)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState.Loading)
}
