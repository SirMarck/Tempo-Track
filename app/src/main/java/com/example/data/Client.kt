package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val hourlyRate: Double,
    val currency: String = "BRL",
    val notes: String? = null,
    val archivedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val defaultHourlyRate: Double get() = hourlyRate
    val isArchived: Boolean get() = archivedAt != null
}

