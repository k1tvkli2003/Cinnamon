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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device proof for Kept and Learned: a bookmark is durable learner intent, but only a successful
 * review of that same still-saved word at least an hour later unlocks catalog progress.
 */
@RunWith(AndroidJUnit4::class)
class SavedItemReviewLifecycleAndroidTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun tenSavedWordsReviewedLater_unlockKeptAndLearned_once_andRenderInProfile() {
        waitForText("Practice")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = runBlocking { GamificationDeviceTestState.reset(context) }
        val repository = LexiconRepository.getInstance(context)
        val gamification = GamificationRepository.getInstance(context)
        val entries = runBlocking { database.lexiconDao().freshEntries(SAVED_ITEM_TARGET) }
        assertEquals(SAVED_ITEM_TARGET, entries.size)
        val genericInjection = runCatching {
            runBlocking {
                gamification.record(
                    LearningEventCommand(
                        eventType = RewardableEventType.SAVED_ITEM_REVIEWED,
                        subjectType = "lexicon_entry",
                        subjectId = entries.first().id.toString(),
                        occurrenceKey = "forged-saved-item-review",
                        completedItemCount = 1,
                        source = LearningEventSource.REVIEW,
                        occurredAtEpochMillis = BASE_TIME_EPOCH_MILLIS
                    )
                )
            }
        }
        assertTrue(genericInjection.exceptionOrNull() is IllegalArgumentException)

        entries.forEachIndexed { index, entry ->
            // Use separate study days: the proof must exercise the catalog unlock, not be
            // coupled to a per-day direct-XP cap.
            val savedAt = BASE_TIME_EPOCH_MILLIS + index * ONE_DAY_MILLIS
            runBlocking { repository.toggleBookmark(entry, savedAt) }
            val reviewed = runBlocking {
                repository.commitReview(
                    expectedEntry = entry,
                    quality = 4,
                    occurrenceKey = "saved-item-review-$index",
                    occurredAtEpochMillis = savedAt + ONE_HOUR_MILLIS
                )
            }
            assertTrue(reviewed.xpAwarded >= REVIEW_XP)
        }

        val dao = database.gamificationDao()
        runBlocking {
            assertEquals(SAVED_ITEM_TARGET, gamification.verifiedSavedItemCount.first())
            val savedReviews = dao.observeRecentLearningActivity(
                actorId = GamificationRepository.LOCAL_ACTOR_ID,
                limit = 64
            ).first().filter { activity ->
                activity.eventType == RewardableEventType.SAVED_ITEM_REVIEWED.wireName
            }
            assertEquals(SAVED_ITEM_TARGET, savedReviews.size)
            savedReviews.forEach { activity ->
                val event = checkNotNull(dao.eventById(activity.eventId))
                assertTrue(event.metadataJson.contains("\"savedItemLinkPresent\":true"))
                assertTrue(event.metadataJson.contains("\"minimumDelayHoursSatisfied\":true"))
                assertTrue(event.metadataJson.contains("\"successfulReviewCount\":1"))
            }

            val unlock = dao.observeAchievementUnlocks(GamificationRepository.LOCAL_ACTOR_ID)
                .first().single { achievement ->
                    achievement.achievementId == SAVED_ITEM_ACHIEVEMENT_ID && achievement.level == 1
                }
            assertTrue(unlock.evidenceJson.contains("distinct_verified_saved_items"))
            assertNull(
                gamification.recordVerifiedSavedItemReview(
                    lexiconEntryId = entries.first().id,
                    occurrenceKey = "saved-item-review-replay",
                    occurredAtEpochMillis = BASE_TIME_EPOCH_MILLIS + 2 * ONE_HOUR_MILLIS
                )
            )
            assertEquals(SAVED_ITEM_TARGET, gamification.verifiedSavedItemCount.first())
        }

        composeRule.activityRule.scenario.recreate()
        waitForText("Profile")
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Kept and Learned"))
        composeRule.onNodeWithText("Kept and Learned").assertIsDisplayed()
        composeRule.onNodeWithText("10 / 10 evidence recorded").assertIsDisplayed()
    }

    private fun waitForText(text: String, timeoutMillis: Long = 45_000L) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithText(text)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private companion object {
        const val SAVED_ITEM_TARGET = 10
        const val REVIEW_XP = 8
        const val ONE_HOUR_MILLIS = 60L * 60L * 1_000L
        const val ONE_DAY_MILLIS = 24L * ONE_HOUR_MILLIS
        const val BASE_TIME_EPOCH_MILLIS = 1_840_000_000_000L
        const val SAVED_ITEM_ACHIEVEMENT_ID = "achievement.collection.learned_not_saved"
    }
}
