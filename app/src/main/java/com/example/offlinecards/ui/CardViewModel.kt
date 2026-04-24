package com.example.offlinecards.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlinecards.data.CardRepository
import com.example.offlinecards.model.PhraseCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class CardUiState(
    val currentCard: PhraseCard? = null,
    val userInput: String = "",
    val checkResult: Boolean? = null,
    val cards: List<PhraseCard> = emptyList()
)

class CardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CardRepository(application)

    private val _state = MutableStateFlow(CardUiState())
    val state: StateFlow<CardUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val cards = repository.loadCards()
            _state.update {
                it.copy(
                    cards = cards,
                    currentCard = pickWeightedCard(cards)
                )
            }
        }
    }

    fun onInputChanged(value: String) {
        _state.update { it.copy(userInput = value) }
    }

    fun checkAnswer() {
        val current = _state.value.currentCard ?: return
        val isCorrect = _state.value.userInput.trim() == current.english.trim()
        _state.update { it.copy(checkResult = isCorrect) }
    }

    fun onKnowResult(known: Boolean) {
        val current = _state.value.currentCard ?: return
        viewModelScope.launch {
            val newWeight = if (known) {
                (current.weight - 1).coerceAtLeast(1)
            } else {
                current.weight + 1
            }
            repository.updateWeight(current, newWeight)
            val updatedCards = repository.loadCards()
            _state.update {
                it.copy(
                    cards = updatedCards,
                    currentCard = pickWeightedCard(updatedCards),
                    userInput = "",
                    checkResult = null
                )
            }
        }
    }

    private fun pickWeightedCard(cards: List<PhraseCard>): PhraseCard? {
        if (cards.isEmpty()) return null
        val totalWeight = cards.sumOf { it.weight.coerceAtLeast(1) }
        var target = Random.nextInt(totalWeight)
        for (card in cards) {
            target -= card.weight.coerceAtLeast(1)
            if (target < 0) return card
        }
        return cards.last()
    }
}
