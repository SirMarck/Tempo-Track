package com.example.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateResult {
    object Downloading : UpdateResult()
    object AlreadyLatest : UpdateResult()
    object RepoNotFoundOrPrivate : UpdateResult()
    object RateLimitOrForbidden : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

object UpdateManager {

    // Substitua pelo seu repo no GitHub: "usuario/repositorio"
    private const val REPO_NAME = "SirMarck/Time-Counter"
    private const val GITHUB_API_URL = "https://api.github.com/repos/$REPO_NAME/releases/latest"

    suspend fun checkForUpdates(context: Context): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            val responseCode = connection.responseCode
            if (responseCode == 404) {
                return@withContext UpdateResult.RepoNotFoundOrPrivate
            }
            if (responseCode == 401 || responseCode == 403) {
                return@withContext UpdateResult.RateLimitOrForbidden
            }

            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonObject = JSONObject(response)
                
                val latestVersion = jsonObject.getString("tag_name").replace("v", "") // Ex: "1.0.1"
                val currentVersion = BuildConfig.VERSION_NAME

                if (isNewerVersion(currentVersion, latestVersion)) {
                    val assets = jsonObject.getJSONArray("assets")
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.getString("name")
                        if (name.endsWith(".apk")) {
                            val downloadUrl = asset.getString("browser_download_url")
                            withContext(Dispatchers.Main) {
                                startDownload(context, downloadUrl, latestVersion)
                            }
                            return@withContext UpdateResult.Downloading
                        }
                    }
                } else {
                    return@withContext UpdateResult.AlreadyLatest
                }
            } else {
                return@withContext UpdateResult.Error("Código HTTP: $responseCode")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext UpdateResult.Error(e.localizedMessage ?: "Erro de conexão")
        }
        return@withContext UpdateResult.AlreadyLatest
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        
        for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
            val c = currentParts.getOrNull(i) ?: 0
            val l = latestParts.getOrNull(i) ?: 0
            if (l > c) return true
            if (c > l) return false
        }
        return false
    }

    private fun startDownload(context: Context, url: String, version: String) {
        Toast.makeText(context, "Baixando atualização v$version...", Toast.LENGTH_LONG).show()
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("TempoTrack Update")
            .setDescription("Baixando nova versão v$version")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "TempoTrack_Update.apk")

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val enqueueId = manager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == enqueueId) {
                    val query = DownloadManager.Query().setFilterById(enqueueId)
                    val cursor = manager.query(query)
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (statusIndex >= 0 && cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                            val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            if (uriIndex >= 0) {
                                val uriString = cursor.getString(uriIndex)
                                installApk(context, Uri.parse(uriString))
                            }
                        }
                    }
                    cursor.close()
                    context.unregisterReceiver(this)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(context: Context, apkUri: Uri) {
        try {
            val file = File(apkUri.path ?: return)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao tentar instalar a atualização.", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleUpdateResult(context: Context, result: UpdateResult) {
        when (result) {
            is UpdateResult.Downloading -> {
                // Já inicia e mostra o Toast "Baixando..."
            }
            is UpdateResult.AlreadyLatest -> {
                Toast.makeText(context, "Sua versão está atualizada!", Toast.LENGTH_SHORT).show()
            }
            is UpdateResult.RepoNotFoundOrPrivate -> {
                Toast.makeText(context, "Erro: Repositório privado ou inexistente. Altere o repo para Público no GitHub para checar atualizações.", Toast.LENGTH_LONG).show()
            }
            is UpdateResult.RateLimitOrForbidden -> {
                Toast.makeText(context, "Erro: Limite de requisições do GitHub excedido.", Toast.LENGTH_LONG).show()
            }
            is UpdateResult.Error -> {
                Toast.makeText(context, "Falha na busca: ${result.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
