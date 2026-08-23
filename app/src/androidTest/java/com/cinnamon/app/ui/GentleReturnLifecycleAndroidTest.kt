package com.cinnamon.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cinnamon.app.MainActivity
import com.cinnamon.app.data.gamification.GamificationRepository
import com.cinnamon.app.data.gamification.LearningEventCommand
import com.cinnamon.app.data.gamification.LearningEventSource
import com.cinnamon.app.data.gamification.RewardableEventType
import com.cinnamon.app.data.local.RewardWriteStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device proof for Gentle Return: only a genuine game session after seven quiet days can unlock
 * the comeback achievement. Replaying the same session neither creates another return nor reward.
 */
@RunWith(AndroidJUnit4::class)
class GentleReturnLifecycleAndroidTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun realPracticeAfterSevenDays_unlocksGentleReturn_once_andRendersInProfile() {
        waitForText("Practice")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = runBlocking { GamificationDeviceTestState.reset(context) }
        val gamification = GamificationRepository.getInstance(context)
        val prior = runBlocking {
            gamification.record(
                LearningEventCommand(
                    eventType = RewardableEventType.REVIEW_COMPLETED,
                    subjectType = "lexicon_entry",
                    subjectId = "return-anchor",
                    occurrenceKey = "gentle-return-anchor",
                    completedItemCount = 1,
                    source = LearningEventSource.REVIEW,
                    occurredAtEpochMillis = BASE_TIME_EPOCH_MILLIS
                )
            )
        }
        assertEquals(RewardWriteStatus.APPLIED, prior.status)

        val session = LearningEventCommand(
            eventType = RewardableEventType.PRACTICE_SESSION_COMPLETED,
            subjectType = "scenario_language_sprint",
            subjectId = "gentle-return-practice",
            occurrenceKey = "gentle-return-session-v1",
            completedItemCount = 3,
            source = LearningEventSource.GAME,
            occurredAtEpochMillis = BASE_TIME_EPOCH_MILLIS + SEVEN_DAYS_MILLIS
        )
        val practice = runBlocking { gamification.recordPracticeSessionWithComeback(session) }
        assertEquals(RewardWriteStatus.APPLIED, practice.status)

        val dao = database.gamificationDao()
        runBlocking {
            assertEquals(1, gamification.verifiedComebackSessionCount.first())
            val returnEvent = dao.observeRecentLearningActivity(
                actorId = GamificationRepository.LOCAL_ACTOR_ID,
                limit = 16
            ).first().single { activity ->
                activity.eventType == RewardableEventType.COMEBACK_SESSION_COMPLETED.wireName
            }
            val event = checkNotNull(dao.eventById(returnEvent.eventId))
            assertTrue(event.metadataJson.contains("\"comebackLinkPresent\":true"))
            assertTrue(event.metadataJson.contains("\"minimumAbsenceDaysSatisfied\":true"))
            assertTrue(event.metadataJson.contains("\"meaningfulActionCount\":3"))

            val unlock = dao.observeAchievementUnlocks(GamificationRepository.LOCAL_ACTOR_ID)
                .first().single { achievement ->
                    achievement.achievementId == COMEBACK_ACHIEVEMENT_ID && achievement.level == 1
                }
            assertTrue(unlock.evidenceJson.contains("verified_comeback_sessions"))
            assertEquals(RewardWriteStatus.DUPLICATE, gamification.recordPracticeSessionWithComeback(session).status)
            assertEquals(1, gamification.verifiedComebackSessionCount.first())
        }

        composeRule.activityRule.scenario.recreate()
        waitForText("Profile")
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Welcome Back, Gently"))
        composeRule.onNodeWithText("Welcome Back, Gently").assertIsDisplayed()
        composeRule.onNodeWithText("Complete one small comeback session").assertIsDisplayed()
    }

    private fun waitForText(text: String, timeoutMillis: Long = 45_000L) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithText(text)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private companion object {
        const val BASE_TIME_EPOCH_MILLIS = 1_830_000_000_000L
        const val SEVEN_DAYS_MILLIS = 7L * 24L * 60L * 60L * 1_000L
        const val COMEBACK_ACHIEVEMENT_ID = "achievement.comeback.gentle_return"
    }
}
