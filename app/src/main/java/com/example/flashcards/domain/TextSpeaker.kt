package com.example.flashcards.domain

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextSpeaker(context: Context) {
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) {
            if (it == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "card_tts")
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
