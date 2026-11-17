package com.aijones.mileage.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class MpdColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val outline: Color,
    val outline2: Color,
    val text: Color,
    val text2: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryC: Color,
    val onPrimaryC: Color,
    val accent: Color,
    val accentC: Color,
    val onAccent: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerC: Color,
    val navBg: Color,
    val mapBg: Color,
    val mapDot: Color,
    val hero: Color,
    val onHero: Color,
    val onHero2: Color,
    val drumTenth: Color,
    val onDrumTenth: Color,
    val storyTop: Color,
    val storyBottom: Color,
)

val LightColors = MpdColors(
    isDark = false,
    bg = Color(0xFFF4F6F2), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFE9EEE8), outline = Color(0xFFDCE2DA),
    outline2 = Color(0xFF7B8781), text = Color(0xFF121815), text2 = Color(0xFF59645E), primary = Color(0xFF0D6B5B),
    onPrimary = Color(0xFFFFFFFF), primaryC = Color(0xFFC9EBE1), onPrimaryC = Color(0xFF05372E), accent = Color(0xFF9A5B00),
    accentC = Color(0xFFFCE7C3), onAccent = Color(0xFFFFFFFF), danger = Color(0xFFB3261E), onDanger = Color(0xFFFFFFFF),
    dangerC = Color(0xFFFBDCD8), navBg = Color(0xFFFFFFFF), mapBg = Color(0xFFEAEFE8), mapDot = Color(0xFFC9D2C6),
    hero = Color(0xFF0D6B5B), onHero = Color(0xFFFFFFFF), onHero2 = Color(0xFFBFE6DA), drumTenth = Color(0xFFF5B85A),
    onDrumTenth = Color(0xFF3B2A0C), storyTop = Color(0xFF0D6B5B), storyBottom = Color(0xFF05302A),
)

val DarkColors = MpdColors(
    isDark = true,
    bg = Color(0xFF0C110F), surface = Color(0xFF141B18), surface2 = Color(0xFF1D2622), outline = Color(0xFF29342F),
    outline2 = Color(0xFF8C9A93), text = Color(0xFFE6EDE9), text2 = Color(0xFF9AA8A1), primary = Color(0xFF5FD8BE),
    onPrimary = Color(0xFF00382E), primaryC = Color(0xFF124A3F), onPrimaryC = Color(0xFFBDF3E4), accent = Color(0xFFF5B85A),
    accentC = Color(0xFF3B2A0C), onAccent = Color(0xFF3B2A0C), danger = Color(0xFFFF8A80), onDanger = Color(0xFF4A0C08),
    dangerC = Color(0xFF4A1A17), navBg = Color(0xFF141B18), mapBg = Color(0xFF121915), mapDot = Color(0xFF2A3630),
    hero = Color(0xFF123F36), onHero = Color(0xFFE6FFF7), onHero2 = Color(0xFF8FD3C1), drumTenth = Color(0xFFF5B85A),
    onDrumTenth = Color(0xFF00382E), storyTop = Color(0xFF145247), storyBottom = Color(0xFF050D0B),
)
