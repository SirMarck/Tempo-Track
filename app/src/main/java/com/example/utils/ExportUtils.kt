package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.Client
import com.example.data.Session
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

object ExportUtils {

    fun parsePauseEvents(pauseEvents: String): List<Pair<Long, Long?>> {
        if (pauseEvents.isEmpty()) return emptyList()
        val list = mutableListOf<Pair<Long, Long?>>()
        val parts = pauseEvents.split(",")
        var lastPause: Long? = null
        for (part in parts) {
            if (part.startsWith("P:")) {
                val ts = part.substring(2).toLongOrNull()
                if (ts != null) {
                    lastPause = ts
                }
            } else if (part.startsWith("R:")) {
                val ts = part.substring(2).toLongOrNull()
                if (ts != null && lastPause != null) {
                    list.add(Pair(lastPause, ts))
                    lastPause = null
                }
            }
        }
        if (lastPause != null) {
            list.add(Pair(lastPause, null))
        }
        return list
    }

    fun generatePdf(context: Context, client: Client, sessions: List<Session>, monthName: String): File? {
        try {
            val document = PdfDocument()
            val width = 595 // Standard A4 width in points (1/72 inch)
            val height = 842 // Standard A4 height in points (1/72 inch)
            
            val paint = Paint().apply {
                isAntiAlias = true
            }
            
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

            // Setup multi-page management
            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(width, height, pageNum).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            
            fun drawPageBackground(canvas: Canvas) {
                paint.color = Color.WHITE
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            
            fun drawFooter(canvas: Canvas, pageNum: Int) {
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
                
                canvas.drawText("Gerado em $reportDateStr | Gerado por TempoTrack | Página $pageNum", width / 2f, footerY, paint)
            }

            drawPageBackground(canvas)

            // Header (Only on page 1)
            var y = 60f
            
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.rgb(24, 43, 73) // Premium Dark Blue
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 20f
            canvas.drawText("Relatório Comercial", 40f, y, paint)
            
            y += 28f
            paint.color = Color.rgb(80, 80, 80)
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10f
            canvas.drawText("Mês de Referência: $monthName", 40f, y, paint)

            // Client and Billing summary cards (Only on page 1)
            y += 40f
            paint.color = Color.rgb(240, 243, 248) // Very light blue/gray background
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

            // Billing summary card
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
            
            // Total a pagar
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
            paint.color = Color.rgb(24, 43, 73)
            canvas.drawLine(40f, y, width - 40f, y, paint)
            y += 20f

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10f // Premium 10pt size for list

            for (session in sortedSessions) {
                val duration = maxOf(0L, (session.endTime!! - session.startTime) - session.pausedDuration)
                val originalValue = (duration.toDouble() / (1000 * 60 * 60)) * client.hourlyRate
                val discountPctVal = originalValue * (session.discountPercentage / 100.0)
                val totalDiscount = discountPctVal + session.discountValue
                val finalValue = maxOf(0.0, originalValue - totalDiscount)
                
                // Estimate height required for this item
                val pausesList = parsePauseEvents(session.pauseEvents)
                val linesRequired = 3 + pausesList.size
                val spaceRequired = linesRequired * 15f + 15f
                
                // Check page overflow
                if (y + spaceRequired > height - 85f) {
                    drawFooter(canvas, pageNum)
                    document.finishPage(page)
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(width, height, pageNum).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    drawPageBackground(canvas)
                    
                    // Sub-header on next page
                    y = 50f
                    paint.color = Color.rgb(100, 100, 100)
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 9f
                    canvas.drawText("Relatório Comercial — Histórico (Continuação) — ${client.name}", 40f, y, paint)
                    y += 8f
                    paint.strokeWidth = 0.5f
                    canvas.drawLine(40f, y, width - 40f, y, paint)
                    y += 20f
                }
                
                // Item Header
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
                
                // Tiny separating line
                y += 12f
                paint.color = Color.rgb(230, 230, 230)
                paint.strokeWidth = 0.5f
                canvas.drawLine(40f, y, width - 40f, y, paint)
                y += 18f
            }

            drawFooter(canvas, pageNum)
            document.finishPage(page)
            
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportsDir, "Relatorio_${client.name.replace(" ", "_")}_$monthName.pdf")
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

    fun generateImage(context: Context, client: Client, sessions: List<Session>, monthName: String): File? {
        try {
            val sortedSessions = sessions.sortedBy { it.startTime }
            
            // Calculate height required dynamically to prevent overlapping and provide a premium presentation
            var dynamicHeight = 350f
            sortedSessions.forEach { session ->
                val pausesList = parsePauseEvents(session.pauseEvents)
                dynamicHeight += 15f // item title
                dynamicHeight += 15f // start/end time
                dynamicHeight += pausesList.size * 15f // pauses
                dynamicHeight += 15f // duration and values
                dynamicHeight += 25f // separation space
            }
            dynamicHeight += 100f // Footer area
            
            val width = 600
            val height = maxOf(850, dynamicHeight.toInt())
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                isAntiAlias = true
            }

            paint.color = Color.WHITE
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

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

            // Header
            var y = 60f
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.rgb(24, 43, 73)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 22f
            canvas.drawText("Relatório Comercial", 40f, y, paint)
            
            y += 28f
            paint.color = Color.rgb(80, 80, 80)
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 12f
            canvas.drawText("Mês de Referência: $monthName", 40f, y, paint)

            // Client and Billing summary cards
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

            // Billing summary card
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
            paint.color = Color.rgb(24, 43, 73)
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
                
                // Item Header
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
                val pausesList = parsePauseEvents(session.pauseEvents)
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
                
                // Tiny separating line
                y += 12f
                paint.color = Color.rgb(230, 230, 230)
                paint.strokeWidth = 0.5f
                canvas.drawLine(40f, y, width - 40f, y, paint)
                y += 18f
            }

            // Footer
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
            
            canvas.drawText("Gerado em $reportDateStr | Gerado por TempoTrack", width / 2f, footerY, paint)

            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportsDir, "Relatorio_${client.name.replace(" ", "_")}_$monthName.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar Relatório"))
    }
}
