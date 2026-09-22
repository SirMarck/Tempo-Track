package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.Session
import com.example.data.gemini.ClientSummaryData
import com.example.ui.theme.*
import com.example.utils.FormatUtils
import com.example.utils.UpdateManager
import com.example.viewmodel.GeminiAnalysisState
import com.example.viewmodel.TimeTrackerViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class DashboardPeriodMode {
    CURRENT_MONTH, LAST_MONTH, CUSTOM
}

/**
 * Tela 5 - Dashboard de Relatórios & Analytics
 * Focada exclusivamente em visualização analítica, gráficos de utilização, faturamento
 * e produtividade. As exportações em PDF/CSV/IMG ficam restritas ao detalhe de cada cliente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: TimeTrackerViewModel,
    onNavigateToClientDetail: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val clients by viewModel.clients.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val geminiState by viewModel.geminiAnalysisState.collectAsState()

    var periodMode by remember { mutableStateOf(DashboardPeriodMode.CURRENT_MONTH) }
    var showMenu by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Gerenciamento de Datas
    val nowCalendar = Calendar.getInstance()
    var customStartMillis by remember {
        mutableLongStateOf(
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            }.timeInMillis
        )
    }
    var customEndMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val monthYearFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale("pt", "BR")) }

    val customStartText = remember(customStartMillis) { dateFormatter.format(Date(customStartMillis)) }
    val customEndText = remember(customEndMillis) { dateFormatter.format(Date(customEndMillis)) }

    // Rótulo dinâmico do período
    val activePeriodLabel = remember(periodMode, customStartMillis, customEndMillis) {
        when (periodMode) {
            DashboardPeriodMode.CURRENT_MONTH -> {
                monthYearFormatter.format(nowCalendar.time).replaceFirstChar { it.uppercase() }
            }
            DashboardPeriodMode.LAST_MONTH -> {
                val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                monthYearFormatter.format(cal.time).replaceFirstChar { it.uppercase() }
            }
            DashboardPeriodMode.CUSTOM -> "$customStartText a $customEndText"
        }
    }

    // Filtro de sessões pelo período
    val filteredSessions = remember(sessions, periodMode, customStartMillis, customEndMillis) {
        val completed = sessions.filter { it.endTime != null }
        when (periodMode) {
            DashboardPeriodMode.CURRENT_MONTH -> {
                val cal = Calendar.getInstance()
                val currentYear = cal.get(Calendar.YEAR)
                val currentMonth = cal.get(Calendar.MONTH)
                completed.filter { s ->
                    val c = Calendar.getInstance().apply { timeInMillis = s.startTime }
                    c.get(Calendar.YEAR) == currentYear && c.get(Calendar.MONTH) == currentMonth
                }
            }
            DashboardPeriodMode.LAST_MONTH -> {
                val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                val targetYear = cal.get(Calendar.YEAR)
                val targetMonth = cal.get(Calendar.MONTH)
                completed.filter { s ->
                    val c = Calendar.getInstance().apply { timeInMillis = s.startTime }
                    c.get(Calendar.YEAR) == targetYear && c.get(Calendar.MONTH) == targetMonth
                }
            }
            DashboardPeriodMode.CUSTOM -> {
                completed.filter { s ->
                    s.startTime >= customStartMillis && (s.endTime ?: s.startTime) <= customEndMillis
                }
            }
        }.sortedByDescending { it.startTime }
    }

    // Totais calculados
    val totalDurationMillis = remember(filteredSessions) {
        filteredSessions.sumOf { it.calculateDurationMillis() }
    }
    val totalHours = totalDurationMillis.toDouble() / (1000 * 60 * 60)

    val billableSessions = remember(filteredSessions) {
        filteredSessions.filter { it.billable }
    }
    val totalBillableMillis = remember(billableSessions) {
        billableSessions.sumOf { it.calculateDurationMillis() }
    }
    val totalBillableHours = totalBillableMillis.toDouble() / (1000 * 60 * 60)

    val totalEarnings = remember(filteredSessions, clients) {
        filteredSessions.sumOf { s ->
            val fallback = clients.find { it.id == s.clientId }?.hourlyRate ?: 0.0
            val effectiveRate = if (s.appliedRate > 0.0) s.appliedRate else fallback
            s.calculateEarnings(effectiveRate)
        }
    }

    val averageRate = remember(totalHours, totalEarnings) {
        if (totalHours > 0.0) totalEarnings / totalHours else 0.0
    }

    val sessionCount = filteredSessions.size

    // Agrupamento por cliente
    val clientDistribution = remember(filteredSessions, clients) {
        clients.mapNotNull { client ->
            val clientSessions = filteredSessions.filter { it.clientId == client.id }
            if (clientSessions.isEmpty()) null
            else {
                val durMillis = clientSessions.sumOf { it.calculateDurationMillis() }
                val hours = durMillis.toDouble() / (1000 * 60 * 60)
                val earnings = clientSessions.sumOf { s ->
                    val rate = if (s.appliedRate > 0.0) s.appliedRate else client.hourlyRate
                    s.calculateEarnings(rate)
                }
                val pct = if (totalHours > 0.0) (hours / totalHours).toFloat() else 0f
                ClientDashboardStat(
                    client = client,
                    hours = hours,
                    earnings = earnings,
                    sessionCount = clientSessions.size,
                    percentage = pct
                )
            }
        }.sortedByDescending { it.hours }
    }

    // Agrupamento por atividade/categoria
    val activityDistribution = remember(filteredSessions, activities) {
        activities.mapNotNull { act ->
            val actSessions = filteredSessions.filter { it.activityId == act.id }
            if (actSessions.isEmpty()) null
            else {
                val durMillis = actSessions.sumOf { it.calculateDurationMillis() }
                val hours = durMillis.toDouble() / (1000 * 60 * 60)
                val pct = if (totalHours > 0.0) (hours / totalHours).toFloat() else 0f
                ActivityDashboardStat(
                    activity = act,
                    hours = hours,
                    sessionCount = actSessions.size,
                    percentage = pct
                )
            }
        }.sortedByDescending { it.hours }
    }

    // Distribuição de horas por dia (para o histograma visual)
    val dailyDistribution: List<Map.Entry<String, Long>> = remember(filteredSessions) {
        val map = linkedMapOf<String, Long>()
        val dayFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        filteredSessions.forEach { s ->
            val dayKey = dayFormat.format(Date(s.startTime))
            map[dayKey] = (map[dayKey] ?: 0L) + s.calculateDurationMillis()
        }
        val list = map.entries.toList()
        if (list.size > 10) list.takeLast(10) else list
    }

    val maxDailyMillis = remember(dailyDistribution) {
        dailyDistribution.maxOfOrNull { it.value }?.coerceAtLeast(3600000L) ?: 3600000L
    }

    // Resumos para o Gemini AI
    val clientSummariesForAi = remember(clientDistribution) {
        clientDistribution.map {
            ClientSummaryData(
                clientName = it.client.name,
                totalHours = it.hours,
                totalBilled = it.earnings,
                hourlyRate = it.client.hourlyRate
            )
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space3),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TempoTextPrimary
                    )
                    Text(
                        text = activePeriodLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = TempoTextSecondary
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(TempoSurface2)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu de opções",
                            tint = TempoTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Configurar Empresa") },
                            onClick = {
                                showMenu = false
                                showSettingsDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Sobre o TempoTrack") },
                            onClick = {
                                showMenu = false
                                showAboutDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Verificar Atualizações") },
                            onClick = {
                                showMenu = false
                                scope.launch {
                                    val res = UpdateManager.checkForUpdates(context)
                                    UpdateManager.handleUpdateResult(context, res)
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Update, contentDescription = null) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = TempoSpacing.space4)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(TempoSpacing.space4)
        ) {
            // ─── 1. Filtro Segmentado de Período ─────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = periodMode == DashboardPeriodMode.CURRENT_MONTH,
                    onClick = { periodMode = DashboardPeriodMode.CURRENT_MONTH },
                    label = { Text("Mês Atual", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TempoSurface3,
                        selectedLabelColor = TempoAccent
                    )
                )
                FilterChip(
                    selected = periodMode == DashboardPeriodMode.LAST_MONTH,
                    onClick = { periodMode = DashboardPeriodMode.LAST_MONTH },
                    label = { Text("Mês Passado", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TempoSurface3,
                        selectedLabelColor = TempoAccent
                    )
                )
                FilterChip(
                    selected = periodMode == DashboardPeriodMode.CUSTOM,
                    onClick = { periodMode = DashboardPeriodMode.CUSTOM },
                    label = { Text("Período", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TempoSurface3,
                        selectedLabelColor = TempoAccent
                    )
                )
            }

            // Seleção de Data Customizada
            if (periodMode == DashboardPeriodMode.CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = customStartMillis }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    cal.set(y, m, d, 0, 0, 0)
                                    customStartMillis = cal.timeInMillis
                                },
                                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = TempoRadius.shapeSm
                    ) {
                        Text("De: $customStartText", style = MaterialTheme.typography.labelSmall, color = TempoTextPrimary)
                    }

                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = customEndMillis }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    cal.set(y, m, d, 23, 59, 59)
                                    customEndMillis = cal.timeInMillis
                                },
                                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = TempoRadius.shapeSm
                    ) {
                        Text("Até: $customEndText", style = MaterialTheme.typography.labelSmall, color = TempoTextPrimary)
                    }
                }
            }

            // ─── 2. Cartões de Indicadores Chave (KPIs) ──────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space3)
            ) {
                // Total Faturado
                DashboardKpiCard(
                    label = "TOTAL FATURADO",
                    value = FormatUtils.formatCurrency(totalEarnings),
                    subLabel = "${FormatUtils.formatDuration(totalBillableMillis)} faturáveis",
                    isAccent = true,
                    modifier = Modifier.weight(1f)
                )

                // Total de Horas
                DashboardKpiCard(
                    label = "HORAS TOTAIS",
                    value = FormatUtils.formatDuration(totalDurationMillis),
                    subLabel = "$sessionCount apontamentos",
                    isAccent = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space3)
            ) {
                // Taxa Média Efetiva
                DashboardKpiCard(
                    label = "TAXA MÉDIA",
                    value = "${FormatUtils.formatCurrency(averageRate)}/h",
                    subLabel = "Média geral do período",
                    isAccent = false,
                    modifier = Modifier.weight(1f)
                )

                // Clientes Atendidos
                DashboardKpiCard(
                    label = "CLIENTES ATIVOS",
                    value = "${clientDistribution.size}",
                    subLabel = "Com trabalho no período",
                    isAccent = false,
                    modifier = Modifier.weight(1f)
                )
            }

            // ─── 3. Card Inteligente Gemini AI ───────────────────────────────
            if (filteredSessions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TempoRadius.shapeMd)
                        .clickable {
                            viewModel.requestMonthlyAnalysis(
                                monthName = activePeriodLabel,
                                totalEarnings = totalEarnings,
                                totalHours = totalHours,
                                clientSummaries = clientSummariesForAi
                            )
                        },
                    colors = CardDefaults.cardColors(containerColor = TempoSurface2)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(TempoSpacing.space4),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space3)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TempoAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✨", fontSize = 20.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Insights com Gemini AI",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TempoAccent
                            )
                            Text(
                                text = "Gerar análise profunda de rentabilidade e produtividade",
                                style = MaterialTheme.typography.bodySmall,
                                color = TempoTextSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TempoTextMuted
                        )
                    }
                }
            }

            // ─── 4. Gráfico de Utilização Diária (Histograma) ────────────────
            if (dailyDistribution.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TempoRadius.shapeMd)
                        .background(TempoSurface1)
                        .padding(TempoSpacing.space4),
                    verticalArrangement = Arrangement.spacedBy(TempoSpacing.space3)
                ) {
                    Text(
                        text = "UTILIZAÇÃO DIÁRIA (HORAS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TempoTextMuted,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        for (entry in dailyDistribution) {
                            val heightFraction = (entry.value.toFloat() / maxDailyMillis.toFloat()).coerceIn(0.08f, 1f)
                            val hoursText = String.format(Locale.US, "%.1fh", entry.value.toDouble() / (1000 * 60 * 60))

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = hoursText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = TempoMono),
                                    color = TempoTextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .fillMaxHeight(heightFraction)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(TempoAccent)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = entry.key,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = TempoMono),
                                    color = TempoTextMuted
                                )
                            }
                        }
                    }
                }
            }

            // ─── 5. Gráfico de Horas e Faturamento por Cliente ───────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TempoRadius.shapeMd)
                    .background(TempoSurface1)
                    .padding(TempoSpacing.space4),
                verticalArrangement = Arrangement.spacedBy(TempoSpacing.space3)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FATURAMENTO POR CLIENTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TempoTextMuted,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Toque no cliente para relatórios",
                        style = MaterialTheme.typography.labelSmall,
                        color = TempoTextMuted
                    )
                }

                if (clientDistribution.isEmpty()) {
                    Text(
                        text = "Nenhum apontamento no período selecionado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TempoTextMuted,
                        modifier = Modifier.padding(vertical = TempoSpacing.space3)
                    )
                } else {
                    clientDistribution.forEach { stat ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(TempoRadius.shapeSm)
                                .clickable { onNavigateToClientDetail(stat.client.id) }
                                .padding(vertical = TempoSpacing.space2),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stat.client.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TempoTextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = TempoTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = FormatUtils.formatDuration((stat.hours * 3600 * 1000).toLong()),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = TempoMono),
                                        color = TempoTextSecondary
                                    )
                                    Text(
                                        text = FormatUtils.formatCurrency(stat.earnings),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TempoMono),
                                        color = TempoAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Barra de progresso proporcional
                            LinearProgressIndicator(
                                progress = stat.percentage,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = TempoAccent,
                                trackColor = TempoSurface2
                            )
                        }
                    }
                }
            }

            // ─── 6. Distribuição por Categoria / Atividade ────────────────────
            if (activityDistribution.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TempoRadius.shapeMd)
                        .background(TempoSurface1)
                        .padding(TempoSpacing.space4),
                    verticalArrangement = Arrangement.spacedBy(TempoSpacing.space3)
                ) {
                    Text(
                        text = "HORAS POR ATIVIDADE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TempoTextMuted,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    activityDistribution.forEach { stat ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stat.activity.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TempoTextPrimary
                                )
                                Text(
                                    text = "${FormatUtils.formatDuration((stat.hours * 3600 * 1000).toLong())} (${(stat.percentage * 100).toInt()}%)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = TempoMono),
                                    color = TempoTextSecondary
                                )
                            }

                            LinearProgressIndicator(
                                progress = stat.percentage,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = TempoTextSecondary,
                                trackColor = TempoSurface2
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(TempoSpacing.space6))
        }
    }

    if (showSettingsDialog) {
        CompanySettingsDialog(onDismiss = { showSettingsDialog = false }, viewModel = viewModel)
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    // Modal de Análise Gemini AI
    when (val state = geminiState) {
        is GeminiAnalysisState.Loading -> {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("✨ Gemini AI", color = TempoTextPrimary) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = TempoAccent)
                        Text("Analisando faturamento e produtividade...", color = TempoTextSecondary)
                    }
                },
                confirmButton = { },
                containerColor = TempoSurface1
            )
        }
        is GeminiAnalysisState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearGeminiState() },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✨", fontSize = 20.sp)
                        Text("Insights de Desempenho", fontWeight = FontWeight.Bold, color = TempoTextPrimary)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = state.analysis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TempoTextPrimary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Análise Gemini", state.analysis))
                            Toast.makeText(context, "Análise copiada!", Toast.LENGTH_SHORT).show()
                            viewModel.clearGeminiState()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TempoAccent),
                        shape = TempoRadius.shapeSm
                    ) {
                        Text("Copiar e Fechar", fontWeight = FontWeight.Bold, color = TempoBgBase)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.clearGeminiState() }) {
                        Text("Fechar", color = TempoTextMuted)
                    }
                },
                containerColor = TempoSurface1
            )
        }
        is GeminiAnalysisState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearGeminiState() },
                title = { Text("Erro na Análise", color = TempoDanger) },
                text = { Text(state.message, color = TempoTextSecondary) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearGeminiState() }) {
                        Text("OK", color = TempoAccent)
                    }
                },
                containerColor = TempoSurface1
            )
        }
        else -> {}
    }
}

@Composable
fun DashboardKpiCard(
    label: String,
    value: String,
    subLabel: String,
    isAccent: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(TempoRadius.shapeMd)
            .background(TempoSurface1)
            .padding(TempoSpacing.space3),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TempoTextMuted,
            letterSpacing = 0.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = TempoMono),
            color = if (isAccent) TempoAccent else TempoTextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subLabel,
            style = MaterialTheme.typography.bodySmall,
            color = TempoTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

data class ClientDashboardStat(
    val client: Client,
    val hours: Double,
    val earnings: Double,
    val sessionCount: Int,
    val percentage: Float
)

data class ActivityDashboardStat(
    val activity: com.example.data.Activity,
    val hours: Double,
    val sessionCount: Int,
    val percentage: Float
)
