package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.Client
import com.example.utils.FormatUtils
import com.example.viewmodel.TimeTrackerViewModel
import kotlinx.coroutines.launch

import com.example.ui.theme.luxBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(viewModel: TimeTrackerViewModel) {
    val clients by viewModel.clients.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var clientToDelete by remember { mutableStateOf<Client?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Meus Clientes") },
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Cliente")
            }
        }
    ) { paddingValues ->
        if (clients.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Nenhum cliente cadastrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
                items(clients) { client ->
                    ClientItem(client, onDelete = { clientToDelete = client })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (clientToDelete != null) {
            AlertDialog(
                onDismissRequest = { clientToDelete = null },
                title = { Text("Excluir Cliente", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                text = { Text("Tem certeza que deseja excluir o cliente \"${clientToDelete?.name}\"? Todos os registros de horas associados serão mantidos ou excluídos dependendo do banco de dados, mas não poderão ser associados novamente.") },
                confirmButton = {
                    Button(
                        onClick = {
                            clientToDelete?.let { viewModel.deleteClient(it.id) }
                            clientToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Excluir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { clientToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showAddDialog) {
            AddClientDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, rate ->
                    viewModel.addClient(name, rate)
                    showAddDialog = false
                }
            )
        }

        if (showSettingsDialog) {
            CompanySettingsDialog(onDismiss = { showSettingsDialog = false })
        }

        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }
    }
}

@Composable
fun ClientItem(client: Client, onDelete: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .luxBorder(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column {
                Text(client.name, style = MaterialTheme.typography.titleMedium)
                Text("Valor/hora: ${FormatUtils.formatCurrency(client.hourlyRate)}", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remover Cliente", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddClientDialog(onDismiss: () -> Unit, onAdd: (String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var rateStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Cliente") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Cliente") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = rateStr,
                    onValueChange = { rateStr = it },
                    label = { Text("Valor por Hora") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = rateStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) onAdd(name, rate)
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
