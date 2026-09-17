package com.example.ui.theme

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─── Glassmorphism border ─────────────────────────────────────────────────────
// Thin gradient border that simulates a glass edge. Use on cards & dialogs.
fun Modifier.luxBorder(shape: Shape): Modifier = this.border(
    width = 1.dp,
    brush = Brush.linearGradient(
        0.0f to Color.White.copy(alpha = 0.18f),
        0.3f to Color.White.copy(alpha = 0.06f),
        0.7f to Color.Transparent,
        1.0f to Color.White.copy(alpha = 0.10f)
    ),
    shape = shape
)

// ─── Orange glow border ───────────────────────────────────────────────────────
// Highlights the active-session card or primary CTA cards.
fun Modifier.orangeGlowBorder(shape: Shape, width: Dp = 1.dp): Modifier = this.border(
    width = width,
    brush = Brush.linearGradient(
        0.0f to VaultoPrimary.copy(alpha = 0.80f),
        0.5f to VaultoPrimary.copy(alpha = 0.25f),
        1.0f to VaultoPrimary.copy(alpha = 0.60f)
    ),
    shape = shape
)

// ─── Green glow border ────────────────────────────────────────────────────────
// Used on earnings / timer elements.
fun Modifier.greenGlowBorder(shape: Shape, width: Dp = 1.dp): Modifier = this.border(
    width = width,
    brush = Brush.linearGradient(
        0.0f to VaultoSecondary.copy(alpha = 0.80f),
        0.5f to VaultoSecondary.copy(alpha = 0.25f),
        1.0f to VaultoSecondary.copy(alpha = 0.60f)
    ),
    shape = shape
)

// ─── Subtle background gradient sweep ────────────────────────────────────────
// Apply on the root Scaffold background for a barely-visible depth effect.
val vaultoBackgroundBrush = Brush.verticalGradient(
    0.0f to Color(0xFF0A0B0E),
    0.4f to Color(0xFF0D0F13),
    1.0f to Color(0xFF080A0D),
)

// ─── Grain effect (kept for compatibility) ───────────────────────────────────
val grainBrush: Brush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.008f),
        Color.Transparent,
        Color.Black.copy(alpha = 0.015f)
    )
)

fun Modifier.grainEffect() = drawWithContent {
    drawContent()
    drawRect(brush = grainBrush, size = size)
}

// Legacy alias kept so existing call-sites compile without changes
object LuxEffects {
    val grainBrush = com.example.ui.theme.grainBrush
}
