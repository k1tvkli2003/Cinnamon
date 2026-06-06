package com.example.ui.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

object SoundSynthesizer {
    private const val SAMPLE_RATE = 22050

    /**
     * Synthesizes and plays a short frequency sweeps representing UI pop, click, or success swoosh sounds.
     * This runs asynchronously on Dispatchers.Default to ensure the main thread never blocks.
     */
    suspend fun playSynthesizedSound(soundType: SoundType) = withContext(Dispatchers.Default) {
        try {
            val durationMs = soundType.durationMs
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val sample = DoubleArray(numSamples)
            val generatedSnd = ByteArray(2 * numSamples)

            // Fill array with sinewave frequencies to create bespoke sound signatures
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                // Sweep frequencies dynamically based on sound type
                val frequency = when (soundType) {
                    SoundType.CLICK -> 440.0 // Constant clear pitch
                    SoundType.POP -> 300.0 + (150.0 * (1.0 - (t / (durationMs / 1000.0)))) // Descending pitch
                    SoundType.SWOOSH -> 150.0 + (800.0 * (t / (durationMs / 1000.0))) // Quick rising pitch sweep
                    SoundType.SUCCESS -> {
                        // Double overlapping chime notes
                        val f1 = 523.25 // C5
                        val f2 = 659.25 // E5
                        (sin(2.0 * Math.PI * f1 * t) + sin(2.0 * Math.PI * f2 * t)) / 2.0
                    }
                }
                
                if (soundType == SoundType.SUCCESS) {
                    sample[i] = frequency
                } else {
                    sample[i] = sin(2.0 * Math.PI * frequency * t)
                }

                // Envelope padding to fade out smoothly and prevent click artifacts
                val envelope = if (i > numSamples - 200) {
                    (numSamples - i) / 200.0
                } else if (i < 200) {
                    i / 200.0
                } else {
                    1.0
                }
                sample[i] = sample[i] * envelope
            }

            // Convert to 16 bit PCM sound array
            var idx = 0
            for (doubleVal in sample) {
                val valShort = (doubleVal * 32767).toInt().toShort()
                generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
            }

            // Initialize and play using AudioTrack builder with modern AudioAttributes
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(generatedSnd.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(generatedSnd, 0, generatedSnd.size)
            audioTrack.play()
            
            // Release after play duration to prevent any memory leaks
            kotlinx.coroutines.delay(durationMs)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    enum class SoundType(val durationMs: Long) {
        CLICK(40L),
        POP(80L),
        SWOOSH(150L),
        SUCCESS(250L)
    }
}
