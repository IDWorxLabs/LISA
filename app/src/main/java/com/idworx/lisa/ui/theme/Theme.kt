package com.idworx.lisa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LisaColorScheme = lightColorScheme(
    primary = LisaBlue,
    onPrimary = LisaWhite,
    primaryContainer = LisaBlueLight,
    onPrimaryContainer = LisaBlueDark,
    secondary = LisaGray,
    onSecondary = LisaWhite,
    background = LisaSoftGray,
    onBackground = Color(0xFF2C3E50),
    surface = LisaWhite,
    onSurface = Color(0xFF2C3E50),
    surfaceVariant = LisaBlueLight,
    onSurfaceVariant = LisaBlueDark,
    outline = Color(0xFFB0BEC5)
)

@Composable
fun LISATheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LisaColorScheme,
        typography = Typography,
        content = content
    )
}
