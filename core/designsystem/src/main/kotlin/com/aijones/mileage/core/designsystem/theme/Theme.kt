package com.aijones.mileage.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalMpdColors = staticCompositionLocalOf { LightColors }
val LocalMpdType = staticCompositionLocalOf { MpdType() }

object MpdTheme {
    val colors: MpdColors
        @Composable @ReadOnlyComposable get() = LocalMpdColors.current
    val type: MpdType
        @Composable @ReadOnlyComposable get() = LocalMpdType.current
}

private val type = MpdType()

@Composable
fun MpdTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (darkTheme) DarkColors else LightColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = c.primary, onPrimary = c.onPrimary, primaryContainer = c.primaryC, onPrimaryContainer = c.onPrimaryC,
            secondary = c.accent, secondaryContainer = c.accentC, error = c.danger, onError = c.onDanger, errorContainer = c.dangerC,
            background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text, surfaceVariant = c.surface2,
            onSurfaceVariant = c.text2, outline = c.outline2, outlineVariant = c.outline, surfaceContainer = c.surface,
            surfaceContainerHigh = c.surface, surfaceContainerLow = c.surface, surfaceContainerHighest = c.surface2,
        )
    } else {
        lightColorScheme(
            primary = c.primary, onPrimary = c.onPrimary, primaryContainer = c.primaryC, onPrimaryContainer = c.onPrimaryC,
            secondary = c.accent, secondaryContainer = c.accentC, error = c.danger, onError = c.onDanger, errorContainer = c.dangerC,
            background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text, surfaceVariant = c.surface2,
            onSurfaceVariant = c.text2, outline = c.outline2, outlineVariant = c.outline, surfaceContainer = c.surface,
            surfaceContainerHigh = c.surface, surfaceContainerLow = c.surface, surfaceContainerHighest = c.surface2,
        )
    }
    CompositionLocalProvider(LocalMpdColors provides c, LocalMpdType provides type) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
