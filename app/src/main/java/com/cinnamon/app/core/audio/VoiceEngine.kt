package com.cinnamon.app.core.audio

/**
 * Honest capability boundary for voice features.
 *
 * No recorder, transcription provider, speech synthesizer, or DSP analyzer is
 * currently wired into the product. Callers must render a guided-practice state
 * until a real implementation and its permissions are available.
 */
object VoiceEngine {
    val capabilities = VoiceCapabilities(
        transcription = false,
        synthesis = false,
        pitchAnalysis = false,
        noiseSuppression = false
    )

    fun analyzePitchAndTone(@Suppress("UNUSED_PARAMETER") audioBuffer: ByteArray): VoiceFeatureResult<ToneAnalysisResult> =
        VoiceFeatureResult.NotConfigured

    suspend fun transcribeAudio(@Suppress("UNUSED_PARAMETER") audioBuffer: ByteArray): VoiceFeatureResult<String> =
        VoiceFeatureResult.NotConfigured

    suspend fun synthesizeSpeech(
        @Suppress("UNUSED_PARAMETER") text: String,
        @Suppress("UNUSED_PARAMETER") emotion: String = "neutral",
        @Suppress("UNUSED_PARAMETER") speedX: Float = 1.0f
    ): VoiceFeatureResult<ByteArray> = VoiceFeatureResult.NotConfigured
}

data class VoiceCapabilities(
    val transcription: Boolean,
    val synthesis: Boolean,
    val pitchAnalysis: Boolean,
    val noiseSuppression: Boolean
)

sealed interface VoiceFeatureResult<out T> {
    data class Available<T>(val value: T) : VoiceFeatureResult<T>
    data object NotConfigured : VoiceFeatureResult<Nothing>
}

data class ToneAnalysisResult(
    val confidenceScore: Float,
    val inflection: String,
    val emotionDetected: String
)
