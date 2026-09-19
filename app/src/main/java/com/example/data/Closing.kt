package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "closing_batches",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("clientId")]
)
data class ClosingBatch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val fromDate: Long,
    val toDate: Long,
    val status: String = "invoiced", // "draft", "invoiced", "paid"
    val totalHours: Double = 0.0,
    val totalAmount: Double = 0.0,
    val sessionCount: Int = 0,
    val invoiceNumber: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "closing_entries",
    primaryKeys = ["closingBatchId", "sessionId"],
    foreignKeys = [
        ForeignKey(
            entity = ClosingBatch::class,
            parentColumns = ["id"],
            childColumns = ["closingBatchId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Session::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("closingBatchId"), Index("sessionId")]
)
data class ClosingEntry(
    val closingBatchId: Long,
    val sessionId: Long
)
