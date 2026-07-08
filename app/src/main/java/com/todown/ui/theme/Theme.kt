package com.todown.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Blue,
    onPrimary = White,
    primaryContainer = DarkBlue,
    onPrimaryContainer = LightBlue,
    secondary = Purple,
    onSecondary = White,
    background = Background,
    onBackground = White,
    surface = Surface,
    onSurface = White,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = Gray,
    error = Red,
    onError = White
)

@Composable
fun ToDownTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
