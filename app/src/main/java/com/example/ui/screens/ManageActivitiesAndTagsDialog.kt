package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Activity
import com.example.ui.theme.*
import com.example.viewmodel.TimeTrackerViewModel

enum class ManageTab {
    ACTIVITIES, TAGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageActivitiesAndTagsDialog(
    viewModel: TimeTrackerViewModel,
    initialTab: ManageTab = ManageTab.ACTIVITIES,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadTags(context)
    }

    val activities by viewModel.activities.collectAsState()
    val tags by viewModel.tags.collectAsState()

    var activeTab by remember { mutableStateOf(initialTab) }

    // Estado para adicionar nova atividade
    var newActivityName by remember { mutableStateOf("") }
    var newActivityBillable by remember { mutableStateOf(true) }

    // Estado para adicionar nova tag
    var newTagName by remember { mutableStateOf("") }

    // Estado para editar atividade
    var activityToEdit by remember { mutableStateOf<Activity?>(null) }
    var editActivityName by remember { mutableStateOf("") }
    var editActivityBillable by remember { mutableStateOf(true) }

    // Estado para excluir atividade
    var activityToDelete by remember { mutableStateOf<Activity?>(null) }

    // Estado para editar tag
    var tagToEdit by remember { mutableStateOf<String?>(null) }
    var editTagName by remember { mutableStateOf("") }

    // Estado para excluir tag
    var tagToDelete by remember { mutableStateOf<String?>(null) }

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
                    .fillMaxHeight(0.85f)
                    .clip(TempoRadius.shapeLg)
                    .background(TempoSurface2)
                    .tempoMaterialHighlight(TempoRadius.shapeLg)
                    .padding(TempoSpacing.space4)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(TempoSpacing.space3)
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gerenciar Atividades & Tags",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TempoTextPrimary
                        )

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TempoTextSecondary)
                        }
                    }

                    // Seletor de Aba (Atividades / Tags)
                    TempoSegmentedFilter(
                        options = listOf(ManageTab.ACTIVITIES, ManageTab.TAGS),
                        selectedOption = activeTab,
                        onOptionSelected = { activeTab = it },
                        labelProvider = { tab ->
                            when (tab) {
                                ManageTab.ACTIVITIES -> "Atividades (${activities.size})"
                                ManageTab.TAGS -> "Tags (${tags.size})"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ══════════════════════════════════════════════════════════
                    // ABA 1: ATIVIDADES
                    // ══════════════════════════════════════════════════════════
                    if (activeTab == ManageTab.ACTIVITIES) {
                        // Linha de adição de nova atividade
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newActivityName,
                                onValueChange = { newActivityName = it },
                                placeholder = { Text("Nome da atividade...", fontSize = 13.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TempoAccent,
                                    unfocusedBorderColor = TempoOutline,
                                    focusedTextColor = TempoTextPrimary,
                                    unfocusedTextColor = TempoTextPrimary
                                )
                            )

                            Button(
                                onClick = {
                                    if (newActivityName.isNotBlank()) {
                                        viewModel.addActivity(newActivityName.trim(), newActivityBillable)
                                        newActivityName = ""
                                        Toast.makeText(context, "Atividade adicionada!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TempoAccent),
                                shape = TempoRadius.shapeSm,
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Criar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Lista de Atividades
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activities.isEmpty()) {
                                item {
                                    Text(
                                        "Nenhuma atividade cadastrada.",
                                        color = TempoTextMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )
                                }
                            } else {
                                items(activities, key = { it.id }) { act ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(TempoRadius.shapeSm)
                                            .background(TempoSurface1)
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = act.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TempoTextPrimary
                                            )
                                            Text(
                                                text = if (act.defaultBillable) "Faturável padrão" else "Não-faturável",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (act.defaultBillable) TempoAccent else TempoTextMuted
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    activityToEdit = act
                                                    editActivityName = act.name
                                                    editActivityBillable = act.defaultBillable
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Editar",
                                                    tint = TempoTextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { activityToDelete = act },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Excluir",
                                                    tint = TempoDanger,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ══════════════════════════════════════════════════════════
                    // ABA 2: TAGS
                    // ══════════════════════════════════════════════════════════
                    if (activeTab == ManageTab.TAGS) {
                        // Linha de adição de nova tag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newTagName,
                                onValueChange = { newTagName = it },
                                placeholder = { Text("Nova Tag (ex: #Urgente)...", fontSize = 13.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TempoAccent,
                                    unfocusedBorderColor = TempoOutline,
                                    focusedTextColor = TempoTextPrimary,
                                    unfocusedTextColor = TempoTextPrimary
                                )
                            )

                            Button(
                                onClick = {
                                    if (newTagName.isNotBlank()) {
                                        viewModel.addTag(context, newTagName)
                                        newTagName = ""
                                        Toast.makeText(context, "Tag adicionada!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TempoAccent),
                                shape = TempoRadius.shapeSm,
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Criar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Lista de Tags
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (tags.isEmpty()) {
                                item {
                                    Text(
                                        "Nenhuma tag cadastrada.",
                                        color = TempoTextMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )
                                }
                            } else {
                                items(tags, key = { it }) { tag ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(TempoRadius.shapeSm)
                                            .background(TempoSurface1)
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(TempoSurface3)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = tag,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = TempoAccent,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    tagToEdit = tag
                                                    editTagName = tag
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Editar",
                                                    tint = TempoTextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { tagToDelete = tag },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Excluir",
                                                    tint = TempoDanger,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal para Editar Atividade
    activityToEdit?.let { act ->
        AlertDialog(
            onDismissRequest = { activityToEdit = null },
            title = { Text("Editar Atividade", fontWeight = FontWeight.Bold, color = TempoTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editActivityName,
                        onValueChange = { editActivityName = it },
                        label = { Text("Nome da Atividade") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Faturável por padrão", color = TempoTextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = editActivityBillable,
                            onCheckedChange = { editActivityBillable = it }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editActivityName.isNotBlank()) {
                            viewModel.updateActivity(
                                act.copy(name = editActivityName.trim(), defaultBillable = editActivityBillable)
                            )
                            activityToEdit = null
                            Toast.makeText(context, "Atividade atualizada!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("SALVAR", color = TempoAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activityToEdit = null }) { Text("CANCELAR", color = TempoTextSecondary) }
            },
            containerColor = TempoSurface1
        )
    }

    // Modal para Confirmar Exclusão de Atividade
    activityToDelete?.let { act ->
        AlertDialog(
            onDismissRequest = { activityToDelete = null },
            title = { Text("Excluir Atividade", fontWeight = FontWeight.Bold, color = TempoTextPrimary) },
            text = { Text("Deseja realmente excluir permanentemente a atividade '${act.name}'?", color = TempoTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteActivity(act.id)
                        activityToDelete = null
                        Toast.makeText(context, "Atividade excluída!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("EXCLUIR", color = TempoDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activityToDelete = null }) { Text("CANCELAR", color = TempoTextSecondary) }
            },
            containerColor = TempoSurface1
        )
    }

    // Modal para Editar Tag
    tagToEdit?.let { oldTag ->
        AlertDialog(
            onDismissRequest = { tagToEdit = null },
            title = { Text("Editar Tag", fontWeight = FontWeight.Bold, color = TempoTextPrimary) },
            text = {
                OutlinedTextField(
                    value = editTagName,
                    onValueChange = { editTagName = it },
                    label = { Text("Nome da Tag") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editTagName.isNotBlank()) {
                            viewModel.updateTag(context, oldTag, editTagName.trim())
                            tagToEdit = null
                            Toast.makeText(context, "Tag atualizada!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("SALVAR", color = TempoAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToEdit = null }) { Text("CANCELAR", color = TempoTextSecondary) }
            },
            containerColor = TempoSurface1
        )
    }

    // Modal para Confirmar Exclusão de Tag
    tagToDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("Excluir Tag", fontWeight = FontWeight.Bold, color = TempoTextPrimary) },
            text = { Text("Deseja remover a tag '$tag' da lista de atalhos rápidos?", color = TempoTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTag(context, tag)
                        tagToDelete = null
                        Toast.makeText(context, "Tag removida!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("EXCLUIR", color = TempoDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) { Text("CANCELAR", color = TempoTextSecondary) }
            },
            containerColor = TempoSurface1
        )
    }
}
