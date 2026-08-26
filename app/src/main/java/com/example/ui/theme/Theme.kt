package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = SpecialistGreenPrimary,
    secondary = AmberPrimary,
    tertiary = CyanPrimary,
    background = TacticalDarkBg,
    surface = TacticalSurface,
    surfaceVariant = TacticalSurfaceVariant,
    onPrimary = Color(0xFF0A0C0B),
    onSecondary = Color(0xFF0A0C0B),
    onTertiary = Color(0xFF0A0C0B),
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFE2E8F0),
    outline = TacticalBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}

