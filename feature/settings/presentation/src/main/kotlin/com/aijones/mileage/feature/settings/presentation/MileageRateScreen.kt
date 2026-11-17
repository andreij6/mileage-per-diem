package com.aijones.mileage.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aijones.mileage.core.analytics.Analytics
import com.aijones.mileage.core.analytics.AnalyticsEvent
import com.aijones.mileage.core.designsystem.component.InfoNote
import com.aijones.mileage.core.designsystem.component.MpdCard
import com.aijones.mileage.core.designsystem.component.MpdChip
import com.aijones.mileage.core.designsystem.component.MpdField
import com.aijones.mileage.core.designsystem.component.MpdRadio
import com.aijones.mileage.core.designsystem.component.MpdTopBar
import com.aijones.mileage.core.designsystem.component.SectionLabel
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.designsystem.theme.tnum
import com.aijones.mileage.core.model.Money
import com.aijones.mileage.core.model.formatCentsPerMile
import com.aijones.mileage.feature.settings.domain.ObserveMileageRateSettingsUseCase
import com.aijones.mileage.feature.settings.domain.SetCustomRateUseCase
import com.aijones.mileage.feature.trips.domain.MileageRate
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class RateRow(val period: String, val rate: String, val current: Boolean)

@Immutable
data class MileageRateUiState(
    val companySelected: Boolean = false,
    val rateText: String = "",
    val irsRates: List<RateRow> = emptyList(),
) {
    val rateInvalid: Boolean get() = companySelected && parseDollarsPerMile(rateText) == null
}

@HiltViewModel
class MileageRateViewModel @Inject constructor(
    observeRate: ObserveMileageRateSettingsUseCase,
    private val setCustomRate: SetCustomRateUseCase,
    private val clock: Clock,
    analytics: Analytics,
) : ViewModel() {
    private data class Draft(val companySelected: Boolean, val text: String)

    private val draft = MutableStateFlow<Draft?>(null)

    val state: StateFlow<MileageRateUiState> = combine(observeRate(), draft) { rates, d ->
        val today = LocalDate.now(clock)
        MileageRateUiState(
            companySelected = d?.companySelected ?: (rates.customRate != null),
            rateText = d?.text ?: rates.customRate?.let(::formatDollarsPerMile).orEmpty(),
            irsRates = rates.irsRates.sortedByDescending { it.from }.map { RateRow(it.period(), it.perMile.formatCentsPerMile(), today in it) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MileageRateUiState())

    init {
        analytics.track(AnalyticsEvent.ScreenView("mileage_rate"))
    }

    fun selectIrs() {
        draft.value = Draft(false, state.value.rateText)
        viewModelScope.launch { setCustomRate(null) }
    }

    fun selectCompany() = changeRate(state.value.rateText)

    fun changeRate(text: String) {
        draft.value = Draft(true, text)
        val rate = parseDollarsPerMile(text) ?: return
        viewModelScope.launch { setCustomRate(rate) }
    }
}

internal fun parseDollarsPerMile(text: String): Money? = text.trim().removePrefix("$").trim().toBigDecimalOrNull()
    ?.movePointRight(6)
    ?.setScale(0, RoundingMode.HALF_UP)
    ?.toLong()
    ?.takeIf { it in 1..MAX_RATE_MICROS }
    ?.let(::Money)

internal fun formatDollarsPerMile(rate: Money): String {
    val dollars = BigDecimal.valueOf(rate.micros, 6).stripTrailingZeros()
    return "$" + (if (dollars.scale() < 2) dollars.setScale(2) else dollars).toPlainString()
}

private const val MAX_RATE_MICROS = 100_000_000L
private val dayFormat = DateTimeFormatter.ofPattern("MMM d")
private val dayYearFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")

private fun MileageRate.period(): String = when {
    from.dayOfYear == 1 && to == from.withDayOfYear(from.lengthOfYear()) -> from.year.toString()
    from.year == to.year -> "${from.format(dayFormat)} – ${to.format(dayYearFormat)}"
    else -> "${from.format(dayYearFormat)} – ${to.format(dayYearFormat)}"
}

@Composable
fun MileageRateRoute(onBack: () -> Unit, viewModel: MileageRateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MileageRateScreen(state, onBack, viewModel::selectIrs, viewModel::selectCompany, viewModel::changeRate)
}

@Composable
fun MileageRateScreen(
    state: MileageRateUiState,
    onBack: () -> Unit,
    onSelectIrs: () -> Unit,
    onSelectCompany: () -> Unit,
    onRateChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    Column(modifier.fillMaxSize()) {
        MpdTopBar(stringResource(R.string.rate_title), onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MpdCard(Modifier.selectableGroup(), padding = PaddingValues(vertical = 4.dp), spacing = 0.dp) {
                RateOption(stringResource(R.string.rate_irs), stringResource(R.string.rate_irs_subtitle), !state.companySelected, onSelectIrs)
                RateOption(stringResource(R.string.rate_company), stringResource(R.string.rate_company_subtitle), state.companySelected, onSelectCompany)
            }
            MpdField(
                stringResource(R.string.rate_company_field), state.rateText, onValueChange = onRateChange, icon = MpdIcons.Receipt,
                helper = if (state.rateInvalid) stringResource(R.string.rate_company_invalid) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            SectionLabel(stringResource(R.string.rate_irs_section))
            MpdCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), spacing = 0.dp) {
                state.irsRates.forEach { r ->
                    Row(
                        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(r.period, Modifier.weight(1f), style = t.bodyM, color = c.text)
                        if (r.current) MpdChip(stringResource(R.string.rate_current), bg = c.primaryC, fg = c.onPrimaryC)
                        Text(r.rate, style = t.labelL.copy(fontSize = t.bodyM.fontSize).tnum(), color = c.text)
                    }
                }
            }
            InfoNote(stringResource(R.string.rate_note))
        }
    }
}

@Composable
private fun RateOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MpdRadio(selected, onClick = null)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MpdTheme.type.bodyLStrong, color = MpdTheme.colors.text)
            Text(subtitle, style = MpdTheme.type.caption, color = MpdTheme.colors.text2)
        }
    }
}

internal val previewRate = MileageRateUiState(
    rateText = "$0.65",
    irsRates = listOf(RateRow("Jul 1 – Dec 31, 2026", "76¢", true), RateRow("Jan 1 – Jun 30, 2026", "72.5¢", false), RateRow("2025", "70¢", false)),
)

@Preview(name = "Mileage rate light", widthDp = 412, heightDp = 915)
@Composable
private fun MileageRateLightPreview() = PreviewSurface(false) { MileageRateScreen(previewRate, {}, {}, {}, {}) }

@Preview(name = "Mileage rate dark", widthDp = 412, heightDp = 915)
@Composable
private fun MileageRateDarkPreview() = PreviewSurface(true) { MileageRateScreen(previewRate, {}, {}, {}, {}) }
