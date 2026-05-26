package com.example.langpic.domain.model

data class ImageChoice(
    val path: String,
    val isCorrect: Boolean,
)

data class LessonItem(
    val id: Int = 0,
    val packId: Int,
    val word1: String,
    val language1: String,
    val word2: String,
    val language2: String,
    val images: List<ImageChoice>,
)
