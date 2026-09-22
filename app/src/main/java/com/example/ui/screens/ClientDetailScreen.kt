package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Client
import com.example.data.Project
import com.example.data.Session
import com.example.ui.theme.*
import com.example.utils.ExportUtils
import com.example.utils.FormatUtils
import com.example.viewmodel.TimeTrackerViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class ClientDetailTab {
    OVERVIEW, PROJECTS, REPORT
}

enum class ClientReportPeriod {
    WEEK, MONTH, CUSTOM
}

/**
 * Tela 6 - Detalhe do Cliente & Relatório Exclusivo
 * Especificação do Guia Oficial - Seção 11 e Pôster Oficial
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: Long,
    viewModel: TimeTrackerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProjectDetail: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val sessions by viewModel.sessions.collectAsState()

    val client = remember(clients, clientId) {
        clients.find { it.id == clientId }
    }

    if (client == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(TempoBgBase),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Cliente não encontrado", color = TempoTextMuted)
                Spacer(modifier = Modifier.height(16.dp))
                TempoSecondaryAction(text = "Voltar", onClick = onNavigateBack)
            }
        }
        return
    }

    val clientProjects = remember(projects, clientId) {
        projects.filter { it.clientId == clientId && it.archivedAt == null }
    }
    val clientProjectIds = remember(clientProjects) { clientProjects.map { it.id }.toSet() }

    val clientSessions = remember(sessions, clientId, clientProjectIds) {
        sessions.filter { session ->
            session.endTime != null && (
                session.clientId == clientId || (session.projectId != null && session.projectId in clientProjectIds)
            ) && (
                session.projectId == null || session.projectId in clientProjectIds
            )
        }.sortedByDescending { it.startTime }
    }

    var activeTab by remember { mutableStateOf(ClientDetailTab.OVERVIEW) }
    var reportPeriod by remember { mutableStateOf(ClientReportPeriod.MONTH) }

    val defaultStartMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val defaultEndMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    var customStartMillis by remember { mutableStateOf(defaultStartMillis) }
    var customEndMillis by remember { mutableStateOf(defaultEndMillis) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    val customStartText = remember(customStartMillis) { dateFormatter.format(Date(customStartMillis)) }
    val customEndText = remember(customEndMillis) { dateFormatter.format(Date(customEndMillis)) }

    fun showStartDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = customStartMillis }
        android.app.DatePickerDialog(
            context,
            { _, y, m, d ->
                val newStart = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                customStartMillis = newStart
                if (newStart > customEndMillis) {
                    val newEnd = Calendar.getInstance().apply {
                        timeInMillis = newStart
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    customEndMillis = newEnd
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showEndDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = customEndMillis }
        android.app.DatePickerDialog(
            context,
            { _, y, m, d ->
                val newEnd = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                customEndMillis = newEnd
                if (newEnd < customStartMillis) {
                    val newStart = Calendar.getInstance().apply {
                        timeInMillis = newEnd
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    customStartMillis = newStart
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun applyShortcut(days: Int? = null, currentMonthOnly: Boolean = false) {
        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val startCal = Calendar.getInstance().apply {
            if (currentMonthOnly) {
                set(Calendar.DAY_OF_MONTH, 1)
            } else if (days != null) {
                add(Calendar.DAY_OF_YEAR, -(days - 1))
            }
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        customStartMillis = startCal.timeInMillis
        customEndMillis = endCal.timeInMillis
    }

    var showEditRateDialog by remember { mutableStateOf(false) }
    var showEditClientDialog by remember { mutableStateOf(false) }
    var showArchiveConfirmDialog by remember { mutableStateOf(false) }
    var rateInput by remember { mutableStateOf(client.hourlyRate.toString()) }
    var editClientNameInput by remember { mutableStateOf(client.name) }
    var editClientRateInput by remember { mutableStateOf(client.hourlyRate.toString()) }

    // Sessões filtradas para o relatório do cliente por período selecionado
    val reportFilteredSessions = remember(clientSessions, reportPeriod, customStartMillis, customEndMillis) {
        when (reportPeriod) {
            ClientReportPeriod.WEEK -> {
                val startOfWeek = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                clientSessions.filter { it.startTime >= startOfWeek }
            }
            ClientReportPeriod.MONTH -> {
                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                clientSessions.filter { it.startTime >= startOfMonth }
            }
            ClientReportPeriod.CUSTOM -> {
                clientSessions.filter { it.startTime in customStartMillis..customEndMillis }
            }
        }
    }

    val reportHours = remember(reportFilteredSessions) {
        reportFilteredSessions.sumOf { it.calculateDurationMillis() }.toDouble() / (1000 * 60 * 60)
    }

    val reportEarnings = remember(reportFilteredSessions, client) {
        reportFilteredSessions.sumOf { s ->
            val rate = if (s.appliedRate > 0.0) s.appliedRate else client.hourlyRate
            s.calculateEarnings(rate)
        }
    }

    val reportPeriodLabel = remember(reportPeriod, customStartText, customEndText) {
        when (reportPeriod) {
            ClientReportPeriod.WEEK -> "Esta Semana"
            ClientReportPeriod.MONTH -> SimpleDateFormat("MMMM 'de' yyyy", Locale("pt", "BR")).format(Date()).replaceFirstChar { it.uppercase() }
            ClientReportPeriod.CUSTOM -> "Período: $customStartText a $customEndText"
        }
    }

    val reportUnbilledSessions = remember(reportFilteredSessions) {
        reportFilteredSessions.filter { it.financialStatus == "unbilled" }
    }

    Scaffold(
        containerColor = TempoBgBase,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TempoTextPrimary
                    )
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
                    IconButton(onClick = {
                        editClientNameInput = client.name
                        editClientRateInput = client.hourlyRate.toString()
                        showEditClientDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Cliente",
                            tint = TempoAccent
                        )
                    }
                    IconButton(onClick = { showArchiveConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Arquivar Cliente",
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
            // ─── Header do Cliente com Avatar e Categoria ─────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = TempoSpacing.space2),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val initial = client.name.firstOrNull()?.uppercaseChar()?.toString() ?: "C"
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(TempoSpacing.space2))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                editClientNameInput = client.name
                                editClientRateInput = client.hourlyRate.toString()
                                showEditClientDialog = true
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = client.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = TempoTextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Nome",
                            tint = TempoAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "Cliente Corporativo • ${FormatUtils.formatCurrency(client.hourlyRate)}/h",
                        style = MaterialTheme.typography.bodySmall,
                        color = TempoTextSecondary
                    )
                }
            }

            // ─── Abas Superiores (Visão Geral / Projetos / Relatório) ───────────
            item {
                TempoSegmentedFilter(
                    options = listOf(ClientDetailTab.OVERVIEW, ClientDetailTab.PROJECTS, ClientDetailTab.REPORT),
                    selectedOption = activeTab,
                    onOptionSelected = { activeTab = it },
                    labelProvider = { tab ->
                        when (tab) {
                            ClientDetailTab.OVERVIEW -> "Visão Geral"
                            ClientDetailTab.PROJECTS -> "Projetos (${clientProjects.size})"
                            ClientDetailTab.REPORT -> "Relatório & PDF"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ══════════════════════════════════════════════════════════════════
            // ABA 1: VISÃO GERAL
            // ══════════════════════════════════════════════════════════════════
            if (activeTab == ClientDetailTab.OVERVIEW) {
                // Card 2x2 de métricas oficiais do Pôster
                item {
                    val totalAllHours = clientSessions.sumOf { it.calculateDurationMillis() }.toDouble() / (1000 * 60 * 60)
                    val totalAllEarnings = clientSessions.sumOf { s ->
                        val rate = if (s.appliedRate > 0.0) s.appliedRate else client.hourlyRate
                        s.calculateEarnings(rate)
                    }
                    val pendingEarnings = clientSessions.filter { it.financialStatus == "unbilled" }.sumOf { s ->
                        val rate = if (s.appliedRate > 0.0) s.appliedRate else client.hourlyRate
                        s.calculateEarnings(rate)
                    }
                    val invoicedEarnings = clientSessions.filter { it.financialStatus != "unbilled" }.sumOf { s ->
                        val rate = if (s.appliedRate > 0.0) s.appliedRate else client.hourlyRate
                        s.calculateEarnings(rate)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(TempoSpacing.space2)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(TempoRadius.shapeSm)
                                    .background(TempoSurface1)
                                    .tempoMaterialHighlight(TempoRadius.shapeSm)
                                    .padding(TempoSpacing.space3)
                            ) {
                                Column {
                                    Text("Tempo total", style = MaterialTheme.typography.labelSmall, color = TempoTextMuted)
                                    Text(
                                        text = String.format(Locale.US, "%.1fh", totalAllHours),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = TempoMono),
                                        fontWeight = FontWeight.Bold,
                                        color = TempoTextPrimary
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(TempoRadius.shapeSm)
                                    .background(TempoSurface1)
                                    .tempoMaterialHighlight(TempoRadius.shapeSm)
                                    .padding(TempoSpacing.space3)
                            ) {
                                Column {
                                    Text("Total Geral", style = MaterialTheme.typography.labelSmall, color = TempoTextMuted)
                                    Text(
                                        text = FormatUtils.formatCurrency(totalAllEarnings),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = TempoMono),
                                        fontWeight = FontWeight.Bold,
                                        color = TempoAccent
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(TempoRadius.shapeSm)
                                    .background(TempoSurface1)
                                    .tempoMaterialHighlight(TempoRadius.shapeSm)
                                    .padding(TempoSpacing.space3)
                            ) {
                                Column {
                                    Text("A Faturar", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF59E0B))
                                    Text(
                                        text = FormatUtils.formatCurrency(pendingEarnings),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = TempoMono),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(TempoRadius.shapeSm)
                                    .background(TempoSurface1)
                                    .tempoMaterialHighlight(TempoRadius.shapeSm)
                                    .padding(TempoSpacing.space3)
                            ) {
                                Column {
                                    Text("Faturado / Pago", style = MaterialTheme.typography.labelSmall, color = TempoSuccess)
                                    Text(
                                        text = FormatUtils.formatCurrency(invoicedEarnings),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = TempoMono),
                                        fontWeight = FontWeight.Bold,
                                        color = TempoSuccess
                                    )
                                }
                            }
                        }

                        // Taxa padrão com botão de alteração rápida
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(TempoRadius.shapeSm)
                                .background(TempoSurface1)
                                .tempoMaterialHighlight(TempoRadius.shapeSm)
                                .padding(horizontal = TempoSpacing.space3, vertical = TempoSpacing.space2),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Taxa Padrão", style = MaterialTheme.typography.labelSmall, color = TempoTextMuted)
                                Text(
                                    text = "${FormatUtils.formatCurrency(client.hourlyRate)}/h",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TempoMono),
                                    fontWeight = FontWeight.Bold,
                                    color = TempoTextPrimary
                                )
                            }

                            TextButton(
                                onClick = {
                                    rateInput = client.hourlyRate.toString()
                                    showEditRateDialog = true
                                }
                            ) {
                                Text("Reajustar Taxa", color = TempoAccent, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Atividade recente
                item {
                    Text(
                        text = "Atividade Recente",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TempoTextPrimary
                    )
                }

                if (clientSessions.isEmpty()) {
                    item {
                        Text("Nenhum registro finalizado para este cliente.", color = TempoTextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    items(clientSessions.take(5)) { session ->
                        val durStr = FormatUtils.formatDuration(session.calculateDurationMillis())
                        val dateStr = FormatUtils.formatDate(session.startTime)
                        val earn = session.calculateEarnings(if (session.appliedRate > 0.0) session.appliedRate else client.hourlyRate)

                        val p = projects.find { it.id == session.projectId }
                        val a = activities.find { it.id == session.activityId }
                        val title = session.description.ifBlank { p?.name ?: a?.name ?: "Trabalho realizado" }
                        val subtitle = listOfNotNull(
                            dateStr,
                            p?.name?.takeIf { it != title },
                            a?.name?.takeIf { it != title },
                            session.tag.takeIf { it.isNotBlank() }
                        ).joinToString(" • ")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(TempoRadius.shapeSm)
                                .background(TempoSurface1)
                                .padding(horizontal = TempoSpacing.space3, vertical = TempoSpacing.space2),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TempoTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TempoTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(durStr, style = MaterialTheme.typography.bodySmall.copy(fontFamily = TempoMono), color = TempoTextPrimary)
                                Text(FormatUtils.formatCurrency(earn), style = MaterialTheme.typography.labelSmall.copy(fontFamily = TempoMono), color = TempoAccent)
                            }
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════════
            // ABA 2: PROJETOS
            // ══════════════════════════════════════════════════════════════════
            if (activeTab == ClientDetailTab.PROJECTS) {
                if (clientProjects.isEmpty()) {
                    item {
                        Text("Nenhum projeto vinculado a este cliente.", color = TempoTextMuted)
                    }
                } else {
                    items(clientProjects) { project ->
                        val pSessions = clientSessions.filter { it.projectId == project.id }
                        val pHours = pSessions.sumOf { it.calculateDurationMillis() }.toDouble() / (1000 * 60 * 60)
                        val budgetHours = project.budgetMinutes?.let { it.toDouble() / 60.0 } ?: 0.0
                        val progress = if (budgetHours > 0) (pHours / budgetHours).toFloat() else 0f

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(TempoRadius.shapeSm)
                                .background(TempoSurface1)
                                .clickable { onNavigateToProjectDetail(project.id) }
                                .padding(TempoSpacing.space3)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(project.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TempoTextPrimary)
                                    Text(String.format(Locale.US, "%.1fh", pHours), style = MaterialTheme.typography.labelMedium.copy(fontFamily = TempoMono), color = TempoAccent)
                                }

                                if (budgetHours > 0) {
                                    LinearProgressIndicator(
                                        progress = { progress.coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                        color = TempoAccent,
                                        trackColor = TempoSurface3
                                    )
                                    Text("${(progress * 100).toInt()}% de ${budgetHours.toInt()}h orçadas", style = MaterialTheme.typography.labelSmall, color = TempoTextMuted)
                                }
                            }
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════════
            // ABA 3: RELATÓRIO DO CLIENTE & EXPORTAÇÃO PDF
            // ══════════════════════════════════════════════════════════════════
            if (activeTab == ClientDetailTab.REPORT) {
                // Filtro de período do relatório: Semana / Mês / Por Período
                item {
                    TempoSegmentedFilter(
                        options = listOf(ClientReportPeriod.WEEK, ClientReportPeriod.MONTH, ClientReportPeriod.CUSTOM),
                        selectedOption = reportPeriod,
                        onOptionSelected = { reportPeriod = it },
                        labelProvider = { period ->
                            when (period) {
                                ClientReportPeriod.WEEK -> "Semana"
                                ClientReportPeriod.MONTH -> "Mês"
                                ClientReportPeriod.CUSTOM -> "Por Período"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Seletor de Intervalo Personalizado de Datas + Atalhos Rápidos
                if (reportPeriod == ClientReportPeriod.CUSTOM) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(TempoRadius.shapeMd)
                                .background(TempoSurface1)
                                .border(1.dp, TempoOutline, TempoRadius.shapeMd)
                                .padding(TempoSpacing.space3),
                            verticalArrangement = Arrangement.spacedBy(TempoSpacing.space3)
                        ) {
                            Text(
                                text = "Definir Período Personalizado",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TempoTextSecondary
                            )

                            // Cards de Data "De" e "Até"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2)
                            ) {
                                // Card "De"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(TempoRadius.shapeSm)
                                        .background(TempoSurface2)
                                        .border(1.dp, TempoOutline, TempoRadius.shapeSm)
                                        .clickable { showStartDatePicker() }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = TempoAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("De", style = MaterialTheme.typography.labelSmall, color = TempoTextMuted)
                                            Text(
                                                text = customStartText,
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = TempoMono),
                                                fontWeight = FontWeight.Bold,
                                                color = TempoTextPrimary
                                            )
                                        }
                                    }
                                }

                                // Card "Até"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(TempoRadius.shapeSm)
                                        .background(TempoSurface2)
                                        .border(1.dp, TempoOutline, TempoRadius.shapeSm)
                                        .clickable { showEndDatePicker() }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = TempoAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Até", style = MaterialTheme.typography.labelSmall, color = TempoTextMuted)
                                            Text(
                                                text = customEndText,
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = TempoMono),
                                                fontWeight = FontWeight.Bold,
                                                color = TempoTextPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // Atalhos Rápidos
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val shortcuts = listOf(
                                    "7 dias" to { applyShortcut(days = 7) },
                                    "15 dias" to { applyShortcut(days = 15) },
                                    "30 dias" to { applyShortcut(days = 30) },
                                    "Mês Atual" to { applyShortcut(currentMonthOnly = true) }
                                )
                                shortcuts.forEach { (label, action) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(TempoSurface2)
                                            .border(0.8.dp, TempoOutline, RoundedCornerShape(6.dp))
                                            .clickable { action() }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TempoTextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Rótulo do período
                item {
                    Text(
                        text = reportPeriodLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = TempoTextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Card Resumo do Relatório do Cliente
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(TempoRadius.shapeMd)
                            .background(tempoSurfaceGradient)
                            .tempoMaterialHighlight(TempoRadius.shapeMd)
                            .padding(TempoSpacing.space4)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(TempoSpacing.space3)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("TEMPO FATURÁVEL", style = MaterialTheme.typography.labelSmall, color = TempoTextMuted)
                                    Text(
                                        text = String.format(Locale.US, "%.1fh", reportHours),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = TempoMono),
                                        fontWeight = FontWeight.Bold,
                                        color = TempoTextPrimary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("TOTAL A COBRAR", style = MaterialTheme.typography.labelSmall, color = TempoTextMuted)
                                    Text(
                                        text = FormatUtils.formatCurrency(reportEarnings),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = TempoMono),
                                        fontWeight = FontWeight.Bold,
                                        color = TempoAccent
                                    )
                                }
                            }

                            Divider(color = TempoOutline, thickness = 0.5.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Registros: ${reportFilteredSessions.size}", style = MaterialTheme.typography.bodySmall, color = TempoTextSecondary)
                                Text("Taxa Base: ${FormatUtils.formatCurrency(client.hourlyRate)}/h", style = MaterialTheme.typography.bodySmall, color = TempoTextSecondary)
                            }
                        }
                    }
                }

                // Botões de Ação de Exportação (PDF A4 e WhatsApp Direto)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2)
                    ) {
                        Button(
                            onClick = {
                                if (reportFilteredSessions.isEmpty()) {
                                    Toast.makeText(context, "Nenhuma sessão no período para gerar relatório.", Toast.LENGTH_SHORT).show()
                                } else {
                                    val pdfFile = ExportUtils.generatePdf(
                                        context = context,
                                        client = client,
                                        sessions = reportFilteredSessions,
                                        monthName = reportPeriodLabel
                                    )
                                    if (pdfFile != null) {
                                        ExportUtils.shareFile(context, pdfFile, "application/pdf")
                                    } else {
                                        Toast.makeText(context, "Erro ao gerar PDF do cliente.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TempoAccent,
                                contentColor = Color.White
                            ),
                            shape = TempoRadius.shapeSm,
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF A4", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (reportFilteredSessions.isEmpty()) {
                                    Toast.makeText(context, "Nenhuma sessão no período para enviar.", Toast.LENGTH_SHORT).show()
                                } else {
                                    val pdfFile = ExportUtils.generatePdf(
                                        context = context,
                                        client = client,
                                        sessions = reportFilteredSessions,
                                        monthName = reportPeriodLabel
                                    )
                                    if (pdfFile != null) {
                                        val msg = "Olá! Segue o fechamento de ${client.name} ($reportPeriodLabel):\n⏱ Total trabalhado: ${String.format(Locale.US, "%.1fh", reportHours)}\n💰 Total faturável: ${FormatUtils.formatCurrency(reportEarnings)}"
                                        ExportUtils.shareViaWhatsApp(context, pdfFile, msg)
                                    } else {
                                        Toast.makeText(context, "Erro ao gerar PDF do cliente.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366),
                                contentColor = Color.White
                            ),
                            shape = TempoRadius.shapeSm,
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Ação Rápida de Fechamento Financeiro
                if (reportUnbilledSessions.isNotEmpty()) {
                    item {
                        OutlinedButton(
                            onClick = {
                                reportUnbilledSessions.forEach { session ->
                                    viewModel.updateSession(session.copy(financialStatus = "invoiced"))
                                }
                                Toast.makeText(context, "${reportUnbilledSessions.size} sessões marcadas como faturadas!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TempoSuccess
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TempoSuccess),
                            shape = TempoRadius.shapeSm,
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = TempoSuccess)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Marcar ${reportUnbilledSessions.size} sessões como Faturadas", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Sessões do Relatório
                item {
                    Text(
                        text = "Detalhamento das Sessões (${reportFilteredSessions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TempoTextPrimary
                    )
                }

                if (reportFilteredSessions.isEmpty()) {
                    item {
                        Text("Nenhuma sessão registrada neste período para este cliente.", color = TempoTextMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    items(reportFilteredSessions) { s ->
                        val dur = s.calculateDurationMillis()
                        val rate = if (s.appliedRate > 0.0) s.appliedRate else client.hourlyRate
                        val valEarned = s.calculateEarnings(rate)
                        val p = projects.find { it.id == s.projectId }
                        val isPending = s.financialStatus == "unbilled"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(TempoRadius.shapeSm)
                                .background(TempoSurface1)
                                .padding(horizontal = TempoSpacing.space3, vertical = TempoSpacing.space2),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.description.ifBlank { p?.name ?: "Sessão" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TempoTextPrimary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${FormatUtils.formatDate(s.startTime)} • ${FormatUtils.formatDuration(dur)}", style = MaterialTheme.typography.labelSmall, color = TempoTextSecondary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isPending) "A FATURAR" else "FATURADO",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPending) Color(0xFFF59E0B) else TempoSuccess
                                    )
                                }
                            }
                            Text(FormatUtils.formatCurrency(valEarned), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TempoMono), fontWeight = FontWeight.Bold, color = TempoAccent)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(TempoSpacing.space6))
            }
        }
    }

    // Modal de Reajuste de Taxa
    if (showEditRateDialog) {
        AlertDialog(
            onDismissRequest = { showEditRateDialog = false },
            title = { Text("Reajustar Taxa do Cliente", fontWeight = FontWeight.Bold, color = TempoTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Defina a nova taxa padrão para futuros trabalhos deste cliente.", style = MaterialTheme.typography.bodySmall, color = TempoTextSecondary)
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = { rateInput = it },
                        label = { Text("Nova Taxa (R$/h)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newRate = rateInput.replace(",", ".").toDoubleOrNull()
                        if (newRate != null && newRate > 0.0) {
                            viewModel.updateClient(client.copy(hourlyRate = newRate))
                        }
                        showEditRateDialog = false
                    }
                ) {
                    Text("CONFIRMAR", color = TempoAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditRateDialog = false }) { Text("CANCELAR", color = TempoTextSecondary) }
            },
            containerColor = TempoSurface1
        )
    }

    // Confirmação de Arquivamento
    if (showArchiveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirmDialog = false },
            title = { Text("Arquivar Cliente", fontWeight = FontWeight.Bold, color = TempoTextPrimary) },
            text = {
                Text(
                    "O cliente será arquivado sem excluir o histórico de faturamento ou projetos passados (regra de proteção anti-perda). Deseja prosseguir?",
                    color = TempoTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.archiveClient(client.id)
                        showArchiveConfirmDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("ARQUIVAR", color = TempoDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirmDialog = false }) { Text("CANCELAR", color = TempoTextSecondary) }
            },
            containerColor = TempoSurface1
        )
    }

    // Modal de Edição Completa do Cliente (Nome e Taxa)
    if (showEditClientDialog) {
        AlertDialog(
            onDismissRequest = { showEditClientDialog = false },
            title = { Text("Editar Informações do Cliente", fontWeight = FontWeight.Bold, color = TempoTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Altere o nome e a taxa padrão deste cliente.", style = MaterialTheme.typography.bodySmall, color = TempoTextSecondary)
                    OutlinedTextField(
                        value = editClientNameInput,
                        onValueChange = { editClientNameInput = it },
                        label = { Text("Nome do Cliente") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editClientRateInput,
                        onValueChange = { editClientRateInput = it },
                        label = { Text("Taxa Padrão (R$/h)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newName = editClientNameInput.trim()
                        val newRate = editClientRateInput.replace(",", ".").toDoubleOrNull() ?: client.hourlyRate
                        if (newName.isNotBlank()) {
                            viewModel.updateClient(client.copy(name = newName, hourlyRate = newRate))
                            showEditClientDialog = false
                            Toast.makeText(context, "Cliente atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("SALVAR", color = TempoAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditClientDialog = false }) { Text("CANCELAR", color = TempoTextSecondary) }
            },
            containerColor = TempoSurface1
        )
    }
}
