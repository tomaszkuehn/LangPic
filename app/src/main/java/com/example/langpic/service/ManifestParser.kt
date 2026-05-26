package com.example.langpic.service

import com.example.langpic.domain.model.LessonItem
import org.json.JSONObject

data class ManifestData(
    val title: String,
    val language: String,
    val items: List<ManifestItem>,
)

data class ManifestItem(
    val word: String,
    val language: String,
    val correctImage: String,
    val wrongImage: String,
)

object ManifestParser {

    fun parse(jsonString: String): Result<ManifestData> {
        return try {
            val root = JSONObject(jsonString)
            val title = root.optString("title", "Untitled Pack")
            val language = root.optString("language", "")
            val itemsArray = root.getJSONArray("items")

            val items = (0 until itemsArray.length()).map { i ->
                val item = itemsArray.getJSONObject(i)
                ManifestItem(
                    word = item.getString("word"),
                    language = item.optString("language", language),
                    correctImage = item.getString("correctImage"),
                    wrongImage = item.getString("wrongImage"),
                )
            }

            if (items.isEmpty()) {
                Result.failure(IllegalArgumentException("Manifest contains no items"))
            } else {
                Result.success(ManifestData(title, language, items))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
