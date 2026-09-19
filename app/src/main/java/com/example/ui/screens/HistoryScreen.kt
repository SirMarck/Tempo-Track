package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
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
import com.example.viewmodel.TimeTrackerViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class HistoryFilterPeriod {
    DAY, WEEK, MONTH
}

/**
 * Tela 4 - Histórico (Timeline editável, detecção de overlaps e lacunas)
 * Especificação do Guia de Redesign v2 - Seção 9 e Prompt 5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: TimeTrackerViewModel,
    onOpenManualEntry: () -> Unit = {}
) {
    val clients by viewModel.clients.collectAsState()
    val activeClients by viewModel.activeClients.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val sessions by viewModel.sessions.collectAsState()

    var selectedPeriod by remember { mutableStateOf(HistoryFilterPeriod.DAY) }
    var selectedSessionForEdit by remember { mutableStateOf<Session?>(null) }
    var showManualDialog by remember { mutableStateOf(false) }

    // Sessões filtradas conforme o período
    val filteredSessions = remember(sessions, selectedPeriod) {
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()
        val completed = sessions.filter { it.endTime != null }

        when (selectedPeriod) {
            HistoryFilterPeriod.DAY -> {
                val todayYear = cal.get(Calendar.YEAR)
                val todayDay = cal.get(Calendar.DAY_OF_YEAR)
                completed.filter { s ->
                    val c = Calendar.getInstance().apply { timeInMillis = s.startTime }
                    c.get(Calendar.YEAR) == todayYear && c.get(Calendar.DAY_OF_YEAR) == todayDay
                }
            }
            HistoryFilterPeriod.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val startOfWeek = cal.timeInMillis
                completed.filter { it.startTime >= startOfWeek }
            }
            HistoryFilterPeriod.MONTH -> {
                val currentMonth = cal.get(Calendar.MONTH)
                val currentYear = cal.get(Calendar.YEAR)
                completed.filter { s ->
                    val c = Calendar.getInstance().apply { timeInMillis = s.startTime }
                    c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
                }
            }
        }.sortedByDescending { it.startTime }
    }

    // Totais do período
    val totalHours = remember(filteredSessions) {
        val totalMillis = filteredSessions.sumOf { it.calculateDurationMillis() }
        totalMillis.toDouble() / (1000 * 60 * 60)
    }

    val totalBillableAmount = remember(filteredSessions, clients) {
        filteredSessions.filter { it.billable }.sumOf { s ->
            val fallback = clients.find { it.id == s.clientId }?.hourlyRate ?: 0.0
            s.calculateEarnings(fallback)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space3)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Histórico",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TempoTextPrimary
                    )

                    IconButton(
                        onClick = {
                            showManualDialog = true
                            onOpenManualEntry()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(TempoSurface2)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Adicionar Lançamento Manual",
                            tint = TempoAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(TempoSpacing.space2))

                // Filtro segmentado discreto
                TempoSegmentedFilter(
                    options = listOf(HistoryFilterPeriod.DAY, HistoryFilterPeriod.WEEK, HistoryFilterPeriod.MONTH),
                    selectedOption = selectedPeriod,
                    onOptionSelected = { selectedPeriod = it },
                    labelProvider = { period ->
                        when (period) {
                            HistoryFilterPeriod.DAY -> "Dia"
                            HistoryFilterPeriod.WEEK -> "Semana"
                            HistoryFilterPeriod.MONTH -> "Mês"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = TempoSpacing.space4),
            verticalArrangement = Arrangement.spacedBy(TempoSpacing.space3)
        ) {
            // Resumo do período selecionado
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TempoRadius.shapeSm)
                        .background(TempoSurface1)
                        .tempoMaterialHighlight(TempoRadius.shapeSm)
                        .padding(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space3),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL HORAS", style = MaterialTheme.typography.labelSmall, color = TempoTextMuted)
                        Text(
                            text = FormatUtils.formatDuration((totalHours * 3600 * 1000).toLong()),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = TempoMono),
                            color = TempoTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(TempoOutline))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("VALOR TOTAL", style = MaterialTheme.typography.labelSmall, color = TempoTextMuted)
                        Text(
                            text = FormatUtils.formatCurrency(totalBillableAmount),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = TempoMono),
                            color = TempoAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (filteredSessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = TempoSpacing.space6),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum apontamento neste período.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TempoTextMuted
                        )
                    }
                }
            } else {
                items(filteredSessions) { session ->
                    val client = clients.find { it.id == session.clientId }
                    val project = projects.find { it.id == session.projectId }
                    val fallback = client?.hourlyRate ?: 0.0

                    // Detecção de sobreposição (overlap com outros registros)
                    val hasOverlap = remember(session, filteredSessions) {
                        filteredSessions.any { other ->
                            other.id != session.id &&
                                    other.endTime != null &&
                                    session.endTime != null &&
                                    session.startTime < other.endTime!! &&
                                    session.endTime!! > other.startTime
                        }
                    }

                    HistoryTimelineRow(
                        session = session,
                        clientName = client?.name ?: "Cliente",
                        projectName = project?.name,
                        effectiveRate = if (session.appliedRate > 0.0) session.appliedRate else fallback,
                        hasOverlap = hasOverlap,
                        onClick = { selectedSessionForEdit = session }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(TempoSpacing.space6))
            }
        }
    }

    if (showManualDialog) {
        ManualEntryDialog(
            clients = activeClients.ifEmpty { clients },
            projects = projects,
            activities = activities,
            onDismiss = { showManualDialog = false },
            onSave = { clientId, projectId, activityId, start, end, desc, billable, discVal, discPct, tag ->
                viewModel.addManualSession(
                    clientId = clientId,
                    projectId = projectId,
                    activityId = activityId,
                    startTime = start,
                    endTime = end,
                    description = desc,
                    billable = billable,
                    discountValue = discVal,
                    discountPercentage = discPct,
                    tag = tag
                )
                showManualDialog = false
            }
        )
    }
}

@Composable
fun HistoryTimelineRow(
    session: Session,
    clientName: String,
    projectName: String?,
    effectiveRate: Double,
    hasOverlap: Boolean,
    onClick: () -> Unit
) {
    val dateStr = FormatUtils.formatDate(session.startTime).substring(0, 5) // "dd/MM"
    val startStr = FormatUtils.formatTime(session.startTime).substring(0, 5)
    val endStr = session.endTime?.let { FormatUtils.formatTime(it).substring(0, 5) } ?: "..."
    val duration = FormatUtils.formatDuration(session.calculateDurationMillis())
    val earnings = session.calculateEarnings(effectiveRate)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TempoRadius.shapeSm)
            .background(TempoSurface1)
            .tempoMaterialHighlight(TempoRadius.shapeSm)
            .clickable(onClick = onClick)
            .padding(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Horários ancorados à esquerda
        Column(modifier = Modifier.width(62.dp)) {
            Text(
                text = "$startStr - $endStr",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = TempoMono),
                color = TempoTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = TempoMono),
                color = TempoTextMuted
            )
        }

        Box(
            modifier = Modifier
                .width(2.dp)
                .height(36.dp)
                .background(if (hasOverlap) TempoDanger else TempoOutline)
        )

        Spacer(modifier = Modifier.width(TempoSpacing.space3))

        // Cliente, projeto e alerta de overlap
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = clientName,
                    style = MaterialTheme.typography.labelLarge,
                    color = TempoTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (hasOverlap) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Sobreposição detectada",
                        tint = TempoWarning,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            val subtitle = listOfNotNull(projectName, session.description.takeIf { it.isNotEmpty() }).joinToString(" • ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TempoTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Duração e valor
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = duration,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = TempoMono),
                color = TempoTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = FormatUtils.formatCurrency(earnings),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = TempoMono),
                color = if (session.billable) TempoAccent else TempoTextMuted
            )
        }
    }
}
