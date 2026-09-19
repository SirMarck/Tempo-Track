package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Client
import com.example.data.Session
import com.example.utils.FormatUtils
import com.example.viewmodel.TimeTrackerViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.data.gemini.ClientSummaryData
import com.example.viewmodel.GeminiAnalysisState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import com.example.ui.theme.*
import com.example.ui.theme.luxBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: TimeTrackerViewModel) {
    val clients by viewModel.clients.collectAsState()
    val sessions by viewModel.sessions.collectAsState()

    // Default to current month
    val calendar = Calendar.getInstance()
    var currentMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }

    val monthName = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR")).format(calendar.apply {
        set(Calendar.MONTH, currentMonth)
        set(Calendar.YEAR, currentYear)
    }.time).replaceFirstChar { it.uppercase() }

    // Custom date range filter
    var useCustomRange by remember { mutableStateOf(false) }
    var customStartMillis by remember { mutableLongStateOf(
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
    ) }
    var customEndMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val customStartText = remember(customStartMillis) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(java.util.Date(customStartMillis))
    }
    val customEndText = remember(customEndMillis) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(java.util.Date(customEndMillis))
    }

    // Active filter label for display
    val filterLabel = if (useCustomRange) "$customStartText — $customEndText" else monthName

    var showMenu by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Calculate report data — supports monthly or custom range filter
    val reportData = remember(sessions, clients, currentMonth, currentYear, useCustomRange, customStartMillis, customEndMillis) {
        val data = mutableMapOf<Client, MutableList<Session>>()
        sessions.filter { it.endTime != null }.forEach { session ->
            val inRange = if (useCustomRange) {
                session.startTime >= customStartMillis && session.startTime <= customEndMillis
            } else {
                val cal = Calendar.getInstance().apply { timeInMillis = session.startTime }
                cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
            }
            if (inRange) {
                val client = clients.find { it.id == session.clientId }
                if (client != null) {
                    if (!data.containsKey(client)) data[client] = mutableListOf()
                    data[client]!!.add(session)
                }
            }
        }
        data
    }

    val geminiState by viewModel.geminiAnalysisState.collectAsState()

    val totalEarnings = remember(reportData) {
        reportData.entries.sumOf { (client, clientSessions) ->
            clientSessions.sumOf { session ->
                val duration = maxOf(0L, (session.endTime!! - session.startTime) - session.pausedDuration)
                val originalValue = (duration.toDouble() / (1000 * 60 * 60)) * client.hourlyRate
                val discountPctVal = originalValue * (session.discountPercentage / 100.0)
                maxOf(0.0, originalValue - discountPctVal - session.discountValue)
            }
        }
    }

    val totalHours = remember(reportData) {
        val totalMillis = reportData.values.flatten().sumOf { session ->
            maxOf(0L, (session.endTime!! - session.startTime) - session.pausedDuration)
        }
        totalMillis.toDouble() / (1000 * 60 * 60)
    }

    val clientSummaries = remember(reportData) {
        reportData.map { (client, clientSessions) ->
            val durationMillis = clientSessions.sumOf { session ->
                maxOf(0L, (session.endTime!! - session.startTime) - session.pausedDuration)
            }
            val hours = durationMillis.toDouble() / (1000 * 60 * 60)
            val billed = clientSessions.sumOf { session ->
                val orig = (maxOf(0L, (session.endTime!! - session.startTime) - session.pausedDuration).toDouble() / (1000 * 60 * 60)) * client.hourlyRate
                val disc = orig * (session.discountPercentage / 100.0) + session.discountValue
                maxOf(0.0, orig - disc)
            }
            ClientSummaryData(
                clientName = client.name,
                totalHours = hours,
                totalBilled = billed,
                hourlyRate = client.hourlyRate
            )
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Relatórios") },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Configurações")
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
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sobre o Tempo Track") },
                                onClick = {
                                    showMenu = false
                                    showAboutDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Procurar Atualizações") },
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        val result = com.example.utils.UpdateManager.checkForUpdates(context)
                                        com.example.utils.UpdateManager.handleUpdateResult(context, result)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Update, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
        ) {
            // ─── Filter mode segmented control ───────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterChip(
                    selected = !useCustomRange,
                    onClick = { useCustomRange = false },
                    label = { Text("Mês") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = useCustomRange,
                    onClick = { useCustomRange = true },
                    label = { Text("Período") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!useCustomRange) {
                // Month navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        if (currentMonth == 0) { currentMonth = 11; currentYear-- } else currentMonth--
                    }) { Text("< Anterior") }

                    Text(monthName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    TextButton(onClick = {
                        if (currentMonth == 11) { currentMonth = 0; currentYear++ } else currentMonth++
                    }) { Text("Próximo >") }
                }
            } else {
                // Custom date range pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = customStartMillis }
                            android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    cal.set(y, m, d, 0, 0, 0)
                                    customStartMillis = cal.timeInMillis
                                },
                                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("De: $customStartText", style = MaterialTheme.typography.labelMedium) }

                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = customEndMillis }
                            android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    cal.set(y, m, d, 23, 59, 59)
                                    customEndMillis = cal.timeInMillis
                                },
                                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Até: $customEndText", style = MaterialTheme.typography.labelMedium) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (reportData.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Nenhum dado para este mês.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .luxBorder(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.requestMonthlyAnalysis(
                                monthName = filterLabel,
                                totalEarnings = totalEarnings,
                                totalHours = totalHours,
                                clientSummaries = clientSummaries
                            )
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("✨", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Análise Inteligente com Gemini AI",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Toque para gerar insights de faturamento e produtividade",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(reportData.keys.toList()) { client ->
                        ClientReportCard(client, reportData[client]!!, filterLabel, viewModel)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            if (showSettingsDialog) {
                CompanySettingsDialog(onDismiss = { showSettingsDialog = false }, viewModel = viewModel)
            }

            if (showAboutDialog) {
                AboutDialog(onDismiss = { showAboutDialog = false })
            }

            when (val state = geminiState) {
                is GeminiAnalysisState.Loading -> {
                    AlertDialog(
                        onDismissRequest = { /* Não fecha durante o carregamento */ },
                        title = { Text("✨ Gemini AI") },
                        text = {
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text("Analisando faturamento e horas de $filterLabel com IA...")
                            }
                        },
                        confirmButton = {}
                    )
                }
                is GeminiAnalysisState.Success -> {
                    AlertDialog(
                        onDismissRequest = { viewModel.clearGeminiAnalysis() },
                        title = { Text("✨ Análise de $filterLabel", color = MaterialTheme.colorScheme.primary) },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = state.analysis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 20.sp
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { viewModel.clearGeminiAnalysis() }) {
                                Text("Fechar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Análise Gemini", state.analysis)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Análise copiada com sucesso!", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Copiar")
                            }
                        }
                    )
                }
                is GeminiAnalysisState.Error -> {
                    AlertDialog(
                        onDismissRequest = { viewModel.clearGeminiAnalysis() },
                        title = { Text("Aviso do Gemini AI") },
                        text = { Text(state.message) },
                        confirmButton = {
                            TextButton(onClick = { viewModel.clearGeminiAnalysis() }) {
                                Text("Entendi")
                            }
                        }
                    )
                }
                GeminiAnalysisState.Idle -> {}
            }
        }
    }
}

@Composable
fun ClientReportCard(
    client: Client,
    sessions: List<Session>,
    monthName: String,
    viewModel: TimeTrackerViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var totalDuration = 0L
    var totalBillableDuration = 0L
    var totalGrossValue = 0.0
    var totalDiscountValue = 0.0

    val sortedSessions = remember(sessions) { sessions.sortedBy { it.startTime } }

    sortedSessions.forEach { session ->
        val duration = session.calculateDurationMillis()
        val effectiveRate = if (session.appliedRate > 0.0) session.appliedRate else client.hourlyRate
        val originalValue = if (session.billable) (duration.toDouble() / (1000 * 60 * 60)) * effectiveRate else 0.0
        val discountPctVal = originalValue * (session.discountPercentage / 100.0)
        val totalDiscount = discountPctVal + session.discountValue

        totalDuration += duration
        if (session.billable) {
            totalBillableDuration += duration
            totalGrossValue += originalValue
            totalDiscountValue += totalDiscount
        }
    }
    val totalNetValue = maxOf(0.0, totalGrossValue - totalDiscountValue)
    val unbilledCount = sessions.count { it.closingBatchId == null && it.billable }

    var showClosingConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TempoRadius.shapeMd)
            .background(TempoSurface2)
            .tempoMaterialHighlight(TempoRadius.shapeMd)
            .padding(TempoSpacing.space4)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TempoSpacing.space3)) {
            // Header do Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TempoTextPrimary
                    )
                    Text(
                        text = "Taxa Base: ${FormatUtils.formatCurrency(client.hourlyRate)}/h",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = TempoMono),
                        color = TempoTextSecondary
                    )
                }

                if (unbilledCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(TempoWarning.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$unbilledCount a faturar",
                            style = MaterialTheme.typography.labelSmall,
                            color = TempoWarning,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(TempoSuccess.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Lote faturado",
                            style = MaterialTheme.typography.labelSmall,
                            color = TempoSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Divider(color = TempoOutline, thickness = 0.5.dp)

            // Resumo de Horas e Valores
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total de Horas:", style = MaterialTheme.typography.bodyMedium, color = TempoTextSecondary)
                Text(FormatUtils.formatDuration(totalDuration), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TempoMono), color = TempoTextPrimary, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Horas Faturáveis:", style = MaterialTheme.typography.bodyMedium, color = TempoTextSecondary)
                Text(FormatUtils.formatDuration(totalBillableDuration), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TempoMono), color = TempoTextPrimary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Valor Bruto:", style = MaterialTheme.typography.bodyMedium, color = TempoTextSecondary)
                Text(FormatUtils.formatCurrency(totalGrossValue), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TempoMono), color = TempoTextPrimary)
            }
            if (totalDiscountValue > 0.0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Descontos:", style = MaterialTheme.typography.bodyMedium, color = TempoDanger)
                    Text("- ${FormatUtils.formatCurrency(totalDiscountValue)}", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TempoMono), fontWeight = FontWeight.Bold, color = TempoDanger)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Líquido a Faturar:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TempoTextPrimary)
                Text(FormatUtils.formatCurrency(totalNetValue), style = MaterialTheme.typography.titleMedium.copy(fontFamily = TempoMono), fontWeight = FontWeight.Bold, color = TempoAccent)
            }

            Divider(color = TempoOutline, thickness = 0.5.dp)

            // Detalhes das sessões
            Text("Lançamentos de Serviço (${sortedSessions.size}):", style = MaterialTheme.typography.labelSmall, color = TempoTextMuted, letterSpacing = 0.5.sp)
            sortedSessions.forEach { session ->
                val duration = session.calculateDurationMillis()
                val effectiveRate = if (session.appliedRate > 0.0) session.appliedRate else client.hourlyRate
                val rawVal = if (session.billable) (duration.toDouble() / (1000 * 60 * 60)) * effectiveRate else 0.0
                val discVal = rawVal * (session.discountPercentage / 100.0) + session.discountValue
                val finalVal = maxOf(0.0, rawVal - discVal)

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
                            text = FormatUtils.formatDate(session.startTime) + " • " + if (session.description.isNotBlank()) session.description else "Sem descrição",
                            style = MaterialTheme.typography.bodySmall,
                            color = TempoTextPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        val timeSpan = "${FormatUtils.formatTime(session.startTime)} - ${session.endTime?.let { FormatUtils.formatTime(it) } ?: "..."}"
                        Text(
                            text = "$timeSpan • ${FormatUtils.formatDuration(duration)}" + if (!session.billable) " • Não faturável" else "",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = TempoMono),
                            color = TempoTextSecondary
                        )
                    }

                    Text(
                        text = FormatUtils.formatCurrency(finalVal),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TempoMono),
                        fontWeight = FontWeight.Bold,
                        color = if (session.billable) TempoAccent else TempoTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(TempoSpacing.space2))

            // Ações de Fechamento e Exportação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (unbilledCount > 0) {
                    OutlinedButton(
                        onClick = { showClosingConfirm = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TempoAccent)
                    ) {
                        Text("Fechar Lote", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedButton(
                    onClick = {
                        val file = com.example.utils.ExportUtils.generateImage(context, client, sessions, monthName)
                        if (file != null) {
                            com.example.utils.ExportUtils.shareFile(context, file, "image/png")
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Exportar IMG", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = {
                        val file = com.example.utils.ExportUtils.generatePdf(context, client, sessions, monthName)
                        if (file != null) {
                            com.example.utils.ExportUtils.shareFile(context, file, "application/pdf")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TempoAccent),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("PDF A4", style = MaterialTheme.typography.labelSmall, color = TempoBgBase, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showClosingConfirm) {
        AlertDialog(
            onDismissRequest = { showClosingConfirm = false },
            title = { Text("Fechar Lote de Cobrança?", color = TempoTextPrimary) },
            text = {
                Text(
                    text = "As $unbilledCount sessões faturáveis serão vinculadas a um novo lote de fechamento. Isso organiza as faturas emitidas e protege suas taxas históricas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TempoTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val sessionIds = sessions.filter { it.closingBatchId == null && it.billable }.map { it.id }
                        val minDate = sessions.minOfOrNull { it.startTime } ?: System.currentTimeMillis()
                        val maxDate = sessions.maxOfOrNull { it.endTime ?: it.startTime } ?: System.currentTimeMillis()
                        viewModel.createClosingBatch(
                            clientId = client.id,
                            fromDate = minDate,
                            toDate = maxDate,
                            sessionIds = sessionIds,
                            notes = "Fechamento $monthName"
                        ) { batchId ->
                            Toast.makeText(context, "Lote #$batchId criado com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                        showClosingConfirm = false
                    }
                ) {
                    Text("Confirmar", color = TempoAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClosingConfirm = false }) {
                    Text("Cancelar", color = TempoTextSecondary)
                }
            },
            containerColor = TempoSurface2
        )
    }
}
