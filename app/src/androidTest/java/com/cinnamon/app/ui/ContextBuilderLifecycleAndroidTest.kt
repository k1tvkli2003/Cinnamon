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
import com.cinnamon.app.data.local.RewardWriteStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device proof for Context Builder: only repository-verified bundled entries create evidence;
 * five distinct applications unlock the cabinet item and the daily one-item quest claims once.
 */
@RunWith(AndroidJUnit4::class)
class ContextBuilderLifecycleAndroidTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verifiedContextApplications_unlockBuilder_andClaimDailyQuest() {
        waitForText("Practice")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = runBlocking { GamificationDeviceTestState.reset(context) }
        val gamification = GamificationRepository.getInstance(context)
        val entries = runBlocking { database.lexiconDao().freshEntries(CONTEXT_TARGET) }
        assertEquals(CONTEXT_TARGET, entries.size)

        entries.forEachIndexed { index, entry ->
            val result = runBlocking {
                gamification.recordVerifiedContextApplication(
                    lexiconEntryId = entry.id,
                    occurrenceKey = "context-builder-$index",
                    occurredAtEpochMillis = BASE_TIME_EPOCH_MILLIS + index
                )
            }
            assertNotNull(result)
        }

        val dao = database.gamificationDao()
        runBlocking {
            assertEquals(CONTEXT_TARGET, gamification.verifiedContextApplicationCount.first())
            val contextEvents = dao.observeRecentLearningActivity(
                actorId = GamificationRepository.LOCAL_ACTOR_ID,
                limit = 32
            ).first().filter { it.eventType == RewardableEventType.CONTEXT_APPLICATION_VERIFIED.wireName }
            assertEquals(CONTEXT_TARGET, contextEvents.size)
            contextEvents.forEach { activity ->
                assertTrue(checkNotNull(dao.eventById(activity.eventId)).metadataJson.contains("\"applicationVerified\":true"))
            }

            val unlock = dao.observeAchievementUnlocks(GamificationRepository.LOCAL_ACTOR_ID)
                .first().single { it.achievementId == CONTEXT_ACHIEVEMENT_ID && it.level == 1 }
            assertTrue(unlock.evidenceJson.contains("distinct_context_applications"))

            val dailyQuest = dao.observeActiveQuestInstances(GamificationRepository.LOCAL_ACTOR_ID)
                .first().single { it.definitionId == DAILY_CONTEXT_QUEST_ID }
            assertEquals(1L, dailyQuest.target)
            assertEquals(1L, dailyQuest.progress)
            assertEquals("completed", dailyQuest.state)
            val beforeClaim = gamification.xpBalance.first()
            val claim = gamification.claimQuest(dailyQuest.questInstanceId)
            assertEquals(RewardWriteStatus.APPLIED, claim.status)
            assertEquals(DAILY_CONTEXT_QUEST_XP, checkNotNull(claim.summary).xpAwarded)
            assertEquals(beforeClaim + DAILY_CONTEXT_QUEST_XP, gamification.xpBalance.first())
            assertEquals(RewardWriteStatus.DUPLICATE, gamification.claimQuest(dailyQuest.questInstanceId).status)
        }

        composeRule.activityRule.scenario.recreate()
        waitForText("Profile")
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Words in the Wild"))
        composeRule.onNodeWithText("Words in the Wild").assertIsDisplayed()
        composeRule.onNodeWithText("5 / 5 evidence recorded").assertIsDisplayed()
    }

    private fun waitForText(text: String, timeoutMillis: Long = 45_000L) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithText(text)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private companion object {
        const val CONTEXT_TARGET = 5
        const val DAILY_CONTEXT_QUEST_XP = 10L
        const val BASE_TIME_EPOCH_MILLIS = 1_820_000_000_000L
        const val CONTEXT_ACHIEVEMENT_ID = "achievement.application.context_builder"
        const val DAILY_CONTEXT_QUEST_ID = "quest.daily.context_use"
    }
}
