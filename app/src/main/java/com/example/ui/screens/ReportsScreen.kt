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
                        ClientReportCard(client, reportData[client]!!, filterLabel)
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
fun ClientReportCard(client: Client, sessions: List<Session>, monthName: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var totalDuration = 0L
    var totalGrossValue = 0.0
    var totalDiscountValue = 0.0
    
    sessions.forEach { session ->
        val duration = maxOf(0L, (session.endTime!! - session.startTime) - session.pausedDuration)
        val originalValue = (duration.toDouble() / (1000 * 60 * 60)) * client.hourlyRate
        val discountPctVal = originalValue * (session.discountPercentage / 100.0)
        val totalDiscount = discountPctVal + session.discountValue
        
        totalDuration += duration
        totalGrossValue += originalValue
        totalDiscountValue += totalDiscount
    }
    val totalNetValue = maxOf(0.0, totalGrossValue - totalDiscountValue)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .luxBorder(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(client.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("Valor da Hora: ${FormatUtils.formatCurrency(client.hourlyRate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Horas:", style = MaterialTheme.typography.bodyMedium)
                Text(FormatUtils.formatDuration(totalDuration), fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Valor Bruto:", style = MaterialTheme.typography.bodyMedium)
                Text(FormatUtils.formatCurrency(totalGrossValue), fontWeight = FontWeight.Bold)
            }
            if (totalDiscountValue > 0.0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Descontos:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Text("- ${FormatUtils.formatCurrency(totalDiscountValue)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Líquido a Cobrar:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(FormatUtils.formatCurrency(totalNetValue), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Detalhes de Serviço:", style = MaterialTheme.typography.labelMedium)
            sessions.sortedBy { it.startTime }.forEach { session ->
                val duration = maxOf(0L, (session.endTime!! - session.startTime) - session.pausedDuration)
                val originalValue = (duration.toDouble() / (1000 * 60 * 60)) * client.hourlyRate
                val discountPctVal = originalValue * (session.discountPercentage / 100.0)
                val totalDiscount = discountPctVal + session.discountValue
                val finalValue = maxOf(0.0, originalValue - totalDiscount)
                val startTimeStr = FormatUtils.formatTime(session.startTime)
                val endTimeStr = FormatUtils.formatTime(session.endTime)
                val pausesList = com.example.utils.ExportUtils.parsePauseEvents(session.pauseEvents)
                
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(FormatUtils.formatDate(session.startTime), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(session.description, style = MaterialTheme.typography.bodySmall)
                            Text("Início: $startTimeStr | Encerramento: $endTimeStr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            if (pausesList.isNotEmpty()) {
                                pausesList.forEach { pausePair ->
                                    val pTime = FormatUtils.formatTime(pausePair.first)
                                    val rTime = pausePair.second?.let { FormatUtils.formatTime(it) } ?: "Sem retomada"
                                    Text(" ↳ Pausa: $pTime | Retomada: $rTime", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            if (totalDiscount > 0.0) {
                                Text(
                                    text = FormatUtils.formatCurrency(originalValue),
                                    style = MaterialTheme.typography.bodySmall.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(FormatUtils.formatCurrency(finalValue), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = {
                        val file = com.example.utils.ExportUtils.generateImage(context, client, sessions, monthName)
                        if (file != null) {
                            com.example.utils.ExportUtils.shareFile(context, file, "image/png")
                        }
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Exportar IMG", style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = {
                        val file = com.example.utils.ExportUtils.generatePdf(context, client, sessions, monthName)
                        if (file != null) {
                            com.example.utils.ExportUtils.shareFile(context, file, "application/pdf")
                        }
                    }
                ) {
                    Text("Exportar PDF", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
