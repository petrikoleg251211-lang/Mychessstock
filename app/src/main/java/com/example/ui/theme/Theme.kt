package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF005325),
    onPrimaryContainer = EmeraldSecondary,
    secondary = EmeraldSecondary,
    onSecondary = Color.Black,
    tertiary = EmeraldTertiary,
    background = SlateBackground,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder
)

@Composable
fun ChessReviewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Defaulting to the requested elegant Emerald & Dark Slate aesthetic
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
