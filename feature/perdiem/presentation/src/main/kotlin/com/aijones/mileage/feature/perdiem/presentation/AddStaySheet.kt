package com.aijones.mileage.feature.perdiem.presentation

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.designsystem.component.Divider
import com.aijones.mileage.core.designsystem.component.Grabber
import com.aijones.mileage.core.designsystem.component.MpdButton
import com.aijones.mileage.core.designsystem.component.MpdCard
import com.aijones.mileage.core.designsystem.component.MpdField
import com.aijones.mileage.core.designsystem.component.MpdListRow
import com.aijones.mileage.core.designsystem.component.MpdSwitch
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.designsystem.theme.tnum
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.model.formatUsd
import com.aijones.mileage.core.navigation.AddStayRoute
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStaySheetRoute(route: AddStayRoute, onDismiss: () -> Unit) {
    val viewModel = hiltViewModel<AddStayViewModel, AddStayViewModel.Factory> { it.create(route) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.saved) {
        if (state.saved) scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MpdTheme.colors.surface,
        dragHandle = { Grabber(Modifier.padding(top = 12.dp)) },
    ) {
        AddStayContent(state, viewModel::setCheckIn, viewModel::setCheckOut, viewModel::setIncludeLodging, viewModel::save)
    }
}

private enum class DateField { CheckIn, CheckOut }

@Composable
fun AddStayContent(
    state: AddStayUiState,
    onCheckIn: (LocalDate) -> Unit,
    onCheckOut: (LocalDate) -> Unit,
    onIncludeLodging: (Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    var picking by rememberSaveable { mutableStateOf<DateField?>(null) }
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.stay_title, state.locationLabel), Modifier.semantics { heading() }, style = t.valueM, color = c.text)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MpdField(
                stringResource(R.string.stay_check_in), state.checkIn.formatStayDate(), Modifier.weight(1f),
                icon = MpdIcons.Calendar, onClick = { picking = DateField.CheckIn },
            )
            MpdField(stringResource(R.string.stay_check_out), state.checkOut.formatStayDate(), Modifier.weight(1f), onClick = { picking = DateField.CheckOut })
        }
        MpdCard(padding = PaddingValues(0.dp), spacing = 0.dp) {
            MpdListRow(
                stringResource(R.string.stay_claim_lodging),
                Modifier.toggleable(state.includeLodging, role = Role.Switch, onValueChange = onIncludeLodging),
                subtitle = stringResource(if (state.includeLodging) R.string.stay_lodging_on else R.string.stay_lodging_off),
                icon = MpdIcons.BedDouble,
                trailing = { MpdSwitch(state.includeLodging, null) },
            )
        }
        if (state.loaded) {
            Column {
                if (state.travelDays > 0) {
                    LineRow(pluralStringResource(R.plurals.stay_travel_days, state.travelDays, state.travelDays, state.travelDayRate.formatRate()), state.travelDaysTotal)
                }
                if (state.fullDays > 0) {
                    LineRow(pluralStringResource(R.plurals.stay_full_days, state.fullDays, state.fullDays, state.fullDayRate.formatRate()), state.fullDaysTotal)
                }
                if (state.includeLodging && state.nights > 0) {
                    LineRow(pluralStringResource(R.plurals.stay_nights, state.nights, state.nights), state.lodgingTotal)
                }
            }
            Divider()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val totalLabel = if (state.includeLodging) R.plurals.stay_total_mie_lodging else R.plurals.stay_total_mie
                Text(pluralStringResource(totalLabel, state.days, state.days), Modifier.weight(1f), style = t.labelL, color = c.text)
                Text(state.total.formatUsd(), style = t.valueM.copy(fontWeight = FontWeight.Bold).tnum(), color = c.text)
            }
        }
        MpdButton(
            stringResource(R.string.stay_add_to_report, state.checkIn.month.fullName()),
            onSave,
            Modifier.fillMaxWidth(),
            enabled = state.loaded && !state.saving && !state.saved,
        )
    }
    picking?.let { field ->
        StayDatePicker(
            initial = if (field == DateField.CheckIn) state.checkIn else state.checkOut,
            earliest = if (field == DateField.CheckOut) state.checkIn else null,
            onPick = { if (field == DateField.CheckIn) onCheckIn(it) else onCheckOut(it) },
            onDismiss = { picking = null },
        )
    }
}

@Composable
private fun LineRow(label: String, amount: Money) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(horizontal = 4.dp, vertical = 8.dp)) {
        Text(label, Modifier.weight(1f), style = MpdTheme.type.bodyM, color = MpdTheme.colors.text2)
        Text(amount.formatUsd(), style = MpdTheme.type.bodyM.copy(fontWeight = FontWeight.Medium).tnum(), color = MpdTheme.colors.text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StayDatePicker(initial: LocalDate, earliest: LocalDate?, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val minMillis = earliest?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    val selectable = remember(minMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = minMillis == null || utcTimeMillis >= minMillis
        }
    }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = selectable,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text(stringResource(R.string.stay_date_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.stay_date_cancel)) } },
    ) { DatePicker(pickerState) }
}

internal val previewAddStayState = AddStayUiState(
    checkIn = LocalDate.of(2026, 11, 18),
    checkOut = LocalDate.of(2026, 11, 25),
    locationLabel = "New York City, NY",
    loaded = true,
    days = 8,
    travelDays = 2,
    travelDayRate = Money.ofDollars(69),
    travelDaysTotal = Money.ofDollars(138),
    fullDays = 6,
    fullDayRate = Money.ofDollars(92),
    fullDaysTotal = Money.ofDollars(552),
    nights = 7,
    total = Money.ofDollars(690),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStayPreview(dark: Boolean) {
    MpdTheme(darkTheme = dark) {
        Box(Modifier.fillMaxSize()) {
            PerDiemScreen(previewPerDiemState, {}, {})
            Box(Modifier.fillMaxSize().background(BottomSheetDefaults.ScrimColor))
            Column(
                Modifier.align(Alignment.BottomCenter).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(MpdTheme.colors.surface),
            ) {
                Grabber(Modifier.padding(top = 12.dp))
                AddStayContent(previewAddStayState, {}, {}, {}, {})
            }
        }
    }
}

@Preview(name = "Add stay · light", widthDp = 412, heightDp = 879)
@Composable
private fun AddStayLightPreview() = AddStayPreview(dark = false)

@Preview(name = "Add stay · dark", widthDp = 412, heightDp = 879)
@Composable
private fun AddStayDarkPreview() = AddStayPreview(dark = true)
