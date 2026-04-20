package com.example.flashcards.domain

data class Card(
    val id: String,
    val front: String,
    val back: String? = null,
)

data class CardStat(
    val weight: Double = 0.0,
)

data class AppSettings(
    val reminderIntervalMinutes: Long = 120,
    val slowAudioSpeed: Float = 0.6f,
    val stepBad: Double = 0.2,
    val stepGood: Double = 0.2,
    val priorityThreshold: Double = 1.0,
    val learnedThreshold: Double = -0.8,
    val hiddenWeight: Double = -1.0,
    val chancePower: Double = 2.0,
    val minChance: Double = 0.05,
    val spoilerSizePx: Int = 512,
)

enum class SwipeResult {
    KNOW,
    DONT_KNOW,
}
