package com.cinnamon.app.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Thin wrapper around the platform TTS for headword pronunciation. */
object TtsSpeaker {

    private var tts: TextToSpeech? = null
    @Volatile
    private var ready = false

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.9f)
                ready = true
            }
        }
    }

    fun speak(text: String) {
        if (ready && text.isNotBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cinnamon_tts")
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
