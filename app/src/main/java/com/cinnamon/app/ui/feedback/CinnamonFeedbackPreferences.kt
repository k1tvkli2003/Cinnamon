package com.cinnamon.app.ui.feedback

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

data class CinnamonFeedbackPreferences(
    val soundEffectsEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val reduceMotion: Boolean = false
)

val LocalCinnamonFeedbackPreferences = staticCompositionLocalOf {
    CinnamonFeedbackPreferences()
}

internal class PreferenceAwareHapticFeedback(
    private val delegate: HapticFeedback,
    private val enabled: Boolean
) : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        if (enabled) delegate.performHapticFeedback(hapticFeedbackType)
    }
}
