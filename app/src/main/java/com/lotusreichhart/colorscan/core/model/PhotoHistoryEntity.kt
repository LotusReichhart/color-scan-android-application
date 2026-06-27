package com.lotusreichhart.colorscan.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_color_history")
data class PhotoHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hex: String,
    val name: String,
    val rgb: String,
    val imagePath: String,
    val timestamp: Long
)
