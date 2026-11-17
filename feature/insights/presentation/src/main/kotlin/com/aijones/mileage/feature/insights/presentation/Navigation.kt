package com.aijones.mileage.feature.insights.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.aijones.mileage.core.navigation.InsightsRoute
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.YearInReviewRoute

fun EntryProviderScope<NavKey>.insightsEntries(navigator: Navigator) {
    entry<InsightsRoute> {
        InsightsRoute(onBack = navigator::back, onOpenYearInReview = { navigator.navigate(YearInReviewRoute(it)) })
    }
    entry<YearInReviewRoute> { key -> YearInReviewRoute(key, onClose = navigator::back) }
}
