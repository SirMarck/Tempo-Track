package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.util.Date
import java.util.Locale

/**
 * Utilitário de Exportação Editorial (PDF A4 e Imagem)
 * Guia de Redesign v2 - Fase 7 e Prompt 7
 * Layout off-white editorial, tipografia executiva, proteção de taxas imutáveis (appliedRate)
 */
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

    fun generatePdf(
        context: Context,
        client: Client,
        sessions: List<Session>,
        monthName: String,
        batchNumber: String? = null
    ): File? {
        try {
            val document = PdfDocument()
            val width = 595 // Padrão A4 em pontos (1/72 polegada)
            val height = 842 // Padrão A4 em pontos (1/72 polegada)

            val paint = Paint().apply {
                isAntiAlias = true
            }

            val sortedSessions = sessions.sortedBy { it.startTime }
            val sharedPrefs = context.getSharedPreferences("time_tracker_prefs", Context.MODE_PRIVATE)
            val compName = sharedPrefs.getString("company_name", "") ?: ""
            val compCnpj = sharedPrefs.getString("company_cnpj", "") ?: ""
            val compPhone = sharedPrefs.getString("company_phone", "") ?: ""
            val compEmail = sharedPrefs.getString("company_email", "") ?: ""
            val compPix = sharedPrefs.getString("company_pix", "") ?: ""
            val reportDateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            // Decodificação e redimensionamento proporcional do logotipo da empresa
            val logoFile = File(context.filesDir, "company_logo.png")
            val logoBitmap: Bitmap? = if (logoFile.exists()) {
                try {
                    val original = BitmapFactory.decodeFile(logoFile.absolutePath)
                    if (original != null && original.width > 0 && original.height > 0) {
                        val maxDimension = 55f // largura máxima de ~50-60pt
                        val scale = minOf(maxDimension / original.width.toFloat(), maxDimension / original.height.toFloat())
                        val targetW = (original.width * scale).toInt().coerceAtLeast(1)
                        val targetH = (original.height * scale).toInt().coerceAtLeast(1)
                        Bitmap.createScaledBitmap(original, targetW, targetH, true)
                    } else null
                } catch (e: Exception) {
                    null
                }
            } else null

            // Cores do Tema Editorial A4 (Não usa dark theme para impressos comerciais)
            val colorBg = Color.rgb(250, 249, 246) // #FAF9F5 Off-white suave
            val colorGraphite = Color.rgb(26, 30, 35) // Texto primário
            val colorSecondary = Color.rgb(100, 108, 115) // Texto secundário
            val colorDivider = Color.rgb(226, 228, 232) // Linhas sutis
            val colorAccent = Color.rgb(212, 98, 56) // Laranja editorial (#D46238)
            val colorSurface = Color.rgb(243, 242, 238) // Fundo dos blocos de resumo

            // Cálculos precisos respeitando appliedRate imutável e status faturável
            var totalDurationMillis = 0L
            var totalBillableMillis = 0L
            var totalGrossValue = 0.0
            var totalDiscountValue = 0.0

            sortedSessions.forEach { session ->
                val duration = session.calculateDurationMillis()
                val effectiveRate = if (session.appliedRate > 0.0) session.appliedRate else client.hourlyRate
                val gross = if (session.billable) (duration.toDouble() / (1000.0 * 3600.0)) * effectiveRate else 0.0
                val discountPctVal = gross * (session.discountPercentage / 100.0)
                val totalDiscount = discountPctVal + session.discountValue

                totalDurationMillis += duration
                if (session.billable) {
                    totalBillableMillis += duration
                    totalGrossValue += gross
                    totalDiscountValue += totalDiscount
                }
            }
            val totalNetValue = maxOf(0.0, totalGrossValue - totalDiscountValue)

            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(width, height, pageNum).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            fun drawPageBackground(c: Canvas) {
                paint.color = colorBg
                c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }

            fun drawFooter(c: Canvas, pNum: Int) {
                paint.color = colorDivider
                paint.strokeWidth = 0.8f
                c.drawLine(40f, height - 70f, width - 40f, height - 70f, paint)

                paint.textAlign = Paint.Align.CENTER
                paint.textSize = 8f
                paint.color = colorSecondary
                paint.typeface = Typeface.DEFAULT

                var footerY = height - 55f
                val compInfo = listOfNotNull(
                    compName.takeIf { it.isNotEmpty() },
                    compCnpj.takeIf { it.isNotEmpty() }?.let { "CNPJ: $it" },
                    compPhone.takeIf { it.isNotEmpty() }?.let { "Tel: $it" },
                    compEmail.takeIf { it.isNotEmpty() }?.let { "E-mail: $it" },
                    compPix.takeIf { it.isNotEmpty() }?.let { "Pix: $it" }
                ).joinToString(" | ")

                if (compInfo.isNotEmpty()) {
                    c.drawText(compInfo, width / 2f, footerY, paint)
                    footerY += 12f
                }

                c.drawText("Relatório Comercial — Emitido em $reportDateStr | TempoTrack | Página $pNum", width / 2f, footerY, paint)
            }

            drawPageBackground(canvas)

            // ─── Cabeçalho Editorial Executivo (Página 1) ───────────────────
            // Desenhar Logo no topo direito (se existir)
            if (logoBitmap != null) {
                val logoX = (width - 40f) - logoBitmap.width
                val logoY = 40f
                canvas.drawBitmap(logoBitmap, logoX, logoY, null)
            }

            var y = 55f
            paint.textAlign = Paint.Align.LEFT
            paint.color = colorGraphite
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 18f
            canvas.drawText(if (compName.isNotEmpty()) compName else "TempoTrack", 40f, y, paint)

            paint.textSize = 8.5f
            paint.typeface = Typeface.DEFAULT
            paint.color = colorSecondary

            val compContact = listOfNotNull(
                compCnpj.takeIf { it.isNotEmpty() }?.let { "CNPJ: $it" },
                compPhone.takeIf { it.isNotEmpty() }?.let { "Tel: $it" },
                compEmail.takeIf { it.isNotEmpty() }?.let { "E-mail: $it" }
            ).joinToString(" • ")

            if (compContact.isNotEmpty()) {
                y += 13f
                canvas.drawText(compContact, 40f, y, paint)
            }

            if (compPix.isNotEmpty()) {
                y += 12f
                canvas.drawText("Chave Pix: $compPix", 40f, y, paint)
            }

            y += 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 13.5f
            paint.color = colorGraphite
            canvas.drawText("RELATÓRIO DE PRESTAÇÃO DE SERVIÇOS", 40f, y, paint)

            y += 14f
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9.5f
            paint.color = colorSecondary
            val displayPeriod = if (monthName.startsWith("Período:")) monthName.removePrefix("Período:").trim() else monthName
            val refText = "Período de Referência: $displayPeriod" + (batchNumber?.let { " • Lote: #$it" } ?: "")
            canvas.drawText(refText, 40f, y, paint)

            // Garantir espaçamento adequado caso o logo seja mais alto que o cabeçalho
            if (logoBitmap != null) {
                y = maxOf(y, 40f + logoBitmap.height + 14f)
            }

            // ─── Blocos de Resumo (Cliente e Faturamento) ────────────────────
            y += 20f
            val cardHeight = 90f

            // Card 1: Dados do Cliente
            paint.color = colorSurface
            canvas.drawRect(40f, y, 290f, y + cardHeight, paint)
            paint.color = colorDivider
            paint.strokeWidth = 0.5f
            canvas.drawLine(40f, y, 290f, y, paint)
            canvas.drawLine(40f, y + cardHeight, 290f, y + cardHeight, paint)

            paint.color = colorGraphite
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10.5f
            canvas.drawText("CLIENTE", 52f, y + 20f, paint)

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9.5f
            paint.color = colorGraphite
            canvas.drawText("Razão / Nome: ${client.name}", 52f, y + 38f, paint)
            canvas.drawText("Taxa Base: ${FormatUtils.formatCurrency(client.hourlyRate)}/h", 52f, y + 54f, paint)
            canvas.drawText("Total de Lançamentos: ${sortedSessions.size}", 52f, y + 70f, paint)

            // Card 2: Resumo Financeiro
            paint.color = colorSurface
            canvas.drawRect(305f, y, width - 40f, y + cardHeight, paint)
            paint.color = colorDivider
            canvas.drawLine(305f, y, width - 40f, y, paint)
            canvas.drawLine(305f, y + cardHeight, width - 40f, y + cardHeight, paint)

            paint.color = colorGraphite
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10.5f
            canvas.drawText("RESUMO FINANCEIRO", 317f, y + 20f, paint)

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9.5f
            canvas.drawText("Horas Faturáveis: ${FormatUtils.formatDuration(totalBillableMillis)}", 317f, y + 38f, paint)
            canvas.drawText("Valor Bruto: ${FormatUtils.formatCurrency(totalGrossValue)}", 317f, y + 54f, paint)

            if (totalDiscountValue > 0.0) {
                paint.color = Color.rgb(180, 50, 50)
                canvas.drawText("Descontos: - ${FormatUtils.formatCurrency(totalDiscountValue)}", 317f, y + 68f, paint)
            }

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = colorAccent
            paint.textSize = 11f
            canvas.drawText("Líquido a Pagar: ${FormatUtils.formatCurrency(totalNetValue)}", 317f, y + 84f, paint)

            // ─── Tabela de Itens e Sessões ───────────────────────────────────
            y += cardHeight + 25f
            paint.color = colorGraphite
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 11f
            canvas.drawText("DETALHAMENTO DAS ATIVIDADES REALIZADAS", 40f, y, paint)

            y += 8f
            paint.strokeWidth = 1f
            paint.color = colorGraphite
            canvas.drawLine(40f, y, width - 40f, y, paint)
            y += 18f

            // Iteração pelas sessões
            for (session in sortedSessions) {
                val duration = session.calculateDurationMillis()
                val effectiveRate = if (session.appliedRate > 0.0) session.appliedRate else client.hourlyRate
                val rawVal = if (session.billable) (duration.toDouble() / (1000.0 * 3600.0)) * effectiveRate else 0.0
                val discVal = rawVal * (session.discountPercentage / 100.0) + session.discountValue
                val finalVal = maxOf(0.0, rawVal - discVal)

                val pausesList = parsePauseEvents(session.pauseEvents)
                val linesRequired = 3 + pausesList.size
                val spaceRequired = linesRequired * 14f + 14f

                // Quebra de página segura
                if (y + spaceRequired > height - 100f) {
                    drawFooter(canvas, pageNum)
                    document.finishPage(page)
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(width, height, pageNum).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    drawPageBackground(canvas)

                    y = 50f
                    paint.color = colorSecondary
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 9f
                    canvas.drawText("Relatório Comercial — Detalhamento (Continuação) — ${client.name}", 40f, y, paint)
                    y += 8f
                    paint.strokeWidth = 0.5f
                    paint.color = colorDivider
                    canvas.drawLine(40f, y, width - 40f, y, paint)
                    y += 18f
                }

                // Linha de Cabeçalho do Item
                val dateStr = FormatUtils.formatDate(session.startTime)
                val desc = if (session.description.isNotBlank()) session.description else "Trabalho sem descrição"
                paint.color = colorGraphite
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 9.5f
                canvas.drawText("$dateStr — $desc", 40f, y, paint)

                // Horários de Início e Fim
                y += 13f
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 8.5f
                paint.color = colorSecondary
                val startStr = FormatUtils.formatTime(session.startTime)
                val endStr = session.endTime?.let { FormatUtils.formatTime(it) } ?: "..."
                val billableTag = if (session.billable) "Faturável" else "Não Faturável"
                canvas.drawText("Horário: $startStr às $endStr | Taxa: ${FormatUtils.formatCurrency(effectiveRate)}/h | $billableTag", 50f, y, paint)

                // Pausas
                pausesList.forEach { pausePair ->
                    y += 12f
                    val pTime = FormatUtils.formatTime(pausePair.first)
                    val rTime = pausePair.second?.let { FormatUtils.formatTime(it) } ?: "Sem retorno"
                    paint.color = Color.rgb(180, 80, 80)
                    canvas.drawText("  ↳ Intervalo: $pTime | Retorno: $rTime", 50f, y, paint)
                }

                // Duração e Valores
                y += 13f
                paint.color = colorGraphite
                var lineVal = "Duração: ${FormatUtils.formatDuration(duration)} | Subtotal: ${FormatUtils.formatCurrency(rawVal)}"
                if (discVal > 0.0) {
                    lineVal += " | Desconto: -${FormatUtils.formatCurrency(discVal)} | Líquido: ${FormatUtils.formatCurrency(finalVal)}"
                }
                canvas.drawText(lineVal, 50f, y, paint)

                // Divisória sutil entre sessões
                y += 10f
                paint.color = colorDivider
                paint.strokeWidth = 0.5f
                canvas.drawLine(40f, y, width - 40f, y, paint)
                y += 14f
            }

            // Bloco de Assinatura / Aceite (na última página se couber)
            if (y + 70f <= height - 85f) {
                y += 35f
                paint.color = colorSecondary
                paint.strokeWidth = 0.8f
                canvas.drawLine(width / 2f - 110f, y, width / 2f + 110f, y, paint)
                y += 12f
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = 8.5f
                canvas.drawText("Aceite e De Acordo do Cliente", width / 2f, y, paint)
                if (compPix.isNotEmpty()) {
                    y += 12f
                    paint.color = colorAccent
                    canvas.drawText("Chave Pix para Liquidação: $compPix", width / 2f, y, paint)
                }
            }

            drawFooter(canvas, pageNum)
            document.finishPage(page)

            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeClientName = client.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val safeMonthName = monthName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(exportsDir, "Relatorio_${safeClientName}_$safeMonthName.pdf")
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

            var dynamicHeight = 350f
            sortedSessions.forEach { session ->
                val pausesList = parsePauseEvents(session.pauseEvents)
                dynamicHeight += 15f
                dynamicHeight += 15f
                dynamicHeight += pausesList.size * 15f
                dynamicHeight += 15f
                dynamicHeight += 25f
            }
            dynamicHeight += 120f

            val width = 600
            val height = maxOf(850, dynamicHeight.toInt())
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint().apply { isAntiAlias = true }

            val colorBg = Color.rgb(250, 249, 246)
            paint.color = colorBg
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            val sharedPrefs = context.getSharedPreferences("time_tracker_prefs", Context.MODE_PRIVATE)
            val compName = sharedPrefs.getString("company_name", "") ?: ""
            val compCnpj = sharedPrefs.getString("company_cnpj", "") ?: ""
            val compPhone = sharedPrefs.getString("company_phone", "") ?: ""
            val compEmail = sharedPrefs.getString("company_email", "") ?: ""
            val compPix = sharedPrefs.getString("company_pix", "") ?: ""
            val reportDateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            var totalDurationMillis = 0L
            var totalGrossValue = 0.0
            var totalDiscountValue = 0.0

            sortedSessions.forEach { session ->
                val duration = session.calculateDurationMillis()
                val effectiveRate = if (session.appliedRate > 0.0) session.appliedRate else client.hourlyRate
                val originalValue = if (session.billable) (duration.toDouble() / (1000.0 * 3600.0)) * effectiveRate else 0.0
                val discountPctVal = originalValue * (session.discountPercentage / 100.0)
                val totalDiscount = discountPctVal + session.discountValue

                totalDurationMillis += duration
                if (session.billable) {
                    totalGrossValue += originalValue
                    totalDiscountValue += totalDiscount
                }
            }
            val totalNetValue = maxOf(0.0, totalGrossValue - totalDiscountValue)

            var y = 55f
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.rgb(26, 30, 35)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 20f
            canvas.drawText(if (compName.isNotEmpty()) compName else "TempoTrack", 40f, y, paint)

            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.rgb(100, 108, 115)
            val compDetails = listOfNotNull(
                compCnpj.takeIf { it.isNotEmpty() }?.let { "CNPJ: $it" },
                compPhone.takeIf { it.isNotEmpty() }?.let { "Tel: $it" },
                compEmail.takeIf { it.isNotEmpty() }?.let { "E-mail: $it" }
            ).joinToString(" • ")
            if (compDetails.isNotEmpty()) {
                y += 14f
                canvas.drawText(compDetails, 40f, y, paint)
            }

            y += 24f
            paint.color = Color.rgb(26, 30, 35)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 14f
            canvas.drawText("Relatório de Horas — $monthName", 40f, y, paint)

            y += 35f
            paint.color = Color.rgb(243, 242, 238)
            canvas.drawRect(40f, y, width - 40f, y + 80f, paint)

            paint.color = Color.rgb(26, 30, 35)
            paint.textSize = 11f
            canvas.drawText("Cliente: ${client.name} | Taxa: ${FormatUtils.formatCurrency(client.hourlyRate)}/h", 55f, y + 26f, paint)
            canvas.drawText("Total de Horas: ${FormatUtils.formatDuration(totalDurationMillis)}", 55f, y + 46f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.rgb(212, 98, 56)
            canvas.drawText("Total Líquido: ${FormatUtils.formatCurrency(totalNetValue)}", 55f, y + 66f, paint)

            y += 110f
            paint.color = Color.rgb(26, 30, 35)
            paint.textSize = 12f
            canvas.drawText("Histórico de Atividades", 40f, y, paint)
            y += 15f

            for (session in sortedSessions) {
                val duration = session.calculateDurationMillis()
                val effectiveRate = if (session.appliedRate > 0.0) session.appliedRate else client.hourlyRate
                val rawVal = if (session.billable) (duration.toDouble() / (1000.0 * 3600.0)) * effectiveRate else 0.0
                val dateStr = FormatUtils.formatDate(session.startTime)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textSize = 10f
                paint.color = Color.rgb(26, 30, 35)
                canvas.drawText("$dateStr — ${session.description}", 40f, y, paint)

                y += 14f
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 9f
                paint.color = Color.rgb(100, 108, 115)
                canvas.drawText("Duração: ${FormatUtils.formatDuration(duration)} | Valor: ${FormatUtils.formatCurrency(rawVal)}", 50f, y, paint)

                y += 16f
            }

            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeClientName = client.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val safeMonthName = monthName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(exportsDir, "Relatorio_${safeClientName}_$safeMonthName.png")
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

    fun shareViaWhatsApp(context: Context, file: File?, message: String) {
        val uri = if (file != null) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else null

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (uri != null) "application/pdf" else "text/plain"
            if (uri != null) {
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (uri != null) "application/pdf" else "text/plain"
                if (uri != null) {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Compartilhar via WhatsApp / App"))
        }
    }
}
