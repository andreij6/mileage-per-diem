package com.aijones.mileage.feature.insights.presentation

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h915dp")
class InsightsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun heatmapCellsDescribeTheirMilesAndTodayIsMarked() {
        compose.setContent { MpdTheme { InsightsScreen(InsightsUiState.Ready(sampleInsights, YearCardUi(2026, "9,412")), {}, {}) } }

        compose.onNodeWithContentDescription("November 10, 86 miles").assertIsDisplayed()
        compose.onNodeWithContentDescription("November 16, 37.5 miles")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "today"))
        compose.onNodeWithText("November insights").assertIsDisplayed()
        compose.onNodeWithText("41.2 mi").assertIsDisplayed()
    }

    @Test
    fun yearCardOpensStoriesForItsYear() {
        var opened: Int? = null
        compose.setContent { MpdTheme { InsightsScreen(InsightsUiState.Ready(sampleInsights, YearCardUi(2026, "9,412")), {}, { opened = it }) } }

        compose.onNodeWithText("Your 2026 on the road").performScrollTo().performClick()

        assertEquals(2026, opened)
    }

    @Test
    fun yearCardIsHiddenWhenTheFlagIsOff() {
        compose.setContent { MpdTheme { InsightsScreen(InsightsUiState.Ready(sampleInsights, null), {}, {}) } }

        compose.onNodeWithText("Your 2026 on the road").assertDoesNotExist()
    }

    @Test
    fun storiesCloseAndMoveWithAccessibilityActions() {
        var story = -1
        var closed = false
        compose.setContent {
            MpdTheme {
                YearInReviewScreen(YearInReviewUiState.Ready(sampleStories, 0), autoAdvance = false, onStory = { story = it }, onClose = { closed = true }, onShare = {})
            }
        }

        compose.onNodeWithText("You drove").assertIsDisplayed()
        compose.onNodeWithContentDescription("9412.0", substring = true).assertExists()
        compose.onNodeWithContentDescription("Close").performClick()
        val actions = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions)).fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnUiThread { actions.first { it.label == "Next story" }.action() }

        assertEquals(true, closed)
        assertEquals(1, story)
    }
}
