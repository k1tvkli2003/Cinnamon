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
import com.cinnamon.app.data.gamification.LearningFocusSelectionRecord
import com.cinnamon.app.data.local.LearningFocusAlreadySelectedException
import com.cinnamon.app.domain.repository.LexiconRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * Proves that Learning Focus is unlocked by real review commits, selected through the production
 * confirmation dialog, grants no XP, cannot be changed, and rehydrates in a fresh app process.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LearningFocusLifecycleAndroidTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun step1_reviewEvidenceUnlocksFocus_selectionIsImmutable_andSurvivesActivityRecreation() {
        waitForText("Practice")
        unlockLearningFocusWithRealReviewCommits()
        composeRule.activityRule.scenario.recreate()
        waitForText("Practice")

        navigateToLearningPlan()
        waitForText("CHOOSE WHAT TO SHARPEN")
        val balanceBeforeSelection = xpBalance()
        composeRule.onNodeWithText("Choose Language Precision")
            .performScrollTo()
            .performClick()
        waitForText("Use Language Precision?")
        composeRule.onNodeWithText("Use this focus").performClick()

        waitForText("CURRENT FOCUS")
        composeRule.onNodeWithText("CURRENT FOCUS")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Saved for this version").assertIsDisplayed()
        assertEquals(balanceBeforeSelection, xpBalance())
        assertCanonicalSelection(LANGUAGE_PRECISION_OPTION_ID)
        assertDifferentSelectionIsRejected(RECALL_RANGE_OPTION_ID)
        assertEquals(balanceBeforeSelection, xpBalance())

        composeRule.activityRule.scenario.recreate()
        waitForText("CURRENT FOCUS")
        composeRule.onNodeWithText("CURRENT FOCUS")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Saved for this version").assertIsDisplayed()

        val stored = proofPreferences().edit()
            .putInt(PROOF_STEP_ONE_PID_KEY, Process.myPid())
            .putString(PROOF_OPTION_ID_KEY, LANGUAGE_PRECISION_OPTION_ID)
            .commit()
        assertTrue(stored)
        Log.i(PROCESS_PROOF_TAG, "step=1 pid=${Process.myPid()}")
    }

    @Test
    fun step2_focusPersistsAcrossFreshInstrumentationProcess() {
        waitForText("Practice")
        val firstProcessId = proofPreferences().getInt(PROOF_STEP_ONE_PID_KEY, -1)
        val persistedOptionId = proofPreferences().getString(PROOF_OPTION_ID_KEY, null)
        val freshProcessRequired = InstrumentationRegistry.getArguments()
            .getString(REQUIRE_FRESH_PROCESS_ARGUMENT)
            ?.toBooleanStrictOrNull() == true
        assertTrue("Step-one process proof is missing", firstProcessId > 0)
        assertEquals(LANGUAGE_PRECISION_OPTION_ID, persistedOptionId)
        if (freshProcessRequired) {
            assertNotEquals(
                "The Learning Focus check reused the step-one Android process",
                firstProcessId,
                Process.myPid()
            )
        }
        Log.i(
            PROCESS_PROOF_TAG,
            "step=2 previousPid=$firstProcessId currentPid=${Process.myPid()} " +
                "freshRequired=$freshProcessRequired"
        )

        assertCanonicalSelection(LANGUAGE_PRECISION_OPTION_ID)
        val balanceBeforeRejectedChange = xpBalance()
        assertDifferentSelectionIsRejected(RECALL_RANGE_OPTION_ID)
        assertEquals(balanceBeforeRejectedChange, xpBalance())

        navigateToLearningPlan()
        waitForText("CURRENT FOCUS")
        composeRule.onNodeWithText("CURRENT FOCUS")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Language Precision").assertIsDisplayed()
        composeRule.onNodeWithText("Saved for this version").assertIsDisplayed()
    }

    private fun unlockLearningFocusWithRealReviewCommits() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = GamificationDeviceTestState.reset(context)
        val repository = LexiconRepository.getInstance(context)
        val entries = database.lexiconDao().freshEntries(FOCUS_UNLOCK_REVIEW_COUNT)
        assertEquals(FOCUS_UNLOCK_REVIEW_COUNT, entries.size)
        val now = System.currentTimeMillis()
        entries.forEachIndexed { index, entry ->
            repository.commitReview(
                expectedEntry = entry,
                quality = 4,
                occurrenceKey = "focus_unlock_v1:${entry.id}",
                occurredAtEpochMillis = now + index
            )
        }

        val foundationStages = database.gamificationDao()
            .observeJourneyStages(GamificationRepository.LOCAL_ACTOR_ID)
            .first()
        val prerequisite = foundationStages.single { stage ->
            stage.stageDefinitionId == FOUNDATION_FOCUS_PREREQUISITE_STAGE_ID
        }
        assertEquals("completed", prerequisite.state)
        assertEquals(prerequisite.target, prerequisite.progress)
        assertNotNull(prerequisite.completionEventId)
    }

    private fun navigateToLearningPlan() {
        composeRule.onNodeWithText("Practice").performClick()
        waitForText("Learning Questboard")
        composeRule.onNodeWithText("Learning Questboard")
            .performScrollTo()
            .performClick()
        waitForText("Learning Plan")
    }

    private fun assertCanonicalSelection(expectedOptionId: String) {
        val selections = runBlocking { gamification().learningFocusSelections.first() }
        assertEquals(1, selections.size)
        val selection = selections.single()
        assertTrue(selection is LearningFocusSelectionRecord.Compatible)
        selection as LearningFocusSelectionRecord.Compatible
        assertEquals(expectedOptionId, selection.optionId)
        assertEquals(LANGUAGE_PRECISION_PLAN_ID, selection.milestonePlanDefinitionId)
    }

    private fun assertDifferentSelectionIsRejected(optionId: String) {
        val error = runCatching {
            runBlocking { gamification().selectLearningFocus(optionId) }
        }.exceptionOrNull()
        assertTrue(error is LearningFocusAlreadySelectedException)
        assertCanonicalSelection(LANGUAGE_PRECISION_OPTION_ID)
    }

    private fun xpBalance(): Long = runBlocking { gamification().xpBalance.first() }

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
        const val FOCUS_UNLOCK_REVIEW_COUNT = 3
        const val FOUNDATION_FOCUS_PREREQUISITE_STAGE_ID = "stage.memory-spark"
        const val LANGUAGE_PRECISION_OPTION_ID = "learning-focus.option.language-precision"
        const val RECALL_RANGE_OPTION_ID = "learning-focus.option.recall-range"
        const val LANGUAGE_PRECISION_PLAN_ID = "milestone-plan.learning-focus.language-precision"
        const val PROOF_PREFERENCES = "learning_focus_process_restart_proof"
        const val PROOF_STEP_ONE_PID_KEY = "step_one_pid"
        const val PROOF_OPTION_ID_KEY = "option_id"
        const val REQUIRE_FRESH_PROCESS_ARGUMENT = "requireFreshProcess"
        const val PROCESS_PROOF_TAG = "FocusProcessProof"
    }
}
