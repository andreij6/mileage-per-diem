package com.aijones.mileage.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.designsystem.component.ButtonKind
import com.aijones.mileage.core.designsystem.component.CardList
import com.aijones.mileage.core.designsystem.component.InfoNote
import com.aijones.mileage.core.designsystem.component.MpdButton
import com.aijones.mileage.core.designsystem.component.MpdChip
import com.aijones.mileage.core.designsystem.component.MpdField
import com.aijones.mileage.core.designsystem.component.MpdListRow
import com.aijones.mileage.core.designsystem.component.MpdTopBar
import com.aijones.mileage.core.designsystem.component.SelectChip
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.model.groupThousands
import com.aijones.mileage.feature.settings.domain.ObserveVehicleUsageUseCase
import com.aijones.mileage.feature.trips.domain.ManageVehiclesUseCase
import com.aijones.mileage.feature.trips.domain.VehicleAction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class VehicleItem(val id: Long, val name: String, val kind: String, val milesThisMonth: String, val isDefault: Boolean)

sealed interface VehicleDialog {
    data object Add : VehicleDialog
    data class Rename(val id: Long, val name: String) : VehicleDialog
}

@Immutable
data class VehiclesUiState(val vehicles: List<VehicleItem> = emptyList(), val dialog: VehicleDialog? = null)

@HiltViewModel
class VehiclesViewModel @Inject constructor(
    observeUsage: ObserveVehicleUsageUseCase,
    private val manageVehicles: ManageVehiclesUseCase,
    analytics: Analytics,
) : ViewModel() {
    private val dialog = MutableStateFlow<VehicleDialog?>(null)

    val state: StateFlow<VehiclesUiState> = combine(observeUsage(), dialog) { usage, d ->
        VehiclesUiState(
            usage.map { (v, miles) -> VehicleItem(v.id, v.name, v.kind, groupThousands(miles.miles.roundToLong()), v.isDefault) },
            d,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VehiclesUiState())

    init {
        analytics.track(AnalyticsEvent.ScreenView("vehicles"))
    }

    fun showAdd() {
        dialog.value = VehicleDialog.Add
    }

    fun showRename(id: Long, name: String) {
        dialog.value = VehicleDialog.Rename(id, name)
    }

    fun dismiss() {
        dialog.value = null
    }

    fun add(name: String, kind: String) = act(VehicleAction.Add(name, kind), name)
    fun rename(id: Long, name: String) = act(VehicleAction.Rename(id, name), name)
    fun setDefault(id: Long) = act(VehicleAction.SetDefault(id))
    fun delete(id: Long) = act(VehicleAction.Delete(id))

    private fun act(action: VehicleAction, name: String? = null) {
        if (name != null && name.isBlank()) return
        dialog.value = null
        viewModelScope.launch { manageVehicles(action) }
    }
}

@Composable
fun VehiclesRoute(onBack: () -> Unit, viewModel: VehiclesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    VehiclesScreen(
        state, onBack, viewModel::showAdd, viewModel::showRename, viewModel::dismiss, viewModel::add, viewModel::rename,
        viewModel::setDefault, viewModel::delete,
    )
}

@Composable
fun VehiclesScreen(
    state: VehiclesUiState,
    onBack: () -> Unit,
    onShowAdd: () -> Unit,
    onShowRename: (Long, String) -> Unit,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    onRename: (Long, String) -> Unit,
    onSetDefault: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        MpdTopBar(stringResource(R.string.vehicles_title), onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.vehicles.isNotEmpty()) {
                CardList(rows = state.vehicles.map { v -> { VehicleRow(v, onShowRename, onSetDefault, onDelete) } })
            }
            MpdButton(stringResource(R.string.vehicles_add), onShowAdd, Modifier.fillMaxWidth(), kind = ButtonKind.Outlined, icon = MpdIcons.Plus)
            InfoNote(stringResource(R.string.vehicles_note))
        }
    }
    when (val d = state.dialog) {
        VehicleDialog.Add -> AddVehicleDialog(onDismiss, onAdd)
        is VehicleDialog.Rename -> TextEditDialog(stringResource(R.string.vehicles_rename), d.name, onDismiss) { onRename(d.id, it) }
        null -> Unit
    }
}

@Composable
private fun VehicleRow(v: VehicleItem, onRename: (Long, String) -> Unit, onSetDefault: (Long) -> Unit, onDelete: (Long) -> Unit) {
    val c = MpdTheme.colors
    var menu by remember { mutableStateOf(false) }
    val miles = stringResource(R.string.vehicles_miles_this_month, v.milesThisMonth)
    val moreLabel = stringResource(R.string.vehicles_more, v.name)
    val renameLabel = stringResource(R.string.vehicles_rename)
    val defaultLabel = stringResource(R.string.vehicles_set_default)
    val deleteLabel = stringResource(R.string.vehicles_delete)
    Box {
        MpdListRow(
            v.name,
            Modifier.semantics(mergeDescendants = true) {
                customActions = buildList {
                    add(CustomAccessibilityAction(renameLabel) { onRename(v.id, v.name); true })
                    if (!v.isDefault) add(CustomAccessibilityAction(defaultLabel) { onSetDefault(v.id); true })
                    add(CustomAccessibilityAction(deleteLabel) { onDelete(v.id); true })
                }
            },
            subtitle = if (v.kind.isBlank()) miles else stringResource(R.string.vehicles_kind_and_miles, v.kind, miles),
            icon = MpdIcons.Car,
            iconBg = if (v.isDefault) c.primaryC else c.surface2,
            iconFg = if (v.isDefault) c.onPrimaryC else c.text,
            onClick = { menu = true },
            trailing = {
                if (v.isDefault) {
                    MpdChip(stringResource(R.string.vehicles_default), bg = c.primaryC, fg = c.onPrimaryC)
                } else {
                    MpdIcon(MpdIcons.EllipsisVertical, c.text2, size = 20.dp, modifier = Modifier.semantics { contentDescription = moreLabel })
                }
            },
        )
        DropdownMenu(menu, onDismissRequest = { menu = false }, containerColor = c.surface) {
            DropdownMenuItem(text = { Text(stringResource(R.string.vehicles_rename)) }, onClick = { menu = false; onRename(v.id, v.name) })
            if (!v.isDefault) {
                DropdownMenuItem(text = { Text(stringResource(R.string.vehicles_set_default)) }, onClick = { menu = false; onSetDefault(v.id) })
            }
            DropdownMenuItem(text = { Text(stringResource(R.string.vehicles_delete), color = c.danger) }, onClick = { menu = false; onDelete(v.id) })
        }
    }
}

@Composable
private fun AddVehicleDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val kinds = listOf(R.string.vehicles_kind_personal, R.string.vehicles_kind_company, R.string.vehicles_kind_rental).map { stringResource(it) }
    var kind by rememberSaveable { mutableStateOf(kinds.first()) }
    SettingsDialog(
        stringResource(R.string.vehicles_add), onDismiss, stringResource(R.string.settings_save), { onAdd(name.trim(), kind) },
        confirmEnabled = name.isNotBlank(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MpdField(stringResource(R.string.vehicles_name), name, onValueChange = { name = it })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                kinds.forEach { SelectChip(it, it == kind, onClick = { kind = it }) }
            }
        }
    }
}

internal val previewVehicles = VehiclesUiState(
    listOf(VehicleItem(1, "2021 Civic", "Personal", "612", true), VehicleItem(2, "Rental", "", "233", false)),
)

@Preview(name = "Vehicles light", widthDp = 412, heightDp = 915)
@Composable
private fun VehiclesLightPreview() = PreviewSurface(false) { VehiclesScreen(previewVehicles, {}, {}, { _, _ -> }, {}, { _, _ -> }, { _, _ -> }, {}, {}) }

@Preview(name = "Vehicles dark", widthDp = 412, heightDp = 915)
@Composable
private fun VehiclesDarkPreview() = PreviewSurface(true) { VehiclesScreen(previewVehicles, {}, {}, { _, _ -> }, {}, { _, _ -> }, { _, _ -> }, {}, {}) }
