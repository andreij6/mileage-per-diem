package com.aijones.mileage.feature.insights.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.navigation.YearInReviewRoute
import com.aijones.mileage.feature.insights.domain.BuildYearInReviewUseCase
import com.aijones.mileage.feature.insights.domain.ExportShareCardUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val STORY_COUNT = 3
const val MONEY_STORY = 2

sealed interface YearInReviewUiState {
    data object Loading : YearInReviewUiState
    data class Ready(
        val stories: StoriesUi,
        val current: Int,
        val sharing: Boolean = false,
        val shareUri: String? = null,
    ) : YearInReviewUiState
}

private data class StoryProgress(val current: Int = 0, val sharing: Boolean = false, val shareUri: String? = null)

@HiltViewModel(assistedFactory = YearInReviewViewModel.Factory::class)
class YearInReviewViewModel @AssistedInject constructor(
    @Assisted key: YearInReviewRoute,
    buildYearInReview: BuildYearInReviewUseCase,
    private val exportShareCard: ExportShareCardUseCase,
    private val analytics: Analytics,
) : ViewModel() {
    private val progress = MutableStateFlow(StoryProgress())
    private val viewed = mutableSetOf<Int>()

    val state: StateFlow<YearInReviewUiState> = combine(flow { emit(buildYearInReview(key.year).toUi()) }, progress) { stories, p ->
        YearInReviewUiState.Ready(stories, p.current, p.sharing, p.shareUri)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YearInReviewUiState.Loading)

    init {
        analytics.track(AnalyticsEvent.ScreenView("year_in_review"))
        showStory(0)
    }

    fun showStory(index: Int) {
        val story = index.coerceIn(0, STORY_COUNT - 1)
        progress.update { it.copy(current = story) }
        if (viewed.add(story)) analytics.track(AnalyticsEvent.YearInReviewViewed(story + 1))
    }

    fun share(png: ByteArray) {
        if (progress.value.sharing) return
        progress.update { it.copy(sharing = true) }
        viewModelScope.launch {
            val uri = try {
                exportShareCard(png, MONEY_STORY + 1)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            progress.update { it.copy(sharing = false, shareUri = uri) }
        }
    }

    fun onShareHandled() = progress.update { it.copy(shareUri = null) }

    @AssistedFactory
    interface Factory {
        fun create(key: YearInReviewRoute): YearInReviewViewModel
    }
}
