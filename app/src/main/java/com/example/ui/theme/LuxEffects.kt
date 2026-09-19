package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─── Gradiente de Superfície Padrão (Tech Sóbrio Chumbo/Grafite) ─────────────
val tempoSurfaceGradient = Brush.verticalGradient(
    0.0f to Color(0xFF1C2027),
    1.0f to Color(0xFF171A21)
)

val tempoElevatedGradient = Brush.verticalGradient(
    0.0f to Color(0xFF20252D),
    1.0f to Color(0xFF191C23)
)

// Highlight de material suave sem borda dura visível (reflexo mínimo de luz no topo)
fun Modifier.tempoMaterialHighlight(shape: Shape): Modifier = this.drawWithContent {
    drawContent()
    drawRect(
        brush = Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.04f),
            0.05f to Color.Transparent
        )
    )
}

// ─── Compatibilidade com telas legadas (sem bordas) ───────────────────────────
fun Modifier.luxBorder(shape: Shape): Modifier = this.tempoMaterialHighlight(shape)

// Glow controlado sem borda sólida
fun Modifier.orangeGlowBorder(shape: Shape, width: Dp = 1.dp): Modifier = this.tempoMaterialHighlight(shape)

// Highlight suave para estados de sucesso/concluído sem borda sólida
fun Modifier.greenGlowBorder(shape: Shape, width: Dp = 1.dp): Modifier = this.tempoMaterialHighlight(shape)

// Fundo padrão chumbo com leve transição de profundidade
val vaultoBackgroundBrush = Brush.verticalGradient(
    0.0f to TempoBgBase,
    0.6f to TempoBgDeep,
    1.0f to TempoBgBase
)

val grainBrush: Brush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.004f),
        Color.Transparent,
        Color.Black.copy(alpha = 0.01f)
    )
)

fun Modifier.grainEffect() = drawWithContent {
    drawContent()
    drawRect(brush = grainBrush, size = size)
}

object LuxEffects {
    val grainBrush = com.example.ui.theme.grainBrush
}

