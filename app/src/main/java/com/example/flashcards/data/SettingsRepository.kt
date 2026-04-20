package com.example.flashcards.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.flashcards.domain.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val reminderInterval = longPreferencesKey("reminder_interval_minutes")
        val slowAudioSpeed = floatPreferencesKey("slow_audio_speed")
        val stepBad = doublePreferencesKey("step_bad")
        val stepGood = doublePreferencesKey("step_good")
        val priorityThreshold = doublePreferencesKey("priority_threshold")
        val learnedThreshold = doublePreferencesKey("learned_threshold")
        val hiddenWeight = doublePreferencesKey("hidden_weight")
        val chancePower = doublePreferencesKey("chance_power")
        val minChance = doublePreferencesKey("min_chance")
        val spoilerSizePx = intPreferencesKey("spoiler_size_px")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            reminderIntervalMinutes = prefs[Keys.reminderInterval] ?: 120,
            slowAudioSpeed = prefs[Keys.slowAudioSpeed] ?: 0.6f,
            stepBad = prefs[Keys.stepBad] ?: 0.2,
            stepGood = prefs[Keys.stepGood] ?: 0.2,
            priorityThreshold = prefs[Keys.priorityThreshold] ?: 1.0,
            learnedThreshold = prefs[Keys.learnedThreshold] ?: -0.8,
            hiddenWeight = prefs[Keys.hiddenWeight] ?: -1.0,
            chancePower = prefs[Keys.chancePower] ?: 2.0,
            minChance = prefs[Keys.minChance] ?: 0.05,
            spoilerSizePx = prefs[Keys.spoilerSizePx] ?: 512,
        )
    }

    suspend fun updateReminderInterval(minutes: Long) {
        context.dataStore.edit { it[Keys.reminderInterval] = minutes }
    }
}
