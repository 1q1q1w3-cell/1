package com.example.flashcards.data

import android.content.Context
import java.io.File

class MediaLocator(private val context: Context) {
    fun findAudio(front: String): String? = findAsset("audio", front, "wav")

    fun findImage(front: String): String? = findAsset("image", front, "png")

    private fun findAsset(folder: String, front: String, ext: String): String? {
        val candidates = buildCandidates(front)
        val assets = runCatching { context.assets.list(folder)?.toSet().orEmpty() }.getOrDefault(emptySet())
        return candidates
            .map { "$it.$ext" }
            .firstOrNull { fileName -> assets.contains(fileName) }
            ?.let { "$folder/$it" }
    }

    private fun buildCandidates(source: String): List<String> {
        val plain = source.trim()
        val noEndPunctuation = plain.trimEnd('.', ',', '!', '?', ';', ':')
        val safe = noEndPunctuation.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return listOf(plain, noEndPunctuation, safe).distinct().filter { it.isNotBlank() }
    }

    fun loadImageBytes(path: String): ByteArray = context.assets.open(path).use { it.readBytes() }

    fun copyAssetToCache(path: String): File {
        val cacheFile = File(context.cacheDir, path.replace('/', '_'))
        if (!cacheFile.exists()) {
            context.assets.open(path).use { input ->
                cacheFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return cacheFile
    }
}
