package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Project
import com.example.data.Session
import com.example.ui.theme.*
import com.example.utils.FormatUtils
import com.example.viewmodel.TimeTrackerViewModel

/**
 * Tela de Detalhe do Projeto (Fase 6)
 * Gestão de escopo, orçamento, progresso e sessões vinculadas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: Long,
    viewModel: TimeTrackerViewModel,
    onNavigateBack: () -> Unit,
    onStartWork: (clientId: Long, projectId: Long) -> Unit = { _, _ -> }
) {
    val projects by viewModel.projects.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val sessions by viewModel.sessions.collectAsState()

    val project = remember(projects, projectId) {
        projects.find { it.id == projectId }
    }
    val client = remember(project, clients) {
        clients.find { it.id == project?.clientId }
    }

    if (project == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(TempoBgBase),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Projeto não encontrado", color = TempoTextMuted)
                Spacer(modifier = Modifier.height(16.dp))
                TempoSecondaryAction(text = "Voltar", onClick = onNavigateBack)
            }
        }
        return
    }

    val projectSessions = remember(sessions, projectId) {
        sessions.filter { it.projectId == projectId && it.endTime != null }
            .sortedByDescending { it.startTime }
    }

    val consumedMillis = remember(projectSessions) {
        projectSessions.sumOf { it.calculateDurationMillis() }
    }
    val consumedHours = consumedMillis.toDouble() / (1000 * 60 * 60)

    val effectiveRate = remember(project, client) {
        FormatUtils.resolveEffectiveRate(project, client, 0.0)
    }

    val totalEarnings = remember(projectSessions, effectiveRate) {
        projectSessions.sumOf { it.calculateEarnings(effectiveRate) }
    }

    // Cálculo do orçamento
    val budgetHours = project.budgetMinutes?.let { it.toDouble() / 60.0 }
    val progressPct = when {
        budgetHours != null && budgetHours > 0 -> (consumedHours / budgetHours).toFloat()
        project.budgetAmount != null && project.budgetAmount > 0 -> (totalEarnings / project.budgetAmount).toFloat()
        else -> 0f
    }

    val progressColor = when {
        progressPct >= 1.0f -> TempoDanger
        progressPct >= 0.75f -> TempoWarning
        else -> TempoAccent
    }

    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = TempoBgBase,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TempoTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = client?.name ?: "Cliente",
                            style = MaterialTheme.typography.bodySmall,
                            color = TempoTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = TempoTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Projeto",
                            tint = TempoTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TempoBgBase)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = TempoSpacing.space4),
            verticalArrangement = Arrangement.spacedBy(TempoSpacing.space4)
        ) {
            // ─── 1. Card de Progresso e Orçamento ──────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TempoRadius.shapeMd)
                        .background(TempoSurface2)
                        .tempoMaterialHighlight(TempoRadius.shapeMd)
                        .padding(TempoSpacing.space4)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(TempoSpacing.space3)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CONSUMO DE ORÇAMENTO",
                                style = MaterialTheme.typography.labelSmall,
                                color = TempoTextMuted,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Status badge
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (project.status == "active") TempoSuccess.copy(alpha = 0.2f) else TempoSurface3)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (project.status == "active") "Ativo" else "Arquivado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (project.status == "active") TempoSuccess else TempoTextMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Números consumidos / total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1fh", consumedHours),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = TempoMono),
                                    color = TempoTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Horas Registradas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TempoTextSecondary
                                )
                            }

                            if (budgetHours != null && budgetHours > 0) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.1fh teto", budgetHours),
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = TempoMono),
                                        color = TempoTextSecondary
                                    )
                                    Text(
                                        text = "${(progressPct * 100).toInt()}% utilizado",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = progressColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Barra de progresso fina de 5dp
                        if (budgetHours != null && budgetHours > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(TempoSurface3)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressPct.coerceIn(0.01f, 1.0f))
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(progressColor)
                                )
                            }
                        }

                        // Informações financeiras
                        Divider(color = TempoOutline, thickness = 0.5.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Valor Total Acumulado:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TempoTextSecondary
                            )
                            Text(
                                text = FormatUtils.formatCurrency(totalEarnings),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = TempoMono),
                                color = TempoAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Taxa Horária Aplicada:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TempoTextSecondary
                            )
                            Text(
                                text = FormatUtils.formatCurrency(effectiveRate) + "/h",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = TempoMono),
                                color = TempoTextPrimary
                            )
                        }
                    }
                }
            }

            // ─── 2. Ação Primária de Iniciar ──────────────────────────────────
            item {
                TempoPrimaryAction(
                    text = "Iniciar Trabalho neste Projeto",
                    onClick = { onStartWork(project.clientId, project.id) },
                    icon = Icons.Default.PlayArrow,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ─── 3. Lista de Sessões do Projeto ───────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sessões Realizadas (${projectSessions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TempoTextPrimary
                    )
                }
            }

            if (projectSessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = TempoSpacing.space4),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum trabalho registrado para este projeto.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TempoTextMuted
                        )
                    }
                }
            } else {
                items(projectSessions) { session ->
                    val durStr = FormatUtils.formatDuration(session.calculateDurationMillis())
                    val dateStr = FormatUtils.formatDate(session.startTime)
                    val earn = session.calculateEarnings(effectiveRate)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(TempoRadius.shapeSm)
                            .background(TempoSurface1)
                            .tempoMaterialHighlight(TempoRadius.shapeSm)
                            .padding(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space3),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (session.description.isNotBlank()) session.description else "Trabalho sem descrição",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TempoTextPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = dateStr + if (session.tag.isNotEmpty()) " • ${session.tag}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = TempoTextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = durStr,
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = TempoMono),
                                color = TempoTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = FormatUtils.formatCurrency(earn),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = TempoMono),
                                color = if (session.billable) TempoAccent else TempoTextMuted
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(TempoSpacing.space6))
            }
        }
    }
}
