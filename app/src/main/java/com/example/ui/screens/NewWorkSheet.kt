package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Activity
import com.example.data.Client
import com.example.data.Project
import com.example.ui.theme.*
import com.example.utils.FormatUtils

/**
 * Tela 2 - Novo Trabalho (Bottom Sheet contextual de início rápido)
 * Especificação do Guia de Redesign v2 - Seção 7 e Prompt 4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewWorkSheet(
    clients: List<Client>,
    projects: List<Project>,
    activities: List<Activity>,
    onDismiss: () -> Unit,
    onStart: (clientId: Long, projectId: Long?, activityId: Long?, description: String, billable: Boolean, tag: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedClientId by remember { mutableLongStateOf(clients.firstOrNull()?.id ?: 0L) }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var selectedActivityId by remember { mutableStateOf<Long?>(activities.firstOrNull()?.id) }
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
        containerColor = TempoSurface1,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = TempoSpacing.space2)
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
                .padding(horizontal = TempoSpacing.space4)
                .padding(bottom = TempoSpacing.space5),
            verticalArrangement = Arrangement.spacedBy(TempoSpacing.space4)
        ) {
            // Título e Fechar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Novo Trabalho",
                    style = MaterialTheme.typography.titleLarge,
                    color = TempoTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TempoTextSecondary)
                }
            }

            // ─── 1. Busca e Seleção de Cliente ────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar cliente...", color = TempoTextMuted) },
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

            // Chips horizontais de clientes
            LazyRow(horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2)) {
                items(filteredClients) { client ->
                    val isSelected = client.id == selectedClientId
                    Box(
                        modifier = Modifier
                            .clip(TempoRadius.shapeSm)
                            .background(if (isSelected) TempoSurface3 else TempoSurface2)
                            .tempoMaterialHighlight(TempoRadius.shapeSm)
                            .clickable {
                                selectedClientId = client.id
                                selectedProjectId = null
                            }
                            .padding(horizontal = TempoSpacing.space3, vertical = TempoSpacing.space2)
                    ) {
                        Text(
                            text = client.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) TempoAccent else TempoTextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // ─── 2. Projetos do Cliente (se houver) ───────────────────────────
            if (clientProjects.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(TempoSpacing.space1)) {
                    Text(
                        text = "PROJETO",
                        style = MaterialTheme.typography.labelSmall,
                        color = TempoTextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2)) {
                        item {
                            val isNone = selectedProjectId == null
                            Box(
                                modifier = Modifier
                                    .clip(TempoRadius.shapeSm)
                                    .background(if (isNone) TempoSurface3 else TempoSurface2)
                                    .clickable { selectedProjectId = null }
                                    .padding(horizontal = TempoSpacing.space3, vertical = TempoSpacing.space2)
                            ) {
                                Text(
                                    text = "Sem projeto direto",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isNone) TempoTextPrimary else TempoTextMuted
                                )
                            }
                        }
                        items(clientProjects) { project ->
                            val isSelected = project.id == selectedProjectId
                            Box(
                                modifier = Modifier
                                    .clip(TempoRadius.shapeSm)
                                    .background(if (isSelected) TempoSurface3 else TempoSurface2)
                                    .clickable { selectedProjectId = project.id }
                                    .padding(horizontal = TempoSpacing.space3, vertical = TempoSpacing.space2)
                            ) {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) TempoAccent else TempoTextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // ─── 3. Atividades (Categorias) ───────────────────────────────────
            if (activities.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(TempoSpacing.space1)) {
                    Text(
                        text = "ATIVIDADE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TempoTextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2)) {
                        items(activities) { act ->
                            val isSelected = act.id == selectedActivityId
                            Box(
                                modifier = Modifier
                                    .clip(TempoRadius.shapeSm)
                                    .background(if (isSelected) TempoSurface3 else TempoSurface2)
                                    .clickable {
                                        selectedActivityId = act.id
                                        billable = act.defaultBillable
                                    }
                                    .padding(horizontal = TempoSpacing.space3, vertical = TempoSpacing.space2)
                            ) {
                                Text(
                                    text = act.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) TempoTextPrimary else TempoTextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // ─── 4. Descrição (opcional, 1 linha que expande) ─────────────────
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Descrição da tarefa (opcional)...", color = TempoTextMuted) },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TempoAccent,
                    unfocusedBorderColor = TempoOutline,
                    focusedTextColor = TempoTextPrimary,
                    unfocusedTextColor = TempoTextPrimary
                ),
                shape = TempoRadius.shapeSm
            )

            // ─── 5. Faturável e Taxa Efetiva ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TempoRadius.shapeSm)
                    .background(TempoSurface2)
                    .padding(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space3),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Trabalho Faturável",
                        style = MaterialTheme.typography.labelLarge,
                        color = TempoTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (effectiveRate > 0.0) "Taxa: ${FormatUtils.formatCurrency(effectiveRate)}/h" else "Sem taxa definida",
                        style = MaterialTheme.typography.bodySmall,
                        color = TempoTextSecondary
                    )
                }

                Switch(
                    checked = billable,
                    onCheckedChange = { billable = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TempoAccent,
                        uncheckedThumbColor = TempoTextMuted,
                        uncheckedTrackColor = TempoSurface3
                    )
                )
            }

            // ─── Botão Iniciar Ação Principal ────────────────────────────────
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
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedClientId > 0L
            )
        }
    }
}
