package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Activity
import com.example.data.Client
import com.example.data.Project
import com.example.data.Session
import com.example.ui.theme.*
import com.example.utils.FormatUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modal completo para edição detalhada de uma sessão/atividade de trabalho.
 * Permite editar Cliente, Projeto, Categoria/Atividade, Data, Horários de Início/Fim,
 * Descrição, Tag, Faturabilidade, Taxa e Descontos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullEditSessionDialog(
    session: Session,
    clients: List<Client>,
    projects: List<Project>,
    activities: List<Activity>,
    onDismiss: () -> Unit,
    onSave: (Session) -> Unit
) {
    val context = LocalContext.current

    var selectedClientId by remember { mutableLongStateOf(session.clientId) }
    var selectedProjectId by remember { mutableStateOf<Long?>(session.projectId) }
    var selectedActivityId by remember { mutableStateOf<Long?>(session.activityId) }

    val selectedClient = remember(selectedClientId, clients) {
        clients.find { it.id == selectedClientId }
    }
    val clientProjects = remember(selectedClientId, projects) {
        projects.filter { it.clientId == selectedClientId && it.archivedAt == null }
    }
    val selectedProject = remember(selectedProjectId, projects) {
        projects.find { it.id == selectedProjectId }
    }

    // Timestamps
    var startTimeMillis by remember { mutableLongStateOf(session.startTime) }
    var endTimeMillis by remember { mutableLongStateOf(session.endTime ?: (session.startTime + 3600000L)) }

    // Textos e números
    var description by remember { mutableStateOf(session.description) }
    var tag by remember { mutableStateOf(session.tag) }
    var billable by remember { mutableStateOf(session.billable) }
    var appliedRateInput by remember {
        val initialRate = if (session.appliedRate > 0.0) session.appliedRate else (selectedClient?.hourlyRate ?: 0.0)
        mutableStateOf(if (initialRate > 0.0) initialRate.toString() else "")
    }
    var discountValInput by remember {
        mutableStateOf(if (session.discountValue > 0.0) session.discountValue.toString() else "")
    }
    var discountPctInput by remember {
        mutableStateOf(if (session.discountPercentage > 0.0) session.discountPercentage.toString() else "")
    }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val dateText = remember(startTimeMillis) { dateFormatter.format(Date(startTimeMillis)) }
    val startTimeText = remember(startTimeMillis) { timeFormatter.format(Date(startTimeMillis)) }
    val endTimeText = remember(endTimeMillis) { timeFormatter.format(Date(endTimeMillis)) }

    val durationMillis = maxOf(0L, endTimeMillis - startTimeMillis)
    val parsedRate = appliedRateInput.toDoubleOrNull() ?: selectedClient?.hourlyRate ?: 0.0
    val parsedDiscountVal = discountValInput.toDoubleOrNull() ?: 0.0
    val parsedDiscountPct = discountPctInput.toDoubleOrNull() ?: 0.0

    val estimatedEarnings = remember(durationMillis, parsedRate, billable, parsedDiscountVal, parsedDiscountPct) {
        if (!billable) 0.0
        else {
            val hours = durationMillis.toDouble() / (1000 * 60 * 60)
            val base = hours * parsedRate
            val discPct = base * (parsedDiscountPct / 100.0)
            maxOf(0.0, base - discPct - parsedDiscountVal)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TempoSurface1,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Editar Atividade",
                    style = MaterialTheme.typography.titleLarge,
                    color = TempoTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TempoTextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(TempoSpacing.space3)
            ) {
                // ─── 1. Cliente ──────────────────────────────────────────────
                Text(
                    text = "CLIENTE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TempoTextMuted,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(TempoSpacing.space2)) {
                    items(clients) { client ->
                        val isSelected = client.id == selectedClientId
                        Box(
                            modifier = Modifier
                                .clip(TempoRadius.shapeSm)
                                .background(if (isSelected) TempoSurface3 else TempoSurface2)
                                .tempoMaterialHighlight(TempoRadius.shapeSm)
                                .clickable {
                                    selectedClientId = client.id
                                    selectedProjectId = null
                                    if (appliedRateInput.isBlank() || appliedRateInput == "0.0") {
                                        appliedRateInput = client.hourlyRate.toString()
                                    }
                                }
                                .padding(horizontal = TempoSpacing.space3, vertical = TempoSpacing.space2)
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

                // ─── 2. Projeto ──────────────────────────────────────────────
                if (clientProjects.isNotEmpty()) {
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
                                    text = "Sem projeto",
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
                                    .clickable {
                                        selectedProjectId = project.id
                                        project.hourlyRate?.let { rate ->
                                            appliedRateInput = rate.toString()
                                        }
                                    }
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

                // ─── 3. Atividade ────────────────────────────────────────────
                if (activities.isNotEmpty()) {
                    Text(
                        text = "CATEGORIA / ATIVIDADE",
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
                                    .clickable { selectedActivityId = act.id }
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

                HorizontalDivider(color = TempoOutline)

                // ─── 4. Data e Horários ──────────────────────────────────────
                Text(
                    text = "DATA E HORÁRIOS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TempoTextMuted,
                    fontWeight = FontWeight.SemiBold
                )

                // Botão de Data
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = startTimeMillis }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val startCal = Calendar.getInstance().apply {
                                    timeInMillis = startTimeMillis
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                val endCal = Calendar.getInstance().apply {
                                    timeInMillis = endTimeMillis
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                startTimeMillis = startCal.timeInMillis
                                endTimeMillis = endCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = TempoRadius.shapeSm
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Data: $dateText", color = TempoTextPrimary)
                }

                // Início e Fim
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = startTimeMillis }
                            TimePickerDialog(
                                context,
                                { _, h, min ->
                                    val startCal = Calendar.getInstance().apply {
                                        timeInMillis = startTimeMillis
                                        set(Calendar.HOUR_OF_DAY, h)
                                        set(Calendar.MINUTE, min)
                                        set(Calendar.SECOND, 0)
                                    }
                                    startTimeMillis = startCal.timeInMillis
                                    if (endTimeMillis <= startTimeMillis) {
                                        endTimeMillis = startTimeMillis + 3600000L
                                    }
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = TempoRadius.shapeSm
                    ) {
                        Text("Início: $startTimeText", color = TempoTextPrimary)
                    }

                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = endTimeMillis }
                            TimePickerDialog(
                                context,
                                { _, h, min ->
                                    val endCal = Calendar.getInstance().apply {
                                        timeInMillis = endTimeMillis
                                        set(Calendar.HOUR_OF_DAY, h)
                                        set(Calendar.MINUTE, min)
                                        set(Calendar.SECOND, 0)
                                    }
                                    endTimeMillis = endCal.timeInMillis
                                    if (endTimeMillis < startTimeMillis) {
                                        startTimeMillis = endTimeMillis - 3600000L
                                    }
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = TempoRadius.shapeSm
                    ) {
                        Text("Fim: $endTimeText", color = TempoTextPrimary)
                    }
                }

                // Duração calculada e valor
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TempoRadius.shapeSm)
                        .background(TempoSurface2)
                        .padding(horizontal = TempoSpacing.space3, vertical = TempoSpacing.space2),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Duração: ${FormatUtils.formatDuration(durationMillis)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TempoMono),
                        color = TempoTextPrimary
                    )
                    Text(
                        text = FormatUtils.formatCurrency(estimatedEarnings),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = TempoMono),
                        color = TempoAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                // ─── 5. Descrição e Tag ──────────────────────────────────────
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição da atividade") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = TempoRadius.shapeSm,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TempoAccent,
                        unfocusedBorderColor = TempoOutline,
                        focusedTextColor = TempoTextPrimary,
                        unfocusedTextColor = TempoTextPrimary
                    )
                )

                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Tag / Marcador (ex: #design, #bugfix)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = TempoRadius.shapeSm,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TempoAccent,
                        unfocusedBorderColor = TempoOutline,
                        focusedTextColor = TempoTextPrimary,
                        unfocusedTextColor = TempoTextPrimary
                    )
                )

                // ─── 6. Faturável e Taxa ─────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trabalho Faturável",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TempoTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = billable,
                        onCheckedChange = { billable = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TempoAccent
                        )
                    )
                }

                if (billable) {
                    OutlinedTextField(
                        value = appliedRateInput,
                        onValueChange = { appliedRateInput = it },
                        label = { Text("Taxa Horária Aplicada (R$/h)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = TempoRadius.shapeSm,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TempoAccent,
                            unfocusedBorderColor = TempoOutline,
                            focusedTextColor = TempoTextPrimary,
                            unfocusedTextColor = TempoTextPrimary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = discountValInput,
                            onValueChange = { discountValInput = it },
                            label = { Text("Desconto (R$)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = TempoRadius.shapeSm,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TempoAccent,
                                unfocusedBorderColor = TempoOutline,
                                focusedTextColor = TempoTextPrimary,
                                unfocusedTextColor = TempoTextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = discountPctInput,
                            onValueChange = { discountPctInput = it },
                            label = { Text("Desconto (%)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = TempoRadius.shapeSm,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TempoAccent,
                                unfocusedBorderColor = TempoOutline,
                                focusedTextColor = TempoTextPrimary,
                                unfocusedTextColor = TempoTextPrimary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalRate = appliedRateInput.toDoubleOrNull() ?: selectedClient?.hourlyRate ?: 0.0
                    val finalDiscVal = discountValInput.toDoubleOrNull() ?: 0.0
                    val finalDiscPct = discountPctInput.toDoubleOrNull() ?: 0.0

                    val updatedSession = session.copy(
                        clientId = selectedClientId,
                        projectId = selectedProjectId,
                        activityId = selectedActivityId,
                        startTime = startTimeMillis,
                        endTime = endTimeMillis,
                        description = description.trim(),
                        tag = tag.trim(),
                        billable = billable,
                        appliedRate = finalRate,
                        discountValue = finalDiscVal,
                        discountPercentage = finalDiscPct
                    )
                    onSave(updatedSession)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TempoAccent),
                shape = TempoRadius.shapeSm
            ) {
                Text("Salvar Alterações", fontWeight = FontWeight.Bold, color = TempoBgBase)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TempoTextMuted)
            }
        }
    )
}
