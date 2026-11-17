package com.aijones.mileage.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aijones.mileage.core.designsystem.R

private fun variable(res: Int, vararg weights: Int) = FontFamily(
    weights.map { w -> Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) },
)

val Inter = variable(R.font.inter, 400, 500, 600, 700)
val SpaceGrotesk = variable(R.font.space_grotesk, 400, 500, 600, 700)

private val trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun style(family: FontFamily, size: Int, weight: Int, lineHeight: TextUnit = (size * 1.3).sp, spacing: TextUnit = 0.sp, tnum: Boolean = false) =
    TextStyle(
        fontFamily = family,
        fontSize = size.sp,
        fontWeight = FontWeight(weight),
        lineHeight = lineHeight,
        letterSpacing = spacing,
        lineHeightStyle = trim,
        fontFeatureSettings = if (tnum) "tnum" else null,
    )

@Immutable
data class MpdType(
    val displayL: TextStyle = style(SpaceGrotesk, 44, 700, 50.sp),
    val displayM: TextStyle = style(SpaceGrotesk, 34, 700, 40.sp),
    val headlineL: TextStyle = style(SpaceGrotesk, 32, 600, 38.sp),
    val headlineM: TextStyle = style(SpaceGrotesk, 28, 600, 34.sp),
    val headlineS: TextStyle = style(SpaceGrotesk, 24, 600, 30.sp),
    val valueL: TextStyle = style(SpaceGrotesk, 30, 700, 36.sp),
    val valueM: TextStyle = style(SpaceGrotesk, 22, 600, 28.sp),
    val valueS: TextStyle = style(SpaceGrotesk, 18, 600, 24.sp),
    val topBar: TextStyle = style(Inter, 22, 400, 28.sp),
    val titleM: TextStyle = style(Inter, 18, 600, 24.sp),
    val bodyL: TextStyle = style(Inter, 16, 400, 24.sp),
    val bodyLStrong: TextStyle = style(Inter, 16, 500, 22.sp),
    val bodyM: TextStyle = style(Inter, 15, 400, 21.sp),
    val bodyS: TextStyle = style(Inter, 14, 400, 20.sp),
    val labelL: TextStyle = style(Inter, 16, 600, 20.sp),
    val labelM: TextStyle = style(Inter, 14, 600, 18.sp),
    val caption: TextStyle = style(Inter, 13, 400, 18.sp),
    val label: TextStyle = style(Inter, 12, 500, 16.sp),
    val overline: TextStyle = style(Inter, 12, 600, 16.sp, 0.06.em),
    val micro: TextStyle = style(Inter, 11, 500, 14.sp),
    val amount: TextStyle = style(Inter, 16, 600, 22.sp, tnum = true),
)

fun TextStyle.tnum() = copy(fontFeatureSettings = "tnum")
