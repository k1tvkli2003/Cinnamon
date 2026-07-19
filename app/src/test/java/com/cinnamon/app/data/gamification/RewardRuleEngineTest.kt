package com.cinnamon.app.data.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardRuleEngineTest {

    @Test
    fun `meaningful review grants the versioned base reward`() {
        val decision = RewardRuleEngine.evaluate(input())

        assertTrue(decision.eligible)
        assertEquals(8L, decision.summary.xpAwarded)
        assertEquals("meaningful_review_completed", decision.reasonCode)
        assertEquals(1, decision.transactions.size)
        assertEquals(
            GamificationPresentationIds.PROGRESS_INLINE,
            decision.presentationReceipts.single().presentationFamily
        )
        assertEquals(CelebrationTier.STANDARD, decision.presentationReceipts.single().tier)
        assertEquals(
            decision.summary.celebrationTier,
            decision.presentationReceipts.single().tier
        )
    }

    @Test
    fun `repeat subject in the same window creates no transaction`() {
        val decision = RewardRuleEngine.evaluate(input(uniqueSubjectInWindow = false))

        assertFalse(decision.eligible)
        assertEquals(0L, decision.summary.xpAwarded)
        assertEquals("repeat_subject_not_rewarded", decision.reasonCode)
        assertTrue(decision.transactions.isEmpty())
        assertTrue(decision.presentationReceipts.isEmpty())
    }

    @Test
    fun `daily cap trims rather than over-awards`() {
        val decision = RewardRuleEngine.evaluate(input(xpAlreadyAwardedInWindow = 155L))

        assertEquals(5L, decision.summary.xpAwarded)
        assertEquals("reward_window_cap_applied", decision.reasonCode)
    }

    @Test
    fun `daily cap blocks a new award when exhausted`() {
        val decision = RewardRuleEngine.evaluate(input(xpAlreadyAwardedInWindow = 160L))

        assertFalse(decision.eligible)
        assertEquals(0L, decision.summary.xpAwarded)
        assertEquals("reward_window_cap_reached", decision.reasonCode)
    }

    @Test
    fun `practice below three completed items is not meaningful enough to reward`() {
        val decision = RewardRuleEngine.evaluate(
            input(
                eventType = RewardableEventType.PRACTICE_SESSION_COMPLETED,
                completedItemCount = 2
            )
        )

        assertFalse(decision.eligible)
        assertEquals("practice_below_minimum_items", decision.reasonCode)
    }

    @Test
    fun `same subject and reward window share one claim transaction across concurrent events`() {
        val first = RewardRuleEngine.evaluate(input(eventId = "event_one"))
        val second = RewardRuleEngine.evaluate(input(eventId = "event_two"))

        assertEquals(first.transactions.single().transactionId, second.transactions.single().transactionId)
    }

    @Test
    fun `same subject can earn a new claim in a new reward window`() {
        val first = RewardRuleEngine.evaluate(input(rewardWindowId = "study_day:100"))
        val second = RewardRuleEngine.evaluate(input(rewardWindowId = "study_day:101"))

        assertNotEquals(first.transactions.single().transactionId, second.transactions.single().transactionId)
    }

    private fun input(
        eventId: String = "event_one",
        eventType: RewardableEventType = RewardableEventType.REVIEW_COMPLETED,
        rewardWindowId: String = "study_day:100",
        xpAlreadyAwardedInWindow: Long = 0L,
        uniqueSubjectInWindow: Boolean = true,
        completedItemCount: Int = 1
    ) = RewardEvaluationInput(
        eventId = eventId,
        actorId = "local_learner",
        eventIdempotencyKey = "idempotency_$eventId",
        eventType = eventType,
        subjectType = "lexicon_entry",
        subjectId = "42",
        occurredAtEpochMillis = 1_000L,
        rewardWindowId = rewardWindowId,
        xpAlreadyAwardedInWindow = xpAlreadyAwardedInWindow,
        meaningful = true,
        firstCompletion = true,
        uniqueSubjectInWindow = uniqueSubjectInWindow,
        correctedAfterMistake = false,
        completedItemCount = completedItemCount
    )
}
