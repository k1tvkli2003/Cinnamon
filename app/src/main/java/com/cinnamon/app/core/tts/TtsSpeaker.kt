package com.cinnamon.app.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Thin wrapper around the platform TTS for headword pronunciation. */
object TtsSpeaker {

    private val lock = Any()
    private var tts: TextToSpeech? = null
    @Volatile
    private var ready = false
    private var pendingText: String? = null
    private var initializing = false
    private var initializationFailed = false

    /**
     * Defers binding the platform speech service until a learner explicitly
     * requests pronunciation. Startup must not wait on an optional device
     * service, and the first requested word is retained until initialization
     * completes.
     */
    fun speak(context: Context, text: String) {
        if (text.isBlank()) return

        var synchronousFirstUtterance: Pair<TextToSpeech, String>? = null
        synchronized(lock) {
            if (ready) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
                return
            }

            pendingText = text
            if (tts != null) return

            initializationFailed = false
            initializing = true
            tts = TextToSpeech(context.applicationContext, ::onInitialized)
            initializing = false

            // Some engines invoke their initialization callback synchronously,
            // before the assignment above has made [tts] visible to it.
            if (initializationFailed) {
                tts?.shutdown()
                tts = null
            } else if (ready) {
                tts?.let { engine ->
                    configure(engine)
                    pendingText?.let { pending ->
                        pendingText = null
                        synchronousFirstUtterance = engine to pending
                    }
                }
            }
        }
        synchronousFirstUtterance?.let { (engine, pending) ->
            engine.speak(pending, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        }
    }

    fun shutdown() {
        synchronized(lock) {
            tts?.shutdown()
            tts = null
            ready = false
            pendingText = null
            initializing = false
            initializationFailed = false
        }
    }

    private fun onInitialized(status: Int) {
        val pendingUtterance = synchronized(lock) {
            if (status != TextToSpeech.SUCCESS) {
                ready = false
                pendingText = null
                initializationFailed = true
                tts = null
                null
            } else {
                ready = true
                tts?.let(::configure)
                if (initializing) {
                    null
                } else {
                    tts?.let { engine ->
                        pendingText?.let { pending ->
                            pendingText = null
                            engine to pending
                        }
                    }
                }
            }
        }
        pendingUtterance?.let { (engine, pending) ->
            engine.speak(pending, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        }
    }

    private fun configure(engine: TextToSpeech) {
        engine.language = Locale.US
        engine.setSpeechRate(0.9f)
    }

    private const val UTTERANCE_ID = "cinnamon_tts"
}
