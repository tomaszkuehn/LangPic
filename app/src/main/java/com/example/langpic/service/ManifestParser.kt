package com.example.langpic.service

import com.example.langpic.domain.model.ImageChoice
import org.json.JSONObject

data class ManifestData(
    val title: String,
    val language: String,
    val items: List<ManifestItem>,
)

data class ManifestItem(
    val word1: String,
    val language1: String,
    val word2: String,
    val language2: String,
    val images: List<ImageChoice>,
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
                val lang1 = item.optString("language1", language)
                val lang2 = item.optString("language2", lang1)

                val images = if (item.has("images")) {
                    val imagesArray = item.getJSONArray("images")
                    (0 until imagesArray.length()).map { j ->
                        val img = imagesArray.getJSONObject(j)
                        ImageChoice(
                            path = img.getString("path"),
                            isCorrect = img.getBoolean("correct"),
                        )
                    }
                } else {
                    // Legacy format: correctImage + wrongImage as single strings
                    val list = mutableListOf<ImageChoice>()
                    if (item.has("correctImage")) {
                        list.add(ImageChoice(item.getString("correctImage"), true))
                    }
                    if (item.has("wrongImage")) {
                        list.add(ImageChoice(item.getString("wrongImage"), false))
                    }
                    list.ifEmpty { emptyList() }
                }

                ManifestItem(
                    word1 = item.getString("word1"),
                    language1 = lang1,
                    word2 = item.optString("word2", ""),
                    language2 = lang2,
                    images = images,
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
