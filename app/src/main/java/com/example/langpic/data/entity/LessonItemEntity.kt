package com.example.langpic.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lesson_items",
    foreignKeys = [
        ForeignKey(
            entity = LessonPackEntity::class,
            parentColumns = ["id"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("packId")],
)
data class LessonItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val packId: Int,
    val word: String,
    val language: String,
    val correctImagePath: String,
    val wrongImagePath: String,
)
