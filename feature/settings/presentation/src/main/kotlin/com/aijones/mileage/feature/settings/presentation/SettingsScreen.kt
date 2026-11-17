package com.aijones.mileage.feature.settings.presentation

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.designsystem.component.MpdSwitch
import com.aijones.mileage.core.designsystem.component.ScreenTitle
import com.aijones.mileage.core.designsystem.component.ValueRow
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.ui.MotionDirection
import com.aijones.mileage.core.ui.staggeredEnter
import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.model.formatCentsPerMile
import com.aijones.mileage.feature.settings.domain.BatteryMode
import com.aijones.mileage.feature.settings.domain.ObserveFeatureFlagUseCase
import com.aijones.mileage.feature.settings.domain.ObserveFeatureFlagsUseCase
import com.aijones.mileage.feature.settings.domain.ObserveMileageRateSettingsUseCase
import com.aijones.mileage.feature.settings.domain.ObserveSettingsUseCase
import com.aijones.mileage.feature.settings.domain.Settings
import com.aijones.mileage.feature.settings.domain.ThemeMode
import com.aijones.mileage.feature.settings.domain.UpdateSettingsUseCase
import com.aijones.mileage.feature.trips.domain.ObserveVehiclesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SettingsEdit { DEFAULT_PURPOSE, BATTERY_MODE, REPORT_NAME, COMPANY, THEME }

@Immutable
data class SettingsUiState(
    val settings: Settings = Settings(),
    val vehicleCount: Int = 0,
    val customRate: Boolean = false,
    val rate: String = "",
    val driveDetectionAvailable: Boolean = false,
    val flagCount: Int = 0,
    val editing: SettingsEdit? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    observeVehicles: ObserveVehiclesUseCase,
    observeRate: ObserveMileageRateSettingsUseCase,
    observeFlag: ObserveFeatureFlagUseCase,
    observeFlags: ObserveFeatureFlagsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    analytics: Analytics,
) : ViewModel() {
    private val editing = MutableStateFlow<SettingsEdit?>(null)

    val state: StateFlow<SettingsUiState> = combine(
        combine(observeSettings(), observeVehicles(), observeRate(), observeFlag(FeatureFlag.DRIVE_DETECTION), observeFlags()) { s, v, r, drive, flags ->
            SettingsUiState(s, v.size, r.customRate != null, r.effectiveRate.formatCentsPerMile(), drive, flags.size)
        },
        editing,
    ) { state, edit -> state.copy(editing = edit) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        analytics.track(AnalyticsEvent.ScreenView("settings"))
    }

    fun edit(field: SettingsEdit) {
        editing.value = field
    }

    fun dismiss() {
        editing.value = null
    }

    fun setDefaultPurpose(value: String) = save { it.copy(defaultPurpose = value) }
    fun setReportName(value: String) = save { it.copy(reportName = value) }
    fun setCompany(value: String) = save { it.copy(company = value) }
    fun setBatteryMode(value: BatteryMode) = save { it.copy(batteryMode = value) }
    fun setTheme(value: ThemeMode) = save { it.copy(theme = value) }
    fun setDriveDetection(enabled: Boolean) = save { it.copy(driveDetection = enabled) }

    private fun save(transform: (Settings) -> Settings) {
        editing.value = null
        viewModelScope.launch { updateSettings(transform) }
    }
}

@Composable
fun SettingsRoute(
    onVehicles: () -> Unit,
    onMileageRate: () -> Unit,
    onAbout: () -> Unit,
    onDeveloperOptions: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val debuggable = remember(context) { context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 }
    SettingsScreen(
        state = state,
        debuggable = debuggable,
        onEdit = viewModel::edit,
        onDismiss = viewModel::dismiss,
        onDefaultPurpose = viewModel::setDefaultPurpose,
        onReportName = viewModel::setReportName,
        onCompany = viewModel::setCompany,
        onBatteryMode = viewModel::setBatteryMode,
        onTheme = viewModel::setTheme,
        onDriveDetection = viewModel::setDriveDetection,
        onVehicles = onVehicles,
        onMileageRate = onMileageRate,
        onAbout = onAbout,
        onDeveloperOptions = onDeveloperOptions,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    debuggable: Boolean,
    onEdit: (SettingsEdit) -> Unit,
    onDismiss: () -> Unit,
    onDefaultPurpose: (String) -> Unit,
    onReportName: (String) -> Unit,
    onCompany: (String) -> Unit,
    onBatteryMode: (BatteryMode) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onDriveDetection: (Boolean) -> Unit,
    onVehicles: () -> Unit,
    onMileageRate: () -> Unit,
    onAbout: () -> Unit,
    onDeveloperOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = state.settings
    val notSet = stringResource(R.string.settings_not_set)
    val batteryLabels = BatteryMode.entries.map { it to stringResource(it.label) }
    val themeLabels = ThemeMode.entries.map { it to stringResource(it.label) }
    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.staggeredEnter(MotionDirection.Top, 0)) {
            ScreenTitle(stringResource(R.string.settings_title))
        }
        Box(Modifier.staggeredEnter(MotionDirection.Start, 40)) {
            SettingsSection(
            stringResource(R.string.settings_section_trips),
            listOf(
                { ValueRow(stringResource(R.string.settings_default_purpose), icon = MpdIcons.Pencil, value = s.defaultPurpose.ifBlank { notSet }, onClick = { onEdit(SettingsEdit.DEFAULT_PURPOSE) }) },
                { ValueRow(stringResource(R.string.settings_vehicles), icon = MpdIcons.Car, value = state.vehicleCount.toString(), onClick = onVehicles) },
                {
                    val rate = stringResource(if (state.customRate) R.string.settings_rate_company else R.string.settings_rate_irs, state.rate)
                    ValueRow(stringResource(R.string.settings_mileage_rate), icon = MpdIcons.Receipt, value = rate, onClick = onMileageRate)
                },
                { ValueRow(stringResource(R.string.settings_battery_mode), icon = MpdIcons.BatteryMedium, value = stringResource(s.batteryMode.label), onClick = { onEdit(SettingsEdit.BATTERY_MODE) }) },
            ),
        )
        }
        if (state.driveDetectionAvailable) {
            Box(Modifier.staggeredEnter(MotionDirection.End, 75)) {
                SettingsSection(
                    stringResource(R.string.settings_section_drive_detection),
                    listOf {
                        ValueRow(
                            stringResource(R.string.settings_drive_detection_title),
                            Modifier.toggleable(s.driveDetection, role = Role.Switch, onValueChange = onDriveDetection),
                            icon = MpdIcons.Zap,
                            subtitle = stringResource(R.string.settings_drive_detection_subtitle),
                            trailing = { MpdSwitch(s.driveDetection, onCheckedChange = null) },
                        )
                    },
                )
            }
        }
        Box(Modifier.staggeredEnter(MotionDirection.End, 100)) {
            SettingsSection(
                stringResource(R.string.settings_section_reports),
                listOf(
                    { ValueRow(stringResource(R.string.settings_report_name), icon = MpdIcons.UserRound, value = s.reportName.ifBlank { notSet }, onClick = { onEdit(SettingsEdit.REPORT_NAME) }) },
                    { ValueRow(stringResource(R.string.settings_company), icon = MpdIcons.Building2, value = s.company.ifBlank { notSet }, onClick = { onEdit(SettingsEdit.COMPANY) }) },
                ),
            )
        }
        Box(Modifier.staggeredEnter(MotionDirection.Bottom, 130)) {
            SettingsSection(
                stringResource(R.string.settings_section_app),
                buildList {
                    add { ValueRow(stringResource(R.string.settings_theme), icon = MpdIcons.Sun, value = stringResource(s.theme.label), onClick = { onEdit(SettingsEdit.THEME) }) }
                    add { ValueRow(stringResource(R.string.settings_about), icon = MpdIcons.Info, onClick = onAbout) }
                    if (debuggable || s.developerUnlocked) {
                        add {
                            ValueRow(
                                stringResource(R.string.settings_developer_options), icon = MpdIcons.Settings,
                                value = pluralStringResource(R.plurals.settings_flag_count, state.flagCount, state.flagCount), onClick = onDeveloperOptions,
                            )
                        }
                    }
                },
            )
        }
    }
    when (state.editing) {
        SettingsEdit.DEFAULT_PURPOSE -> TextEditDialog(stringResource(R.string.settings_default_purpose), s.defaultPurpose, onDismiss, onDefaultPurpose)
        SettingsEdit.REPORT_NAME -> TextEditDialog(stringResource(R.string.settings_report_name), s.reportName, onDismiss, onReportName)
        SettingsEdit.COMPANY -> TextEditDialog(stringResource(R.string.settings_company), s.company, onDismiss, onCompany)
        SettingsEdit.BATTERY_MODE -> ChoiceDialog(stringResource(R.string.settings_battery_mode), batteryLabels, s.batteryMode, onDismiss, onBatteryMode)
        SettingsEdit.THEME -> ChoiceDialog(stringResource(R.string.settings_theme), themeLabels, s.theme, onDismiss, onTheme)
        null -> Unit
    }
}

private val BatteryMode.label
    get() = when (this) {
        BatteryMode.AUTO -> R.string.settings_battery_auto
        BatteryMode.ACCURATE -> R.string.settings_battery_accurate
        BatteryMode.SAVER -> R.string.settings_battery_saver
    }

private val ThemeMode.label
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }

internal val previewSettingsState = SettingsUiState(
    settings = Settings(defaultPurpose = "Client meeting", driveDetection = true, reportName = "Jordan Lee", company = "Acme Corp"),
    vehicleCount = 2,
    rate = "76¢",
    driveDetectionAvailable = true,
    flagCount = 4,
)

@Composable
private fun SettingsPreview(dark: Boolean) {
    PreviewSurface(dark) {
        SettingsScreen(previewSettingsState, true, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Settings light", widthDp = 412, heightDp = 915)
@Composable
private fun SettingsLightPreview() = SettingsPreview(false)

@Preview(name = "Settings dark", widthDp = 412, heightDp = 915)
@Composable
private fun SettingsDarkPreview() = SettingsPreview(true)
