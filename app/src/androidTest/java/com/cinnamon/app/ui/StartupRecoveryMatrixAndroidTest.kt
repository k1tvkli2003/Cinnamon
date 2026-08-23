package com.cinnamon.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinnamon.app.data.seed.LexiconSeedState
import com.cinnamon.app.data.startup.AppStartupStage
import com.cinnamon.app.data.startup.AppStartupState
import com.cinnamon.app.ui.screens.home.StartupCheckpointCard
import com.cinnamon.app.ui.screens.home.startupCheckpointPresentation
import com.cinnamon.app.ui.theme.CinnamonTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupRecoveryMatrixAndroidTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everyFailureStage_rendersSpecificRecoveryAndWorkingAction() {
        var startupState by mutableStateOf<AppStartupState>(
            AppStartupState.Failed(AppStartupStage.GamificationCatalog)
        )
        var retryCount by mutableIntStateOf(0)

        composeRule.setContent {
            CinnamonTheme {
                StartupCheckpointCard(
                    startupState = startupState,
                    seedState = LexiconSeedState.Failed,
                    onRetry = { retryCount += 1 }
                )
            }
        }

        AppStartupStage.entries.forEach { stage ->
            val expected = startupCheckpointPresentation(
                startupState = AppStartupState.Failed(stage),
                seedState = LexiconSeedState.Failed
            )
            composeRule.runOnIdle { startupState = AppStartupState.Failed(stage) }
            composeRule.onNodeWithText(expected.title).assertIsDisplayed()
            composeRule.onNodeWithText(expected.body).assertIsDisplayed()
            composeRule.onNodeWithText(expected.protectionNote).assertIsDisplayed()
            composeRule.onNodeWithText(expected.actionLabel!!)
                .assertIsDisplayed()
                .assertHasClickAction()
        }

        val active = startupCheckpointPresentation(startupState, LexiconSeedState.Failed)
        composeRule.onNodeWithText(active.actionLabel!!).performClick()
        composeRule.runOnIdle { assertEquals(1, retryCount) }
    }
}
