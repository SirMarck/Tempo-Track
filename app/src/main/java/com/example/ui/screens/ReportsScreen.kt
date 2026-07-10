package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Client
import com.example.data.Session
import com.example.utils.FormatUtils
import com.example.viewmodel.TimeTrackerViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

    var showMenu by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Calculate report data
    val reportData = remember(sessions, clients, currentMonth, currentYear) {
        val data = mutableMapOf<Client, MutableList<Session>>()
        sessions.filter { it.endTime != null }.forEach { session ->
            val cal = Calendar.getInstance().apply { timeInMillis = session.startTime }
            if (cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear) {
                val client = clients.find { it.id == session.clientId }
                if (client != null) {
                    if (!data.containsKey(client)) data[client] = mutableListOf()
                    data[client]!!.add(session)
                }
            }
        }
        data
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

            Spacer(modifier = Modifier.height(16.dp))

            if (reportData.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Nenhum dado para este mês.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(reportData.keys.toList()) { client ->
                        ClientReportCard(client, reportData[client]!!, monthName)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            if (showSettingsDialog) {
                CompanySettingsDialog(onDismiss = { showSettingsDialog = false })
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
