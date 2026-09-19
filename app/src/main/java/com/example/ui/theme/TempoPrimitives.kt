package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Primitives Visuais Canônicas do TempoTrack (Guia de Redesign v2 - Prompt 1)
 */

/**
 * 1. Surface: Superfície padrão discreta para listas, blocos e agrupamentos.
 * Usa gradiente sutil #1A1F24 -> #111519 e destaque de material sem borda 1px.
 */
@Composable
fun TempoSurface(
    modifier: Modifier = Modifier,
    shape: Shape = TempoRadius.shapeMd,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = TempoShadows.elevationSmall,
                shape = shape,
                ambientColor = TempoShadows.shadowSmallColor,
                spotColor = TempoShadows.shadowSmallColor
            )
            .clip(shape)
            .background(tempoSurfaceGradient)
            .tempoMaterialHighlight(shape)
    ) {
        Column(
            modifier = Modifier.padding(TempoSpacing.space4),
            content = content
        )
    }
}

/**
 * 2. ElevatedSurface: Superfície elevada para controles centrais, modais e elementos destacados.
 */
@Composable
fun TempoElevatedSurface(
    modifier: Modifier = Modifier,
    shape: Shape = TempoRadius.shapeMd,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = TempoShadows.elevationDeep,
                shape = shape,
                ambientColor = TempoShadows.shadowDeepColor,
                spotColor = TempoShadows.shadowDeepColor
            )
            .clip(shape)
            .background(tempoElevatedGradient)
            .tempoMaterialHighlight(shape)
    ) {
        Column(
            modifier = Modifier.padding(TempoSpacing.space4),
            content = content
        )
    }
}

/**
 * 3. StatBlock: Bloco de estatística integrado (sem card pesado).
 * Mostra rótulo sutil em caixa alta e valor em numerais tabulares monoespacados.
 */
@Composable
fun TempoStatBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subValue: String? = null,
    isAccent: Boolean = false,
    isSuccess: Boolean = false
) {
    val valueColor = when {
        isAccent -> TempoAccent
        isSuccess -> TempoSuccess
        else -> TempoTextPrimary
    }

    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TempoTextMuted,
            letterSpacing = 0.8.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(TempoSpacing.space1))
        Text(
            text = value,
            style = MaterialTheme.typography.displayMedium,
            color = valueColor,
            fontWeight = FontWeight.Bold
        )
        if (subValue != null) {
            Spacer(modifier = Modifier.height(TempoSpacing.space1))
            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall,
                color = TempoTextSecondary
            )
        }
    }
}

/**
 * 4. SectionHeader: Cabeçalho de seção compacto sem H1 gigante.
 */
@Composable
fun TempoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TempoSpacing.space2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TempoTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(TempoSpacing.space1))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TempoTextSecondary
                )
            }
        }
        if (action != null) {
            action()
        }
    }
}

/**
 * 5. PrimaryAction: Ação principal com o accent laranja queimado (#E46F43),
 * sem neon e com altura ergonômica mínima de 48dp.
 */
@Composable
fun TempoPrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp),
        enabled = enabled,
        shape = TempoRadius.shapeSm,
        colors = ButtonDefaults.buttonColors(
            containerColor = TempoAccent,
            contentColor = Color.White,
            disabledContainerColor = TempoSurface2,
            disabledContentColor = TempoTextMuted
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        contentPadding = PaddingValues(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space3)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(TempoSpacing.space2))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * 6. SecondaryAction: Ação secundária em superfície grafite (#181D22), sem borda de 1px.
 */
@Composable
fun TempoSecondaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = TempoRadius.shapeSm,
        colors = ButtonDefaults.buttonColors(
            containerColor = TempoSurface2,
            contentColor = TempoTextPrimary,
            disabledContainerColor = TempoSurface1,
            disabledContentColor = TempoTextMuted
        ),
        contentPadding = PaddingValues(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space3)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TempoTextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(TempoSpacing.space2))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 7. SegmentedFilter: Filtro discreto sem pílula verde saltando.
 * O item ativo recebe fundo de superfície contrastante e tipografia clara.
 */
@Composable
fun <T> TempoSegmentedFilter(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(TempoRadius.shapeSm)
            .background(TempoSurface1)
            .padding(TempoSpacing.space1),
        horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space1)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val bg = if (isSelected) TempoSurface3 else Color.Transparent
            val textColor = if (isSelected) TempoTextPrimary else TempoTextMuted

            Box(
                modifier = Modifier
                    .clip(TempoRadius.shapeSm)
                    .background(bg)
                    .clickable { onOptionSelected(option) }
                    .padding(horizontal = TempoSpacing.space3, vertical = TempoSpacing.space2),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelProvider(option),
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/**
 * 8. IconAction: Ação de ícone com área de toque acessível (mínimo 44dp)
 * e sem círculo chamativo por trás de todo ícone.
 */
@Composable
fun TempoIconAction(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    size: Dp = 44.dp
) {
    val tint = if (isActive) TempoAccent else TempoTextMuted
    val bg = if (isActive) TempoAccentGlow else Color.Transparent

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

