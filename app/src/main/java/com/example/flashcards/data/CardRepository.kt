package com.example.flashcards.data

import android.content.Context
import android.os.Environment
import com.example.flashcards.domain.Card
import java.io.File

class CardRepository(private val context: Context) {
    private fun engKaRoot(): File {
        val public = File(Environment.getExternalStorageDirectory(), "ENG_KA2")
        if (public.exists() || public.mkdirs()) return public
        return File(context.getExternalFilesDir(null), "ENG_KA2")
    }

    private fun ensureDefaultStructure() {
        val root = engKaRoot()
        val audio = File(root, "audio")
        val image = File(root, "image")
        if (!audio.exists()) audio.mkdirs()
        if (!image.exists()) image.mkdirs()

        val data = File(root, "data.txt")
        if (!data.exists()) {
            val default = context.assets.open("data.txt").bufferedReader().use { it.readText() }
            data.parentFile?.mkdirs()
            runCatching { data.writeText(default) }.onFailure {
                val fallback = File(context.getExternalFilesDir(null), "ENG_KA2/data.txt")
                fallback.parentFile?.mkdirs()
                fallback.writeText(default)
            }
        }
    }

    fun loadCards(fileName: String = "data.txt"): List<Card> {
        ensureDefaultStructure()
        val externalFile = File(engKaRoot(), fileName)
        val lines = if (externalFile.exists()) {
            externalFile.readLines()
        } else {
            context.assets.open(fileName).bufferedReader().use { it.readLines() }
        }
        return lines.mapNotNull(CardParser::parse)
    }
}
