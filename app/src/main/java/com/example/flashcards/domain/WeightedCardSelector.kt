package com.example.flashcards.domain

import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random

class WeightedCardSelector {
    fun pick(cards: List<Card>, stats: Map<String, CardStat>, settings: AppSettings): Card? {
        val available = cards.filter { (stats[it.id]?.weight ?: 0.0) > settings.hiddenWeight }
        if (available.isEmpty()) return null

        val weighted = available.map { card ->
            val weight = stats[card.id]?.weight ?: 0.0
            val normalized = ((weight + 1.0) / 2.0).coerceIn(0.0, 1.0)
            val chance = max(normalized.pow(settings.chancePower), settings.minChance)
            card to chance
        }

        val sum = weighted.sumOf { it.second }
        var ticket = Random.nextDouble() * sum

        weighted.forEach { (card, chance) ->
            ticket -= chance
            if (ticket <= 0.0) return card
        }
        return weighted.last().first
    }
}
