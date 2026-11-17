package com.aijones.mileage.feature.trips.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.designsystem.component.ButtonKind
import com.aijones.mileage.core.designsystem.component.MpdButton
import com.aijones.mileage.core.designsystem.component.MpdField
import com.aijones.mileage.core.designsystem.component.MpdTopBar
import com.aijones.mileage.core.designsystem.component.SaveBar
import com.aijones.mileage.core.designsystem.component.SectionLabel
import com.aijones.mileage.core.designsystem.component.SegmentedControl
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.navigation.Navigator
import com.aijones.mileage.feature.trips.domain.TripCategory

@Composable
internal fun EditTripEntry(tripId: Long, navigator: Navigator) {
    val viewModel = hiltViewModel<EditTripViewModel, EditTripViewModel.Factory> { it.create(tripId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) navigator.back() }
    EditTripScreen(state, onBack = navigator::back, onEdit = viewModel::edit, onSave = viewModel::save, onDelete = viewModel::delete)
}

@Composable
fun EditTripScreen(
    state: EditTripUiState,
    onBack: () -> Unit,
    onEdit: ((EditTripForm) -> EditTripForm) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = MpdTheme.colors
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var vehicleMenu by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(c.bg)) {
        MpdTopBar(stringResource(R.string.trips_edit_title), onBack = onBack)
        val form = state.form ?: return@Column
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MpdField(stringResource(R.string.trips_purpose), form.purpose, onValueChange = { v -> onEdit { it.copy(purpose = v) } }, icon = MpdIcons.Pencil)
            SectionLabel(stringResource(R.string.trips_category))
            SegmentedControl(
                listOf(stringResource(R.string.trips_business), stringResource(R.string.trips_personal)),
                selected = form.category.ordinal,
                onSelect = { i -> onEdit { it.copy(category = TripCategory.entries[i]) } },
            )
            Box {
                MpdField(
                    stringResource(R.string.trips_vehicle),
                    state.vehicleLabel ?: stringResource(R.string.trips_none),
                    icon = MpdIcons.Car,
                    trailingIcon = MpdIcons.ChevronDown,
                    onClick = { vehicleMenu = true },
                )
                DropdownMenu(expanded = vehicleMenu, onDismissRequest = { vehicleMenu = false }) {
                    state.vehicles.forEach { v ->
                        DropdownMenuItem(text = { Text(v.label) }, onClick = { vehicleMenu = false; onEdit { it.copy(vehicleId = v.id) } })
                    }
                }
            }
            MpdField(
                stringResource(R.string.trips_distance),
                form.distance,
                onValueChange = { v -> onEdit { it.copy(distance = v) } },
                icon = MpdIcons.Route,
                helper = stringResource(R.string.trips_distance_helper, state.recordedDistance),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            MpdField(
                stringResource(R.string.trips_notes),
                form.notes,
                onValueChange = { v -> onEdit { it.copy(notes = v) } },
                icon = MpdIcons.FileText,
                minHeight = 88.dp,
                singleLine = false,
            )
            MpdButton(
                stringResource(R.string.trips_delete_trip),
                { confirmDelete = true },
                kind = ButtonKind.Text,
                icon = MpdIcons.Trash2,
                height = 44.dp,
                content = c.danger,
            )
        }
        SaveBar(if (state.changes == 0) stringResource(R.string.trips_no_changes) else pluralStringResource(R.plurals.trips_changes, state.changes, state.changes)) {
            MpdButton(stringResource(R.string.trips_save), onSave, height = 48.dp, enabled = state.canSave)
        }
    }
    if (confirmDelete) DeleteTripDialog(onConfirm = { confirmDelete = false; onDelete() }, onDismiss = { confirmDelete = false })
}

internal val sampleEdit = EditTripUiState(
    loading = false,
    form = EditTripForm("Client meeting", TripCategory.BUSINESS, 1, "18.4 mi", "Quarterly review with the Acme team"),
    vehicles = listOf(VehicleOption(1, "Personal · 2021 Civic"), VehicleOption(2, "Rental · Rental")),
    recordedDistance = "18.4 mi",
    changes = 2,
    canSave = true,
)

@Preview(widthDp = 412, heightDp = 879)
@Composable
private fun EditTripLightPreview() = MpdTheme(darkTheme = false) { EditTripScreen(sampleEdit, {}, {}, {}, {}) }

@Preview(widthDp = 412, heightDp = 879)
@Composable
private fun EditTripDarkPreview() = MpdTheme(darkTheme = true) { EditTripScreen(sampleEdit, {}, {}, {}, {}) }
