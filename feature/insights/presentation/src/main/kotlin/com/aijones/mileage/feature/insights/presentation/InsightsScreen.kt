package com.aijones.mileage.feature.insights.presentation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.designsystem.component.MpdCard
import com.aijones.mileage.core.designsystem.component.MpdTopBar
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.designsystem.theme.tnum

private val LEVEL_ALPHA = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
private val CELL = 40.dp
private val CELL_GAP = 6.dp

@Composable
fun InsightsRoute(onBack: () -> Unit, onOpenYearInReview: (Int) -> Unit, viewModel: InsightsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InsightsScreen(state, onBack, onOpenYearInReview)
}

@Composable
fun InsightsScreen(state: InsightsUiState, onBack: () -> Unit, onOpenYearInReview: (Int) -> Unit) {
    val c = MpdTheme.colors
    Column(Modifier.fillMaxSize().background(c.bg)) {
        val ready = state as? InsightsUiState.Ready
        MpdTopBar(ready?.let { stringResource(R.string.insights_title, it.insights.monthName) }.orEmpty(), onBack = onBack)
        if (ready == null) return@Column
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HeatmapCard(ready.insights)
            Highlights(ready.insights)
            WeekdayCard(ready.insights.weekdays)
            ready.yearCard?.let { YearCard(it, onOpen = { onOpenYearInReview(it.year) }) }
        }
    }
}

@Composable
private fun HeatmapCard(ui: InsightsUi) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    MpdCard(spacing = 10.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.insights_miles_by_day), Modifier.weight(1f).semantics { heading() }, style = t.labelL, color = c.text)
            Text(ui.total, style = t.bodyS.copy(fontWeight = FontWeight.Medium).tnum(), color = c.text2)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cell = minOf(CELL, (maxWidth - CELL_GAP * 6) / 7)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                    ui.weekdays.forEach { w ->
                        Text(w.letter, Modifier.width(cell), style = t.micro.copy(fontWeight = FontWeight.SemiBold), color = c.text2, textAlign = TextAlign.Center)
                    }
                }
                val slots = List(ui.leadingBlanks) { null } + ui.days
                slots.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                        week.forEach { day -> if (day == null) Spacer(Modifier.size(cell)) else DayCell(day, cell) }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.insights_less), style = t.micro.copy(fontWeight = FontWeight.Normal), color = c.text2)
            LEVEL_ALPHA.forEachIndexed { level, alpha ->
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(if (level == 0) c.surface2 else c.primary.copy(alpha = alpha)))
            }
            Text(stringResource(R.string.insights_more), style = t.micro.copy(fontWeight = FontWeight.Normal), color = c.text2)
        }
    }
}

@Composable
internal fun DayCell(day: DayCellUi, size: Dp = CELL) {
    val c = MpdTheme.colors
    val shape = RoundedCornerShape(10.dp)
    val description = pluralStringResource(R.plurals.insights_day_description, if (day.isOneMile) 1 else 2, day.dateLabel, day.miles)
    val today = stringResource(R.string.insights_day_today)
    val bg = when {
        day.isFuture -> null
        day.level == 0 -> c.surface2
        else -> c.primary.copy(alpha = LEVEL_ALPHA[day.level.coerceIn(0, 4)])
    }
    val ink = when {
        day.isFuture -> c.text2
        day.level >= 3 -> c.onPrimary
        else -> c.text
    }
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .then(if (bg != null) Modifier.background(bg) else Modifier)
            .then(
                when {
                    day.isToday -> Modifier.border(2.dp, c.text, shape)
                    day.isFuture -> Modifier.border(1.dp, c.outline, shape)
                    else -> Modifier
                },
            )
            .clearAndSetSemantics {
                contentDescription = description
                if (day.isToday) stateDescription = today
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(day.day.toString(), style = MpdTheme.type.micro, color = ink, maxLines = 1)
    }
}

@Composable
private fun Highlights(ui: InsightsUi) {
    val none = stringResource(R.string.insights_none)
    val noTrips = stringResource(R.string.insights_no_trips)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val mod = Modifier.weight(1f).fillMaxHeight()
        Highlight(MpdIcons.Route, stringResource(R.string.insights_longest), ui.longest?.value ?: none, ui.longest?.detail ?: noTrips, mod)
        Highlight(MpdIcons.Calendar, stringResource(R.string.insights_busiest), ui.busiest?.value ?: none, ui.busiest?.detail ?: noTrips, mod)
        Highlight(
            MpdIcons.MapPin,
            stringResource(R.string.insights_top_stop),
            ui.topStopCount?.let { stringResource(R.string.insights_top_stop_value, it) } ?: none,
            ui.topStop ?: noTrips,
            mod,
        )
    }
}

@Composable
private fun Highlight(@DrawableRes icon: Int, label: String, value: String, detail: String, modifier: Modifier) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    MpdCard(modifier.semantics(mergeDescendants = true) { }, radius = 16.dp, padding = PaddingValues(12.dp), spacing = 4.dp) {
        MpdIcon(icon, c.primary, size = 18.dp)
        Text(label, style = t.label.copy(fontWeight = FontWeight.Normal), color = c.text2)
        Text(value, style = t.valueS, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(detail, style = t.micro.copy(fontWeight = FontWeight.Normal), color = c.text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun WeekdayCard(weekdays: List<WeekdayBarUi>) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    MpdCard(spacing = 10.dp) {
        Text(stringResource(R.string.insights_by_weekday), Modifier.semantics { heading() }, style = t.labelL, color = c.text)
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            weekdays.forEach { w ->
                val description = stringResource(R.string.insights_weekday_description, w.name, w.miles)
                Column(
                    Modifier.clearAndSetSemantics { contentDescription = description },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        Modifier
                            .size(width = 30.dp, height = maxOf(4.dp, 60.dp * w.fraction))
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (w.isMax) c.primary else c.primary.copy(alpha = 0.35f)),
                    )
                    Text(w.letter, style = t.micro, color = c.text2)
                }
            }
        }
    }
}

@Composable
private fun YearCard(card: YearCardUi, onOpen: () -> Unit) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    val shape = RoundedCornerShape(20.dp)
    val openLabel = stringResource(R.string.insights_year_open)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.hero, lerp(c.hero, c.primary, 0.3f))))
            .clickable(role = Role.Button, onClickLabel = openLabel, onClick = onOpen)
            .semantics(mergeDescendants = true) { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.insights_year_title, card.year), style = t.titleM.copy(fontSize = 17.sp), color = c.onHero)
            Text(stringResource(R.string.insights_year_subtitle, card.miles), style = t.caption, color = c.onHero2)
        }
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(c.onHero), contentAlignment = Alignment.Center) {
                MpdIcon(MpdIcons.Play, c.hero, size = 20.dp)
            }
        }
    }
}

internal val sampleInsights = InsightsUi(
    monthName = "November",
    total = "845.0 mi",
    leadingBlanks = 0,
    days = listOf(0.0, 38.0, 12.0, 52.0, 0.0, 31.0, 0.0, 0.0, 44.0, 86.0, 27.0, 19.0, 49.0, 0.0, 0.0, 37.5).let { real ->
        (1..30).map { d ->
            val miles = real.getOrNull(d - 1) ?: 0.0
            val level = when {
                miles <= 0.0 -> 0
                miles < 25 -> 1
                miles < 45 -> 2
                miles < 70 -> 3
                else -> 4
            }
            DayCellUi(d, "November $d", if (miles % 1.0 == 0.0) miles.toInt().toString() else miles.toString(), false, level, d == 16, d > 16)
        }
    },
    longest = HighlightUi("41.2 mi", "JFK → Stamford"),
    busiest = HighlightUi("86 mi", "Tue, Nov 10"),
    topStopCount = 12,
    topStop = "Acme HQ",
    weekdays = listOf(0, 60, 186, 115, 104, 98, 0).zip(listOf("S", "M", "T", "W", "T", "F", "S")).map { (v, l) ->
        WeekdayBarUi(l, l, "$v.0 mi", v / 186f, v == 186)
    },
)

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun InsightsLightPreview() {
    MpdTheme(darkTheme = false) { InsightsScreen(InsightsUiState.Ready(sampleInsights, YearCardUi(2026, "9,412")), {}, {}) }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun InsightsDarkPreview() {
    MpdTheme(darkTheme = true) { InsightsScreen(InsightsUiState.Ready(sampleInsights, YearCardUi(2026, "9,412")), {}, {}) }
}
