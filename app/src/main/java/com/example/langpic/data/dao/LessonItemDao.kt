package com.example.langpic.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.langpic.data.entity.LessonItemEntity

@Dao
interface LessonItemDao {

    @Insert
    suspend fun insertAll(items: List<LessonItemEntity>)

    @Query("SELECT * FROM lesson_items WHERE packId = :packId")
    suspend fun getByPackId(packId: Int): List<LessonItemEntity>

    @Query("SELECT * FROM lesson_items WHERE id IN (SELECT id FROM lesson_items GROUP BY id)")
    suspend fun getAll(): List<LessonItemEntity>

    @Query("SELECT * FROM lesson_items WHERE packId IN (SELECT id FROM lesson_packs WHERE enabled = 1)")
    suspend fun getEnabledItems(): List<LessonItemEntity>

    @Query("SELECT COUNT(*) FROM lesson_items WHERE packId = :packId")
    suspend fun countByPackId(packId: Int): Int

    @Query("DELETE FROM lesson_items WHERE packId = :packId")
    suspend fun deleteByPackId(packId: Int)
}
