package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.Client
import com.example.data.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument

object MonthlyReportManager {

    fun checkAndGenerateMonthlyReports(context: Context) {
        val sharedPrefs = context.getSharedPreferences("time_tracker_prefs", Context.MODE_PRIVATE)
        val closingDay = sharedPrefs.getInt("closing_day", 1)

        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) // 0 to 11
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)

        // Only run if we are on or after the closing day
        if (currentDay >= closingDay) {
            val cycleId = "${currentYear}_${currentMonth + 1}"
            val lastGeneratedCycle = sharedPrefs.getString("last_generated_cycle", "") ?: ""
            if (lastGeneratedCycle == cycleId) {
                // Already generated for this month
                return
            }

            // Set up start and end calendars
            val endCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, closingDay)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endTime = endCal.timeInMillis

            val startCal = Calendar.getInstance().apply {
                timeInMillis = endTime
                add(Calendar.MONTH, -1)
            }
            val startTime = startCal.timeInMillis

            // Reference month formatting for file naming
            val displayMonthName = if (closingDay == 1) {
                // If closing day is 1, the period is exactly the previous month
                SimpleDateFormat("MMMM yyyy", Locale("pt", "BR")).format(startCal.time).replaceFirstChar { it.uppercase() }
            } else {
                val shortFormat = SimpleDateFormat("dd_MM", Locale("pt", "BR"))
                "${shortFormat.format(startCal.time)}_a_${shortFormat.format(endCal.time)}_${currentYear}"
            }

            val displayPeriodTitle = if (closingDay == 1) {
                SimpleDateFormat("MMMM yyyy", Locale("pt", "BR")).format(startCal.time).replaceFirstChar { it.uppercase() }
            } else {
                val shortFormat = SimpleDateFormat("dd/MM", Locale("pt", "BR"))
                "${shortFormat.format(startCal.time)} a ${shortFormat.format(endCal.time)} de ${currentYear}"
            }

            // Launch database operation in background coroutine
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.timeTrackerDao()
                    
                    val allClients = dao.getAllClientsSync()
                    val allSessions = dao.getAllSessionsSync().filter { it.endTime != null }
                    
                    // Filter completed sessions within [startTime, endTime)
                    val sessionsInPeriod = allSessions.filter { it.startTime >= startTime && it.startTime < endTime }
                    
                    if (sessionsInPeriod.isNotEmpty()) {
                        val reportsDir = File(context.getExternalFilesDir(null), "Relatorios_Mensais").apply { mkdirs() }
                        var generatedCount = 0

                        val clientsWithActivity = allClients.filter { client ->
                            sessionsInPeriod.any { it.clientId == client.id }
                        }

                        for (client in clientsWithActivity) {
                            val clientSessions = sessionsInPeriod.filter { it.clientId == client.id }
                            
                            // Generate premium PDF directly inside reportsDir
                            val file = generateMonthlyPdfFile(context, reportsDir, client, clientSessions, displayPeriodTitle)
                            if (file != null) {
                                generatedCount++
                            }
                        }

                        if (generatedCount > 0) {
                            // Persist generation success
                            sharedPrefs.edit().putString("last_generated_cycle", cycleId).apply()
                            // Notify user
                            showNotification(context, displayPeriodTitle, generatedCount)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun generateMonthlyPdfFile(
        context: Context,
        reportsDir: File,
        client: Client,
        sessions: List<Session>,
        monthName: String
    ): File? {
        try {
            val document = PdfDocument()
            val width = 595
            val height = 842
            val paint = Paint().apply { isAntiAlias = true }

            val sortedSessions = sessions.sortedBy { it.startTime }
            val sharedPrefs = context.getSharedPreferences("time_tracker_prefs", Context.MODE_PRIVATE)
            val compName = sharedPrefs.getString("company_name", "") ?: ""
            val compCnpj = sharedPrefs.getString("company_cnpj", "") ?: ""
            val reportDateStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

            // Calculations
            var totalDuration = 0L
            var totalGrossValue = 0.0
            var totalDiscountValue = 0.0
            
            sortedSessions.forEach { session ->
                val duration = maxOf(0L, (session.endTime!! - session.startTime) - session.pausedDuration)
                val originalValue = (duration.toDouble() / (1000 * 60 * 60)) * client.hourlyRate
                val discountPctVal = originalValue * (session.discountPercentage / 100.0)
                val totalDiscount = discountPctVal + session.discountValue
                
                totalDuration += duration
                totalGrossValue += originalValue
                totalDiscountValue += totalDiscount
            }
            val totalNetValue = maxOf(0.0, totalGrossValue - totalDiscountValue)

            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(width, height, pageNum).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            fun drawBackground(canvas: Canvas) {
                paint.color = Color.WHITE
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }

            fun drawPageFooter(canvas: Canvas, num: Int) {
                paint.color = Color.rgb(200, 200, 200)
                paint.strokeWidth = 0.8f
                canvas.drawLine(40f, height - 70f, width - 40f, height - 70f, paint)

                paint.textAlign = Paint.Align.CENTER
                paint.textSize = 8f
                paint.color = Color.rgb(100, 100, 100)
                paint.typeface = Typeface.DEFAULT
                
                var footerY = height - 55f
                if (compName.isNotEmpty()) {
                    val footerText = if (compCnpj.isNotEmpty()) "$compName — CNPJ: $compCnpj" else compName
                    canvas.drawText(footerText, width / 2f, footerY, paint)
                    footerY += 12f
                }
                canvas.drawText("Gerado em $reportDateStr | Relatório Mensal Fechamento | Página $num", width / 2f, footerY, paint)
            }

            drawBackground(canvas)

            var y = 60f
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.rgb(24, 43, 73)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 20f
            canvas.drawText("Relatório Mensal de Fechamento", 40f, y, paint)

            y += 28f
            paint.color = Color.rgb(80, 80, 80)
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10f
            canvas.drawText("Mês de Referência: $monthName", 40f, y, paint)

            // Cards (Client + Resumo)
            y += 40f
            paint.color = Color.rgb(240, 243, 248)
            canvas.drawRect(40f, y, width - 40f, y + 105f, paint)
            
            paint.color = Color.rgb(24, 43, 73)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12f
            canvas.drawText("DADOS DO CLIENTE", 55f, y + 25f, paint)
            
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10f
            paint.color = Color.BLACK
            canvas.drawText("Cliente: ${client.name}", 55f, y + 45f, paint)
            canvas.drawText("Valor da Hora: ${FormatUtils.formatCurrency(client.hourlyRate)}", 55f, y + 62f, paint)
            canvas.drawText("Período: $monthName", 55f, y + 79f, paint)

            // Resumo
            paint.color = Color.rgb(242, 245, 250)
            canvas.drawRect(310f, y, width - 40f, y + 105f, paint)
            
            paint.color = Color.rgb(24, 43, 73)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12f
            canvas.drawText("RESUMO FINANCEIRO", 325f, y + 25f, paint)
            
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10f
            paint.color = Color.BLACK
            canvas.drawText("Total de Horas: ${FormatUtils.formatDuration(totalDuration)}", 325f, y + 45f, paint)
            canvas.drawText("Valor Bruto: ${FormatUtils.formatCurrency(totalGrossValue)}", 325f, y + 62f, paint)
            
            if (totalDiscountValue > 0.0) {
                paint.color = Color.rgb(180, 40, 40)
                canvas.drawText("Desconto: - ${FormatUtils.formatCurrency(totalDiscountValue)}", 325f, y + 79f, paint)
                paint.color = Color.BLACK
            } else {
                canvas.drawText("Desconto: ${FormatUtils.formatCurrency(0.0)}", 325f, y + 79f, paint)
            }
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.rgb(24, 43, 73)
            canvas.drawText("Líquido a Cobrar: ${FormatUtils.formatCurrency(totalNetValue)}", 325f, y + 96f, paint)

            y += 135f
            paint.color = Color.rgb(24, 43, 73)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12f
            canvas.drawText("HISTÓRICO DE SERVIÇOS", 40f, y, paint)
            
            y += 8f
            paint.strokeWidth = 1.2f
            canvas.drawLine(40f, y, width - 40f, y, paint)
            y += 20f

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10f

            for (session in sortedSessions) {
                val duration = maxOf(0L, (session.endTime!! - session.startTime) - session.pausedDuration)
                val originalValue = (duration.toDouble() / (1000 * 60 * 60)) * client.hourlyRate
                val discountPctVal = originalValue * (session.discountPercentage / 100.0)
                val totalDiscount = discountPctVal + session.discountValue
                val finalValue = maxOf(0.0, originalValue - totalDiscount)
                
                val pausesList = ExportUtils.parsePauseEvents(session.pauseEvents)
                val spaceRequired = (3 + pausesList.size) * 15f + 15f
                
                if (y + spaceRequired > height - 85f) {
                    drawPageFooter(canvas, pageNum)
                    document.finishPage(page)
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(width, height, pageNum).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    drawBackground(canvas)
                    
                    y = 50f
                    paint.color = Color.rgb(100, 100, 100)
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 9f
                    canvas.drawText("Relatório Mensal — Histórico (Continuação) — ${client.name}", 40f, y, paint)
                    y += 8f
                    paint.strokeWidth = 0.5f
                    canvas.drawLine(40f, y, width - 40f, y, paint)
                    y += 20f
                }

                // Header Item
                paint.color = Color.BLACK
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10f
                val dateStr = FormatUtils.formatDate(session.startTime)
                canvas.drawText("$dateStr — ${session.description}", 40f, y, paint)
                
                // Start and End Times
                y += 15f
                paint.typeface = Typeface.DEFAULT
                paint.color = Color.rgb(80, 80, 80)
                val startTimeStr = FormatUtils.formatTime(session.startTime)
                val endTimeStr = FormatUtils.formatTime(session.endTime)
                canvas.drawText("Horário de início: $startTimeStr | Horário de encerramento: $endTimeStr", 50f, y, paint)
                
                // Pauses List
                pausesList.forEach { pausePair ->
                    y += 15f
                    val pTime = FormatUtils.formatTime(pausePair.first)
                    val rTime = pausePair.second?.let { FormatUtils.formatTime(it) } ?: "Sem retomada"
                    canvas.drawText("  ↳ Pausa: $pTime | Retomada: $rTime", 50f, y, paint)
                }

                // Duration, Discounts & Subtotals
                y += 15f
                paint.color = Color.BLACK
                var subtotalText = "Duração: ${FormatUtils.formatDuration(duration)} | Valor: ${FormatUtils.formatCurrency(originalValue)}"
                if (totalDiscount > 0.0) {
                    val discountLabel = StringBuilder()
                    if (session.discountPercentage > 0.0) {
                        discountLabel.append("${session.discountPercentage}%")
                    }
                    if (session.discountValue > 0.0) {
                        if (discountLabel.isNotEmpty()) discountLabel.append(" + ")
                        discountLabel.append(FormatUtils.formatCurrency(session.discountValue))
                    }
                    subtotalText += " | Desconto: $discountLabel | Subtotal Líquido: ${FormatUtils.formatCurrency(finalValue)}"
                }
                canvas.drawText(subtotalText, 50f, y, paint)
                
                y += 12f
                paint.color = Color.rgb(230, 230, 230)
                paint.strokeWidth = 0.5f
                canvas.drawLine(40f, y, width - 40f, y, paint)
                y += 18f
            }

            drawPageFooter(canvas, pageNum)
            document.finishPage(page)

            val safeClientName = client.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val file = File(reportsDir, "Relatorio_Mensal_${safeClientName}_${monthName.replace(" ", "_")}.pdf")
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun showNotification(context: Context, monthName: String, reportCount: Int) {
        val channelId = "tempo_track_monthly_reports"
        val notificationId = 1001

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Relatórios Mensais",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de relatórios de fechamento mensal."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Relatório Mensal Pronto!")
            .setContentText("Os relatórios de fechamento de $monthName estão prontos para $reportCount empresa(s). Toque para ver.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Os relatórios de fechamento do período de $monthName foram gerados automaticamente em PDF para as $reportCount empresa(s) que registraram atividades. Acesse a pasta do aplicativo para visualizar."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
}
