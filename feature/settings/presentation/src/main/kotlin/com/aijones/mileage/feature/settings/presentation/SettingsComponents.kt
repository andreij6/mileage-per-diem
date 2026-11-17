package com.aijones.mileage.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aijones.mileage.core.designsystem.component.ButtonKind
import com.aijones.mileage.core.designsystem.component.CardList
import com.aijones.mileage.core.designsystem.component.MpdButton
import com.aijones.mileage.core.designsystem.component.MpdField
import com.aijones.mileage.core.designsystem.component.MpdRadio
import com.aijones.mileage.core.designsystem.component.SectionLabel
import com.aijones.mileage.core.designsystem.theme.MpdTheme

@Composable
internal fun SettingsSection(title: String, rows: List<@Composable () -> Unit>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(title)
        CardList(rows = rows)
    }
}

@Composable
internal fun SettingsDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String? = stringResource(R.string.settings_cancel),
    confirmEnabled: Boolean = true,
    confirmKind: ButtonKind = ButtonKind.Text,
    content: @Composable () -> Unit,
) {
    val c = MpdTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(title, style = MpdTheme.type.headlineS, color = c.text) },
        text = content,
        confirmButton = { MpdButton(confirmLabel, onConfirm, kind = confirmKind, height = 48.dp, enabled = confirmEnabled) },
        dismissButton = dismissLabel?.let { { MpdButton(it, onDismiss, kind = ButtonKind.Text, height = 48.dp) } },
    )
}

@Composable
internal fun MessageDialog(title: String, body: String, onDismiss: () -> Unit) {
    SettingsDialog(title, onDismiss, stringResource(R.string.settings_done), onDismiss, dismissLabel = null) {
        Text(body, Modifier.verticalScroll(rememberScrollState()), style = MpdTheme.type.bodyM, color = MpdTheme.colors.text2)
    }
}

@Composable
internal fun TextEditDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    SettingsDialog(title, onDismiss, stringResource(R.string.settings_save), { onSave(text.trim()) }) {
        MpdField(title, text, onValueChange = { text = it })
    }
}

@Composable
internal fun <T> ChoiceDialog(title: String, options: List<Pair<T, String>>, selected: T, onDismiss: () -> Unit, onSelect: (T) -> Unit) {
    SettingsDialog(title, onDismiss, stringResource(R.string.settings_cancel), onDismiss, dismissLabel = null) {
        Column(Modifier.selectableGroup()) {
            options.forEach { (value, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(value == selected, role = Role.RadioButton, onClick = { onSelect(value) }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MpdRadio(value == selected, onClick = null)
                    Text(label, style = MpdTheme.type.bodyL, color = MpdTheme.colors.text)
                }
            }
        }
    }
}

@Composable
internal fun PreviewSurface(dark: Boolean, content: @Composable () -> Unit) {
    MpdTheme(darkTheme = dark) {
        Box(Modifier.fillMaxSize().background(MpdTheme.colors.bg)) { content() }
    }
}
