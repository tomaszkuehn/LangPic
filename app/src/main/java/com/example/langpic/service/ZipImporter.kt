package com.example.langpic.service

import android.content.Context
import com.example.langpic.domain.model.LessonItem
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class ImportResult(
    val title: String,
    val language: String,
    val itemCount: Int,
    val extractedPath: String,
    val items: List<LessonItem>,
)

object ZipImporter {

    fun import(context: Context, zipPath: String): Result<ImportResult> {
        return try {
            val zipFile = File(zipPath)
            if (!zipFile.exists()) {
                return Result.failure(IllegalArgumentException("ZIP file not found"))
            }

            val imageEntries = mutableMapOf<String, ByteArray>()
            var manifestJson: String? = null

            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val bytes = zis.readBytes()
                        when {
                            entry.name == "manifest.json" -> {
                                manifestJson = String(bytes, Charsets.UTF_8)
                            }
                            entry.name.startsWith("images/") -> {
                                imageEntries[entry.name] = bytes
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val json = manifestJson
                ?: return Result.failure(IllegalArgumentException("manifest.json not found in ZIP"))

            val manifest = ManifestParser.parse(json).getOrElse {
                return Result.failure(it)
            }

            // Validate all referenced images exist in the ZIP
            for (item in manifest.items) {
                for (img in item.images) {
                    if (!imageEntries.containsKey(img.path)) {
                        return Result.failure(
                            IllegalArgumentException("Missing image: ${img.path}")
                        )
                    }
                }
            }

            val packDir = File(context.filesDir, "packs/${System.currentTimeMillis()}")
            val imagesDir = File(packDir, "images")
            imagesDir.mkdirs()

            for ((name, bytes) in imageEntries) {
                val outFile = File(packDir, name)
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { it.write(bytes) }
            }

            val items = manifest.items.map { item ->
                LessonItem(
                    packId = 0,
                    word1 = item.word1,
                    language1 = item.language1,
                    word2 = item.word2,
                    language2 = item.language2,
                    images = item.images,
                )
            }

            Result.success(
                ImportResult(
                    title = manifest.title,
                    language = manifest.language,
                    itemCount = items.size,
                    extractedPath = packDir.absolutePath,
                    items = items,
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
