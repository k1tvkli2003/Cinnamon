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
import com.cinnamon.app.data.gamification.LearningEventSource
import com.cinnamon.app.data.gamification.PresentationReceiptState
import com.cinnamon.app.data.gamification.RewardableEventType
import com.cinnamon.app.data.local.AppDatabase
import com.cinnamon.app.data.local.RewardWriteStatus
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
 * Proves the first fully produced correction achievement: an Again attempt writes zero-value
 * mistake evidence, a later due correction links to it, five unique subjects unlock Turnaround,
 * and replay cannot farm another repair or reward.
 */
@RunWith(AndroidJUnit4::class)
class RepairLoopLifecycleAndroidTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun fiveVerifiedRepairs_unlockTurnaround_once_andRenderInProfile() {
        waitForText("Practice")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = runBlocking { GamificationDeviceTestState.reset(context) }
        val repository = LexiconRepository.getInstance(context)
        val gamification = GamificationRepository.getInstance(context)
        val entries = runBlocking { database.lexiconDao().freshEntries(REPAIR_TARGET) }
        assertEquals(REPAIR_TARGET, entries.size)

        entries.forEachIndexed { index, entry ->
            val mistakeAt = BASE_TIME_EPOCH_MILLIS + index * 1_000L
            val missed = runBlocking {
                repository.commitReview(
                    expectedEntry = entry,
                    quality = 1,
                    occurrenceKey = "repair-loop-$index-again",
                    occurredAtEpochMillis = mistakeAt
                )
            }
            assertEquals(0, missed.xpAwarded)

            val repaired = runBlocking {
                repository.commitReview(
                    expectedEntry = missed.entry,
                    quality = 4,
                    occurrenceKey = "repair-loop-$index-good",
                    occurredAtEpochMillis = missed.entry.dueAt
                )
            }
            assertTrue(repaired.xpAwarded >= REVIEW_AND_REPAIR_XP)
        }

        val dao = database.gamificationDao()
        runBlocking {
            assertEquals(
                REPAIR_TARGET,
                dao.observeEventCount(
                    GamificationRepository.LOCAL_ACTOR_ID,
                    RewardableEventType.MISTAKE_RECORDED.wireName
                ).first()
            )
            assertEquals(
                REPAIR_TARGET,
                gamification.repairedSubjectCount.first()
            )

            val repairUnlock = dao.observeAchievementUnlocks(GamificationRepository.LOCAL_ACTOR_ID)
                .first()
                .single { unlock ->
                    unlock.achievementId == REPAIR_ACHIEVEMENT_ID && unlock.level == 1
                }
            assertTrue(repairUnlock.evidenceJson.contains("distinct_repaired_subjects"))

            val activities = dao.observeRecentLearningActivity(
                actorId = GamificationRepository.LOCAL_ACTOR_ID,
                limit = 64
            ).first()
            assertTrue(activities.none { activity ->
                activity.eventType == RewardableEventType.MISTAKE_RECORDED.wireName
            })
            val corrections = activities.filter { activity ->
                activity.eventType == RewardableEventType.MISTAKE_CORRECTED.wireName
            }
            assertEquals(REPAIR_TARGET, corrections.size)
            corrections.forEach { correction ->
                val event = checkNotNull(dao.eventById(correction.eventId))
                assertTrue(event.metadataJson.contains("\"repairLinkPresent\":true"))
                assertTrue(event.metadataJson.contains("\"incorrectAttemptPrecedesCorrection\":true"))
                assertTrue(event.metadataJson.contains("\"repairOfEventId\""))
            }

            val repairQuest = dao.observeActiveQuestInstances(
                GamificationRepository.LOCAL_ACTOR_ID
            ).first().single { instance ->
                instance.definitionId == DAILY_REPAIR_QUEST_ID
            }
            assertEquals(1L, repairQuest.target)
            assertEquals(1L, repairQuest.progress)
            assertEquals("completed", repairQuest.state)
            val balanceBeforeQuestClaim = gamification.xpBalance.first()
            val questClaim = gamification.claimQuest(
                questInstanceId = repairQuest.questInstanceId,
                occurredAtEpochMillis = BASE_TIME_EPOCH_MILLIS + 90_000_000L
            )
            assertEquals(RewardWriteStatus.APPLIED, questClaim.status)
            assertEquals(DAILY_REPAIR_QUEST_XP, checkNotNull(questClaim.summary).xpAwarded)
            assertEquals(
                balanceBeforeQuestClaim + DAILY_REPAIR_QUEST_XP,
                gamification.xpBalance.first()
            )
            assertEquals("claimed", checkNotNull(dao.questInstanceById(repairQuest.questInstanceId)).state)
            assertEquals(
                RewardWriteStatus.DUPLICATE,
                gamification.claimQuest(repairQuest.questInstanceId).status
            )

            val repairValueBeforeReplay = dao.observeRewardTransactions(
                GamificationRepository.LOCAL_ACTOR_ID
            ).first().filter { transaction ->
                transaction.ruleId == REPAIR_RULE_ID ||
                    transaction.ruleId == REPAIR_ACHIEVEMENT_RULE_ID
            }.sumOf { transaction -> transaction.amount }
            assertEquals(EXPECTED_REPAIR_VALUE_XP, repairValueBeforeReplay)

            val replay = gamification.recordVerifiedMistakeCorrection(
                subjectType = "lexicon_entry",
                subjectId = entries.first().id.toString(),
                occurrenceKey = "repair-loop-replay",
                source = LearningEventSource.REVIEW,
                occurredAtEpochMillis = BASE_TIME_EPOCH_MILLIS + 86_400_000L
            )
            assertNull(replay)
            val repairValueAfterReplay = dao.observeRewardTransactions(
                GamificationRepository.LOCAL_ACTOR_ID
            ).first().filter { transaction ->
                transaction.ruleId == REPAIR_RULE_ID ||
                    transaction.ruleId == REPAIR_ACHIEVEMENT_RULE_ID
            }.sumOf { transaction -> transaction.amount }
            assertEquals(repairValueBeforeReplay, repairValueAfterReplay)

            dao.observePendingPresentationReceipts(
                GamificationRepository.LOCAL_ACTOR_ID,
                Long.MIN_VALUE
            ).first().forEach { receipt ->
                dao.transitionPresentationReceipt(
                    receiptId = receipt.receiptId,
                    allowedCurrentStates = listOf(
                        PresentationReceiptState.PENDING.wireName,
                        PresentationReceiptState.READY.wireName,
                        PresentationReceiptState.INTERRUPTED.wireName
                    ),
                    newState = PresentationReceiptState.ACKNOWLEDGED.wireName,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                    acknowledge = true,
                    suppressionReason = null
                )
            }
            assertTrue(gamification.xpBalance.first() >= EXPECTED_REPAIR_VALUE_XP)
        }

        composeRule.activityRule.scenario.recreate()
        waitForText("Profile")
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Turnaround"))
        composeRule.onNodeWithText("Turnaround")
            .assertIsDisplayed()
        composeRule.onNodeWithText("5 / 5 evidence recorded")
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
        const val REPAIR_TARGET = 5
        const val REVIEW_AND_REPAIR_XP = 12
        const val EXPECTED_REPAIR_VALUE_XP = 40L
        const val DAILY_REPAIR_QUEST_XP = 10L
        const val BASE_TIME_EPOCH_MILLIS = 1_800_000_000_000L
        const val REPAIR_ACHIEVEMENT_ID = "achievement.correction.repair_loop"
        const val DAILY_REPAIR_QUEST_ID = "quest.daily.repair_one"
        const val REPAIR_RULE_ID = "reward.v1.mistake_corrected"
        const val REPAIR_ACHIEVEMENT_RULE_ID =
            "catalog.achievement.correction.repair_loop.level.1"
    }
}
