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
import com.cinnamon.app.domain.repository.LexiconRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device proof for Clear the Deck: the opening size and terminal zero both come from Room, while
 * repeated callbacks and generic event injection cannot manufacture another clear day.
 */
@RunWith(AndroidJUnit4::class)
class ReviewQueueResolutionLifecycleAndroidTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun fiveDueItemsCleared_unlockAchievement_completeWeeklyQuest_andRenderInProfile() {
        waitForText("Practice")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = runBlocking {
            GamificationDeviceTestState.reset(context) { appDatabase, nowEpochMillis ->
                appDatabase.lexiconDao().freshEntries(QUEUE_TARGET).forEach { entry ->
                    appDatabase.lexiconDao().update(entry.copy(dueAt = nowEpochMillis - 1_000L))
                }
            }
        }
        val repository = LexiconRepository.getInstance(context)
        val gamification = GamificationRepository.getInstance(context)

        val genericInjection = runCatching {
            runBlocking {
                gamification.record(
                    LearningEventCommand(
                        eventType = RewardableEventType.REVIEW_QUEUE_CLEARED,
                        subjectType = "review_queue",
                        subjectId = "forged-day",
                        occurrenceKey = "forged-queue-clear",
                        completedItemCount = QUEUE_TARGET,
                        source = LearningEventSource.REVIEW,
                        occurredAtEpochMillis = System.currentTimeMillis()
                    )
                )
            }
        }
        assertTrue(genericInjection.exceptionOrNull() is IllegalArgumentException)

        val session = runBlocking { repository.buildReviewSession(limit = QUEUE_TARGET) }
        assertEquals(QUEUE_TARGET, session.size)
        session.forEachIndexed { index, entry ->
            runBlocking {
                repository.commitReview(
                    expectedEntry = entry,
                    quality = 4,
                    occurrenceKey = "queue-clear-review-$index",
                    occurredAtEpochMillis = System.currentTimeMillis()
                )
            }
        }

        val dao = database.gamificationDao()
        runBlocking {
            assertEquals(0, database.lexiconDao().dueCountOnce(System.currentTimeMillis()))
            assertEquals(1, gamification.verifiedReviewQueueClearDayCount.first())

            val unlock = dao.observeAchievementUnlocks(GamificationRepository.LOCAL_ACTOR_ID)
                .first().single { achievement ->
                    achievement.achievementId == QUEUE_ACHIEVEMENT_ID && achievement.level == 1
                }
            assertTrue(unlock.evidenceJson.contains("verified_review_queue_clear_days"))

            val queueQuest = dao.observeActiveQuestInstances(GamificationRepository.LOCAL_ACTOR_ID)
                .first().single { quest -> quest.definitionId == QUEUE_QUEST_ID }
            assertEquals(1L, queueQuest.progress)
            assertEquals("completed", queueQuest.state)

            // A second terminal check reuses the same stable day-level event and cannot farm
            // progress, XP, unlocks, or another quest completion.
            gamification.recordVerifiedReviewQueueCleared(System.currentTimeMillis())
            assertEquals(1, gamification.verifiedReviewQueueClearDayCount.first())
            assertEquals(
                1,
                dao.observeAchievementUnlocks(GamificationRepository.LOCAL_ACTOR_ID)
                    .first().count { achievement ->
                        achievement.achievementId == QUEUE_ACHIEVEMENT_ID &&
                            achievement.level == 1
                    }
            )
        }

        composeRule.activityRule.scenario.recreate()
        waitForText("Profile")
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Clear the Deck"))
        composeRule.onNodeWithText("Clear the Deck").assertIsDisplayed()
        composeRule.onNodeWithText("1 / 1 evidence recorded").assertIsDisplayed()
    }

    private fun waitForText(text: String, timeoutMillis: Long = 45_000L) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithText(text)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private companion object {
        const val QUEUE_TARGET = 5
        const val QUEUE_ACHIEVEMENT_ID = "achievement.review.queue_resolved"
        const val QUEUE_QUEST_ID = "quest.weekly.queue_relief"
    }
}
