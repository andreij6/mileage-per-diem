package com.aijones.mileage.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.icon.MpdIcons
import com.aijones.mileage.core.designsystem.theme.MpdTheme

@Composable
fun MpdTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = MpdTheme.colors
    Row(
        modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(start = if (onBack != null) 4.dp else 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { MpdIcon(MpdIcons.ArrowLeft, c.text, contentDescription = "Back") }
        }
        Text(title, Modifier.weight(1f).padding(start = 4.dp).semantics { heading() }, style = MpdTheme.type.topBar, color = c.text)
        actions()
    }
}

@Composable
fun BarAction(@DrawableRes icon: Int, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) { MpdIcon(icon, MpdTheme.colors.text2, contentDescription = contentDescription) }
}

@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier, eyebrow: String? = null, subtitle: String? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (eyebrow != null) Text(eyebrow, style = MpdTheme.type.bodyS.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = MpdTheme.colors.text2)
            Text(title, Modifier.semantics { heading() }, style = MpdTheme.type.headlineL, color = MpdTheme.colors.text)
            if (subtitle != null) Text(subtitle, style = MpdTheme.type.bodyS, color = MpdTheme.colors.text2)
        }
        trailing()
    }
}

data class NavItem(@DrawableRes val icon: Int, val label: String)

@Composable
fun MpdNavBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = MpdTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(c.navBg)
            .drawBehind { drawLine(c.outline, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding()
            .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 16.dp),
    ) {
        items.forEachIndexed { i, item ->
            val on = i == selected
            Column(
                Modifier.weight(1f).selectable(on, role = Role.Tab, onClick = { onSelect(i) }),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier.size(width = 64.dp, height = 32.dp).clip(CircleShape).background(if (on) c.primaryC else c.navBg),
                    contentAlignment = Alignment.Center,
                ) { MpdIcon(item.icon, if (on) c.onPrimaryC else c.text2) }
                Text(
                    item.label,
                    style = if (on) MpdTheme.type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) else MpdTheme.type.label,
                    color = if (on) c.text else c.text2,
                )
            }
        }
    }
}

@Composable
fun SaveBar(caption: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit) {
    val c = MpdTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(c.surface)
            .drawBehind { drawLine(c.outline, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(caption, Modifier.weight(1f), style = MpdTheme.type.bodyS.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = c.text2)
        actions()
    }
}
