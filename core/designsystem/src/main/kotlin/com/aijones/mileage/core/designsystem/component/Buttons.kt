package com.aijones.mileage.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aijones.mileage.core.designsystem.icon.MpdIcon
import com.aijones.mileage.core.designsystem.theme.MpdTheme
import androidx.compose.material3.Button as M3Button

enum class ButtonKind { Filled, Tonal, Outlined, Text, Danger }

@Composable
fun MpdButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Filled,
    @DrawableRes icon: Int? = null,
    height: Dp = 56.dp,
    enabled: Boolean = true,
    container: Color? = null,
    content: Color? = null,
) {
    val c = MpdTheme.colors
    val bg = container ?: when (kind) {
        ButtonKind.Filled -> c.primary
        ButtonKind.Tonal -> c.primaryC
        ButtonKind.Danger -> c.danger
        ButtonKind.Outlined, ButtonKind.Text -> Color.Transparent
    }
    val fg = content ?: when (kind) {
        ButtonKind.Filled -> c.onPrimary
        ButtonKind.Tonal -> c.onPrimaryC
        ButtonKind.Danger -> c.onDanger
        ButtonKind.Outlined, ButtonKind.Text -> c.primary
    }
    M3Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = height),
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg, disabledContainerColor = bg.copy(alpha = 0.38f)),
        border = if (kind == ButtonKind.Outlined) BorderStroke(1.dp, c.outline) else null,
        contentPadding = PaddingValues(start = if (icon != null) 20.dp else 24.dp, end = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                MpdIcon(icon, fg, size = 20.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MpdTheme.type.labelL, color = fg)
        }
    }
}
