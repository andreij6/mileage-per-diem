package com.aijones.mileage.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.theme.MpdTheme

@Composable
fun MpdChip(
    label: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    bg: Color = MpdTheme.colors.surface2,
    fg: Color = MpdTheme.colors.text2,
    outlined: Boolean = false,
    selected: Boolean? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .then(if (onClick != null) Modifier.minimumInteractiveComponentSize() else Modifier)
            .clip(CircleShape)
            .then(if (outlined) Modifier.border(1.dp, MpdTheme.colors.outline, CircleShape) else Modifier.background(bg))
            .then(if (onClick != null) Modifier.clickable(role = Role.Checkbox, onClick = onClick) else Modifier)
            .then(if (selected != null) Modifier.semantics { this.selected = selected } else Modifier)
            .padding(start = if (icon != null) 10.dp else 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) MpdIcon(icon, fg, size = 16.dp)
        Text(label, style = MpdTheme.type.label, color = fg)
    }
}

@Composable
fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null) {
    val c = MpdTheme.colors
    MpdChip(
        label, modifier, icon = icon ?: if (selected) com.aijones.mileage.core.designsystem.icon.MpdIcons.Check else null,
        bg = if (selected) c.primaryC else c.surface2, fg = if (selected) c.onPrimaryC else c.text2, outlined = !selected,
        selected = selected, onClick = onClick,
    )
}
