package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.utils.BackupResult
import com.example.viewmodel.TimeTrackerViewModel

@Composable
fun CompanySettingsDialog(
    onDismiss: () -> Unit,
    viewModel: TimeTrackerViewModel? = null
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("time_tracker_prefs", Context.MODE_PRIVATE) }

    var companyName by remember { mutableStateOf(sharedPrefs.getString("company_name", "") ?: "") }
    var companyCnpj by remember { mutableStateOf(sharedPrefs.getString("company_cnpj", "") ?: "") }
    var closingDay by remember { mutableStateOf(sharedPrefs.getInt("closing_day", 1)) }
    var closingDayStr by remember { mutableStateOf(closingDay.toString()) }

    // Feedback state for backup/restore operations
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    // File picker for restore
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && viewModel != null) {
            viewModel.restoreBackup(context, uri) { result ->
                feedbackMessage = when (result) {
                    is BackupResult.Success ->
                        "✅ Backup restaurado: ${result.clientCount} clientes, ${result.sessionCount} sessões."
                    is BackupResult.Error ->
                        "❌ Erro: ${result.message}"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurações da Empresa", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Insira os dados da sua empresa para que sejam impressos automaticamente no rodapé de cada relatório exportado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Nome da Empresa") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = companyCnpj,
                    onValueChange = { companyCnpj = it },
                    label = { Text("CNPJ") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = closingDayStr,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            closingDayStr = newValue
                            val parsed = newValue.toIntOrNull()
                            if (parsed != null && parsed in 1..31) {
                                closingDay = parsed
                            }
                        }
                    },
                    label = { Text("Dia de Fechamento do Mês (1-31)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // ─── Backup / Restore ────────────────────────────────────
                if (viewModel != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Text(
                        "Backup de Dados",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Export button
                        OutlinedButton(
                            onClick = { viewModel.exportAndShareBackup(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Upload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exportar", style = MaterialTheme.typography.labelMedium)
                        }

                        // Restore button
                        OutlinedButton(
                            onClick = { restoreLauncher.launch("application/json") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restaurar", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    feedbackMessage?.let { msg ->
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (msg.startsWith("✅"))
                                MaterialTheme.colorScheme.secondary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    sharedPrefs.edit()
                        .putString("company_name", companyName)
                        .putString("company_cnpj", companyCnpj)
                        .putInt("closing_day", closingDay)
                        .apply()
                    onDismiss()
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
