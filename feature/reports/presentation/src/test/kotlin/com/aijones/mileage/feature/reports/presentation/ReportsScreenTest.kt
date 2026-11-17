package com.aijones.mileage.feature.reports.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.feature.reports.domain.ReportFormat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReportsScreenTest {
    @get:Rule val compose = createComposeRule()

    private val exported = mutableListOf<ReportFormat>()
    private var toggles = 0

    private fun show(state: ReportsUiState = sampleState) = compose.setContent {
        MpdTheme(darkTheme = false) {
            ReportsScreen(state, { _, _ -> }, { toggles++ }, { exported += it }, {}, {}, {})
        }
    }

    @Test
    fun showsTotalsAndIncludedItems() {
        show()
        compose.onNodeWithText("Nov 1 – Nov 30, 2026").assertIsDisplayed()
        compose.onNodeWithText("$1,654.20").assertIsDisplayed()
        compose.onNodeWithText("845.0 mi · 32 trips").assertIsDisplayed()
        compose.onNodeWithText("12 days · 2 cities").assertIsDisplayed()
        compose.onNodeWithText("Boston, MA").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Nov 18 – 25 · M&IE only").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun exportButtonsCallThrough() {
        show()
        compose.onNodeWithText("PDF").performScrollTo().performClick()
        compose.onNodeWithText("CSV").performScrollTo().performClick()
        assertEquals(listOf(ReportFormat.PDF, ReportFormat.CSV), exported)
    }

    @Test
    fun businessOnlyChipToggles() {
        show()
        compose.onNodeWithText("Business only").performClick()
        assertEquals(1, toggles)
    }

    @Test
    fun pressedButtonShowsProgressInsteadOfLabel() {
        show(sampleState.copy(exporting = ReportFormat.PDF))
        compose.onNodeWithText("PDF").assertDoesNotExist()
        compose.onNodeWithText("CSV").performScrollTo().assertIsDisplayed()
    }
}
