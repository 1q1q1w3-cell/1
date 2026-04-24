package com.example.offlinecards.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.offlinecards.model.PhraseCard
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "card_weights")

class CardRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val weightsKey = stringPreferencesKey("weights_json")

    suspend fun loadCards(): List<PhraseCard> {
        val base = parseAsset()
        val savedWeights = loadWeights()
        return base.map { card ->
            val weight = savedWeights[card.english] ?: 1
            card.copy(weight = weight)
        }
    }

    suspend fun updateWeight(card: PhraseCard, newWeight: Int) {
        val weights = loadWeights().toMutableMap()
        weights[card.english] = newWeight.coerceAtLeast(1)
        context.dataStore.edit { prefs ->
            prefs[weightsKey] = json.encodeToString(weights)
        }
    }

    private suspend fun loadWeights(): Map<String, Int> {
        val raw = context.dataStore.data.first()[weightsKey] ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, Int>>(raw) }
            .getOrDefault(emptyMap())
    }

    private fun parseAsset(): List<PhraseCard> {
        return context.assets.open("data.txt").bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isBlank()) return@mapNotNull null
                val open = trimmed.indexOf('<')
                val close = trimmed.lastIndexOf('>')
                if (open <= 0 || close <= open) return@mapNotNull null

                val english = trimmed.substring(0, open).trim()
                val russian = trimmed.substring(open + 1, close).trim()
                if (english.isBlank() || russian.isBlank()) return@mapNotNull null

                PhraseCard(english = english, russian = russian)
            }.toList()
        }
    }
}
