package com.example.flashcards.data

import com.example.flashcards.domain.Card

object CardParser {
    fun parse(line: String): Card? {
        val trimmed = line.trim()
        if (trimmed.isBlank()) return null

        val open = trimmed.indexOf('<')
        val close = trimmed.lastIndexOf('>')

        return if (open >= 0 && close > open) {
            val front = trimmed.substring(0, open).trim()
            val back = trimmed.substring(open + 1, close).trim().ifBlank { null }
            if (front.isBlank()) null else Card(id = front, front = front, back = back)
        } else {
            Card(id = trimmed, front = trimmed, back = null)
        }
    }
}
