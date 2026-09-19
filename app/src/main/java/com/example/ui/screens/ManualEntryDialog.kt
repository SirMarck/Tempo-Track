package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Activity
import com.example.data.Client
import com.example.data.Project
import com.example.ui.theme.*
import com.example.utils.FormatUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Diálogo de Lançamento Manual de Sessão com os novos tokens e modelo de dados.
 * Guia de Redesign v2 - Fase 5
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManualEntryDialog(
    clients: List<Client>,
    projects: List<Project> = emptyList(),
    activities: List<Activity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        clientId: Long,
        projectId: Long?,
        activityId: Long?,
        startTime: Long,
        endTime: Long,
        description: String,
        billable: Boolean,
        discountValue: Double,
        discountPercentage: Double,
        tag: String
    ) -> Unit
) {
    if (clients.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Nenhum cliente cadastrado", color = TempoTextPrimary) },
            text = { Text("Cadastre um cliente na aba Projetos antes de lançar horas manuais.", color = TempoTextSecondary) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("OK", color = TempoAccent)
                }
            },
            containerColor = TempoSurface2
        )
        return
    }

    val context = LocalContext.current

    var selectedClientId by remember { mutableStateOf(clients.first().id) }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var selectedActivityId by remember { mutableStateOf<Long?>(null) }
    var description by remember { mutableStateOf("") }
    var isBillable by remember { mutableStateOf(true) }
    var discountValInput by remember { mutableStateOf("") }
    var discountPctInput by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("") }

    val filteredProjects = remember(selectedClientId, projects) {
        projects.filter { it.clientId == selectedClientId && it.archivedAt == null }
    }

    // Gerenciamento de data e hora
    val startCalendar = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, -1)
        }
    }
    val endCalendar = remember { Calendar.getInstance() }

    var startTimeMillis by remember { mutableLongStateOf(startCalendar.timeInMillis) }
    var endTimeMillis by remember { mutableLongStateOf(endCalendar.timeInMillis) }

    val dateFormatted = remember(startTimeMillis) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(startTimeMillis))
    }
    val startFormatted = remember(startTimeMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(startTimeMillis))
    }
    val endFormatted = remember(endTimeMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(endTimeMillis))
    }

    val durationMillis = maxOf(0L, endTimeMillis - startTimeMillis)
    val durationText = FormatUtils.formatDuration(durationMillis)

    val selectedClient = clients.find { it.id == selectedClientId }
    val effectiveRate = remember(selectedClient, selectedProjectId, filteredProjects) {
        val proj = filteredProjects.find { it.id == selectedProjectId }
        FormatUtils.resolveEffectiveRate(proj, selectedClient, 0.0)
    }

    val tags = listOf("#Dev", "#Reunião", "#Design", "#Suporte", "#Consultoria", "#Operacional")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TempoBgBase.copy(alpha = 0.85f))
                .padding(TempoSpacing.space4),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(TempoRadius.shapeLg)
                    .background(TempoSurface2)
                    .tempoMaterialHighlight(TempoRadius.shapeLg)
                    .padding(TempoSpacing.space5)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Top Bar do Modal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lançar Trabalho Manual",
                            style = MaterialTheme.typography.titleMedium,
                            color = TempoTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = TempoTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(TempoSpacing.space4))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(TempoSpacing.space4)
                    ) {
                        // 1. Seleção de Cliente
                        item {
                            Text(
                                text = "CLIENTE *",
                                style = MaterialTheme.typography.labelSmall,
                                color = TempoTextMuted,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(TempoSpacing.space2))
                            Column(verticalArrangement = Arrangement.spacedBy(TempoSpacing.space2)) {
                                clients.forEach { client ->
                                    val isSelected = client.id == selectedClientId
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(TempoRadius.shapeSm)
                                            .background(if (isSelected) TempoSurface3 else TempoSurface1)
                                            .clickable {
                                                selectedClientId = client.id
                                                selectedProjectId = null
                                            }
                                            .padding(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space3),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = client.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) TempoAccent else TempoTextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = FormatUtils.formatCurrency(client.hourlyRate) + "/h",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = TempoMono),
                                            color = TempoTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Projeto (se houver)
                        if (filteredProjects.isNotEmpty()) {
                            item {
                                Text(
                                    text = "PROJETO (OPCIONAL)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TempoTextMuted,
                                    letterSpacing = 0.8.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(TempoSpacing.space2))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2)
                                ) {
                                    filteredProjects.forEach { proj ->
                                        val isSelected = proj.id == selectedProjectId
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedProjectId = if (isSelected) null else proj.id
                                            },
                                            label = { Text(proj.name) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TempoAccent,
                                                selectedLabelColor = TempoBgBase,
                                                containerColor = TempoSurface1,
                                                labelColor = TempoTextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Atividades (se houver)
                        if (activities.isNotEmpty()) {
                            item {
                                Text(
                                    text = "ATIVIDADE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TempoTextMuted,
                                    letterSpacing = 0.8.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(TempoSpacing.space2))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    activities.forEach { act ->
                                        val isSelected = act.id == selectedActivityId
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedActivityId = if (isSelected) null else act.id
                                            },
                                            label = { Text(act.name) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TempoAccent,
                                                selectedLabelColor = TempoBgBase,
                                                containerColor = TempoSurface1,
                                                labelColor = TempoTextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Data e Horários
                        item {
                            Text(
                                text = "DATA E HORÁRIOS",
                                style = MaterialTheme.typography.labelSmall,
                                color = TempoTextMuted,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(TempoSpacing.space2))

                            // Seletor de Data
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(TempoRadius.shapeSm)
                                    .background(TempoSurface1)
                                    .clickable {
                                        startCalendar.timeInMillis = startTimeMillis
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                startCalendar.set(y, m, d)
                                                endCalendar.set(y, m, d)
                                                startTimeMillis = startCalendar.timeInMillis
                                                endTimeMillis = endCalendar.timeInMillis
                                            },
                                            startCalendar.get(Calendar.YEAR),
                                            startCalendar.get(Calendar.MONTH),
                                            startCalendar.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                                    .padding(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space3),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space3)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TempoAccent, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Data: $dateFormatted",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TempoTextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(TempoSpacing.space2))

                            // Seletores de Início e Término
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space3)
                            ) {
                                // Início
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(TempoRadius.shapeSm)
                                        .background(TempoSurface1)
                                        .clickable {
                                            startCalendar.timeInMillis = startTimeMillis
                                            TimePickerDialog(
                                                context,
                                                { _, h, m ->
                                                    startCalendar.set(Calendar.HOUR_OF_DAY, h)
                                                    startCalendar.set(Calendar.MINUTE, m)
                                                    startTimeMillis = startCalendar.timeInMillis
                                                    if (endTimeMillis < startTimeMillis) {
                                                        endTimeMillis = startTimeMillis + 3600000L
                                                    }
                                                },
                                                startCalendar.get(Calendar.HOUR_OF_DAY),
                                                startCalendar.get(Calendar.MINUTE),
                                                true
                                            ).show()
                                        }
                                        .padding(horizontal = TempoSpacing.space3, vertical = TempoSpacing.space3),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = TempoTextSecondary, modifier = Modifier.size(16.dp))
                                    Column {
                                        Text("Início", style = MaterialTheme.typography.labelSmall, color = TempoTextMuted)
                                        Text(startFormatted, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TempoMono), color = TempoTextPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Término
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(TempoRadius.shapeSm)
                                        .background(TempoSurface1)
                                        .clickable {
                                            endCalendar.timeInMillis = endTimeMillis
                                            TimePickerDialog(
                                                context,
                                                { _, h, m ->
                                                    endCalendar.set(Calendar.HOUR_OF_DAY, h)
                                                    endCalendar.set(Calendar.MINUTE, m)
                                                    endTimeMillis = endCalendar.timeInMillis
                                                },
                                                endCalendar.get(Calendar.HOUR_OF_DAY),
                                                endCalendar.get(Calendar.MINUTE),
                                                true
                                            ).show()
                                        }
                                        .padding(horizontal = TempoSpacing.space3, vertical = TempoSpacing.space3),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = TempoTextSecondary, modifier = Modifier.size(16.dp))
                                    Column {
                                        Text("Término", style = MaterialTheme.typography.labelSmall, color = TempoTextMuted)
                                        Text(endFormatted, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TempoMono), color = TempoTextPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(TempoSpacing.space2))

                            // Resumo de Duração calculada
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(TempoRadius.shapeSm)
                                    .background(TempoSurface3)
                                    .padding(horizontal = TempoSpacing.space4, vertical = TempoSpacing.space2),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Duração Total:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TempoTextSecondary
                                )
                                Text(
                                    text = durationText,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TempoMono),
                                    color = TempoAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 5. Faturamento e Descrição
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Horas Faturáveis",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TempoTextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isBillable) "Incluir no cálculo financeiro" else "Não contabilizar valor",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TempoTextSecondary
                                    )
                                }
                                Switch(
                                    checked = isBillable,
                                    onCheckedChange = { isBillable = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TempoAccent,
                                        checkedTrackColor = TempoSurface3,
                                        uncheckedThumbColor = TempoTextMuted,
                                        uncheckedTrackColor = TempoSurface1
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(TempoSpacing.space3))

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Descrição das tarefas realizadas") },
                                placeholder = { Text("Ex: Implementação da API de pagamentos") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TempoAccent,
                                    unfocusedBorderColor = TempoOutline,
                                    focusedTextColor = TempoTextPrimary,
                                    unfocusedTextColor = TempoTextPrimary,
                                    focusedLabelColor = TempoAccent,
                                    unfocusedLabelColor = TempoTextSecondary,
                                    cursorColor = TempoAccent
                                )
                            )
                        }

                        // 6. Tags rápidas
                        item {
                            Text(
                                text = "TAG",
                                style = MaterialTheme.typography.labelSmall,
                                color = TempoTextMuted,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(TempoSpacing.space2))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                tags.forEach { tag ->
                                    val isSelected = selectedTag == tag
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedTag = if (isSelected) "" else tag },
                                        label = { Text(tag) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TempoSurface3,
                                            selectedLabelColor = TempoAccent,
                                            containerColor = TempoSurface1,
                                            labelColor = TempoTextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(TempoSpacing.space3))

                    // Botão de Gravar Lançamento
                    TempoPrimaryAction(
                        text = "Salvar Lançamento Manual",
                        onClick = {
                            val discVal = discountValInput.toDoubleOrNull() ?: 0.0
                            val discPct = discountPctInput.toDoubleOrNull() ?: 0.0
                            onSave(
                                selectedClientId,
                                selectedProjectId,
                                selectedActivityId,
                                startTimeMillis,
                                endTimeMillis,
                                description.trim(),
                                isBillable,
                                discVal,
                                discPct,
                                selectedTag
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
