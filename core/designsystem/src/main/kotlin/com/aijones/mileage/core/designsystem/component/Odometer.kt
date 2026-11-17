package com.aijones.mileage.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aijones.mileage.core.designsystem.theme.SpaceGrotesk

data class OdometerColors(val digit: Color, val drum: Color, val tenthDrum: Color, val tenthDigit: Color)

@Composable
fun Odometer(
    tenths: Long,
    colors: OdometerColors,
    modifier: Modifier = Modifier,
    wholeDigits: Int = 4,
    size: Int = 26,
    unit: String? = "mi",
    animate: Boolean = true,
) {
    val whole = (tenths / 10).toString().padStart(wholeDigits, '0')
    val tenth = (tenths % 10).toString()
    val drumW = (size * 0.78f).dp
    val drumH = (size * 1.3f).dp
    val style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = size.sp)
    val spoken = "${tenths / 10}.${tenths % 10} ${if (unit == "mi") "miles" else unit.orEmpty()}"
    Row(
        modifier.clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        whole.forEach { d -> Drum(d, colors.drum, colors.digit, drumW, drumH, style, size, animate) }
        Box(Modifier.size(4.dp).clip(RoundedCornerShape(2.dp)).background(colors.digit))
        Drum(tenth.first(), colors.tenthDrum, colors.tenthDigit, drumW, drumH, style, size, animate)
        if (unit != null) Text(unit, style = style.copy(fontSize = (size * 0.55f).sp, fontWeight = FontWeight.SemiBold), color = colors.digit)
    }
}

@Composable
private fun Drum(digit: Char, drum: Color, ink: Color, w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp, style: TextStyle, size: Int, animate: Boolean) {
    val shape = RoundedCornerShape((size * 0.22f).dp)
    Box(
        Modifier
            .size(w, h)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(drum.copy(alpha = drum.alpha * 0.75f), drum, drum.copy(alpha = drum.alpha * 0.75f)))),
        contentAlignment = Alignment.Center,
    ) {
        if (animate) {
            AnimatedContent(
                targetState = digit,
                transitionSpec = { slideInVertically(tween(260)) { it } togetherWith slideOutVertically(tween(260)) { -it } },
                label = "drum",
            ) { Text(it.toString(), style = style, color = ink) }
        } else {
            Text(digit.toString(), style = style, color = ink)
        }
    }
}
