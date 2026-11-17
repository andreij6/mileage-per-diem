package com.aijones.mileage.feature.perdiem.presentation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.designsystem.component.ButtonKind
import com.aijones.mileage.core.designsystem.component.IconTile
import com.aijones.mileage.core.designsystem.component.MpdButton
import com.aijones.mileage.core.designsystem.component.MpdCard
import com.aijones.mileage.core.designsystem.component.ScreenTitle
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.designsystem.theme.tnum
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.model.formatUsd
import java.time.Month

@Composable
fun PerDiemScreenRoute(onSearch: () -> Unit, onAddStay: (Long) -> Unit, trackScreenView: Boolean = true) {
    val viewModel = hiltViewModel<PerDiemViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (trackScreenView) LaunchedEffect(viewModel) { viewModel.onShown() }
    PerDiemScreen(state, onSearch, onAddStay)
}

@Composable
fun PerDiemScreen(state: PerDiemUiState, onSearch: () -> Unit, onAddStay: (Long) -> Unit, modifier: Modifier = Modifier) {
    val c = MpdTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle(stringResource(R.string.perdiem_title), subtitle = stringResource(R.string.perdiem_subtitle, state.fiscalYear))
        val label = if (state.isStandard) stringResource(R.string.perdiem_standard_label) else state.locationLabel
        SearchPill(label, onSearch)
        if (state.loading || state.locationId == null) return@Column
        RateCards(state)
        LodgingChart(state)
        MieBreakdownCard(state)
        EstimateHero(state.tripEstimate, onAddStay = { onAddStay(state.locationId) })
    }
}

@Composable
private fun SearchPill(label: String, onSearch: () -> Unit) {
    val c = MpdTheme.colors
    val description = stringResource(R.string.perdiem_search_city, label)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(CircleShape)
            .background(c.surface2)
            .clickable(role = Role.Button, onClick = onSearch)
            .clearAndSetSemantics { contentDescription = description; role = Role.Button }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MpdIcon(MpdIcons.Search, c.text2, size = 22.dp)
        Text(label, Modifier.weight(1f), style = MpdTheme.type.bodyL, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        MpdIcon(MpdIcons.X, c.text2, size = 20.dp)
    }
}

@Composable
private fun RateCards(state: PerDiemUiState) {
    val c = MpdTheme.colors
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RateCard(
            MpdIcons.BedDouble, c.primaryC, c.onPrimaryC, stringResource(R.string.perdiem_lodging), state.lodgingThisMonth,
            stringResource(R.string.perdiem_lodging_sub, state.currentMonth.shortName()), Modifier.weight(1f),
        )
        RateCard(
            MpdIcons.Utensils, c.accentC, c.accent, stringResource(R.string.perdiem_mie), state.mieDaily,
            stringResource(R.string.perdiem_mie_sub), Modifier.weight(1f),
        )
    }
}

@Composable
private fun RateCard(@DrawableRes icon: Int, iconBg: Color, iconFg: Color, label: String, amount: Money, caption: String, modifier: Modifier) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    MpdCard(modifier.fillMaxHeight().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }, spacing = 8.dp) {
        IconTile(icon, iconBg, iconFg)
        Text(label, style = t.caption, color = c.text2)
        Text(amount.formatRate(), style = t.valueL.tnum(), color = c.text)
        Text(caption, style = t.caption, color = c.text2)
    }
}

@Composable
private fun LodgingChart(state: PerDiemUiState) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    val summary = stringResource(
        R.string.perdiem_lodging_chart_summary,
        state.fiscalYear,
        state.bars.joinToString(", ") { "${it.month.fullName()} ${it.lodging.formatRate()}" },
    )
    MpdCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.perdiem_lodging_by_month), Modifier.weight(1f).semantics { heading() }, style = t.labelL, color = c.text)
            Text(stringResource(R.string.perdiem_fy, state.fiscalYear), style = t.caption, color = c.text2)
        }
        Row(
            Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = summary },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            state.bars.forEach { bar ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.height(BAR_MAX), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(BAR_MAX * bar.fraction)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (bar.isCurrent) c.primary else c.surface2),
                        )
                    }
                    Text(bar.month.initial(), style = t.micro, color = if (bar.isCurrent) c.text else c.text2)
                }
            }
        }
    }
}

private val BAR_MAX = 56.dp

@Composable
private fun MieBreakdownCard(state: PerDiemUiState) {
    MpdCard(padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), spacing = 0.dp) {
        BreakdownRow(stringResource(R.string.perdiem_breakfast), state.breakfast)
        BreakdownRow(stringResource(R.string.perdiem_lunch), state.lunch)
        BreakdownRow(stringResource(R.string.perdiem_dinner), state.dinner)
        BreakdownRow(stringResource(R.string.perdiem_incidentals), state.incidentals)
    }
}

@Composable
private fun BreakdownRow(label: String, amount: Money) {
    val c = MpdTheme.colors
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MpdTheme.type.bodyM, color = c.text2)
        Text(amount.formatRate(), style = MpdTheme.type.bodyM.copy(fontWeight = FontWeight.SemiBold).tnum(), color = c.text)
    }
}

@Composable
private fun EstimateHero(estimate: Money, onAddStay: () -> Unit) {
    val c = MpdTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.hero).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.perdiem_trip_estimate), style = MpdTheme.type.caption, color = c.onHero2)
            Text(estimate.formatUsd(), style = MpdTheme.type.headlineS.copy(fontWeight = FontWeight.Bold).tnum(), color = c.onHero)
        }
        MpdButton(
            stringResource(R.string.perdiem_add_stay), onAddStay, kind = ButtonKind.Filled, icon = MpdIcons.Plus, height = 48.dp,
            container = c.onHero, content = c.hero,
        )
    }
}

internal val previewPerDiemState = PerDiemUiState(
    fiscalYear = 2027,
    loading = false,
    locationId = 2,
    locationLabel = "New York City, NY",
    currentMonth = Month.NOVEMBER,
    lodgingThisMonth = Money.ofDollars(286),
    mieDaily = Money.ofDollars(92),
    bars = listOf(286, 286, 286, 214, 214, 274, 274, 274, 248, 248, 312, 312).mapIndexed { i, v ->
        MonthBar(Month.OCTOBER.plus(i.toLong()), Money.ofDollars(v.toLong()), v / 312f, i == 1)
    },
    breakfast = Money.ofDollars(22),
    lunch = Money.ofDollars(25),
    dinner = Money.ofDollars(40),
    incidentals = Money.ofDollars(5),
    tripEstimate = Money.ofDollars(802),
)

@Preview(name = "Per diem · light", widthDp = 412, heightDp = 827)
@Composable
private fun PerDiemLightPreview() {
    MpdTheme(darkTheme = false) { PerDiemScreen(previewPerDiemState, {}, {}) }
}

@Preview(name = "Per diem · dark", widthDp = 412, heightDp = 827)
@Composable
private fun PerDiemDarkPreview() {
    MpdTheme(darkTheme = true) { PerDiemScreen(previewPerDiemState, {}, {}) }
}
