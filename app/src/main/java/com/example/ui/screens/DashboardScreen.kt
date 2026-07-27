package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Client
import com.example.data.Session
import com.example.utils.FormatUtils
import com.example.utils.UpdateManager
import com.example.viewmodel.TimeTrackerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material.icons.filled.Edit

import androidx.compose.material.icons.filled.Settings

import com.example.ui.theme.luxBorder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(viewModel: TimeTrackerViewModel) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()

    // Automatically manage Foreground Service based on activeSession status
    LaunchedEffect(activeSession) {
        if (activeSession != null) {
            val intent = android.content.Intent(context, com.example.services.TimerService::class.java).apply {
                action = com.example.services.TimerService.ACTION_START
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        } else {
            val intent = android.content.Intent(context, com.example.services.TimerService::class.java)
            context.stopService(intent)
        }
    }

    var showStartDialog by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var selectedSessionForOptions by remember { mutableStateOf<Session?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Calculate estimating earnings this month
    var estimatedEarnings by remember { mutableStateOf(0.0) }
    
    LaunchedEffect(sessions, clients) {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        var total = 0.0
        sessions.filter { it.endTime != null }.forEach { session ->
            val cal = Calendar.getInstance().apply { timeInMillis = session.startTime }
            if (cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear) {
                val client = clients.find { it.id == session.clientId }
                if (client != null) {
                    val durationMillis = maxOf(0L, (session.endTime!! - session.startTime) - session.pausedDuration)
                    val durationHours = durationMillis.toDouble() / (1000 * 60 * 60)
                    val originalValue = durationHours * client.hourlyRate
                    val discountPctVal = originalValue * (session.discountPercentage / 100.0)
                    val finalValue = maxOf(0.0, originalValue - discountPctVal - session.discountValue)
                    total += finalValue
                }
            }
        }
        estimatedEarnings = total
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
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
                                        val result = UpdateManager.checkForUpdates(context)
                                        UpdateManager.handleUpdateResult(context, result)
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
        },
        floatingActionButton = {
            if (activeSession == null && clients.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { showManualDialog = true },
                        icon = { Icon(Icons.Default.Edit, contentDescription = "Lançar Manual") },
                        text = { Text("Lançar Manual") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    ExtendedFloatingActionButton(
                        onClick = { showStartDialog = true },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Iniciar") },
                        text = { Text("Iniciar Trabalho") }
                    )
                }
            } else if (activeSession != null) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.stopActiveSession() },
                    icon = { Icon(Icons.Default.Stop, contentDescription = "Parar") },
                    text = { Text("Parar Tempo") },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .luxBorder(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "GANHOS ESTIMADOS (MÊS ATUAL)", 
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = FormatUtils.formatCurrency(estimatedEarnings),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            if (activeSession != null) {
                ActiveSessionCard(
                    session = activeSession!!,
                    clients = clients,
                    onPause = { viewModel.pauseActiveSession() },
                    onResume = { viewModel.resumeActiveSession() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Trabalhos Recentes", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            val completedSessions = sessions.filter { it.endTime != null }.take(10)
            if (completedSessions.isEmpty()) {
                Text("Nenhum trabalho finalizado ainda.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(completedSessions) { session ->
                        val client = clients.find { it.id == session.clientId }
                        SessionItem(
                            session = session,
                            client = client,
                            onLongClick = {
                                selectedSessionForOptions = session
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        if (showStartDialog) {
            StartSessionDialog(
                clients = clients,
                onDismiss = { showStartDialog = false },
                onStart = { clientId, desc ->
                    viewModel.startSession(clientId, desc)
                    showStartDialog = false
                }
            )
        }

        if (showManualDialog) {
            ManualSessionDialog(
                clients = clients,
                onDismiss = { showManualDialog = false },
                onSave = { clientId, start, end, desc, discountVal, discountPct ->
                    viewModel.addManualSession(clientId, start, end, desc, discountVal, discountPct)
                    showManualDialog = false
                }
            )
        }

        if (showSettingsDialog) {
            CompanySettingsDialog(onDismiss = { showSettingsDialog = false })
        }

        if (selectedSessionForOptions != null) {
            AlertDialog(
                onDismissRequest = { selectedSessionForOptions = null },
                title = { Text("Opções do Trabalho") },
                text = { Text("Escolha uma ação para o trabalho selecionado:") },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showRenameDialog = true
                            }
                        ) {
                            Text("Renomear")
                        }
                        TextButton(
                            onClick = {
                                showDiscountDialog = true
                            }
                        ) {
                            Text("Desconto")
                        }
                        Button(
                            onClick = {
                                showDeleteConfirmDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("Deletar")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedSessionForOptions = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showDiscountDialog && selectedSessionForOptions != null) {
            var valInput by remember(selectedSessionForOptions) { mutableStateOf(selectedSessionForOptions!!.discountValue.toString()) }
            var pctInput by remember(selectedSessionForOptions) { mutableStateOf(selectedSessionForOptions!!.discountPercentage.toString()) }
            
            AlertDialog(
                onDismissRequest = { 
                    showDiscountDialog = false 
                    selectedSessionForOptions = null
                },
                title = { Text("Aplicar Desconto") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Defina os descontos a serem aplicados a este trabalho:")
                        OutlinedTextField(
                            value = valInput,
                            onValueChange = { valInput = it },
                            label = { Text("Desconto em Valor (R$)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = pctInput,
                            onValueChange = { pctInput = it },
                            label = { Text("Desconto em Percentual (%)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val v = valInput.toDoubleOrNull() ?: 0.0
                            val p = pctInput.toDoubleOrNull() ?: 0.0
                            viewModel.updateSession(selectedSessionForOptions!!.copy(discountValue = v, discountPercentage = p))
                            showDiscountDialog = false
                            selectedSessionForOptions = null
                        }
                    ) {
                        Text("Salvar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showDiscountDialog = false 
                            selectedSessionForOptions = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showRenameDialog && selectedSessionForOptions != null) {
            var newDescription by remember(selectedSessionForOptions) { mutableStateOf(selectedSessionForOptions!!.description) }
            AlertDialog(
                onDismissRequest = { 
                    showRenameDialog = false 
                    selectedSessionForOptions = null
                },
                title = { Text("Renomear Trabalho") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Edite a descrição do seu registro de trabalho:")
                        OutlinedTextField(
                            value = newDescription,
                            onValueChange = { newDescription = it },
                            label = { Text("Descrição") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateSession(selectedSessionForOptions!!.copy(description = newDescription))
                            showRenameDialog = false
                            selectedSessionForOptions = null
                        }
                    ) {
                        Text("Salvar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showRenameDialog = false 
                            selectedSessionForOptions = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showDeleteConfirmDialog && selectedSessionForOptions != null) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteConfirmDialog = false 
                    selectedSessionForOptions = null
                },
                title = { Text("Deletar Trabalho") },
                text = { Text("Tem certeza que deseja deletar este registro de trabalho? Esta ação não pode ser desfeita.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteSession(selectedSessionForOptions!!.id)
                            showDeleteConfirmDialog = false
                            selectedSessionForOptions = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Deletar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showDeleteConfirmDialog = false 
                            selectedSessionForOptions = null
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun ActiveSessionCard(
    session: Session,
    clients: List<Client>,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(session.isPaused) {
        if (!session.isPaused) {
            while (true) {
                delay(1000)
                currentTime = System.currentTimeMillis()
            }
        }
    }

    val isPaused = session.isPaused
    val backgroundColor = if (isPaused) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }
    val contentColor = if (isPaused) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .luxBorder(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        val client = clients.find { it.id == session.clientId }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = if (isPaused) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                            contentColor = if (isPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                        ) {
                            Text(
                                text = if (isPaused) "PAUSADO" else "EM ANDAMENTO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (client != null) {
                        Text(client.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    if (session.description.isNotEmpty()) {
                        Text(
                            session.description, 
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                
                val duration = if (isPaused) {
                    maxOf(0L, (session.lastPausedTime ?: currentTime) - session.startTime - session.pausedDuration)
                } else {
                    maxOf(0L, currentTime - session.startTime - session.pausedDuration)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatUtils.formatDuration(duration),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPaused) {
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Retomar")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retomar")
                    }
                } else {
                    FilledTonalButton(
                        onClick = onPause,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        PauseIcon(color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pausar")
                    }
                }
            }
        }
    }
}

@Composable
fun PauseIcon(color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.size(18.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(3.dp).height(12.dp).background(color, shape = androidx.compose.foundation.shape.RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width(3.dp).height(12.dp).background(color, shape = androidx.compose.foundation.shape.RoundedCornerShape(1.dp)))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionItem(session: Session, client: Client?, onLongClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
            .luxBorder(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(client?.name ?: "Cliente Desconhecido", fontWeight = FontWeight.Bold)
                Text(FormatUtils.formatDate(session.startTime))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(session.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val duration = if (session.endTime != null) maxOf(0L, (session.endTime - session.startTime) - session.pausedDuration) else 0L
            val originalValue = if (client != null) {
                (duration.toDouble() / (1000 * 60 * 60)) * client.hourlyRate
            } else 0.0
            val discountPctVal = originalValue * (session.discountPercentage / 100.0)
            val totalDiscount = discountPctVal + session.discountValue
            val finalValue = maxOf(0.0, originalValue - totalDiscount)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Duração: ${FormatUtils.formatDuration(duration)}", style = MaterialTheme.typography.bodySmall)
                    if (totalDiscount > 0.0) {
                        val descLabel = StringBuilder()
                        if (session.discountPercentage > 0.0) {
                            descLabel.append("${session.discountPercentage}%")
                        }
                        if (session.discountValue > 0.0) {
                            if (descLabel.isNotEmpty()) descLabel.append(" + ")
                            descLabel.append(FormatUtils.formatCurrency(session.discountValue))
                        }
                        Text("Desconto: $descLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (totalDiscount > 0.0) {
                        Text(
                            text = FormatUtils.formatCurrency(originalValue),
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(FormatUtils.formatCurrency(finalValue), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun StartSessionDialog(
    clients: List<Client>,
    onDismiss: () -> Unit,
    onStart: (Long, String) -> Unit
) {
    if (clients.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Nenhum cliente") },
            text = { Text("Adicione um cliente primeiro na aba Clientes.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
        )
        return
    }

    var selectedClientId by remember { mutableStateOf(clients.first().id) }
    var description by remember { mutableStateOf("") }
    
    // In a real app we'd use a Dropdown or similar. For simplicity, just next/prev or a simple list.
    // Let's use a very simple setup.
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Iniciar Trabalho") },
        text = {
            Column {
                Text("Selecione o Cliente:")
                clients.forEach { client ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = client.id == selectedClientId,
                            onClick = { selectedClientId = client.id }
                        )
                        Text(client.name)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição do Serviço") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onStart(selectedClientId, description) }) {
                Text("Iniciar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun ManualSessionDialog(
    clients: List<Client>,
    onDismiss: () -> Unit,
    onSave: (Long, Long, Long, String, Double, Double) -> Unit
) {
    if (clients.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Nenhum cliente") },
            text = { Text("Adicione um cliente primeiro na aba Clientes.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
        )
        return
    }

    val context = LocalContext.current
    var selectedClientId by remember { mutableStateOf(clients.first().id) }
    var description by remember { mutableStateOf("") }
    var discountValInput by remember { mutableStateOf("") }
    var discountPctInput by remember { mutableStateOf("") }

    // Start/End date-time management
    val startCalendar = remember { 
        Calendar.getInstance().apply { 
            add(Calendar.HOUR_OF_DAY, -1) 
        } 
    }
    val endCalendar = remember { Calendar.getInstance() }

    var startTimeMillis by remember { mutableLongStateOf(startCalendar.timeInMillis) }
    var endTimeMillis by remember { mutableLongStateOf(endCalendar.timeInMillis) }

    val startDateText = remember(startTimeMillis) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(java.util.Date(startTimeMillis))
    }
    val startTimeText = remember(startTimeMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(startTimeMillis))
    }
    val endTimeText = remember(endTimeMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(endTimeMillis))
    }

    val durationMillis = maxOf(0L, endTimeMillis - startTimeMillis)
    val hours = durationMillis / (1000 * 60 * 60)
    val minutes = (durationMillis / (1000 * 60)) % 60

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lançar Trabalho Manual", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text("Selecione o Cliente:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Column {
                        clients.forEach { client ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedClientId = client.id }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = client.id == selectedClientId,
                                    onClick = { selectedClientId = client.id }
                                )
                                Text(client.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                item {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Período do Trabalho:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }

                item {
                    // Date picker button
                    OutlinedButton(
                        onClick = {
                            startCalendar.timeInMillis = startTimeMillis
                            val dpd = android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    startCalendar.timeInMillis = startTimeMillis
                                    startCalendar.set(Calendar.YEAR, year)
                                    startCalendar.set(Calendar.MONTH, month)
                                    startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    startTimeMillis = startCalendar.timeInMillis
                                    
                                    endCalendar.timeInMillis = endTimeMillis
                                    endCalendar.set(Calendar.YEAR, year)
                                    endCalendar.set(Calendar.MONTH, month)
                                    endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    endTimeMillis = endCalendar.timeInMillis
                                },
                                startCalendar.get(Calendar.YEAR),
                                startCalendar.get(Calendar.MONTH),
                                startCalendar.get(Calendar.DAY_OF_MONTH)
                            )
                            dpd.show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Update, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Data: $startDateText")
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                startCalendar.timeInMillis = startTimeMillis
                                val tpd = android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        startCalendar.timeInMillis = startTimeMillis
                                        startCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        startCalendar.set(Calendar.MINUTE, minute)
                                        startCalendar.set(Calendar.SECOND, 0)
                                        startCalendar.set(Calendar.MILLISECOND, 0)
                                        startTimeMillis = startCalendar.timeInMillis
                                        
                                        if (endTimeMillis < startCalendar.timeInMillis) {
                                            endTimeMillis = startCalendar.timeInMillis + 3600000 // +1h
                                        }
                                    },
                                    startCalendar.get(Calendar.HOUR_OF_DAY),
                                    startCalendar.get(Calendar.MINUTE),
                                    true
                                )
                                tpd.show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Início: $startTimeText")
                        }

                        OutlinedButton(
                            onClick = {
                                endCalendar.timeInMillis = endTimeMillis
                                val tpd = android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        endCalendar.timeInMillis = endTimeMillis
                                        endCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        endCalendar.set(Calendar.MINUTE, minute)
                                        endCalendar.set(Calendar.SECOND, 0)
                                        endCalendar.set(Calendar.MILLISECOND, 0)
                                        
                                        if (endCalendar.timeInMillis < startTimeMillis) {
                                            endCalendar.add(Calendar.DAY_OF_MONTH, 1)
                                        }
                                        endTimeMillis = endCalendar.timeInMillis
                                    },
                                    endCalendar.get(Calendar.HOUR_OF_DAY),
                                    endCalendar.get(Calendar.MINUTE),
                                    true
                                )
                                tpd.show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Fim: $endTimeText")
                        }
                    }
                }

                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            Text(
                                text = "Duração calculada: ${hours}h ${minutes}min",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição do Serviço") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = discountValInput,
                            onValueChange = { discountValInput = it },
                            label = { Text("Desconto (R$)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = discountPctInput,
                            onValueChange = { discountPctInput = it },
                            label = { Text("Desconto (%)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val discountVal = discountValInput.toDoubleOrNull() ?: 0.0
                    val discountPct = discountPctInput.toDoubleOrNull() ?: 0.0
                    onSave(
                        selectedClientId,
                        startTimeMillis,
                        endTimeMillis,
                        description,
                        discountVal,
                        discountPct
                    )
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
