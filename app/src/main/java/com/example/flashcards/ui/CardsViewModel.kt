package com.example.flashcards.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashcards.data.CardRepository
import com.example.flashcards.data.MediaLocator
import com.example.flashcards.data.SettingsRepository
import com.example.flashcards.data.StatsRepository
import com.example.flashcards.domain.AppSettings
import com.example.flashcards.domain.AudioPlayer
import com.example.flashcards.domain.Card
import com.example.flashcards.domain.CardStat
import com.example.flashcards.domain.SwipeResult
import com.example.flashcards.domain.TextSpeaker
import com.example.flashcards.domain.WeightedCardSelector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class CardsUiState(
    val loading: Boolean = true,
    val currentCard: Card? = null,
    val cardWeight: Double = 0.0,
    val showTranslation: Boolean = false,
    val message: String? = null,
    val audioPath: String? = null,
    val imagePath: String? = null,
    val spoilerOpened: Boolean = false,
    val settings: AppSettings = AppSettings(),
)

class CardsViewModel(app: Application) : AndroidViewModel(app) {
    private val cardRepo = CardRepository(app)
    private val statsRepo = StatsRepository(app)
    private val settingsRepo = SettingsRepository(app)
    private val mediaLocator = MediaLocator(app)
    private val selector = WeightedCardSelector()
    private val audioPlayer = AudioPlayer(mediaLocator)
    private val textSpeaker = TextSpeaker(app)

    private val _uiState = MutableStateFlow(CardsUiState())
    val uiState: StateFlow<CardsUiState> = _uiState.asStateFlow()

    private var cards: List<Card> = emptyList()
    private var stats: MutableMap<String, CardStat> = mutableMapOf()

    init {
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
                if (cards.isEmpty()) load()
            }
        }
    }

    private fun load() {
        cards = cardRepo.loadCards()
        stats = statsRepo.read().toMutableMap()
        nextCard()
    }

    fun revealTranslation() {
        _uiState.update { it.copy(showTranslation = true) }
    }

    fun playAudio(slow: Boolean, startRatio: Float? = null, endRatio: Float? = null) {
        val path = _uiState.value.audioPath ?: return
        val speed = if (slow) _uiState.value.settings.slowAudioSpeed else 1.0f
        audioPlayer.play(path, speed, startRatio, endRatio)
    }

    fun openSpoiler() {
        _uiState.update { it.copy(spoilerOpened = true) }
    }

    fun speakText(text: String) {
        textSpeaker.speak(text)
    }

    fun saveSettings(updated: AppSettings) {
        viewModelScope.launch {
            settingsRepo.updateAll(updated)
        }
    }

    fun onSwipe(result: SwipeResult) {
        viewModelScope.launch {
            runCatching {
                val card = _uiState.value.currentCard ?: return@runCatching
                val settings = _uiState.value.settings

                val safeStepBad = settings.stepBad.takeIf { it.isFinite() && it >= 0.0 } ?: 0.2
                val safeStepGood = settings.stepGood.takeIf { it.isFinite() && it >= 0.0 } ?: 0.2
                val safeLearned = settings.learnedThreshold.takeIf { it.isFinite() } ?: -0.8
                val safeHidden = settings.hiddenWeight.takeIf { it.isFinite() } ?: -1.0
                val current = stats[card.id]?.weight ?: 0.0
                val updated = when (result) {
                    SwipeResult.DONT_KNOW -> min(1.0, current + safeStepBad)
                    SwipeResult.KNOW -> {
                        if (current <= safeLearned) safeHidden
                        else max(safeHidden, current - safeStepGood)
                    }
                }
                val normalized = normalizeWeight(updated)
                stats[card.id] = CardStat(weight = normalized)
                statsRepo.write(stats)

                val label = if (result == SwipeResult.KNOW) "Знаю" else "Не знаю"
                _uiState.update { it.copy(message = label) }
                nextCard()
            }.onFailure {
                _uiState.update {
                    it.copy(message = "Ошибка при сохранении ответа. Проверьте настройки.")
                }
            }
        }
    }

    fun nextCard() {
        val selected = selector.pick(cards, stats, _uiState.value.settings)
        if (selected == null) {
            _uiState.update { it.copy(loading = false, currentCard = null, message = "Активных карточек нет") }
            return
        }
        val weight = stats[selected.id]?.weight ?: 0.0
        _uiState.update {
            it.copy(
                loading = false,
                currentCard = selected,
                cardWeight = weight,
                showTranslation = false,
                audioPath = mediaLocator.findAudio(selected.front),
                imagePath = mediaLocator.findImage(selected.front),
                spoilerOpened = false,
            )
        }
    }

    override fun onCleared() {
        audioPlayer.stop()
        textSpeaker.release()
        super.onCleared()
    }

    private fun normalizeWeight(value: Double): Double {
        val clamped = value.coerceIn(-1.0, 1.0)
        return (clamped * 10.0).roundToInt() / 10.0
    }
}
