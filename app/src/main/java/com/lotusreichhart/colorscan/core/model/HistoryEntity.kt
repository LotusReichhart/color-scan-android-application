package com.lotusreichhart.colorscan.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "color_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hex: String,
    val name: String,
    val rgb: String,
    val timestamp: Long
)
