package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TempoDarkColorScheme = darkColorScheme(
    primary              = TempoAccent,
    onPrimary            = TempoTextPrimary,
    primaryContainer     = TempoAccentContainer,
    onPrimaryContainer   = Color(0xFFFFD5C7),

    secondary            = TempoSurface2,
    onSecondary          = TempoTextPrimary,
    secondaryContainer   = TempoSurface3,
    onSecondaryContainer = TempoTextPrimary,

    tertiary             = TempoSuccess,
    onTertiary           = TempoTextPrimary,
    tertiaryContainer    = Color(0xFF14241B),
    onTertiaryContainer  = Color(0xFFD0EBE0),

    background           = TempoBgBase,
    onBackground         = TempoTextPrimary,

    surface              = TempoSurface1,
    onSurface            = TempoTextPrimary,
    surfaceVariant       = TempoSurface2,
    onSurfaceVariant     = TempoTextSecondary,

    outline              = TempoOutline,
    outlineVariant       = Color(0xFF222830),

    error                = TempoDanger,
    onError              = TempoTextPrimary,
    errorContainer       = Color(0xFF331616),
    onErrorContainer     = Color(0xFFFFD5D5),
)

// Legacy alias
val VaultoDarkColorScheme = TempoDarkColorScheme

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = TempoDarkColorScheme,
        typography  = Typography,
        content     = content,
    )
}

