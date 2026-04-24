package com.example.offlinecards.model

import kotlinx.serialization.Serializable

@Serializable
data class PhraseCard(
    val english: String,
    val russian: String,
    val weight: Int = 1
)
