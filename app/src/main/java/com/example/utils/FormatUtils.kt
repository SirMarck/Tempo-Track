package com.example.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.example.data.Client
import com.example.data.Project

object FormatUtils {
    fun formatCurrency(value: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return format.format(value)
    }

    fun formatDuration(durationMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Aplica regra de arredondamento de faturamento (5/10/15/30 min) apenas no cálculo de valor,
     * preservando a duração real armazenada sem adulteração.
     */
    fun calculateBillableDurationMillis(rawDurationMillis: Long, roundingMinutes: Int): Long {
        if (roundingMinutes <= 0) return rawDurationMillis
        val intervalMillis = roundingMinutes * 60 * 1000L
        val remainder = rawDurationMillis % intervalMillis
        return if (remainder == 0L) rawDurationMillis else rawDurationMillis + (intervalMillis - remainder)
    }

    /**
     * Resolução de taxa efetiva conforme prioridade do Guia v2:
     * Taxa do Projeto -> Taxa padrão do Cliente -> Taxa Global
     */
    fun resolveEffectiveRate(projectRate: Double?, clientRate: Double, globalRate: Double = 0.0): Double {
        return projectRate?.takeIf { it > 0.0 }
            ?: clientRate.takeIf { it > 0.0 }
            ?: globalRate
    }

    fun resolveEffectiveRate(project: Project?, client: Client?, globalRate: Double = 0.0): Double {
        return resolveEffectiveRate(project?.hourlyRate, client?.hourlyRate ?: 0.0, globalRate)
    }
}

