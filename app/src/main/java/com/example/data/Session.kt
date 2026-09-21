package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("clientId"),
        Index("projectId"),
        Index("closingBatchId")
    ]
)
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val startTime: Long,
    val endTime: Long? = null,
    val description: String = "",
    val isPaused: Boolean = false,
    val lastPausedTime: Long? = null,
    val pausedDuration: Long = 0L,
    val pauseEvents: String = "",
    val discountValue: Double = 0.0,
    val discountPercentage: Double = 0.0,
    val tag: String = "",

    // ─── Novos campos do Guia de Redesign v2 (WorkSession) ─────────────────
    val projectId: Long? = null,
    val activityId: Long? = null,
    val billable: Boolean = true,
    val appliedRate: Double = 0.0, // Snapshot imutável da taxa no momento da sessão!
    val status: String = "completed", // "running", "paused", "completed"
    val source: String = "timer", // "timer", "manual", "quick"
    val financialStatus: String = "unbilled", // "unbilled", "invoiced", "paid"
    val closingBatchId: Long? = null
) {
    val startedAt: Long get() = startTime
    val endedAt: Long? get() = endTime

    /**
     * Duração bruta em milissegundos calculada por timestamps
     */
    fun calculateDurationMillis(currentTime: Long = System.currentTimeMillis()): Long {
        return if (endTime != null) {
            maxOf(0L, (endTime - startTime) - pausedDuration)
        } else if (isPaused) {
            maxOf(0L, (lastPausedTime ?: currentTime) - startTime - pausedDuration)
        } else {
            maxOf(0L, currentTime - startTime - pausedDuration)
        }
    }

    /**
     * Valor faturável calculado considerando snapshot appliedRate e descontos
     */
    fun calculateEarnings(hourlyRateFallback: Double = 0.0): Double {
        if (!billable) return 0.0
        val effectiveRate = if (appliedRate > 0.0) appliedRate else hourlyRateFallback
        val durationHours = calculateDurationMillis().toDouble() / (1000 * 60 * 60)
        val gross = durationHours * effectiveRate
        val discountPctVal = gross * (discountPercentage / 100.0)
        return maxOf(0.0, gross - discountPctVal - discountValue)
    }
}

typealias WorkSession = Session

