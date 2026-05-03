package com.example.flashcards.domain

import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random

class WeightedCardSelector {
    fun pick(cards: List<Card>, stats: Map<String, CardStat>, settings: AppSettings): Card? {
        val safeHiddenWeight = settings.hiddenWeight.takeIf { it.isFinite() } ?: -1.0
        val safeChancePower = settings.chancePower.takeIf { it.isFinite() && it >= 0.0 } ?: 2.0
        val safeMinChance = settings.minChance.takeIf { it.isFinite() && it >= 0.0 } ?: 0.05

        val available = cards.filter { (stats[it.id]?.weight ?: 0.0) > safeHiddenWeight }
        if (available.isEmpty()) return null

        val weighted = available.map { card ->
            val weight = stats[card.id]?.weight ?: 0.0
            val normalized = ((weight + 1.0) / 2.0).coerceIn(0.0, 1.0)
            val chance = max(normalized.pow(safeChancePower), safeMinChance)
            card to chance
        }

        val sum = weighted.sumOf { it.second }.takeIf { it.isFinite() && it > 0.0 } ?: return available.random()
        var ticket = Random.nextDouble() * sum

        weighted.forEach { (card, chance) ->
            ticket -= chance
            if (ticket <= 0.0) return card
        }
        return weighted.last().first
    }
}
