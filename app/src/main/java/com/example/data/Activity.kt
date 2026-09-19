package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class Activity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultBillable: Boolean = true,
    val archivedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
