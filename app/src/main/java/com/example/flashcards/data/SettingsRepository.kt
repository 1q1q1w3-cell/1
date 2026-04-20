package com.example.flashcards.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.flashcards.domain.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val slowAudioSpeed = floatPreferencesKey("slow_audio_speed")
        val stepBad = doublePreferencesKey("step_bad")
        val stepGood = doublePreferencesKey("step_good")
        val priorityThreshold = doublePreferencesKey("priority_threshold")
        val learnedThreshold = doublePreferencesKey("learned_threshold")
        val hiddenWeight = doublePreferencesKey("hidden_weight")
        val chancePower = doublePreferencesKey("chance_power")
        val minChance = doublePreferencesKey("min_chance")
        val spoilerSizePx = intPreferencesKey("spoiler_size_px")
        val showCardWeight = booleanPreferencesKey("show_card_weight")
        val useSwipeMode = booleanPreferencesKey("use_swipe_mode")
        val showAndroidTtsButton = booleanPreferencesKey("show_android_tts_button")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            slowAudioSpeed = prefs[Keys.slowAudioSpeed] ?: 0.6f,
            stepBad = prefs[Keys.stepBad] ?: 0.2,
            stepGood = prefs[Keys.stepGood] ?: 0.2,
            priorityThreshold = prefs[Keys.priorityThreshold] ?: 1.0,
            learnedThreshold = prefs[Keys.learnedThreshold] ?: -0.8,
            hiddenWeight = prefs[Keys.hiddenWeight] ?: -1.0,
            chancePower = prefs[Keys.chancePower] ?: 2.0,
            minChance = prefs[Keys.minChance] ?: 0.05,
            spoilerSizePx = prefs[Keys.spoilerSizePx] ?: 512,
            showCardWeight = prefs[Keys.showCardWeight] ?: false,
            useSwipeMode = prefs[Keys.useSwipeMode] ?: true,
            showAndroidTtsButton = prefs[Keys.showAndroidTtsButton] ?: true,
        )
    }

    suspend fun updateAll(settings: AppSettings) {
        context.dataStore.edit {
            it[Keys.slowAudioSpeed] = settings.slowAudioSpeed
            it[Keys.stepBad] = settings.stepBad
            it[Keys.stepGood] = settings.stepGood
            it[Keys.priorityThreshold] = settings.priorityThreshold
            it[Keys.learnedThreshold] = settings.learnedThreshold
            it[Keys.hiddenWeight] = settings.hiddenWeight
            it[Keys.chancePower] = settings.chancePower
            it[Keys.minChance] = settings.minChance
            it[Keys.spoilerSizePx] = settings.spoilerSizePx
            it[Keys.showCardWeight] = settings.showCardWeight
            it[Keys.useSwipeMode] = settings.useSwipeMode
            it[Keys.showAndroidTtsButton] = settings.showAndroidTtsButton
        }
    }
}
