package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LuxDarkColorScheme =
  darkColorScheme(
    primary = LuxGreenPrimary,
    onPrimary = LuxGreenOnPrimary,
    primaryContainer = LuxGreenPrimaryContainer,
    onPrimaryContainer = LuxGreenOnPrimaryContainer,
    secondary = LuxGreenSecondary,
    onSecondary = LuxGreenOnSecondary,
    secondaryContainer = LuxGreenSecondaryContainer,
    onSecondaryContainer = LuxGreenOnSecondaryContainer,
    background = LuxGreenBackground,
    onBackground = LuxGreenOnBackground,
    surface = LuxGreenSurface,
    onSurface = LuxGreenOnSurface,
    surfaceVariant = LuxGreenSurfaceVariant,
    onSurfaceVariant = LuxGreenOnSurfaceVariant,
    outline = LuxGreenOutline,
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = LuxDarkColorScheme, typography = Typography, content = content)
}
