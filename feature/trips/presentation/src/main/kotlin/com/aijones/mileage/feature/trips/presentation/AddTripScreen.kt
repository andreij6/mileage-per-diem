package com.aijones.mileage.feature.trips.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.designsystem.component.ButtonKind
import com.aijones.mileage.core.designsystem.component.MpdButton
import com.aijones.mileage.core.designsystem.component.MpdCard
import com.aijones.mileage.core.designsystem.component.MpdField
import com.aijones.mileage.core.designsystem.component.MpdListRow
import com.aijones.mileage.core.designsystem.component.MpdSwitch
import com.aijones.mileage.core.designsystem.component.MpdTopBar
import com.aijones.mileage.core.designsystem.component.SaveBar
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.designsystem.theme.tnum
import com.aijones.mileage.core.navigation.Navigator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
internal fun AddTripEntry(navigator: Navigator) {
    val viewModel: AddTripViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.added) { if (state.added) navigator.back() }
    AddTripScreen(state, onBack = navigator::back, onEdit = viewModel::edit, onAdd = viewModel::add)
}

@Composable
fun AddTripScreen(
    state: AddTripUiState,
    onBack: () -> Unit,
    onEdit: ((AddTripForm) -> AddTripForm) -> Unit,
    onAdd: () -> Unit,
) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    val form = state.form
    var picking by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(c.bg).imePadding()) {
        MpdTopBar(stringResource(R.string.trips_add_title), onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MpdField(stringResource(R.string.trips_date), state.dateLabel, icon = MpdIcons.Calendar, onClick = { picking = true })
            MpdField(stringResource(R.string.trips_from), form.from, onValueChange = { v -> onEdit { it.copy(from = v) } }, icon = MpdIcons.Circle)
            MpdField(stringResource(R.string.trips_to), form.to, onValueChange = { v -> onEdit { it.copy(to = v) } }, icon = MpdIcons.MapPin)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MpdField(
                    stringResource(R.string.trips_distance),
                    form.distance,
                    Modifier.weight(1f),
                    onValueChange = { v -> onEdit { it.copy(distance = v) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                MpdField(stringResource(R.string.trips_purpose), form.purpose, Modifier.weight(1f), onValueChange = { v -> onEdit { it.copy(purpose = v) } })
            }
            MpdCard(padding = PaddingValues(0.dp), spacing = 0.dp) {
                MpdListRow(
                    stringResource(R.string.trips_round_trip),
                    Modifier.toggleable(form.roundTrip, role = Role.Switch, onValueChange = { v -> onEdit { it.copy(roundTrip = v) } }),
                    subtitle = stringResource(R.string.trips_round_trip_body),
                ) { MpdSwitch(form.roundTrip, null) }
            }
            state.estimate?.let { e ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.primaryC)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.trips_reimbursement), style = t.caption, color = c.onPrimaryC)
                        Text(e.amount, style = t.headlineS.copy(fontWeight = t.displayL.fontWeight).tnum(), color = c.onPrimaryC)
                    }
                    Text(
                        stringResource(R.string.trips_estimate_line, e.distance, e.rate, state.rateDate),
                        style = t.caption.tnum(),
                        color = c.onPrimaryC,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
        SaveBar(stringResource(R.string.trips_manual_entry)) {
            MpdButton(stringResource(R.string.trips_add_trip), onAdd, height = 48.dp, enabled = state.canAdd)
        }
    }
    if (picking) {
        TripDatePicker(form.date, state.today, onPick = { d -> picking = false; onEdit { it.copy(date = d) } }, onDismiss = { picking = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDatePicker(date: LocalDate, today: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val maxMillis = today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= maxMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            MpdButton(stringResource(R.string.trips_ok), {
                picker.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: onDismiss()
            }, kind = ButtonKind.Text, height = 48.dp)
        },
        dismissButton = { MpdButton(stringResource(R.string.trips_cancel), onDismiss, kind = ButtonKind.Text, height = 48.dp) },
    ) { DatePicker(picker) }
}

internal val sampleAdd = AddTripUiState(
    form = AddTripForm(LocalDate.of(2026, 11, 16), "Brooklyn office", "Acme HQ, Midtown", "18.4 mi", "Client meeting"),
    dateLabel = "Sun, Nov 16, 2026",
    rateDate = "Nov 16",
    today = LocalDate.of(2026, 11, 16),
    estimate = AddTripEstimate("18.4 mi", "76¢", "$13.98"),
    canAdd = true,
)

@Preview(widthDp = 412, heightDp = 879)
@Composable
private fun AddTripLightPreview() = MpdTheme(darkTheme = false) { AddTripScreen(sampleAdd, {}, {}, {}) }

@Preview(widthDp = 412, heightDp = 879)
@Composable
private fun AddTripDarkPreview() = MpdTheme(darkTheme = true) { AddTripScreen(sampleAdd, {}, {}, {}) }
