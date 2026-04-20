package com.example.flashcards.data

import android.content.Context
import com.example.flashcards.domain.CardStat
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class StatsRepository(private val context: Context) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val file: File = File(context.filesDir, "stats.json")

    @Serializable
    private data class StatsPayload(val stats: Map<String, Double> = emptyMap())

    fun read(): Map<String, CardStat> {
        if (!file.exists()) return emptyMap()
        return runCatching {
            val payload = json.decodeFromString<StatsPayload>(file.readText())
            payload.stats.mapValues { CardStat(weight = it.value) }
        }.getOrDefault(emptyMap())
    }

    fun write(stats: Map<String, CardStat>) {
        val payload = StatsPayload(stats = stats.mapValues { it.value.weight })
        file.writeText(json.encodeToString(payload))
    }
}
