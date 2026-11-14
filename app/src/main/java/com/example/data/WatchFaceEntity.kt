package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_faces")
data class WatchFaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val targetWearOsVersion: Int,
    val clockType: String,
    val themeColor: String,
    val configJson: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
