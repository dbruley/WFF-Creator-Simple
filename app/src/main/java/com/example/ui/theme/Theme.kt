package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = StudioCyan,
    onPrimary = Color(0xFF090D16),
    secondary = StudioAmber,
    onSecondary = Color(0xFF090D16),
    tertiary = StudioEmerald,
    onTertiary = Color(0xFF090D16),
    background = StudioBackground,
    onBackground = StudioOnBackground,
    surface = StudioSurface,
    onSurface = StudioOnSurface,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = StudioOutline,
    error = StudioCoral
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
