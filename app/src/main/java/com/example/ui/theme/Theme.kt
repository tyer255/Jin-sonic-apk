package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JinSonicColorScheme = darkColorScheme(
    primary = AccentIce,
    onPrimary = DeepNavy,
    secondary = AccentViolet,
    onSecondary = SkyTopNight,
    tertiary = SkyHorizonDay,
    background = SkyTopDay,
    onBackground = InkPrimary,
    surface = SkyMidDay,
    onSurface = InkPrimary,
    surfaceVariant = Color(0x22FFFFFF),
    onSurfaceVariant = InkSecondary,
    outline = Color(0x2BFFFFFF)
)

@Composable
fun JinSonicTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JinSonicColorScheme,
        typography = JinSonicTypography,
        content = content
    )
}
