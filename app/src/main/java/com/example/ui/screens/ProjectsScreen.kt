package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
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

enum class ProjectViewTab {
    PROJECTS, CLIENTS
}

/**
 * Tela 5 - Projetos e Gestão de Escopo/Clientes
 * Especificação do Guia de Redesign v2 - Seção 10 e Prompt 6
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: TimeTrackerViewModel,
    onNavigateToProjectDetail: (Long) -> Unit = {},
    onNavigateToClientDetail: (Long) -> Unit = {}
) {
    val clients by viewModel.clients.collectAsState()
    val activeClients by viewModel.activeClients.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val activeProjects by viewModel.activeProjects.collectAsState()
    val sessions by viewModel.sessions.collectAsState()

    var activeTab by remember { mutableStateOf(ProjectViewTab.PROJECTS) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Filtrados pela busca
    val displayedProjects = remember(activeProjects, searchQuery) {
        if (searchQuery.isBlank()) activeProjects
        else activeProjects.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val displayedClients = remember(activeClients, searchQuery) {
        if (searchQuery.isBlank()) activeClients
        else activeClients.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space3),
                verticalArrangement = Arrangement.spacedBy(TempoSpacing.space2)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Projetos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TempoTextPrimary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                activeTab = ProjectViewTab.CLIENTS
                                showAddDialog = true
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(TempoSurface2)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Novo Cliente",
                                tint = TempoAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                activeTab = ProjectViewTab.PROJECTS
                                showAddDialog = true
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(TempoSurface2)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Novo Projeto",
                                tint = TempoTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Quick Action Bar for instant one-tap creation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            activeTab = ProjectViewTab.CLIENTS
                            showAddDialog = true
                        },
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
                        onClick = {
                            activeTab = ProjectViewTab.PROJECTS
                            showAddDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TempoSurface2,
                            contentColor = TempoTextPrimary
                        ),
                        shape = TempoRadius.shapeSm,
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Novo Projeto", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }

                // Alternância entre Projetos e Clientes
                TempoSegmentedFilter(
                    options = listOf(ProjectViewTab.PROJECTS, ProjectViewTab.CLIENTS),
                    selectedOption = activeTab,
                    onOptionSelected = { activeTab = it },
                    labelProvider = { tab ->
                        when (tab) {
                            ProjectViewTab.PROJECTS -> "Projetos Ativos (${activeProjects.size})"
                            ProjectViewTab.CLIENTS -> "Clientes (${activeClients.size})"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Barra de pesquisa
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = if (activeTab == ProjectViewTab.PROJECTS) "Buscar projeto..." else "Buscar cliente...",
                            color = TempoTextMuted
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TempoTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TempoAccent,
                        unfocusedBorderColor = TempoOutline,
                        focusedTextColor = TempoTextPrimary,
                        unfocusedTextColor = TempoTextPrimary
                    ),
                    shape = TempoRadius.shapeSm
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
            if (activeTab == ProjectViewTab.PROJECTS) {
                if (displayedProjects.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = TempoSpacing.space6),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isEmpty()) "Nenhum projeto cadastrado ainda." else "Nenhum projeto encontrado.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TempoTextMuted
                            )
                        }
                    }
                } else {
                    items(displayedProjects) { project ->
                        val client = clients.find { it.id == project.clientId }
                        val projectSessions = sessions.filter { it.projectId == project.id && it.endTime != null }
                        val consumedMillis = projectSessions.sumOf { it.calculateDurationMillis() }
                        val consumedHours = consumedMillis.toDouble() / (1000 * 60 * 60)

                        ProjectCardItem(
                            project = project,
                            clientName = client?.name ?: "Cliente",
                            consumedHours = consumedHours,
                            onClick = { onNavigateToProjectDetail(project.id) }
                        )
                    }
                }
            } else {
                // Aba de Clientes
                if (displayedClients.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = TempoSpacing.space6),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum cliente cadastrado ainda.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TempoTextMuted
                            )
                        }
                    }
                } else {
                    items(displayedClients) { client ->
                        val clientProjects = projects.filter { it.clientId == client.id && it.status == "active" }
                        val clientSessions = sessions.filter { it.clientId == client.id && it.endTime != null }
                        val totalClientHours = clientSessions.sumOf { it.calculateDurationMillis() }.toDouble() / (1000 * 60 * 60)

                        ClientCardItem(
                            client = client,
                            projectCount = clientProjects.size,
                            totalHours = totalClientHours,
                            onArchive = { viewModel.archiveClient(client.id) },
                            onClick = { onNavigateToClientDetail(client.id) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(TempoSpacing.space6))
            }
        }
    }

    if (showAddDialog) {
        if (activeTab == ProjectViewTab.PROJECTS) {
            AddProjectDialog(
                clients = activeClients.ifEmpty { clients },
                onDismiss = { showAddDialog = false },
                onSave = { clientId, name, rate, budgetAmt, budgetHours ->
                    viewModel.addProject(
                        clientId = clientId,
                        name = name,
                        hourlyRate = rate,
                        budgetAmount = budgetAmt,
                        budgetMinutes = budgetHours?.let { (it * 60).toLong() }
                    )
                    showAddDialog = false
                }
            )
        } else {
            AddClientDialog(
                onDismiss = { showAddDialog = false },
                onSave = { name, rate ->
                    viewModel.addClient(name, rate)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun ProjectCardItem(
    project: Project,
    clientName: String,
    consumedHours: Double,
    onClick: () -> Unit
) {
    val budgetHours = project.budgetMinutes?.let { it.toDouble() / 60.0 }
    val progressRatio = if (budgetHours != null && budgetHours > 0.0) {
        (consumedHours / budgetHours).toFloat()
    } else null

    val progressColor = when {
        progressRatio == null -> TempoAccent
        progressRatio > 1.0f -> TempoDanger
        progressRatio >= 0.8f -> TempoWarning
        else -> TempoAccent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TempoRadius.shapeSm)
            .background(TempoSurface1)
            .tempoMaterialHighlight(TempoRadius.shapeSm)
            .clickable(onClick = onClick)
            .padding(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space3)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TempoSpacing.space2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val initial = project.name.firstOrNull()?.uppercaseChar()?.toString() ?: "P"
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TempoAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TempoAccent
                        )
                    }

                    Column {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TempoTextPrimary
                        )
                        Text(
                            text = clientName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TempoTextSecondary
                        )
                    }
                }

                Text(
                    text = String.format("%.1fh", consumedHours),
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = TempoMono),
                    color = TempoTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Barra fina integrada de progresso do orçamento
            if (progressRatio != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progressRatio.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = progressColor,
                        trackColor = TempoSurface3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(progressRatio * 100).toInt()}% consumido",
                            style = MaterialTheme.typography.labelSmall,
                            color = progressColor
                        )
                        Text(
                            text = "Meta: ${budgetHours.toInt()}h",
                            style = MaterialTheme.typography.labelSmall,
                            color = TempoTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClientCardItem(
    client: Client,
    projectCount: Int,
    totalHours: Double,
    onArchive: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TempoRadius.shapeSm)
            .background(TempoSurface1)
            .tempoMaterialHighlight(TempoRadius.shapeSm)
            .clickable(onClick = onClick)
            .padding(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space3),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val initial = client.name.firstOrNull()?.uppercaseChar()?.toString() ?: "C"
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2563EB).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF60A5FA)
                )
            }

            Column {
                Text(
                    text = client.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TempoTextPrimary
                )
                Text(
                    text = "$projectCount projeto(s) • ${FormatUtils.formatCurrency(client.hourlyRate)}/h",
                    style = MaterialTheme.typography.bodySmall,
                    color = TempoTextSecondary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = String.format("%.1fh total", totalHours),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = TempoMono),
                color = TempoTextMuted
            )

            IconButton(
                onClick = onArchive,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = "Arquivar Cliente",
                    tint = TempoTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddProjectDialog(
    clients: List<Client>,
    onDismiss: () -> Unit,
    onSave: (clientId: Long, name: String, rate: Double?, budgetAmount: Double?, budgetHours: Double?) -> Unit
) {
    var selectedClientId by remember { mutableLongStateOf(clients.firstOrNull()?.id ?: 0L) }
    var name by remember { mutableStateOf("") }
    var rateStr by remember { mutableStateOf("") }
    var budgetHoursStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Projeto", fontWeight = FontWeight.Bold, color = TempoTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TempoSpacing.space3)) {
                // Cliente
                Text("Cliente", style = MaterialTheme.typography.labelMedium, color = TempoTextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2)) {
                    items(clients) { client ->
                        val isSelected = client.id == selectedClientId
                        Box(
                            modifier = Modifier
                                .clip(TempoRadius.shapeSm)
                                .background(if (isSelected) TempoSurface3 else TempoSurface2)
                                .clickable { selectedClientId = client.id }
                                .padding(horizontal = TempoSpacing.space3, vertical = TempoSpacing.space2)
                        ) {
                            Text(
                                text = client.name,
                                color = if (isSelected) TempoAccent else TempoTextSecondary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Projeto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rateStr,
                    onValueChange = { rateStr = it },
                    label = { Text("Taxa Específica/hora (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = budgetHoursStr,
                    onValueChange = { budgetHoursStr = it },
                    label = { Text("Orçamento de Horas (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedClientId > 0L && name.isNotBlank()) {
                        val rate = rateStr.toDoubleOrNull()
                        val hours = budgetHoursStr.toDoubleOrNull()
                        onSave(selectedClientId, name.trim(), rate, null, hours)
                    }
                },
                enabled = selectedClientId > 0L && name.isNotBlank()
            ) {
                Text("CRIAR PROJETO", color = TempoAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = TempoTextSecondary) }
        },
        containerColor = TempoSurface1
    )
}

@Composable
fun AddClientDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, rate: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var rateStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Cliente", fontWeight = FontWeight.Bold, color = TempoTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TempoSpacing.space3)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Cliente") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rateStr,
                    onValueChange = { rateStr = it },
                    label = { Text("Taxa Horária Padrão (R$)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val rate = rateStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onSave(name.trim(), rate)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("SALVAR", color = TempoAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = TempoTextSecondary) }
        },
        containerColor = TempoSurface1
    )
}
