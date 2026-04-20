package com.example.flashcards.data

import android.content.Context
import com.example.flashcards.domain.Card
import java.io.File

class CardRepository(private val context: Context) {
    private fun engKaRoot(): File = File(context.getExternalFilesDir(null), "ENG_KA")

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
            data.writeText(default)
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
