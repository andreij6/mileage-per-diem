package com.aijones.mileage.feature.perdiem.presentation

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.analytics.FakeAnalytics
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.navigation.AddStayRoute
import com.aijones.mileage.core.testing.FakeClock
import com.aijones.mileage.feature.perdiem.domain.AddStayUseCase
import com.aijones.mileage.feature.perdiem.domain.EstimateStayUseCase
import com.aijones.mileage.feature.perdiem.domain.FakePerDiemRateRepository
import com.aijones.mileage.feature.perdiem.domain.FakeRecentSearchRepository
import com.aijones.mileage.feature.perdiem.domain.FakeStayRepository
import com.aijones.mileage.feature.perdiem.domain.GetRatesForLocationUseCase
import com.aijones.mileage.feature.perdiem.domain.SamplePerDiem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PerDiemScreensUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `add stay sheet total follows the lodging switch and saves the stay`() {
        val stays = FakeStayRepository()
        val analytics = FakeAnalytics()
        val vm = AddStayViewModel(
            AddStayRoute(SamplePerDiem.NYC_LOCATION.id),
            GetRatesForLocationUseCase(FakePerDiemRateRepository(), FakeRecentSearchRepository()),
            EstimateStayUseCase(),
            AddStayUseCase(stays, analytics),
            analytics,
            FakeClock(),
        )
        compose.setContent {
            val state by vm.uiState.collectAsStateWithLifecycle()
            MpdTheme { AddStayContent(state, vm::setCheckIn, vm::setCheckOut, vm::setIncludeLodging, vm::save) }
        }

        compose.onNodeWithText("Add stay · New York City, NY").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Mon, Nov 16").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("2 travel days × $69 (75%)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("1 full day × $92").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("3 days · M&IE").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("$230.00").performScrollTo().assertIsDisplayed()

        compose.onNodeWithText("Claim lodging").assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("2 nights lodging").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("3 days · M&IE + lodging").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("$802.00").performScrollTo().assertIsDisplayed()

        compose.onNodeWithText("Add to November report").performScrollTo().assertIsEnabled().performClick()
        compose.waitForIdle()
        assertEquals(Money.ofDollars(802), stays.stays.single().total)
        assertEquals(AnalyticsEvent.StayAdded(3, true), analytics.events.last())
    }

    @Test
    fun `per diem screen shows rates and routes to search and add stay`() {
        var searched = 0
        var addStayFor: Long? = null
        compose.setContent { MpdTheme { PerDiemScreen(previewPerDiemState, { searched++ }, { addStayFor = it }) } }

        compose.onNodeWithText("$286").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("per night · Nov").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Lodging by month, FY2027, October $286", substring = true).assertExists()
        compose.onNodeWithContentDescription("Search per diem city. Selected: New York City, NY").performClick()
        compose.onNodeWithText("$802.00").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Add stay").performClick()

        assertEquals(1, searched)
        assertEquals(2L, addStayFor)
    }

    @Test
    fun `search screen lists results, recents and the standard rate note`() {
        val picked = mutableListOf<Long>()
        compose.setContent { MpdTheme { PerDiemSearchScreen(previewSearchState, {}, {}, { picked += it }, {}, autoFocus = false) } }

        compose.onNodeWithText("RESULTS · FY2027").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Bossier City, LA").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Places not listed use the standard CONUS rate: $113 lodging, $68 M&IE.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Boston, MA").performScrollTo().performClick()
        compose.onNodeWithText("Chicago, IL").performScrollTo().performClick()

        assertEquals(listOf(3L, 7L), picked)
    }
}
