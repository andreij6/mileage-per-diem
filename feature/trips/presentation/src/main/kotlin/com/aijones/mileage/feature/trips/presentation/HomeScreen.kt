package com.aijones.mileage.feature.trips.presentation

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.designsystem.component.ButtonKind
import com.aijones.mileage.core.designsystem.component.CardList
import com.aijones.mileage.core.designsystem.component.IconTile
import com.aijones.mileage.core.designsystem.component.MpdButton
import com.aijones.mileage.core.designsystem.component.MpdCard
import com.aijones.mileage.core.designsystem.component.MpdChip
import com.aijones.mileage.core.designsystem.component.Odometer
import com.aijones.mileage.core.designsystem.component.OdometerColors
import com.aijones.mileage.core.designsystem.component.ScreenTitle
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.ui.MotionDirection
import com.aijones.mileage.core.ui.staggeredEnter
import com.aijones.mileage.core.designsystem.theme.tnum
import com.aijones.mileage.core.navigation.AddTripRoute
import com.aijones.mileage.core.navigation.AllTripsRoute
import com.aijones.mileage.core.navigation.InsightsRoute
import com.aijones.mileage.core.navigation.LiveTripRoute
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.ReviewTripsRoute
import com.aijones.mileage.core.navigation.TripDetailRoute

@Composable
internal fun HomeRoute(navigator: Navigator, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReportDrawnWhen { !state.loading }
    HomeScreen(
        state = state,
        onStartTrip = { navigator.navigate(LiveTripRoute("app")) },
        onInsights = { navigator.navigate(InsightsRoute) },
        onReview = { navigator.navigate(ReviewTripsRoute) },
        onSeeAll = { navigator.navigate(AllTripsRoute) },
        onTrip = { navigator.navigate(TripDetailRoute(it)) },
        onAddPastTrip = { navigator.navigate(AddTripRoute) },
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onStartTrip: () -> Unit,
    onInsights: () -> Unit,
    onReview: () -> Unit,
    onSeeAll: () -> Unit,
    onTrip: (Long) -> Unit,
    onAddPastTrip: () -> Unit,
) {
    val c = MpdTheme.colors
    LazyColumn(
        Modifier.fillMaxSize().background(c.bg).statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.loading) return@LazyColumn
        item(key = "title") {
            Box(Modifier.staggeredEnter(MotionDirection.Top, 0)) {
                ScreenTitle(stringResource(R.string.trips_title), eyebrow = state.month) {
                    if (!state.empty) MpdChip(stringResource(R.string.trips_offline), icon = MpdIcons.CloudOff, outlined = true)
                }
            }
        }
        item(key = "hero") {
            Box(Modifier.staggeredEnter(MotionDirection.Start, 40)) {
                HeroCard(state, onStartTrip, onInsights)
            }
        }
        if (state.empty) {
            item(key = "empty") {
                Box(Modifier.staggeredEnter(MotionDirection.Bottom, 80)) {
                    EmptyCard(onAddPastTrip)
                }
            }
            return@LazyColumn
        }
        if (state.needsReview > 0) {
            item(key = "review") {
                Box(Modifier.staggeredEnter(MotionDirection.End, 80)) {
                    ReviewBanner(state.needsReview, onReview)
                }
            }
        }
        item(key = "recentHeader") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp).staggeredEnter(MotionDirection.Bottom, 110), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.trips_recent), Modifier.weight(1f).semantics { heading() }, style = MpdTheme.type.titleM, color = c.text)
                Text(
                    stringResource(R.string.trips_see_all),
                    Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onSeeAll).padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MpdTheme.type.labelM,
                    color = c.primary,
                )
            }
        }
        item(key = "recent") {
            Box(Modifier.staggeredEnter(MotionDirection.Bottom, 140)) {
                CardList(rows = state.recent.map { row -> { TripRow(row, homeSubtitle(row), onClick = { onTrip(row.id) }) } })
            }
        }
    }
}

@Composable
private fun homeSubtitle(row: TripRowUi) = stringResource(R.string.trips_dot3, row.day.text(), row.times, row.distance)

@Composable
private fun HeroCard(state: HomeUiState, onStartTrip: () -> Unit, onInsights: () -> Unit) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    MpdCard(radius = 24.dp, padding = PaddingValues(20.dp), spacing = 14.dp, container = c.hero) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.trips_reimbursable_month), Modifier.weight(1f), style = t.bodyS, color = c.onHero2)
            if (!state.empty) {
                Row(
                    Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onInsights).padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(stringResource(R.string.trips_insights), style = t.caption.copy(fontWeight = t.labelM.fontWeight), color = c.onHero)
                    MpdIcon(MpdIcons.ChevronRight, c.onHero, size = 16.dp)
                }
            }
        }
        Text(state.reimbursement, style = t.displayL.tnum(), color = c.onHero)
        if (state.empty) {
            Text(stringResource(R.string.trips_irs_rate_line, state.rate), style = t.bodyS, color = c.onHero2)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Odometer(
                    tenths = state.odometerTenths,
                    colors = OdometerColors(digit = c.onHero, drum = c.onHero.copy(alpha = 0.12f), tenthDrum = c.drumTenth, tenthDigit = c.onDrumTenth),
                    size = 26,
                )
                Text(
                    pluralStringResource(R.plurals.trips_count, state.tripCount, state.tripCount) + "\n" +
                        stringResource(R.string.trips_rate_per_mile, state.rate),
                    Modifier.weight(1f),
                    style = t.caption,
                    color = c.onHero2,
                    textAlign = TextAlign.End,
                )
            }
        }
        MpdButton(
            stringResource(R.string.trips_start_trip),
            onStartTrip,
            Modifier.fillMaxWidth(),
            icon = MpdIcons.Play,
            container = c.onHero,
            content = c.hero,
        )
    }
}

@Composable
private fun ReviewBanner(count: Int, onReview: () -> Unit) {
    val c = MpdTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.accentC)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StackedCards()
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(pluralStringResource(R.plurals.trips_need_purpose, count, count), style = MpdTheme.type.bodyM.copy(fontWeight = MpdTheme.type.labelM.fontWeight), color = c.text)
            Text(stringResource(R.string.trips_review_banner_body), style = MpdTheme.type.caption, color = c.text2)
        }
        MpdButton(stringResource(R.string.trips_review), onReview, height = 40.dp, container = c.accent, content = c.onAccent)
    }
}

@Composable
private fun StackedCards() {
    val c = MpdTheme.colors
    Box(Modifier.size(width = 44.dp, height = 40.dp)) {
        listOf(8, 4, 0).forEach { o ->
            Box(
                Modifier
                    .offset(x = (o + 2).dp, y = (2 + (8 - o) / 4f).dp)
                    .size(width = 30.dp, height = 36.dp)
                    .rotate(((o - 4) * 2).toFloat())
                    .clip(RoundedCornerShape(6.dp))
                    .background(c.surface)
                    .border(1.5.dp, c.accent, RoundedCornerShape(6.dp)),
            )
        }
    }
}

@Composable
private fun EmptyCard(onAddPastTrip: () -> Unit) {
    val c = MpdTheme.colors
    MpdCard(padding = PaddingValues(horizontal = 24.dp, vertical = 32.dp), spacing = 12.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(MpdIcons.Route, c.primaryC, c.onPrimaryC, size = 64.dp, iconSize = 30.dp, radius = 32.dp)
            Text(stringResource(R.string.trips_empty_title), Modifier.semantics { heading() }, style = MpdTheme.type.titleM, color = c.text)
            Text(stringResource(R.string.trips_empty_body), style = MpdTheme.type.bodyM, color = c.text2, textAlign = TextAlign.Center)
            MpdButton(stringResource(R.string.trips_add_past), onAddPastTrip, kind = ButtonKind.Text, icon = MpdIcons.Plus, height = 44.dp)
        }
    }
}

internal val sampleRows = listOf(
    TripRowUi(1, "Office", "Acme HQ", DayLabel.Today, "9:12 – 9:41", "18.4 mi", "$13.98", "Client meeting"),
    TripRowUi(2, "Acme HQ", "Office", DayLabel.Today, "15:02 – 15:38", "19.1 mi", "$14.52", "Client meeting"),
    TripRowUi(3, "Office", "JFK Airport", DayLabel.Yesterday, "06:40", "16.2 mi", "$12.31", "Travel"),
    TripRowUi(4, "Home", "Newark Supply", DayLabel.On("Nov 13"), "11:05", "24.8 mi", "$18.85", "Site visit"),
)

internal val sampleHome = HomeUiState(
    loading = false,
    month = "November 2026",
    reimbursement = "$642.20",
    odometerTenths = 8450,
    tripCount = 32,
    rate = "76¢",
    needsReview = 3,
    recent = sampleRows,
)

@Composable
private fun HomePreview(state: HomeUiState, dark: Boolean) {
    MpdTheme(darkTheme = dark) { HomeScreen(state, {}, {}, {}, {}, {}, {}) }
}

@Preview(widthDp = 412, heightDp = 879)
@Composable
private fun HomeLightPreview() = HomePreview(sampleHome, false)

@Preview(widthDp = 412, heightDp = 879)
@Composable
private fun HomeDarkPreview() = HomePreview(sampleHome, true)

@Preview(widthDp = 412, heightDp = 879)
@Composable
private fun HomeEmptyLightPreview() = HomePreview(HomeUiState(loading = false, month = "November 2026", reimbursement = "$0.00", rate = "76¢"), false)

@Preview(widthDp = 412, heightDp = 879)
@Composable
private fun HomeEmptyDarkPreview() = HomePreview(HomeUiState(loading = false, month = "November 2026", reimbursement = "$0.00", rate = "76¢"), true)

@Preview(widthDp = 412, heightDp = 879, fontScale = 2f)
@Composable
private fun HomeLargeFontPreview() = HomePreview(sampleHome, false)
