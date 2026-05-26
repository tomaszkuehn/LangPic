package com.example.langpic.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.langpic.data.dao.LessonItemDao
import com.example.langpic.data.dao.LessonPackDao
import com.example.langpic.data.entity.LessonItemEntity
import com.example.langpic.data.entity.LessonPackEntity

@Database(
    entities = [LessonPackEntity::class, LessonItemEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun lessonPackDao(): LessonPackDao
    abstract fun lessonItemDao(): LessonItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "langpic.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
