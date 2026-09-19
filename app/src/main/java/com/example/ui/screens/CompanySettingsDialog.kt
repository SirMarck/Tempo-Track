package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.utils.BackupResult
import com.example.viewmodel.TimeTrackerViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun CompanySettingsDialog(
    onDismiss: () -> Unit,
    viewModel: TimeTrackerViewModel? = null
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("time_tracker_prefs", Context.MODE_PRIVATE) }

    var companyName by remember { mutableStateOf(sharedPrefs.getString("company_name", "") ?: "") }
    var companyCnpj by remember { mutableStateOf(sharedPrefs.getString("company_cnpj", "") ?: "") }
    var companyPhone by remember { mutableStateOf(sharedPrefs.getString("company_phone", "") ?: "") }
    var companyPix by remember { mutableStateOf(sharedPrefs.getString("company_pix", "") ?: "") }
    var companyEmail by remember { mutableStateOf(sharedPrefs.getString("company_email", "") ?: "") }
    var closingDay by remember { mutableStateOf(sharedPrefs.getInt("closing_day", 1)) }
    var closingDayStr by remember { mutableStateOf(closingDay.toString()) }

    val logoFile = remember { File(context.filesDir, "company_logo.png") }
    var logoBitmap by remember {
        mutableStateOf<Bitmap?>(
            if (logoFile.exists()) {
                try {
                    BitmapFactory.decodeFile(logoFile.absolutePath)
                } catch (e: Exception) {
                    null
                }
            } else null
        )
    }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(logoFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                sharedPrefs.edit().putBoolean("has_company_logo", true).apply()
                logoBitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeLogo() {
        if (logoFile.exists()) {
            logoFile.delete()
        }
        sharedPrefs.edit().putBoolean("has_company_logo", false).apply()
        logoBitmap = null
    }

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Insira os dados da sua empresa para que sejam impressos automaticamente no cabeçalho e rodapé de cada relatório exportado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ─── Seção da Logo da Empresa ────────────────────────────
                Text(
                    "Logotipo da Empresa",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (logoBitmap != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            bitmap = logoBitmap!!.asImageBitmap(),
                            contentDescription = "Logo da Empresa",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Fit
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(
                                onClick = { logoPickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Alterar Logo", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = { removeLogo() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remover Logo", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { logoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adicionar Logo da Empresa")
                    }
                }

                // ─── Campos Cadastrais da Empresa ────────────────────────
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
                    value = companyPhone,
                    onValueChange = { companyPhone = it },
                    label = { Text("Telefone / WhatsApp") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = companyEmail,
                    onValueChange = { companyEmail = it },
                    label = { Text("E-mail Comercial") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = companyPix,
                    onValueChange = { companyPix = it },
                    label = { Text("Chave Pix (para liquidação no relatório)") },
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
                        .putString("company_name", companyName.trim())
                        .putString("company_cnpj", companyCnpj.trim())
                        .putString("company_phone", companyPhone.trim())
                        .putString("company_pix", companyPix.trim())
                        .putString("company_email", companyEmail.trim())
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
