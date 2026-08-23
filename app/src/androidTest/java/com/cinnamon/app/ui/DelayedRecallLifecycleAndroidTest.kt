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
import com.cinnamon.app.data.gamification.RewardableEventType
import com.cinnamon.app.data.local.AppDatabase
import com.cinnamon.app.domain.repository.LexiconRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves durable recall comes from the real review producer, not a generic reward call: three
 * later successful reviews are linked to three prior reviews exactly 72 hours earlier, unlock
 * the catalog achievement once, and render their verified progress in Profile.
 */
@RunWith(AndroidJUnit4::class)
class DelayedRecallLifecycleAndroidTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun threeLaterReviews_unlockDelayedRecall_once_andRenderInProfile() {
        waitForText("Practice")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = runBlocking { GamificationDeviceTestState.reset(context) }
        val repository = LexiconRepository.getInstance(context)
        val gamification = GamificationRepository.getInstance(context)
        val entries = runBlocking { database.lexiconDao().freshEntries(DELAYED_RECALL_TARGET) }
        assertEquals(DELAYED_RECALL_TARGET, entries.size)

        val reviewedEntries = entries.mapIndexed { index, entry ->
            runBlocking {
                repository.commitReview(
                    expectedEntry = entry,
                    quality = 4,
                    occurrenceKey = "delayed-recall-$index-first",
                    occurredAtEpochMillis = BASE_TIME_EPOCH_MILLIS + index
                ).entry
            }
        }
        reviewedEntries.forEachIndexed { index, entry ->
            val result = runBlocking {
                repository.commitReview(
                    expectedEntry = entry,
                    quality = 4,
                    occurrenceKey = "delayed-recall-$index-later",
                    occurredAtEpochMillis = BASE_TIME_EPOCH_MILLIS + DELAYED_RECALL_MILLIS + index
                )
            }
            assertTrue(result.xpAwarded >= REVIEW_XP)
        }

        val dao = database.gamificationDao()
        runBlocking {
            assertEquals(DELAYED_RECALL_TARGET, gamification.delayedRecallCount.first())
            val recalls = dao.observeRecentLearningActivity(
                actorId = GamificationRepository.LOCAL_ACTOR_ID,
                limit = 64
            ).first().filter { activity ->
                activity.eventType == RewardableEventType.DELAYED_RECALL_SUCCEEDED.wireName
            }
            assertEquals(DELAYED_RECALL_TARGET, recalls.size)
            recalls.forEach { recall ->
                val event = checkNotNull(dao.eventById(recall.eventId))
                assertTrue(event.metadataJson.contains("\"delayedRecallLinkPresent\":true"))
                assertTrue(event.metadataJson.contains("\"minimumDelayHoursSatisfied\":true"))
                assertTrue(event.metadataJson.contains("\"priorReviewEventId\""))
            }

            val unlock = dao.observeAchievementUnlocks(GamificationRepository.LOCAL_ACTOR_ID)
                .first().single { achievement ->
                    achievement.achievementId == DELAYED_RECALL_ACHIEVEMENT_ID && achievement.level == 1
                }
            assertTrue(unlock.evidenceJson.contains("distinct_delayed_recalls"))

            val replay = gamification.recordVerifiedDelayedRecall(
                lexiconEntryId = reviewedEntries.first().id,
                occurrenceKey = "delayed-recall-replay",
                occurredAtEpochMillis = BASE_TIME_EPOCH_MILLIS + DELAYED_RECALL_MILLIS + 10_000L
            )
            assertNull(replay)
            assertEquals(DELAYED_RECALL_TARGET, gamification.delayedRecallCount.first())
        }

        composeRule.activityRule.scenario.recreate()
        waitForText("Profile")
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Still There Tomorrow"))
        composeRule.onNodeWithText("Still There Tomorrow")
            .assertIsDisplayed()
        composeRule.onNodeWithText("3 / 3 evidence recorded")
            .assertIsDisplayed()
    }

    private fun waitForText(text: String, timeoutMillis: Long = 45_000L) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithText(text)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private companion object {
        const val DELAYED_RECALL_TARGET = 3
        const val REVIEW_XP = 12
        const val DELAYED_RECALL_MILLIS = 72L * 60L * 60L * 1_000L
        const val BASE_TIME_EPOCH_MILLIS = 1_800_000_000_000L
        const val DELAYED_RECALL_ACHIEVEMENT_ID = "achievement.challenge.delayed_recall"
    }
}
