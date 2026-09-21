package com.example.ui.screens

import androidx.compose.animation.*
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
import com.example.utils.FormatUtils
import com.example.utils.UpdateManager
import com.example.viewmodel.TimeTrackerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tela 1 - Hoje / Dashboard Operacional
 * Especificação do Guia de Redesign v2 - Seção 6 e Prompt 4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TimeTrackerViewModel,
    onNavigateToFocus: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val clients by viewModel.clients.collectAsState()
    val activeClients by viewModel.activeClients.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()

    // Foreground service sync
    LaunchedEffect(activeSession) {
        try {
            if (activeSession != null) {
                val intent = android.content.Intent(context, com.example.services.TimerService::class.java).apply {
                    action = com.example.services.TimerService.ACTION_START
                }
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } else {
                val intent = android.content.Intent(context, com.example.services.TimerService::class.java)
                context.stopService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("TodayScreen", "Foreground service start/stop failed", e)
        }
    }

    var showNewWorkSheet by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var showAddClientDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Feedback temporário com opção de desfazer (Undo)
    var lastCompletedSession by remember { mutableStateOf<Session?>(null) }
    var showUndoBanner by remember { mutableStateOf(false) }

    // Data de hoje formatada
    val todayFormatted = remember {
        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))
        sdf.format(Date()).replaceFirstChar { it.uppercase() }
    }

    // Sessões de hoje
    val todaySessions = remember(sessions) {
        val calToday = Calendar.getInstance()
        val todayYear = calToday.get(Calendar.YEAR)
        val todayDayOfYear = calToday.get(Calendar.DAY_OF_YEAR)

        sessions.filter { session ->
            session.endTime != null && run {
                val cal = Calendar.getInstance().apply { timeInMillis = session.startTime }
                cal.get(Calendar.YEAR) == todayYear && cal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear
            }
        }
    }

    // Totais de hoje
    val todayHours = remember(todaySessions) {
        val totalMillis = todaySessions.sumOf { it.calculateDurationMillis() }
        totalMillis.toDouble() / (1000 * 60 * 60)
    }

    val todayEarnings = remember(todaySessions, clients) {
        todaySessions.sumOf { session ->
            val fallbackRate = clients.find { it.id == session.clientId }?.hourlyRate ?: 0.0
            session.calculateEarnings(fallbackRate)
        }
    }

    // Trabalhos recentes para continuação em 1 toque
    val recentContinuations = remember(sessions, clients) {
        sessions
            .filter { it.endTime != null }
            .distinctBy { "${it.clientId}-${it.projectId}-${it.activityId}" }
            .take(4)
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
                        text = "Hoje",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TempoTextPrimary
                    )
                    Text(
                        text = todayFormatted,
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
                            text = { Text("Lançamento Manual") },
                            onClick = {
                                showMenu = false
                                showManualDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.AddAlarm, contentDescription = null) }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = TempoSpacing.space4),
            verticalArrangement = Arrangement.spacedBy(TempoSpacing.space4)
        ) {
            // ─── 1. Resumo Integrado em Linha (Sem cards pesados) ───────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TempoRadius.shapeMd)
                        .background(tempoSurfaceGradient)
                        .tempoMaterialHighlight(TempoRadius.shapeMd)
                        .padding(TempoSpacing.space4),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TempoStatBlock(
                        label = "Horas Hoje",
                        value = FormatUtils.formatDuration((todayHours * 3600 * 1000).toLong()),
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(TempoOutline)
                    )
                    TempoStatBlock(
                        label = "Faturável Hoje",
                        value = FormatUtils.formatCurrency(todayEarnings),
                        modifier = Modifier.weight(1f),
                        isAccent = true
                    )
                }
            }

            // ─── Ações Rápidas: Novo Cliente & Iniciar Trabalho ───────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAddClientDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TempoSurface2,
                            contentColor = TempoAccent
                        ),
                        shape = TempoRadius.shapeSm,
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Novo Cliente", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showManualDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TempoSurface2,
                            contentColor = TempoTextPrimary
                        ),
                        shape = TempoRadius.shapeSm,
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.AddAlarm, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lançar Manual", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ─── Banner de Desfazer Registro Concluído ────────────────────────
            item {
                AnimatedVisibility(
                    visible = showUndoBanner && lastCompletedSession != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(TempoRadius.shapeSm)
                            .background(TempoSurface2)
                            .tempoMaterialHighlight(TempoRadius.shapeSm)
                            .padding(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space3),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = TempoSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Trabalho finalizado e salvo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TempoTextPrimary
                            )
                        }
                        TextButton(
                            onClick = {
                                val s = lastCompletedSession
                                if (s != null) {
                                    // Retoma a sessão removendo endTime
                                    viewModel.updateSession(s.copy(endTime = null, status = "running"))
                                }
                                showUndoBanner = false
                            }
                        ) {
                            Text("DESFAZER", color = TempoAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ─── 2. Trabalho Ativo OU Ação Iniciar + Continuar ─────────────────
            item {
                if (activeSession != null) {
                    ActiveTimerPanel(
                        session = activeSession!!,
                        clients = clients,
                        projects = projects,
                        onOpenFocus = onNavigateToFocus,
                        onPause = { viewModel.pauseActiveSession() },
                        onResume = { viewModel.resumeActiveSession() },
                        onFinish = {
                            val sessionToFinish = activeSession
                            lastCompletedSession = sessionToFinish
                            viewModel.stopActiveSession()
                            showUndoBanner = true
                            scope.launch {
                                delay(6000)
                                showUndoBanner = false
                            }
                        }
                    )
                } else {
                    // Sem sessão ativa: botão primário Iniciar + Continuar Recentes
                    Column(verticalArrangement = Arrangement.spacedBy(TempoSpacing.space3)) {
                        TempoPrimaryAction(
                            text = "Iniciar Trabalho",
                            onClick = { showNewWorkSheet = true },
                            icon = Icons.Default.PlayArrow,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (recentContinuations.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(TempoSpacing.space1))
                            Text(
                                text = "CONTINUAR",
                                style = MaterialTheme.typography.labelSmall,
                                color = TempoTextMuted,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(TempoSpacing.space2)) {
                                recentContinuations.forEach { recent ->
                                    val client = clients.find { it.id == recent.clientId }
                                    val project = projects.find { it.id == recent.projectId }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(TempoRadius.shapeSm)
                                            .background(TempoSurface1)
                                            .tempoMaterialHighlight(TempoRadius.shapeSm)
                                            .clickable {
                                                // Fluxo de 1 toque: inicia imediatamente com contexto anterior
                                                viewModel.startSession(
                                                    clientId = recent.clientId,
                                                    projectId = recent.projectId,
                                                    activityId = recent.activityId,
                                                    billable = recent.billable,
                                                    tag = recent.tag,
                                                    description = recent.description
                                                )
                                            }
                                            .padding(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space3),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space3)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(TempoSurface2),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = TempoAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            Column {
                                                Text(
                                                    text = client?.name ?: "Cliente",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = TempoTextPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                val detail = listOfNotNull(project?.name, recent.tag.takeIf { it.isNotEmpty() }).joinToString(" • ")
                                                if (detail.isNotEmpty()) {
                                                    Text(
                                                        text = detail,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TempoTextSecondary
                                                    )
                                                }
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = TempoTextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }



            item {
                Spacer(modifier = Modifier.height(TempoSpacing.space6))
            }
        }
    }

    if (showNewWorkSheet) {
        NewWorkSheet(
            clients = activeClients.ifEmpty { clients },
            projects = projects,
            activities = activities,
            onDismiss = { showNewWorkSheet = false },
            onStart = { clientId, projectId, activityId, desc, billable, tag ->
                viewModel.startSession(
                    clientId = clientId,
                    projectId = projectId,
                    activityId = activityId,
                    description = desc,
                    billable = billable,
                    tag = tag
                )
                showNewWorkSheet = false
            }
        )
    }

    if (showManualDialog) {
        ManualEntryDialog(
            clients = activeClients.ifEmpty { clients },
            projects = projects,
            activities = activities,
            onDismiss = { showManualDialog = false },
            onSave = { clientId, projectId, activityId, start, end, desc, billable, discountVal, discountPct, tag ->
                viewModel.addManualSession(
                    clientId = clientId,
                    projectId = projectId,
                    activityId = activityId,
                    startTime = start,
                    endTime = end,
                    description = desc,
                    billable = billable,
                    discountValue = discountVal,
                    discountPercentage = discountPct,
                    tag = tag
                )
                showManualDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        CompanySettingsDialog(
            onDismiss = { showSettingsDialog = false },
            viewModel = viewModel
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showAddClientDialog) {
        AddClientDialog(
            onDismiss = { showAddClientDialog = false },
            onSave = { name, rate ->
                viewModel.addClient(name, rate)
                showAddClientDialog = false
            }
        )
    }
}

/**
 * Painel da Sessão Ativa na tela Hoje com cálculo em tempo real e atalhos rápidos.
 */
@Composable
fun ActiveTimerPanel(
    session: Session,
    clients: List<Client>,
    projects: List<Project>,
    onOpenFocus: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(session.isPaused, session.id) {
        if (!session.isPaused) {
            while (true) {
                delay(1000)
                currentTime = System.currentTimeMillis()
            }
        }
    }

    val client = clients.find { it.id == session.clientId }
    val project = projects.find { it.id == session.projectId }

    val durationMillis = session.calculateDurationMillis(currentTime)
    val durationText = FormatUtils.formatDuration(durationMillis)

    val rate = if (session.appliedRate > 0.0) session.appliedRate else (client?.hourlyRate ?: 0.0)
    val durationHours = durationMillis.toDouble() / (1000 * 60 * 60)
    val accumulatedEarnings = durationHours * rate

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TempoRadius.shapeMd)
            .background(tempoElevatedGradient)
            .tempoMaterialHighlight(TempoRadius.shapeMd)
            .clickable(onClick = onOpenFocus)
            .padding(TempoSpacing.space4)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TempoSpacing.space3)) {
            // Cabeçalho: Badge de status e cliente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (session.isPaused) TempoWarning else TempoAccent)
                    )
                    Text(
                        text = if (session.isPaused) "PAUSADO" else "EM ANDAMENTO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (session.isPaused) TempoWarning else TempoAccent,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "Toque para abrir foco",
                    style = MaterialTheme.typography.labelSmall,
                    color = TempoTextMuted
                )
            }

            // Nome do cliente e projeto/atividade
            Column {
                Text(
                    text = client?.name ?: "Cliente",
                    style = MaterialTheme.typography.titleLarge,
                    color = TempoTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                val detail = listOfNotNull(project?.name, session.description.takeIf { it.isNotEmpty() }).joinToString(" — ")
                if (detail.isNotEmpty()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TempoTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Cronômetro Central e Valor Acumulado (sem jitter)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.displayLarge.copy(fontFamily = TempoMono),
                    color = TempoTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "VALOR ACUMULADO",
                        style = MaterialTheme.typography.labelSmall,
                        color = TempoTextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = FormatUtils.formatCurrency(accumulatedEarnings),
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = TempoMono),
                        color = TempoAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Ações: Pausar/Retomar e Finalizar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space3)
            ) {
                TempoSecondaryAction(
                    text = if (session.isPaused) "Retomar" else "Pausar",
                    onClick = { if (session.isPaused) onResume() else onPause() },
                    icon = if (session.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    modifier = Modifier.weight(1f)
                )

                TempoPrimaryAction(
                    text = "Finalizar",
                    onClick = onFinish,
                    icon = Icons.Default.Stop,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}


