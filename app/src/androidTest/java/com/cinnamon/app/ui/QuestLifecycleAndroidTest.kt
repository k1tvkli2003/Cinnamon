package com.cinnamon.app.ui

import android.content.Context
import android.os.Process
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cinnamon.app.MainActivity
import com.cinnamon.app.data.gamification.GamificationRepository
import com.cinnamon.app.data.gamification.QuestAssignmentContracts
import com.cinnamon.app.data.local.AppDatabase
import com.cinnamon.app.data.local.QuestInstanceEntity
import com.cinnamon.app.data.local.RewardWriteStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters
import org.junit.runner.RunWith

/**
 * Device-level proof for the complete durable-mastery quest lifecycle.
 *
 * The first ordered test prepares eight real lexicon rows at their third successful repetition,
 * grades them through the production Review UI, claims from the production Questboard, and then
 * recreates the Activity. The second test intentionally depends on the first test's on-disk state:
 * the host verifier runs the two methods in separate instrumentation invocations with an explicit
 * force-stop between them, proving that the claimed state and exactly-once reward survive a fresh
 * app process rather than only a recomposition.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class QuestLifecycleAndroidTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun step1_reviewCompletesQuest_claimsOnce_andSurvivesActivityRecreation() {
        waitForText("Practice")
        val assignedQuest = resetLearningStateAndPrepareMasteryCards()
        val claim = checkNotNull(QuestAssignmentContracts.resolveClaim(assignedQuest))

        // Recreate once after the test-only data preparation so every screen and ViewModel reads
        // the same persisted state a normal learner launch would observe.
        composeRule.activityRule.scenario.recreate()
        waitForText("Practice")

        composeRule.onNodeWithText("Practice").performClick()
        waitForText("Spaced repetition")
        composeRule.onNodeWithText("Spaced repetition").performClick()

        waitForText("0 / 16")
        repeat(PREPARED_MASTERY_CARD_COUNT) { index ->
            waitForText("Flip")
            composeRule.onNodeWithText("Flip").performClick()
            waitForText("Easy")
            composeRule.onNodeWithText("Easy").performClick()
            waitForText("${index + 1} / 16")
        }

        waitForQuestState(assignedQuest.questInstanceId, QUEST_COMPLETED_STATE)
        val completedQuest = questById(assignedQuest.questInstanceId)
        assertEquals(PREPARED_MASTERY_CARD_COUNT.toLong(), completedQuest.progress)
        assertEquals(completedQuest.target, completedQuest.progress)
        assertNotNull(completedQuest.completionEventId)

        // Drive the Activity's real back dispatcher instead of targeting an icon node: reward
        // receipts can add other close-shaped affordances to the semantics tree while reviewing.
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        waitForText("Practice")
        navigateFromPracticeToQuests()

        val claimXp = claim.xpAmount.toInt()
        val claimLabel = "CLAIM +$claimXp XP"
        waitForText("8 of 8 complete")
        waitForText(claimLabel)
        val balanceBeforeClaim = xpBalance()
        composeRule.onNodeWithText(claimLabel)
            .performScrollTo()
            .performClick()

        val claimedLabel = "$claimXp XP EARNED · CLAIMED ONCE"
        waitForText(claimedLabel)
        waitForQuestState(assignedQuest.questInstanceId, QUEST_CLAIMED_STATE)
        val balanceAfterClaim = xpBalance()
        assertEquals(balanceBeforeClaim + claim.xpAmount, balanceAfterClaim)

        val duplicate = runBlocking {
            gamification().claimQuest(assignedQuest.questInstanceId)
        }
        assertEquals(RewardWriteStatus.DUPLICATE, duplicate.status)
        assertEquals(balanceAfterClaim, xpBalance())

        composeRule.activityRule.scenario.recreate()
        waitForText(claimedLabel)
        composeRule.onNodeWithText(claimedLabel).assertIsDisplayed()
        assertEquals(QUEST_CLAIMED_STATE, questById(assignedQuest.questInstanceId).state)
        val stored = proofPreferences().edit()
            .putInt(PROOF_STEP_ONE_PID_KEY, Process.myPid())
            .putString(PROOF_QUEST_INSTANCE_KEY, assignedQuest.questInstanceId)
            .commit()
        assertTrue(stored)
        Log.i(PROCESS_PROOF_TAG, "step=1 pid=${Process.myPid()}")
    }

    @Test
    fun step2_claimPersistsAcrossFreshInstrumentationProcess() {
        waitForText("Practice")
        val persistedQuest = currentWeeklyQuest()
        val claim = checkNotNull(QuestAssignmentContracts.resolveClaim(persistedQuest))
        val firstProcessId = proofPreferences().getInt(PROOF_STEP_ONE_PID_KEY, -1)
        val persistedQuestId = proofPreferences().getString(PROOF_QUEST_INSTANCE_KEY, null)
        val freshProcessRequired = InstrumentationRegistry.getArguments()
            .getString(REQUIRE_FRESH_PROCESS_ARGUMENT)
            ?.toBooleanStrictOrNull() == true
        assertTrue("Step-one process proof is missing", firstProcessId > 0)
        assertEquals(persistedQuestId, persistedQuest.questInstanceId)
        if (freshProcessRequired) {
            assertNotEquals(
                "The persistence check reused the step-one Android process",
                firstProcessId,
                Process.myPid()
            )
        }
        Log.i(
            PROCESS_PROOF_TAG,
            "step=2 previousPid=$firstProcessId currentPid=${Process.myPid()} " +
                "freshRequired=$freshProcessRequired"
        )
        assertEquals(QUEST_CLAIMED_STATE, persistedQuest.state)
        assertEquals(persistedQuest.target, persistedQuest.progress)
        assertNotNull(persistedQuest.claimedAtEpochMillis)

        val balanceBeforeDuplicate = xpBalance()
        val duplicate = runBlocking {
            gamification().claimQuest(persistedQuest.questInstanceId)
        }
        assertEquals(RewardWriteStatus.DUPLICATE, duplicate.status)
        assertEquals(balanceBeforeDuplicate, xpBalance())

        composeRule.onNodeWithText("Practice").performClick()
        navigateFromPracticeToQuests()
        waitForText("8 of 8 complete")
        val claimedLabel = "${claim.xpAmount.toInt()} XP EARNED · CLAIMED ONCE"
        waitForText(claimedLabel)
        composeRule.onNodeWithText(claimedLabel)
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun resetLearningStateAndPrepareMasteryCards(): QuestInstanceEntity = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        GamificationDeviceTestState.reset(context) { database, now ->
            val prepared = database.lexiconDao().freshEntries(PREPARED_MASTERY_CARD_COUNT)
            assertEquals(PREPARED_MASTERY_CARD_COUNT, prepared.size)
            prepared.forEachIndexed { index, entry ->
                database.lexiconDao().update(
                    entry.copy(
                        reps = 2,
                        easeFactor = 2.5f,
                        intervalDays = 3f,
                        dueAt = now - (PREPARED_MASTERY_CARD_COUNT - index) * 1_000L,
                        timesSeen = 2
                    )
                )
            }
        }

        val quest = currentWeeklyQuest()
        assertEquals(0L, quest.progress)
        assertTrue(quest.state == "available" || quest.state == "in_progress")
        quest
    }

    private fun navigateFromPracticeToQuests() {
        waitForText("Learning Questboard")
        composeRule.onNodeWithText("Learning Questboard")
            .performScrollTo()
            .performClick()
        waitForText("Learning Plan")
        composeRule.onNodeWithText("Quests").performClick()
        waitForText("Make eight memories stick")
    }

    private fun waitForQuestState(questInstanceId: String, expectedState: String) {
        composeRule.waitUntil(timeoutMillis = 30_000L) {
            runBlocking { questByIdOrNull(questInstanceId)?.state == expectedState }
        }
    }

    private fun currentWeeklyQuest(): QuestInstanceEntity = runBlocking {
        database().gamificationDao()
            .activeQuestInstancesOnce(GamificationRepository.LOCAL_ACTOR_ID)
            .single { instance -> instance.definitionId == WEEKLY_MASTERY_QUEST_ID }
    }

    private fun questById(questInstanceId: String): QuestInstanceEntity = runBlocking {
        checkNotNull(questByIdOrNull(questInstanceId))
    }

    private suspend fun questByIdOrNull(questInstanceId: String): QuestInstanceEntity? =
        database().gamificationDao().questInstanceById(questInstanceId)

    private fun xpBalance(): Long = runBlocking { gamification().xpBalance.first() }

    private fun database(): AppDatabase = AppDatabase.getDatabase(
        InstrumentationRegistry.getInstrumentation().targetContext
    )

    private fun gamification(): GamificationRepository = GamificationRepository.getInstance(
        InstrumentationRegistry.getInstrumentation().targetContext
    )

    private fun proofPreferences() = InstrumentationRegistry.getInstrumentation()
        .targetContext
        .getSharedPreferences(PROOF_PREFERENCES, Context.MODE_PRIVATE)

    private fun waitForText(text: String, timeoutMillis: Long = 45_000L) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithText(text)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private companion object {
        const val PREPARED_MASTERY_CARD_COUNT = 8
        const val WEEKLY_MASTERY_QUEST_ID = "quest.weekly.durable_mastery"
        const val QUEST_COMPLETED_STATE = "completed"
        const val QUEST_CLAIMED_STATE = "claimed"
        const val PROOF_PREFERENCES = "quest_process_restart_proof"
        const val PROOF_STEP_ONE_PID_KEY = "step_one_pid"
        const val PROOF_QUEST_INSTANCE_KEY = "quest_instance_id"
        const val REQUIRE_FRESH_PROCESS_ARGUMENT = "requireFreshProcess"
        const val PROCESS_PROOF_TAG = "QuestProcessProof"
    }
}
