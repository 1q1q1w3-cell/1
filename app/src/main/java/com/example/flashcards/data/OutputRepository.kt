package com.example.flashcards.data

import android.content.Context
import java.io.File

class OutputRepository(context: Context) {
    private val file = File(context.filesDir, "output.txt")

    fun appendUnique(line: String) {
        val existing = if (file.exists()) file.readLines().toSet() else emptySet()
        if (line !in existing) {
            file.appendText(if (file.exists() && file.length() > 0) "\n$line" else line)
        }
    }
}
