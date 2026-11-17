package com.aijones.mileage.feature.perdiem.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijones.mileage.core.designsystem.component.CardList
import com.aijones.mileage.core.designsystem.component.InfoNote
import com.aijones.mileage.core.designsystem.component.MpdListRow
import com.aijones.mileage.core.designsystem.component.SectionLabel
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import com.aijones.mileage.core.model.Money

@Composable
fun PerDiemSearchScreenRoute(onBack: () -> Unit) {
    val viewModel = hiltViewModel<PerDiemSearchViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.selected) { if (state.selected) onBack() }
    PerDiemSearchScreen(state, viewModel::onQueryChange, viewModel::clear, viewModel::select, onBack)
}

@Composable
fun PerDiemSearchScreen(
    state: PerDiemSearchUiState,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSelect: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
) {
    val c = MpdTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SearchField(state.query, onQueryChange, onClear, onBack, autoFocus)
        if (state.query.isNotBlank()) {
            SectionLabel(stringResource(R.string.perdiem_results, state.fiscalYear))
            when {
                state.results.isNotEmpty() -> CardList(rows = state.results.map { row -> @Composable { ResultRow(row, onSelect) } })
                state.searchedQuery == state.query.trim() -> InfoNote(stringResource(R.string.perdiem_no_results), icon = MpdIcons.MapPin)
            }
        }
        if (state.recents.isNotEmpty()) {
            SectionLabel(stringResource(R.string.perdiem_recent))
            CardList(
                rows = state.recents.map { row ->
                    @Composable {
                        MpdListRow(
                            row.label, icon = MpdIcons.History, iconFg = c.text2, chevron = true, onClick = { onSelect(row.id) },
                            modifier = Modifier.semantics(mergeDescendants = true) {},
                        )
                    }
                },
            )
        }
        state.standard?.let { InfoNote(stringResource(R.string.perdiem_standard_note, it.lodging.formatRate(), it.mie.formatRate())) }
    }
}

@Composable
private fun ResultRow(row: LocationRow, onSelect: (Long) -> Unit) {
    MpdListRow(
        row.label,
        Modifier.semantics(mergeDescendants = true) {},
        subtitle = row.county ?: stringResource(R.string.perdiem_standard_area),
        icon = MpdIcons.MapPin,
        iconFg = MpdTheme.colors.text2,
        amount = row.mieDaily?.formatRate(),
        amountCaption = row.mieDaily?.let { stringResource(R.string.perdiem_mie_per_day) },
        onClick = { onSelect(row.id) },
    )
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit, onBack: () -> Unit, autoFocus: Boolean) {
    val c = MpdTheme.colors
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val hint = stringResource(R.string.perdiem_search_hint)
    if (autoFocus) LaunchedEffect(focus) { focus.requestFocus() }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(CircleShape)
            .background(c.surface2)
            .border(2.dp, c.primary, CircleShape)
            .padding(end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { MpdIcon(MpdIcons.ArrowLeft, c.text, size = 22.dp, contentDescription = stringResource(R.string.perdiem_search_back)) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text(hint, style = MpdTheme.type.bodyL, color = c.text2)
            BasicTextField(
                query,
                onQueryChange,
                Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = hint },
                textStyle = MpdTheme.type.bodyL.copy(color = c.text),
                cursorBrush = SolidColor(c.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = onClear) { MpdIcon(MpdIcons.X, c.text2, size = 20.dp, contentDescription = stringResource(R.string.perdiem_search_clear)) }
        }
    }
}

internal val previewSearchState = PerDiemSearchUiState(
    fiscalYear = 2027,
    query = "bos",
    searchedQuery = "bos",
    results = listOf(
        LocationRow(3, "Boston, MA", "Suffolk County", Money.ofDollars(92)),
        LocationRow(4, "Boston (Cambridge), MA", "Middlesex County", Money.ofDollars(92)),
        LocationRow(5, "Bossier City, LA", null, Money.ofDollars(68)),
        LocationRow(6, "Boscawen, NH", null, Money.ofDollars(68)),
    ),
    recents = listOf(LocationRow(2, "New York City, NY", null), LocationRow(7, "Chicago, IL", null)),
    standard = StandardRate(Money.ofDollars(113), Money.ofDollars(68)),
)

@Preview(name = "Per diem search · light", widthDp = 412, heightDp = 879)
@Composable
private fun PerDiemSearchLightPreview() {
    MpdTheme(darkTheme = false) { PerDiemSearchScreen(previewSearchState, {}, {}, {}, {}, autoFocus = false) }
}

@Preview(name = "Per diem search · dark", widthDp = 412, heightDp = 879)
@Composable
private fun PerDiemSearchDarkPreview() {
    MpdTheme(darkTheme = true) { PerDiemSearchScreen(previewSearchState, {}, {}, {}, {}, autoFocus = false) }
}
