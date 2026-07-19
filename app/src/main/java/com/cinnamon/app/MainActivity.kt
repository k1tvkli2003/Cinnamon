package com.cinnamon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cinnamon.app.ui.feedback.CinnamonFeedbackPreferences
import com.cinnamon.app.ui.feedback.LocalCinnamonFeedbackPreferences
import com.cinnamon.app.ui.feedback.PreferenceAwareHapticFeedback
import com.cinnamon.app.ui.navigation.AppNavigation
import com.cinnamon.app.ui.theme.CinnamonTheme
import com.cinnamon.app.ui.util.SoundSynthesizer
import com.cinnamon.app.viewmodel.UserProgressViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaunchedEffect(Unit) {
                // Submit a truthful, non-interactive loading frame before the
                // CPU/IO startup pipeline begins. The work itself remains on
                // the Application-owned scope and survives Activity recreation.
                // The first callback is the start of a frame; the second is
                // reached only after that first loading frame has been drawn.
                withFrameNanos { }
                withFrameNanos { }
                (application as CinnamonApp).launchStartupPreparation()
            }
            val progressViewModel: UserProgressViewModel = viewModel()
            val selectedTheme by progressViewModel.selectedTheme.collectAsState()
            val soundEffectsEnabled by progressViewModel.soundEffectsEnabled.collectAsState()
            val hapticFeedbackEnabled by progressViewModel.hapticFeedbackEnabled.collectAsState()
            val reduceMotion by progressViewModel.reduceMotion.collectAsState()
            val platformHaptics = LocalHapticFeedback.current
            val effectiveHaptics = remember(platformHaptics, hapticFeedbackEnabled) {
                PreferenceAwareHapticFeedback(platformHaptics, hapticFeedbackEnabled)
            }
            val feedbackPreferences = remember(soundEffectsEnabled, hapticFeedbackEnabled, reduceMotion) {
                CinnamonFeedbackPreferences(
                    soundEffectsEnabled = soundEffectsEnabled,
                    hapticFeedbackEnabled = hapticFeedbackEnabled,
                    reduceMotion = reduceMotion
                )
            }

            LaunchedEffect(soundEffectsEnabled) {
                SoundSynthesizer.setEffectsEnabled(soundEffectsEnabled)
            }

            CompositionLocalProvider(
                LocalHapticFeedback provides effectiveHaptics,
                LocalCinnamonFeedbackPreferences provides feedbackPreferences
            ) {
                CinnamonTheme(themeName = selectedTheme) {
                    AppNavigation(progressViewModel = progressViewModel)
                }
            }
        }
    }
}
