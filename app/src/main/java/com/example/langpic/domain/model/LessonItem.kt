package com.example.langpic.domain.model

data class LessonItem(
    val id: Int = 0,
    val packId: Int,
    val word: String,
    val language: String,
    val correctImagePath: String,
    val wrongImagePath: String,
)
