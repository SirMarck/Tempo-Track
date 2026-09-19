package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Client
import com.example.data.Project
import com.example.data.Session
import com.example.ui.theme.*
import com.example.utils.FormatUtils
import kotlinx.coroutines.delay

/**
 * Barra compacta persistente do Mini Timer visível acima da barra de navegação.
 * Guia de Redesign v2 - Prompt 3
 */
@Composable
fun MiniTimerBar(
    session: Session?,
    clients: List<Client>,
    projects: List<Project>,
    onOpenFocus: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = session != null,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
    ) {
        if (session != null) {
            var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

            LaunchedEffect(session.isPaused, session.id) {
                if (!session.isPaused) {
                    while (true) {
                        delay(1000)
                        currentTime = System.currentTimeMillis()
                    }
                }
            }

            val client = remember(session.clientId, clients) {
                clients.find { it.id == session.clientId }
            }
            val project = remember(session.projectId, projects) {
                projects.find { it.id == session.projectId }
            }

            val durationMillis = session.calculateDurationMillis(currentTime)
            val durationText = FormatUtils.formatDuration(durationMillis)

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space2)
                    .clip(TempoRadius.shapeSm)
                    .background(TempoSurface2)
                    .tempoMaterialHighlight(TempoRadius.shapeSm)
                    .clickable(onClick = onOpenFocus)
                    .padding(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space2)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Status dot + Informações do trabalho
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space3)
                    ) {
                        // Dot pulsante ou estático
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (session.isPaused) TempoWarning else TempoAccent)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = client?.name ?: "Cliente",
                                style = MaterialTheme.typography.labelLarge,
                                color = TempoTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subtitle = if (project != null) {
                                "${project.name} • ${if (session.isPaused) "Pausado" else "Em andamento"}"
                            } else {
                                if (session.isPaused) "Pausado" else "Em andamento"
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TempoTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Tempo decorrido (tabular monoespacado) + Ação rápida
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2)
                    ) {
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = TempoMono),
                            color = if (session.isPaused) TempoTextMuted else TempoTextPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        IconButton(
                            onClick = {
                                if (session.isPaused) onResume() else onPause()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(TempoSurface3)
                        ) {
                            Icon(
                                imageVector = if (session.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (session.isPaused) "Retomar" else "Pausar",
                                tint = if (session.isPaused) TempoSuccess else TempoAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
