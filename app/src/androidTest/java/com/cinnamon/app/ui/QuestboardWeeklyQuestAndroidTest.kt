package com.cinnamon.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinnamon.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the real navigation path that exposes the persisted weekly quest.
 *
 * This is deliberately an Activity-level test instead of a composable fixture: startup, Room
 * reconciliation, bottom navigation, the Practice information architecture, Questboard tabs, and
 * the final projection all have to agree before the assertion can pass.
 */
@RunWith(AndroidJUnit4::class)
class QuestboardWeeklyQuestAndroidTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun freshStartup_weeklyQuestIsReachableFromPractice() {
        waitForText("Practice")
        composeRule.onNodeWithText("Practice").performClick()

        waitForText("Learning Questboard")
        composeRule.onNodeWithText("Learning Questboard")
            .performScrollTo()
            .performClick()

        waitForText("Learning Plan")
        composeRule.onNodeWithText("Quests").performClick()

        waitForText("Make eight memories stick")
        composeRule.onNodeWithText("YOUR QUESTS").assertIsDisplayed()
        composeRule.onNodeWithText("Make eight memories stick").assertIsDisplayed()
        composeRule.onNode(hasText("of 8 complete", substring = true)).assertIsDisplayed()
        composeRule.onNodeWithText("SAVED PROGRESS").assertIsDisplayed()
    }

    private fun waitForText(text: String, timeoutMillis: Long = 30_000L) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithText(text)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }
}
