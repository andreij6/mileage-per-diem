package com.aijones.mileage.feature.tracking.presentation

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.model.Money
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h915dp")
class TrackingScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun slideToEndExposesEndTripAction() {
        var ended = 0
        compose.setContent { MpdTheme { SlideToEnd(onEnd = { ended++ }, modifier = Modifier.width(300.dp)) } }
        val node = compose.onNodeWithContentDescription("Slide to end trip").fetchSemanticsNode()
        val action = node.config[SemanticsActions.CustomActions].single()
        assertEquals("End trip", action.label)
        compose.runOnIdle { action.action() }
        assertEquals(1, ended)
    }

    @Test
    fun slidingPastTheThresholdEndsTheTrip() {
        var ended = 0
        compose.setContent { MpdTheme { SlideToEnd(onEnd = { ended++ }, modifier = Modifier.width(300.dp)) } }
        compose.onNodeWithContentDescription("Slide to end trip").performTouchInput { swipeRight(startX = left + 30f, endX = right, durationMillis = 600) }
        compose.waitForIdle()
        assertEquals(1, ended)
        compose.onNodeWithText("Trip ended", useUnmergedTree = true).assertExists()
    }

    @Test
    fun liveTripShowsStatsAndPauses() {
        var paused = 0
        val state = LiveTripUiState(
            status = LiveStatus.Recording, tenths = 126, amount = Money(9_576_000), elapsedSeconds = 1458, speedMph = 31,
            accuracyM = 5, purpose = "Client meeting", policy = "balanced", batteryPct = 82, route = previewRoute,
        )
        compose.setContent { MpdTheme { LiveTripScreen(state, onTogglePause = { paused++ }, onEnd = {}) } }
        compose.onNodeWithText("Recording").assertIsDisplayed()
        compose.onNodeWithText("GPS ±5 m").assertIsDisplayed()
        compose.onNodeWithText("Client meeting").assertIsDisplayed()
        compose.onNodeWithText("Balanced · 82%").assertIsDisplayed()
        compose.onNodeWithContentDescription("12.6 miles, \$9.58, 24:18 elapsed").assertExists()
        compose.onNodeWithContentDescription("Pause trip").performClick()
        assertEquals(1, paused)
    }

    @Test
    fun receiptShowsLineItemsAndActions() {
        var done = 0
        var edit = 0
        val receipt = ReceiptUi(
            tripId = 186, number = "#0186", dateLine = "Sun, Nov 16 · 9:12 – 9:41 AM", route = previewRoute,
            places = "Brooklyn office → Acme HQ", distance = "18.4 mi", minutes = 29, rateFrom = "Jul 1", rate = "76¢",
            purpose = "Client meeting", total = "\$13.98",
        )
        compose.setContent { MpdTheme { TripReceiptScreen(TripReceiptUiState(receipt), onEdit = { edit++ }, onDone = { done++ }, animate = false) } }
        compose.onNodeWithText("Trip saved").assertIsDisplayed()
        listOf("TRIP RECEIPT", "#0186", "Sun, Nov 16 · 9:12 – 9:41 AM", "Brooklyn office → Acme HQ", "18.4 mi", "29 min", "IRS rate (from Jul 1)", "× 76¢", "Client meeting", "TOTAL", "\$13.98")
            .forEach { compose.onNodeWithText(it, useUnmergedTree = true).performScrollTo().assertExists() }
        compose.onNodeWithText("Edit details").performClick()
        compose.onNodeWithText("Done").performClick()
        assertEquals(1 to 1, edit to done)
    }

    @Test
    fun interruptedSheetOffersResumeFinishDiscard() {
        val calls = mutableListOf<String>()
        compose.setContent {
            MpdTheme {
                InterruptedTripContent(
                    InterruptedTripUi(1, "9:27 AM", "8.2 mi"), busy = false,
                    onResume = { calls += "resume" }, onFinish = { calls += "finish" }, onDiscard = { calls += "discard" },
                )
            }
        }
        compose.onNodeWithText("Recording stopped at 9:27 AM after 8.2 mi. The phone restarted or Android closed the app.").assertIsDisplayed()
        compose.onNodeWithText("Resume recording").performClick()
        compose.onNodeWithText("Finish at 8.2 mi").performClick()
        compose.onNodeWithText("Discard trip").performClick()
        assertEquals(listOf("resume", "finish", "discard"), calls)
    }
}
