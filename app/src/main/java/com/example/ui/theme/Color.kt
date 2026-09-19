package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Design System: Tech Sóbrio (TempoTrack Guia de Redesign v2) ─────────────

// Backgrounds & Superfícies (Cinza Chumbo / Grafite Sóbrio)
val TempoBgBase         = Color(0xFF15181D) // Fundo principal; cinza grafite do pôster
val TempoBgDeep         = Color(0xFF171B21) // Variação para gradientes de profundidade
val TempoSurface1       = Color(0xFF1C2027) // Superfície discreta; listas e agrupamentos
val TempoSurface2       = Color(0xFF20252D) // Elementos elevados e controles
val TempoSurface3       = Color(0xFF262B34) // Estados selecionados sem borda
val TempoOutline        = Color(0xFF1E232B) // Divisores e highlights quase invisíveis

// Tipografia e Conteúdo
val TempoTextPrimary    = Color(0xFFF2F0EB) // Títulos, números, informação principal
val TempoTextSecondary  = Color(0xFFA9AFB5) // Descrição e metadados
val TempoTextMuted      = Color(0xFF747C84) // Informação terciária

// Accent (Laranja Queimado Controlado)
val TempoAccent         = Color(0xFFE46F43) // Ação principal e estado ativo
val TempoAccentSoft     = Color(0xFFB95736) // Estados menos intensos
val TempoAccentGlow     = Color(0x1FE46F43) // Brilho local muito sutil (~12% opacidade)
val TempoAccentContainer= Color(0xFF261814) // Container de accent escuro

// Estados Funcionais Sóbrios (Sem neon)
val TempoSuccess        = Color(0xFF5F987A) // Pago, concluído, dentro da meta
val TempoWarning        = Color(0xFFBE9663) // Atenção, limite próximo
val TempoDanger         = Color(0xFFB85A5A) // Excluir, erro, estourado

// ─── Aliases de Compatibilidade (mantém telas existentes compilando) ──────────
val VaultoBg                 = TempoBgBase
val VaultoSurface            = TempoSurface1
val VaultoSurfaceVar         = TempoSurface2
val VaultoOutline            = TempoOutline

val VaultoPrimary            = TempoAccent
val VaultoOnPrimary          = Color(0xFFFFFFFF)
val VaultoPrimaryContainer   = TempoAccentContainer
val VaultoOnPrimaryContainer = Color(0xFFFFD5C7)

val VaultoSecondary          = TempoSuccess       // Substitui antigo verde neon por tom sóbrio
val VaultoOnSecondary        = Color(0xFF0E1A14)
val VaultoSecondaryContainer = Color(0xFF14241B)
val VaultoOnSecondaryContainer = Color(0xFFD0EBE0)

val VaultoOnBg               = TempoTextPrimary
val VaultoOnSurface          = TempoTextPrimary
val VaultoOnSurfaceVar       = TempoTextSecondary

val VaultoError              = TempoDanger
val VaultoOnError            = Color(0xFFFFFFFF)

val VaultoGlow               = TempoAccentGlow
val VaultoGlowGreen          = Color(0x185F987A)
val VaultoGlassStroke        = Color(0x0DFFFFFF) // Highlight sutil 5%, sem borda pesada
