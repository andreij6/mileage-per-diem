package com.aijones.mileage.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme

@Composable
fun MpdCard(
    modifier: Modifier = Modifier,
    radius: Dp = 20.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    spacing: Dp = 12.dp,
    container: Color? = null,
    border: Boolean = container == null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MpdTheme.colors
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container ?: c.surface)
            .then(if (border) Modifier.border(1.dp, c.outline, shape) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier.semantics { heading() }, style = MpdTheme.type.overline, color = MpdTheme.colors.text2)
}

@Composable
fun IconTile(@DrawableRes icon: Int, bg: Color, fg: Color, modifier: Modifier = Modifier, size: Dp = 40.dp, iconSize: Dp = 20.dp, radius: Dp = 12.dp) {
    Box(modifier.size(size).clip(RoundedCornerShape(radius)).background(bg), contentAlignment = Alignment.Center) {
        MpdIcon(icon, fg, size = iconSize)
    }
}

@Composable
fun MpdListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    iconBg: Color = MpdTheme.colors.surface2,
    iconFg: Color = MpdTheme.colors.text,
    amount: String? = null,
    amountCaption: String? = null,
    chevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = MpdTheme.colors
    val t = MpdTheme.type
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) IconTile(icon, iconBg, iconFg)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = t.bodyLStrong, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = t.caption.copy(fontFeatureSettings = "tnum"), color = c.text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (amount != null) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(amount, style = t.amount, color = c.text)
                if (amountCaption != null) Text(amountCaption, style = t.label.copy(fontWeight = null), color = c.text2)
            }
        }
        trailing?.invoke(this)
        if (chevron) MpdIcon(MpdIcons.ChevronRight, c.text2, size = 20.dp)
    }
}

@Composable
fun CardList(modifier: Modifier = Modifier, rows: List<@Composable () -> Unit>) {
    MpdCard(modifier, padding = PaddingValues(vertical = 4.dp), spacing = 0.dp) {
        rows.forEachIndexed { i, row ->
            row()
            if (i < rows.lastIndex) HorizontalDivider(thickness = 1.dp, color = MpdTheme.colors.outline)
        }
    }
}

@Composable
fun ValueRow(
    title: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    value: String? = null,
    subtitle: String? = null,
    color: Color? = null,
    chevron: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = MpdTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) MpdIcon(icon, color ?: c.text2, size = 22.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MpdTheme.type.bodyLStrong, color = color ?: c.text)
            if (subtitle != null) Text(subtitle, style = MpdTheme.type.caption, color = c.text2)
        }
        if (value != null) Text(value, style = MpdTheme.type.bodyS.copy(fontFeatureSettings = "tnum"), color = c.text2)
        when {
            trailing != null -> trailing()
            chevron -> MpdIcon(MpdIcons.ChevronRight, c.text2, size = 20.dp)
        }
    }
}

@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier, @DrawableRes icon: Int = MpdIcons.Info) {
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MpdIcon(icon, MpdTheme.colors.text2, size = 16.dp, modifier = Modifier.padding(top = 1.dp))
        Text(text, style = MpdTheme.type.caption, color = MpdTheme.colors.text2)
    }
}

@Composable
fun Banner(text: String, modifier: Modifier = Modifier, bg: Color = MpdTheme.colors.accentC, fg: Color = MpdTheme.colors.accent, @DrawableRes icon: Int = MpdIcons.Info) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MpdIcon(icon, fg, size = 20.dp)
        Text(text, style = MpdTheme.type.bodyS, color = MpdTheme.colors.text)
    }
}

@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun Grabber(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 32.dp, height = 4.dp).clip(CircleShape).background(MpdTheme.colors.outline2))
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(MpdTheme.colors.outline))
}
