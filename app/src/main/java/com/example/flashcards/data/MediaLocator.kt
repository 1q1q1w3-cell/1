package com.example.flashcards.data

import android.content.Context
import java.io.File

class MediaLocator(private val context: Context) {
    private fun engKaRoot(): File = File(context.getExternalFilesDir(null), "ENG_KA")

    fun findAudio(front: String): String? = findMedia("audio", front, "wav")

    fun findImage(front: String): String? = findMedia("image", front, "png")

    private fun findMedia(folder: String, front: String, ext: String): String? {
        val candidates = buildCandidates(front)
        val externalFolder = File(engKaRoot(), folder)
        externalFolder.mkdirs()

        candidates.forEach { name ->
            val f = File(externalFolder, "$name.$ext")
            if (f.exists()) return "file://${f.absolutePath}"
        }

        val assets = runCatching { context.assets.list(folder)?.toSet().orEmpty() }.getOrDefault(emptySet())
        return candidates
            .map { "$it.$ext" }
            .firstOrNull { fileName -> assets.contains(fileName) }
            ?.let { "asset://$folder/$it" }
    }

    private fun buildCandidates(source: String): List<String> {
        val plain = source.trim()
        val noEndPunctuation = plain.trimEnd('.', ',', '!', '?', ';', ':')
        val safe = noEndPunctuation.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return listOf(plain, noEndPunctuation, safe).distinct().filter { it.isNotBlank() }
    }

    fun loadImageBytes(path: String): ByteArray {
        return when {
            path.startsWith("file://") -> File(path.removePrefix("file://")).readBytes()
            path.startsWith("asset://") -> context.assets.open(path.removePrefix("asset://")).use { it.readBytes() }
            else -> context.assets.open(path).use { it.readBytes() }
        }
    }

    fun copyAssetToCache(path: String): File {
        if (path.startsWith("file://")) return File(path.removePrefix("file://"))
        val cacheFile = File(context.cacheDir, path.replace('/', '_').replace(':', '_'))
        if (!cacheFile.exists()) {
            val assetPath = path.removePrefix("asset://")
            context.assets.open(assetPath).use { input ->
                cacheFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return cacheFile
    }
}
