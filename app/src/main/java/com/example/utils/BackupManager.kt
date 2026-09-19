package com.example.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.Client
import com.example.data.Session
import com.example.data.TimeTrackerRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class BackupResult {
    data class Success(
        val clientCount: Int,
        val sessionCount: Int,
        val companyName: String,
        val companyCnpj: String,
        val closingDay: Int
    ) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

object BackupManager {

    fun exportBackup(
        context: Context,
        clients: List<Client>,
        sessions: List<Session>
    ): File? {
        return try {
            val sharedPrefs = context.getSharedPreferences("time_tracker_prefs", Context.MODE_PRIVATE)
            val compName = sharedPrefs.getString("company_name", "") ?: ""
            val compCnpj = sharedPrefs.getString("company_cnpj", "") ?: ""
            val compPhone = sharedPrefs.getString("company_phone", "") ?: ""
            val compEmail = sharedPrefs.getString("company_email", "") ?: ""
            val compPix = sharedPrefs.getString("company_pix", "") ?: ""
            val closingDay = sharedPrefs.getInt("closing_day", 1)

            val root = JSONObject()
            root.put("version", 2)
            root.put("app", "TempoTrack")
            root.put("exportDate", System.currentTimeMillis())
            root.put("exportDateFormatted", SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date()))
            
            val companyObj = JSONObject().apply {
                put("name", compName)
                put("cnpj", compCnpj)
                put("phone", compPhone)
                put("email", compEmail)
                put("pix", compPix)
                put("closingDay", closingDay)
            }
            root.put("company", companyObj)

            val clientsArr = JSONArray()
            for (c in clients) {
                val cObj = JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("hourlyRate", c.hourlyRate)
                    put("currency", c.currency)
                    put("notes", c.notes ?: JSONObject.NULL)
                    put("archivedAt", c.archivedAt ?: JSONObject.NULL)
                    put("createdAt", c.createdAt)
                    put("updatedAt", c.updatedAt)
                }
                clientsArr.put(cObj)
            }
            root.put("clients", clientsArr)

            val sessionsArr = JSONArray()
            for (s in sessions) {
                val sObj = JSONObject().apply {
                    put("id", s.id)
                    put("clientId", s.clientId)
                    put("startTime", s.startTime)
                    put("endTime", if (s.endTime != null) s.endTime else JSONObject.NULL)
                    put("description", s.description)
                    put("isPaused", s.isPaused)
                    put("lastPausedTime", if (s.lastPausedTime != null) s.lastPausedTime else JSONObject.NULL)
                    put("pausedDuration", s.pausedDuration)
                    put("pauseEvents", s.pauseEvents)
                    put("discountValue", s.discountValue)
                    put("discountPercentage", s.discountPercentage)
                    put("tag", s.tag)
                    put("projectId", if (s.projectId != null) s.projectId else JSONObject.NULL)
                    put("activityId", if (s.activityId != null) s.activityId else JSONObject.NULL)
                    put("billable", s.billable)
                    put("appliedRate", s.appliedRate)
                    put("status", s.status)
                    put("source", s.source)
                    put("financialStatus", s.financialStatus)
                    put("closingBatchId", if (s.closingBatchId != null) s.closingBatchId else JSONObject.NULL)
                }
                sessionsArr.put(sObj)
            }
            root.put("sessions", sessionsArr)

            val backupsDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(backupsDir, "TempoTrack_Backup_$timeStamp.json")
            FileOutputStream(file).use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareBackup(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Backup de Dados - TempoTrack")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar Backup JSON"))
    }

    fun parseAndRestore(
        context: Context,
        jsonString: String,
        repository: TimeTrackerRepository,
        onComplete: (BackupResult) -> Unit
    ) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("clients") || !root.has("sessions")) {
                onComplete(BackupResult.Error("Formato de backup inválido (chaves ausentes)."))
                return
            }

            // Restore Company Settings
            val companyObj = root.optJSONObject("company")
            val compName = companyObj?.optString("name", "") ?: ""
            val compCnpj = companyObj?.optString("cnpj", "") ?: ""
            val compPhone = companyObj?.optString("phone", "") ?: ""
            val compEmail = companyObj?.optString("email", "") ?: ""
            val compPix = companyObj?.optString("pix", "") ?: ""
            val closingDay = companyObj?.optInt("closingDay", 1) ?: 1

            val sharedPrefs = context.getSharedPreferences("time_tracker_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putString("company_name", compName)
                .putString("company_cnpj", compCnpj)
                .putString("company_phone", compPhone)
                .putString("company_email", compEmail)
                .putString("company_pix", compPix)
                .putInt("closing_day", closingDay)
                .apply()

            // Restore Clients
            val clientsArr = root.getJSONArray("clients")
            val parsedClients = mutableListOf<Client>()
            val clientRateMap = mutableMapOf<Long, Double>()
            for (i in 0 until clientsArr.length()) {
                val cObj = clientsArr.getJSONObject(i)
                val clientId = cObj.optLong("id", 0)
                val hourlyRate = cObj.getDouble("hourlyRate")
                clientRateMap[clientId] = hourlyRate
                parsedClients.add(
                    Client(
                        id = clientId,
                        name = cObj.getString("name"),
                        hourlyRate = hourlyRate,
                        currency = cObj.optString("currency", "BRL"),
                        notes = if (cObj.isNull("notes")) null else cObj.optString("notes"),
                        archivedAt = if (cObj.isNull("archivedAt")) null else cObj.optLong("archivedAt"),
                        createdAt = cObj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = cObj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // Restore Sessions
            val sessionsArr = root.getJSONArray("sessions")
            val parsedSessions = mutableListOf<Session>()
            for (i in 0 until sessionsArr.length()) {
                val sObj = sessionsArr.getJSONObject(i)
                val clientId = sObj.getLong("clientId")
                val endTime = if (sObj.isNull("endTime")) null else sObj.getLong("endTime")
                val lastPaused = if (sObj.isNull("lastPausedTime")) null else sObj.getLong("lastPausedTime")

                // Preserva appliedRate ou faz snapshot a partir da taxa do cliente
                val rate = if (sObj.has("appliedRate") && !sObj.isNull("appliedRate")) {
                    sObj.getDouble("appliedRate")
                } else {
                    clientRateMap[clientId] ?: 0.0
                }

                val status = sObj.optString("status", if (endTime != null) "completed" else "running")
                val billable = sObj.optBoolean("billable", true)
                val financialStatus = sObj.optString("financialStatus", "unbilled")
                val projectId = if (sObj.has("projectId") && !sObj.isNull("projectId")) sObj.getLong("projectId") else null
                val activityId = if (sObj.has("activityId") && !sObj.isNull("activityId")) sObj.getLong("activityId") else null
                val closingBatchId = if (sObj.has("closingBatchId") && !sObj.isNull("closingBatchId")) sObj.getLong("closingBatchId") else null

                parsedSessions.add(
                    Session(
                        id = sObj.optLong("id", 0),
                        clientId = clientId,
                        startTime = sObj.getLong("startTime"),
                        endTime = endTime,
                        description = sObj.optString("description", ""),
                        isPaused = sObj.optBoolean("isPaused", false),
                        lastPausedTime = lastPaused,
                        pausedDuration = sObj.optLong("pausedDuration", 0L),
                        pauseEvents = sObj.optString("pauseEvents", ""),
                        discountValue = sObj.optDouble("discountValue", 0.0),
                        discountPercentage = sObj.optDouble("discountPercentage", 0.0),
                        tag = sObj.optString("tag", ""),
                        projectId = projectId,
                        activityId = activityId,
                        billable = billable,
                        appliedRate = rate,
                        status = status,
                        source = sObj.optString("source", "timer"),
                        financialStatus = financialStatus,
                        closingBatchId = closingBatchId
                    )
                )
            }

            CoroutineScope(Dispatchers.IO).launch {
                for (client in parsedClients) {
                    repository.insertClient(client)
                }
                for (session in parsedSessions) {
                    repository.insertSession(session)
                }
                withContext(Dispatchers.Main) {
                    onComplete(
                        BackupResult.Success(
                            clientCount = parsedClients.size,
                            sessionCount = parsedSessions.size,
                            companyName = compName,
                            companyCnpj = compCnpj,
                            closingDay = closingDay
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(BackupResult.Error("Falha ao processar arquivo JSON: ${e.localizedMessage}"))
        }
    }
}
