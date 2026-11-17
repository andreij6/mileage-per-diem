package com.aijones.mileage.feature.trips.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.designsystem.component.BarAction
import com.aijones.mileage.core.designsystem.component.CardList
import com.aijones.mileage.core.designsystem.component.MpdChip
import com.aijones.mileage.core.designsystem.component.MpdField
import com.aijones.mileage.core.designsystem.component.MpdTopBar
import com.aijones.mileage.core.designsystem.component.SectionLabel
import com.aijones.mileage.core.designsystem.component.SelectChip
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.designsystem.theme.tnum
import com.aijones.mileage.core.navigation.AddTripRoute
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.core.navigation.TripDetailRoute

@Composable
internal fun TripListRoute(navigator: Navigator) {
    val viewModel: TripListViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    TripListScreen(
        state = state,
        onBack = navigator::back,
        onToggleSearch = viewModel::toggleSearch,
        onQuery = viewModel::search,
        onToggleBusiness = viewModel::toggleBusinessOnly,
        onCycleVehicle = viewModel::cycleVehicle,
        onTrip = { navigator.navigate(TripDetailRoute(it)) },
        onAddTrip = { navigator.navigate(AddTripRoute) },
    )
}

@Composable
fun TripListScreen(
    state: TripListUiState,
    onBack: () -> Unit,
    onToggleSearch: () -> Unit,
    onQuery: (String) -> Unit,
    onToggleBusiness: () -> Unit,
    onCycleVehicle: () -> Unit,
    onTrip: (Long) -> Unit,
    onAddTrip: () -> Unit,
) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            MpdTopBar(stringResource(R.string.trips_all_title), onBack = onBack) {
                BarAction(if (state.searching) MpdIcons.X else MpdIcons.Search, stringResource(R.string.trips_search), onToggleSearch)
            }
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.searching) {
                    item(key = "search") {
                        MpdField(stringResource(R.string.trips_search_hint), state.query, onValueChange = onQuery, icon = MpdIcons.Search)
                    }
                }
                item(key = "filters") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { MpdChip(state.month, icon = MpdIcons.Calendar, bg = c.primaryC, fg = c.onPrimaryC) }
                        item { FilterChip(stringResource(R.string.trips_filter_business), state.businessOnly, onToggleBusiness) }
                        item { FilterChip(state.vehicle ?: stringResource(R.string.trips_filter_all_vehicles), state.vehicle != null, onCycleVehicle) }
                    }
                }
                if (!state.loading) {
                    item(key = "summary") {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp).semantics(mergeDescendants = true) {}) {
                            Text(
                                stringResource(R.string.trips_summary, pluralStringResource(R.plurals.trips_count, state.count, state.count), state.distance),
                                Modifier.weight(1f),
                                style = t.bodyS.copy(fontWeight = t.label.fontWeight).tnum(),
                                color = c.text2,
                            )
                            Text(state.total, style = t.labelM.tnum(), color = c.text)
                        }
                    }
                }
                if (!state.loading && state.groups.isEmpty()) {
                    item(key = "none") {
                        Text(stringResource(R.string.trips_none_match), Modifier.fillMaxWidth().padding(24.dp), style = t.bodyM, color = c.text2, textAlign = TextAlign.Center)
                    }
                }
                state.groups.forEach { group ->
                    item(key = "label-${group.trips.first().id}") { SectionLabel(group.day.text(), Modifier.padding(top = 4.dp)) }
                    item(key = "group-${group.trips.first().id}") {
                        CardList(rows = group.trips.map { row -> { TripRow(row, stringResource(R.string.trips_dot, row.times, row.distance), { onTrip(row.id) }, showPurpose = false) } })
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAddTrip,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = 32.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = c.primaryC,
            contentColor = c.onPrimaryC,
            icon = { MpdIcon(MpdIcons.Plus, c.onPrimaryC, size = 22.dp) },
            text = { Text(stringResource(R.string.trips_add_trip), style = t.labelL) },
        )
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) SelectChip(label, true, onClick) else MpdChip(label, outlined = true, selected = false, onClick = onClick)
}

internal val sampleTripList = TripListUiState(
    loading = false,
    month = "November 2026",
    count = 32,
    distance = "845.0 mi",
    total = "$642.20",
    groups = listOf(
        TripGroupUi(DayLabel.Today, listOf(sampleRows[0], sampleRows[1])),
        TripGroupUi(DayLabel.Yesterday, listOf(sampleRows[2].copy(times = "06:40 – 07:22"))),
        TripGroupUi(
            DayLabel.On("Thu, Nov 13"),
            listOf(sampleRows[3].copy(times = "11:05 – 11:51"), TripRowUi(5, "Newark Supply", "Home", DayLabel.On("Nov 13"), "14:20 – 15:02", "24.6 mi", "$18.70", "Site visit")),
        ),
    ),
)

@Preview(widthDp = 412, heightDp = 879)
@Composable
private fun TripListLightPreview() = MpdTheme(darkTheme = false) { TripListScreen(sampleTripList, {}, {}, {}, {}, {}, {}, {}) }

@Preview(widthDp = 412, heightDp = 879)
@Composable
private fun TripListDarkPreview() = MpdTheme(darkTheme = true) { TripListScreen(sampleTripList, {}, {}, {}, {}, {}, {}, {}) }
