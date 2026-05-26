package com.example.langpic.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lesson_packs")
data class LessonPackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val language: String,
    val enabled: Boolean = true,
    val extractedPath: String,
    val highScore: Float = 0f,
)
