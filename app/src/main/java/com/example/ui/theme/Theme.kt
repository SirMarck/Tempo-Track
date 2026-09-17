package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VaultoDarkColorScheme = darkColorScheme(
    primary              = VaultoPrimary,
    onPrimary            = VaultoOnPrimary,
    primaryContainer     = VaultoPrimaryContainer,
    onPrimaryContainer   = VaultoOnPrimaryContainer,

    secondary            = VaultoSecondary,
    onSecondary          = VaultoOnSecondary,
    secondaryContainer   = VaultoSecondaryContainer,
    onSecondaryContainer = VaultoOnSecondaryContainer,

    background           = VaultoBg,
    onBackground         = VaultoOnBg,

    surface              = VaultoSurface,
    onSurface            = VaultoOnSurface,
    surfaceVariant       = VaultoSurfaceVar,
    onSurfaceVariant     = VaultoOnSurfaceVar,

    outline              = VaultoOutline,

    error                = VaultoError,
    onError              = VaultoOnError,
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = VaultoDarkColorScheme,
        typography  = Typography,
        content     = content,
    )
}
