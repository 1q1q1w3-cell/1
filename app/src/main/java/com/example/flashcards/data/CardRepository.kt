package com.example.flashcards.data

import android.content.Context
import com.example.flashcards.domain.Card

class CardRepository(private val context: Context) {
    fun loadCards(fileName: String = "data.txt"): List<Card> {
        val lines = context.assets.open(fileName).bufferedReader().use { it.readLines() }
        return lines.mapNotNull(CardParser::parse)
    }
}
