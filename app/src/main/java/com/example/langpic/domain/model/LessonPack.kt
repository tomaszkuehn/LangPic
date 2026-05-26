package com.example.langpic.domain.model

data class LessonPack(
    val id: Int = 0,
    val title: String,
    val language: String,
    val enabled: Boolean = true,
    val extractedPath: String,
    val itemCount: Int = 0,
)
