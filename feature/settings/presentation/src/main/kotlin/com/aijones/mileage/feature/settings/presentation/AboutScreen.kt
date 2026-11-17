package com.aijones.mileage.feature.settings.presentation

import android.content.Context
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
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
import com.aijones.mileage.core.designsystem.component.IconTile
import com.aijones.mileage.core.designsystem.component.InfoNote
import com.aijones.mileage.core.designsystem.component.MpdCard
import com.aijones.mileage.core.designsystem.component.MpdTopBar
import com.aijones.mileage.core.designsystem.component.ValueRow
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.feature.settings.domain.DeleteAllDataUseCase
import com.aijones.mileage.feature.settings.domain.UnlockDeveloperOptionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AboutDialog { DATA_SOURCES, LICENSES, CONFIRM_DELETE }

@Immutable
data class AboutUiState(val tapsRemaining: Int? = null, val dialog: AboutDialog? = null, val deleted: Boolean = false)

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val unlockDeveloperOptions: UnlockDeveloperOptionsUseCase,
    private val deleteAllData: DeleteAllDataUseCase,
    analytics: Analytics,
) : ViewModel() {
    private val _state = MutableStateFlow(AboutUiState())
    val state: StateFlow<AboutUiState> = _state.asStateFlow()

    init {
        analytics.track(AnalyticsEvent.ScreenView("about"))
    }

    fun tapVersion() {
        viewModelScope.launch {
            val remaining = unlockDeveloperOptions()
            _state.update { it.copy(tapsRemaining = remaining) }
        }
    }

    fun show(dialog: AboutDialog) = _state.update { it.copy(dialog = dialog) }

    fun dismiss() = _state.update { it.copy(dialog = null) }

    fun confirmDelete() {
        _state.update { it.copy(dialog = null) }
        viewModelScope.launch {
            deleteAllData()
            _state.update { it.copy(deleted = true) }
        }
    }
}

@Immutable
data class AppVersion(val name: String, val code: Long)

@Composable
fun AboutRoute(onBack: () -> Unit, viewModel: AboutViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val version = remember(context) { context.appVersion() }
    val uriHandler = LocalUriHandler.current
    val policyUrl = stringResource(R.string.about_privacy_policy_url)
    AboutScreen(
        state, version, onBack, viewModel::tapVersion,
        onPrivacyPolicy = { runCatching { uriHandler.openUri(policyUrl) } },
        onShow = viewModel::show, onDismiss = viewModel::dismiss, onConfirmDelete = viewModel::confirmDelete,
    )
}

private fun Context.appVersion(): AppVersion {
    val info = packageManager.getPackageInfo(packageName, 0)
    @Suppress("DEPRECATION")
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
    return AppVersion(info.versionName.orEmpty(), code)
}

@Composable
fun AboutScreen(
    state: AboutUiState,
    version: AppVersion,
    onBack: () -> Unit,
    onVersionTap: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onShow: (AboutDialog) -> Unit,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    Column(modifier.fillMaxSize()) {
        MpdTopBar(stringResource(R.string.about_title), onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onVersionTap),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IconTile(MpdIcons.Route, c.primary, c.onPrimary, size = 56.dp, iconSize = 28.dp, radius = 16.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.about_app_name), style = t.titleM, color = c.text)
                    Text(stringResource(R.string.about_version, version.name, version.code), style = t.caption, color = c.text2)
                    val remaining = state.tapsRemaining
                    if (remaining != null) {
                        Text(
                            if (remaining > 0) pluralStringResource(R.plurals.about_taps_remaining, remaining, remaining) else stringResource(R.string.about_developer_unlocked),
                            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            style = t.label,
                            color = c.primary,
                        )
                    }
                }
            }
            MpdCard(container = c.primaryC, padding = PaddingValues(18.dp), spacing = 10.dp) {
                Text(stringResource(R.string.about_privacy_title), style = t.titleM, color = c.onPrimaryC)
                listOf(R.string.about_privacy_1, R.string.about_privacy_2, R.string.about_privacy_3).forEach {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MpdIcon(MpdIcons.Check, c.onPrimaryC, size = 18.dp)
                        Text(stringResource(it), style = t.bodyM, color = c.onPrimaryC)
                    }
                }
            }
            SettingsSection(
                stringResource(R.string.about_section_info),
                listOf(
                    {
                        val opens = stringResource(R.string.about_open_externally)
                        ValueRow(
                            stringResource(R.string.about_privacy_policy), icon = MpdIcons.ShieldCheck, onClick = onPrivacyPolicy,
                            trailing = { MpdIcon(MpdIcons.ExternalLink, c.text2, size = 18.dp, contentDescription = opens) },
                        )
                    },
                    {
                        ValueRow(
                            stringResource(R.string.about_data_sources), icon = MpdIcons.ScrollText,
                            value = stringResource(R.string.about_data_sources_value), onClick = { onShow(AboutDialog.DATA_SOURCES) },
                        )
                    },
                    { ValueRow(stringResource(R.string.about_licenses), icon = MpdIcons.FileText, onClick = { onShow(AboutDialog.LICENSES) }) },
                ),
            )
            SettingsSection(
                stringResource(R.string.about_section_data),
                listOf {
                    ValueRow(
                        stringResource(R.string.about_delete_all), icon = MpdIcons.Trash2, color = c.danger, chevron = false,
                        onClick = { onShow(AboutDialog.CONFIRM_DELETE) },
                    )
                },
            )
            if (state.deleted) {
                InfoNote(stringResource(R.string.about_deleted), Modifier.semantics { liveRegion = LiveRegionMode.Polite }, icon = MpdIcons.CircleCheck)
            }
        }
    }
    when (state.dialog) {
        AboutDialog.DATA_SOURCES -> MessageDialog(stringResource(R.string.about_data_sources), stringResource(R.string.about_data_sources_body), onDismiss)
        AboutDialog.LICENSES -> MessageDialog(stringResource(R.string.about_licenses), stringResource(R.string.about_licenses_body), onDismiss)
        AboutDialog.CONFIRM_DELETE -> SettingsDialog(
            stringResource(R.string.about_delete_title), onDismiss, stringResource(R.string.about_delete_confirm), onConfirmDelete,
            confirmKind = ButtonKind.Danger,
        ) { Text(stringResource(R.string.about_delete_body), style = t.bodyM, color = c.text2) }
        null -> Unit
    }
}

private val previewVersion = AppVersion("1.0.0", 1)

@Preview(name = "About light", widthDp = 412, heightDp = 915)
@Composable
private fun AboutLightPreview() = PreviewSurface(false) { AboutScreen(AboutUiState(), previewVersion, {}, {}, {}, {}, {}, {}) }

@Preview(name = "About dark", widthDp = 412, heightDp = 915)
@Composable
private fun AboutDarkPreview() = PreviewSurface(true) { AboutScreen(AboutUiState(), previewVersion, {}, {}, {}, {}, {}, {}) }
