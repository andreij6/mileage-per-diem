package com.aijones.mileage.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme

@Composable
fun MpdSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val c = MpdTheme.colors
    Switch(
        checked, onCheckedChange, modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.onPrimary, checkedTrackColor = c.primary, checkedBorderColor = c.primary,
            uncheckedThumbColor = c.outline2, uncheckedTrackColor = c.surface2, uncheckedBorderColor = c.outline2,
        ),
    )
}

@Composable
fun MpdRadio(selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    RadioButton(selected, onClick, modifier, colors = RadioButtonDefaults.colors(selectedColor = MpdTheme.colors.primary, unselectedColor = MpdTheme.colors.outline2))
}

@Composable
fun SegmentedControl(items: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = MpdTheme.colors
    Row(
        modifier.fillMaxWidth().height(40.dp).clip(CircleShape).border(1.dp, c.outline2, CircleShape),
    ) {
        items.forEachIndexed { i, label ->
            val on = i == selected
            if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(c.outline2))
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (on) c.primaryC else c.surface.copy(alpha = 0f))
                    .semantics { this.selected = on; role = Role.Tab }
                    .clickable { onSelect(i) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (on) {
                    MpdIcon(MpdIcons.Check, c.onPrimaryC, size = 16.dp)
                    Box(Modifier.width(6.dp))
                }
                Text(label, style = if (on) MpdTheme.type.labelM.copy(fontSize = MpdTheme.type.caption.fontSize) else MpdTheme.type.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = if (on) c.onPrimaryC else c.text)
            }
        }
    }
}

@Composable
fun MpdField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: ((String) -> Unit)? = null,
    @DrawableRes icon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
    helper: String? = null,
    minHeight: Dp = 64.dp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val c = MpdTheme.colors
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .clip(shape)
                .border(if (focused) 2.dp else 1.dp, if (focused) c.primary else c.outline2, shape)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) MpdIcon(icon, c.text2, size = 22.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MpdTheme.type.label, color = if (focused) c.primary else c.text2)
                if (onValueChange != null) {
                    BasicTextField(
                        value, onValueChange,
                        Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.semantics { contentDescription = label },
                        textStyle = MpdTheme.type.bodyL.copy(color = c.text, fontFeatureSettings = "tnum"),
                        cursorBrush = SolidColor(c.primary),
                        keyboardOptions = keyboardOptions,
                        singleLine = singleLine,
                    )
                } else {
                    Text(value, style = MpdTheme.type.bodyL.copy(fontFeatureSettings = "tnum"), color = c.text)
                }
            }
            if (trailingIcon != null) MpdIcon(trailingIcon, c.text2, size = 20.dp)
        }
        if (helper != null) Text(helper, Modifier.padding(horizontal = 16.dp), style = MpdTheme.type.label.copy(fontWeight = null), color = c.text2)
    }
}
