package com.aijones.mileage.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.aijones.mileage.core.designsystem.component.Banner
import com.aijones.mileage.core.designsystem.component.ButtonKind
import com.aijones.mileage.core.designsystem.component.CardList
import com.aijones.mileage.core.designsystem.component.MpdButton
import com.aijones.mileage.core.designsystem.component.MpdChip
import com.aijones.mileage.core.designsystem.component.MpdSwitch
import com.aijones.mileage.core.designsystem.component.MpdTopBar
import com.aijones.mileage.core.designsystem.component.SectionLabel
import com.aijones.mileage.core.designsystem.component.ValueRow
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.featureflags.FeatureFlag
import com.aijones.mileage.core.featureflags.FlagSource
import com.aijones.mileage.core.featureflags.FlagState
import com.aijones.mileage.core.model.groupThousands
import com.aijones.mileage.feature.settings.domain.ObserveAppHealthUseCase
import com.aijones.mileage.feature.settings.domain.ObserveFeatureFlagsUseCase
import com.aijones.mileage.feature.settings.domain.ResetFeatureFlagOverridesUseCase
import com.aijones.mileage.feature.settings.domain.SetFeatureFlagOverrideUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class DeveloperOptionsUiState(val flags: List<FlagState> = emptyList(), val eventCount: Int = 0) {
    val hasOverrides: Boolean get() = flags.any { it.source == FlagSource.OVERRIDE }
}

@HiltViewModel
class DeveloperOptionsViewModel @Inject constructor(
    observeFlags: ObserveFeatureFlagsUseCase,
    observeAppHealth: ObserveAppHealthUseCase,
    private val setOverride: SetFeatureFlagOverrideUseCase,
    private val resetOverrides: ResetFeatureFlagOverridesUseCase,
    analytics: Analytics,
) : ViewModel() {
    val state: StateFlow<DeveloperOptionsUiState> = combine(observeFlags(), observeAppHealth(Instant.EPOCH)) { flags, health ->
        DeveloperOptionsUiState(flags, health.summary.totalEvents)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeveloperOptionsUiState())

    init {
        analytics.track(AnalyticsEvent.ScreenView("developer_options"))
    }

    fun toggle(flag: FeatureFlag, enabled: Boolean) {
        viewModelScope.launch { setOverride(flag, enabled) }
    }

    fun resetAll() {
        viewModelScope.launch { resetOverrides() }
    }
}

@Composable
fun DeveloperOptionsRoute(onBack: () -> Unit, onAppHealth: () -> Unit, viewModel: DeveloperOptionsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DeveloperOptionsScreen(state, onBack, viewModel::toggle, viewModel::resetAll, onAppHealth)
}

@Composable
fun DeveloperOptionsScreen(
    state: DeveloperOptionsUiState,
    onBack: () -> Unit,
    onToggle: (FeatureFlag, Boolean) -> Unit,
    onReset: () -> Unit,
    onAppHealth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        MpdTopBar(stringResource(R.string.dev_title), onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Banner(stringResource(R.string.dev_note))
            SectionLabel(stringResource(R.string.dev_section_flags))
            CardList(rows = state.flags.map { f -> { FlagRow(f, onToggle) } })
            MpdButton(
                stringResource(R.string.dev_reset), onReset, Modifier.fillMaxWidth(), kind = ButtonKind.Outlined, icon = MpdIcons.RotateCcw,
                height = 48.dp, enabled = state.hasOverrides,
            )
            SettingsSection(
                stringResource(R.string.dev_section_diagnostics),
                listOf {
                    ValueRow(
                        stringResource(R.string.dev_app_health), icon = MpdIcons.ChartColumn,
                        value = pluralStringResource(R.plurals.dev_events, state.eventCount, groupThousands(state.eventCount.toLong())), onClick = onAppHealth,
                    )
                },
            )
        }
    }
}

@Composable
private fun FlagRow(f: FlagState, onToggle: (FeatureFlag, Boolean) -> Unit) {
    val c = MpdTheme.colors
    val override = f.source == FlagSource.OVERRIDE
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(f.enabled, role = Role.Switch, onValueChange = { onToggle(f.flag, it) })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(f.flag.title, Modifier.weight(1f, fill = false), style = MpdTheme.type.bodyLStrong, color = c.text)
                MpdChip(
                    stringResource(if (override) R.string.dev_source_override else R.string.dev_source_default),
                    bg = if (override) c.accentC else c.surface2,
                    fg = if (override) c.accent else c.text2,
                )
            }
            Text(
                stringResource(R.string.dev_flag_detail, f.flag.key, f.flag.description),
                style = MpdTheme.type.label.copy(fontWeight = null),
                color = c.text2,
            )
        }
        MpdSwitch(f.enabled, onCheckedChange = null)
    }
}

internal val previewDeveloperOptions = DeveloperOptionsUiState(
    flags = listOf(
        FlagState(FeatureFlag.YEAR_IN_REVIEW, true, FlagSource.DEFAULT),
        FlagState(FeatureFlag.DRIVE_DETECTION, true, FlagSource.DEFAULT),
        FlagState(FeatureFlag.ROUTE_REPLAY, false, FlagSource.OVERRIDE),
        FlagState(FeatureFlag.LIVE_UPDATE, false, FlagSource.DEFAULT),
    ),
    eventCount = 1284,
)

@Preview(name = "Developer options light", widthDp = 412, heightDp = 915)
@Composable
private fun DeveloperOptionsLightPreview() = PreviewSurface(false) { DeveloperOptionsScreen(previewDeveloperOptions, {}, { _, _ -> }, {}, {}) }

@Preview(name = "Developer options dark", widthDp = 412, heightDp = 915)
@Composable
private fun DeveloperOptionsDarkPreview() = PreviewSurface(true) { DeveloperOptionsScreen(previewDeveloperOptions, {}, { _, _ -> }, {}, {}) }
