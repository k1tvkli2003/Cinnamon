package com.cinnamon.app.ui.feedback

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferenceAwareHapticFeedbackTest {
    @Test
    fun disabled_feedback_does_not_reach_platform_delegate() {
        val delegate = RecordingHaptics()

        PreferenceAwareHapticFeedback(delegate, enabled = false)
            .performHapticFeedback(HapticFeedbackType.LongPress)

        assertEquals(0, delegate.callCount)
    }

    @Test
    fun enabled_feedback_reaches_platform_delegate_once() {
        val delegate = RecordingHaptics()

        PreferenceAwareHapticFeedback(delegate, enabled = true)
            .performHapticFeedback(HapticFeedbackType.LongPress)

        assertEquals(1, delegate.callCount)
    }

    private class RecordingHaptics : HapticFeedback {
        var callCount = 0

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            callCount += 1
        }
    }
}
