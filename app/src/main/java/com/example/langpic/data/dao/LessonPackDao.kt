package com.example.langpic.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.langpic.data.entity.LessonPackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonPackDao {

    @Insert
    suspend fun insert(pack: LessonPackEntity): Long

    @Update
    suspend fun update(pack: LessonPackEntity)

    @Delete
    suspend fun delete(pack: LessonPackEntity)

    @Query("SELECT * FROM lesson_packs ORDER BY id DESC")
    fun getAll(): Flow<List<LessonPackEntity>>

    @Query("SELECT * FROM lesson_packs WHERE id = :id")
    suspend fun getById(id: Int): LessonPackEntity?

    @Query("SELECT * FROM lesson_packs WHERE enabled = 1")
    suspend fun getEnabled(): List<LessonPackEntity>

    @Query("SELECT COUNT(*) FROM lesson_packs WHERE title = :title")
    suspend fun countByTitle(title: String): Int
}
