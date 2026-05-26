package com.example.langpic.data.repository

import com.example.langpic.data.dao.LessonItemDao
import com.example.langpic.data.dao.LessonPackDao
import com.example.langpic.data.entity.LessonItemEntity
import com.example.langpic.data.entity.LessonPackEntity
import com.example.langpic.domain.model.ImageChoice
import com.example.langpic.domain.model.LessonItem
import com.example.langpic.domain.model.LessonPack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import org.json.JSONArray
import org.json.JSONObject

class LessonRepository(
    private val packDao: LessonPackDao,
    private val itemDao: LessonItemDao,
) {

    fun getAllPacks(): Flow<List<LessonPack>> {
        return packDao.getAll().flatMapLatest { entities ->
            val result = mutableListOf<LessonPack>()
            for (entity in entities) {
                val count = itemDao.countByPackId(entity.id)
                result.add(entity.toDomain(itemCount = count))
            }
            flowOf(result)
        }
    }

    suspend fun getPackById(id: Int): LessonPack? {
        val entity = packDao.getById(id) ?: return null
        val count = itemDao.countByPackId(id)
        return entity.toDomain(itemCount = count)
    }

    suspend fun getEnabledItems(): List<LessonItem> {
        return itemDao.getEnabledItems().map { it.toDomain() }
    }

    suspend fun getItemsForPack(packId: Int): List<LessonItem> {
        return itemDao.getByPackId(packId).map { it.toDomain() }
    }

    suspend fun insertPack(
        title: String,
        language: String,
        extractedPath: String,
        items: List<LessonItem>,
    ): Long {
        val packId = packDao.insert(
            LessonPackEntity(
                title = title,
                language = language,
                extractedPath = extractedPath,
            )
        ).toInt()

        val entities = items.map { it.toEntity(packId) }
        itemDao.insertAll(entities)
        return packId.toLong()
    }

    suspend fun updatePackEnabled(packId: Int, enabled: Boolean) {
        val pack = packDao.getById(packId) ?: return
        packDao.update(pack.copy(enabled = enabled))
    }

    suspend fun deletePack(packId: Int) {
        val pack = packDao.getById(packId) ?: return
        packDao.delete(pack)
    }

    suspend fun countByTitle(title: String): Int {
        return packDao.countByTitle(title)
    }

    // ---- mappers ----

    private fun LessonPackEntity.toDomain(itemCount: Int = 0) = LessonPack(
        id = id,
        title = title,
        language = language,
        enabled = enabled,
        extractedPath = extractedPath,
        itemCount = itemCount,
    )

    private fun LessonItemEntity.toDomain(): LessonItem {
        val images = try {
            val arr = JSONArray(imagesJson)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ImageChoice(
                    path = obj.getString("path"),
                    isCorrect = obj.getBoolean("correct"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
        return LessonItem(
            id = id,
            packId = packId,
            word1 = word1,
            language1 = language1,
            word2 = word2,
            language2 = language2,
            images = images,
        )
    }

    private fun LessonItem.toEntity(packId: Int): LessonItemEntity {
        val arr = JSONArray()
        for (img in images) {
            arr.put(JSONObject().apply {
                put("path", img.path)
                put("correct", img.isCorrect)
            })
        }
        return LessonItemEntity(
            packId = packId,
            word1 = word1,
            language1 = language1,
            word2 = word2,
            language2 = language2,
            imagesJson = arr.toString(),
        )
    }
}
