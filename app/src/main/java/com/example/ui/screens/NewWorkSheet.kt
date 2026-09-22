package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Activity
import com.example.data.Client
import com.example.data.Project
import com.example.ui.theme.*
import com.example.utils.FormatUtils

/**
 * Tela 2 - Novo Trabalho (Bottom Sheet contextual compacta sem rolagem)
 * Especificação Otimizada - Todos os campos cabem em tela única sem rolagem.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewWorkSheet(
    clients: List<Client>,
    projects: List<Project>,
    activities: List<Activity>,
    initialClientId: Long? = null,
    onDismiss: () -> Unit,
    onManageActivities: () -> Unit = {},
    onStart: (clientId: Long, projectId: Long?, activityId: Long?, description: String, billable: Boolean, tag: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedClientId by remember(initialClientId, clients) {
        mutableLongStateOf(initialClientId ?: clients.firstOrNull()?.id ?: 0L)
    }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var selectedActivityId by remember(activities) { mutableStateOf<Long?>(activities.firstOrNull()?.id) }
    var description by remember { mutableStateOf("") }
    var billable by remember { mutableStateOf(true) }
    var tag by remember { mutableStateOf("") }

    val selectedClient = remember(selectedClientId, clients) {
        clients.find { it.id == selectedClientId }
    }
    val clientProjects = remember(selectedClientId, projects) {
        projects.filter { it.clientId == selectedClientId && it.status == "active" }
    }
    val selectedProject = remember(selectedProjectId, projects) {
        projects.find { it.id == selectedProjectId }
    }

    // Taxa efetiva resolvida
    val effectiveRate = remember(selectedClient, selectedProject) {
        FormatUtils.resolveEffectiveRate(
            projectRate = selectedProject?.hourlyRate,
            clientRate = selectedClient?.hourlyRate ?: 0.0
        )
    }

    // Clientes filtrados por busca
    val filteredClients = remember(searchQuery, clients) {
        if (searchQuery.isBlank()) clients
        else clients.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TempoSurface1,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(TempoTextMuted)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = TempoSpacing.space4)
                .padding(bottom = TempoSpacing.space3),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Título e Fechar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Iniciar Novo Trabalho",
                    style = MaterialTheme.typography.titleMedium,
                    color = TempoTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TempoTextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            // ─── 1. Seleção de Cliente ─────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CLIENTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TempoTextMuted,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    if (clients.size > 4) {
                        Text(
                            text = "${filteredClients.size} disponíveis",
                            style = MaterialTheme.typography.labelSmall,
                            color = TempoTextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredClients) { client ->
                        val isSelected = client.id == selectedClientId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) TempoAccent.copy(alpha = 0.2f) else TempoSurface2)
                                .clickable {
                                    selectedClientId = client.id
                                    selectedProjectId = null
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = client.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) TempoAccent else TempoTextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // ─── 2. Projetos do Cliente (se houver) ───────────────────────────
            if (clientProjects.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "PROJETO",
                        style = MaterialTheme.typography.labelSmall,
                        color = TempoTextMuted,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            val isNone = selectedProjectId == null
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isNone) TempoSurface3 else TempoSurface2)
                                    .clickable { selectedProjectId = null }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Geral",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isNone) TempoTextPrimary else TempoTextMuted
                                )
                            }
                        }
                        items(clientProjects) { project ->
                            val isSelected = project.id == selectedProjectId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) TempoAccent.copy(alpha = 0.2f) else TempoSurface2)
                                    .clickable { selectedProjectId = project.id }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) TempoAccent else TempoTextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // ─── 3. Atividades (Categorias) com atalho de gerenciar ───────────
            if (activities.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ATIVIDADE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TempoTextMuted,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onManageActivities() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Gerenciar",
                                tint = TempoAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Editar lista",
                                style = MaterialTheme.typography.labelSmall,
                                color = TempoAccent,
                                fontSize = 11.sp
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(activities) { act ->
                            val isSelected = act.id == selectedActivityId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) TempoSurface3 else TempoSurface2)
                                    .clickable {
                                        selectedActivityId = act.id
                                        billable = act.defaultBillable
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = act.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) TempoTextPrimary else TempoTextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // ─── 4. Descrição (1 Linha Compacta) ──────────────────────────────
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("O que você está fazendo? (opcional)", color = TempoTextMuted, fontSize = 13.sp) },
                singleLine = true,
                maxLines = 1,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TempoAccent,
                    unfocusedBorderColor = TempoOutline,
                    focusedTextColor = TempoTextPrimary,
                    unfocusedTextColor = TempoTextPrimary
                ),
                shape = TempoRadius.shapeSm
            )

            // ─── 5. Faturável e Taxa Efetiva (Slim Row) ──────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TempoRadius.shapeSm)
                    .background(TempoSurface2)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (effectiveRate > 0.0) "Faturável • ${FormatUtils.formatCurrency(effectiveRate)}/h" else "Trabalho Faturável",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (billable) TempoAccent else TempoTextMuted,
                    fontWeight = FontWeight.Medium
                )

                Switch(
                    checked = billable,
                    onCheckedChange = { billable = it },
                    modifier = Modifier.scale(0.75f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TempoAccent,
                        uncheckedThumbColor = TempoTextMuted,
                        uncheckedTrackColor = TempoSurface3
                    )
                )
            }

            // ─── 6. Botão Iniciar Ação Principal ─────────────────────────────
            TempoPrimaryAction(
                text = "Iniciar Cronômetro",
                onClick = {
                    if (selectedClientId > 0L) {
                        onStart(
                            selectedClientId,
                            selectedProjectId,
                            selectedActivityId,
                            description.trim(),
                            billable,
                            tag.trim()
                        )
                    }
                },
                icon = Icons.Default.PlayArrow,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                enabled = selectedClientId > 0L
            )
        }
    }
}
