package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Escala Oficial de Espaçamento e Formas - TempoTrack Guia de Redesign v2
 */
object TempoSpacing {
    val space1: Dp = 4.dp   // Micro espaçamento
    val space2: Dp = 8.dp   // Entre ícone e texto
    val space3: Dp = 12.dp  // Itens internos
    val space4: Dp = 16.dp  // Agrupamento padrão
    val space5: Dp = 24.dp  // Separação de seções
    val space6: Dp = 32.dp  // Separação forte
}

object TempoRadius {
    val sm: Dp = 12.dp      // Chips e controles compactos
    val md: Dp = 18.dp      // Painéis e cards de superfície
    val lg: Dp = 26.dp      // Modal e bottom sheets especiais

    val shapeSm = RoundedCornerShape(sm)
    val shapeMd = RoundedCornerShape(md)
    val shapeLg = RoundedCornerShape(lg)
}

object TempoShadows {
    // Especificações de sombra suave do Guia v2 (sem bordas visíveis de 1px)
    val shadowSmallColor = Color(0x38000000)   // Preto ~22%
    val shadowDeepColor = Color(0x48000000)    // Preto ~28%
    val elevationSmall: Dp = 6.dp
    val elevationDeep: Dp = 16.dp
}
