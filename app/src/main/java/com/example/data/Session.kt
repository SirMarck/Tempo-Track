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
    indices = [Index("clientId")]
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
    val discountPercentage: Double = 0.0
)
