package com.cinnamon.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.cinnamon.app.data.seed.LexiconSeedState
import com.cinnamon.app.data.startup.AppStartupStage
import com.cinnamon.app.data.startup.AppStartupState
import com.cinnamon.app.ui.theme.CinnamonTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi", sdk = [35])
class StartupCheckpointScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lexiconRecoveryCheckpoint_360dp_screenshot() {
        composeRule.setContent {
            CinnamonTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(20.dp)
                ) {
                    StartupCheckpointCard(
                        startupState = AppStartupState.Failed(AppStartupStage.Lexicon),
                        seedState = LexiconSeedState.Failed,
                        onRetry = {}
                    )
                }
            }
        }

        composeRule.onRoot().captureRoboImage(
            filePath = "../output/cinnamon-v7-startup-recovery-360dp.png"
        )
    }
}
