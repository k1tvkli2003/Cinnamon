package com.cinnamon.app.data.gamification

import kotlin.math.min

/**
 * Version-one local reward policy.
 *
 * This engine never reads a clock, database, UI state, or random source. The
 * caller supplies the authoritative window totals and uniqueness facts, then
 * persists the returned decision through the Room transaction boundary.
 */
object RewardRuleEngine {
    const val XP_PER_REWARD_WINDOW_CAP: Long = 160L

    private data class Rule(
        val id: String,
        val baseXp: Long,
        val reasonCode: String
    )

    fun evaluate(input: RewardEvaluationInput): RewardDecision {
        val rule = ruleFor(input.eventType)
        val denialReason = eligibilityDenial(input, rule)
        val remainingXp = (XP_PER_REWARD_WINDOW_CAP - input.xpAlreadyAwardedInWindow)
            .coerceAtLeast(0L)
        val awardedXp = if (denialReason == null && rule != null) {
            min(rule.baseXp, remainingXp)
        } else {
            0L
        }
        val reasonCode = when {
            denialReason != null -> denialReason
            awardedXp == 0L -> "reward_window_cap_reached"
            awardedXp < requireNotNull(rule).baseXp -> "reward_window_cap_applied"
            else -> requireNotNull(rule).reasonCode
        }
        val tier = tierFor(awardedXp)

        val transactions = if (awardedXp > 0L) {
            val applicableRule = requireNotNull(rule)
            val transactionId = StableRewardIds.rewardClaimTransactionId(
                actorId = input.actorId,
                ruleId = applicableRule.id,
                currency = RewardCurrencies.XP,
                rewardWindowId = input.rewardWindowId,
                subjectType = input.subjectType,
                subjectId = input.subjectId
            )
            listOf(
                RewardTransactionDraft(
                    transactionId = transactionId,
                    idempotencyKey = transactionId,
                    ruleId = applicableRule.id,
                    ruleVersion = GamificationVersions.CURRENT_RULE_VERSION,
                    kind = RewardTransactionKind.GRANT,
                    currency = RewardCurrencies.XP,
                    amount = awardedXp,
                    reasonCode = reasonCode
                )
            )
        } else {
            emptyList()
        }

        val summary = RewardSummaryDraft(
            eventId = input.eventId,
            ruleVersion = GamificationVersions.CURRENT_RULE_VERSION,
            xpAwarded = awardedXp,
            currencyRewards = if (awardedXp > 0L) {
                listOf(CurrencyRewardDraft(RewardCurrencies.XP, awardedXp))
            } else {
                emptyList()
            },
            questProgress = emptyList(),
            achievementUnlocks = emptyList(),
            unlockedContentIds = emptyList(),
            celebrationTier = tier,
            reasonCode = reasonCode
        )

        val presentationReceipts = transactions.firstOrNull()?.let { transaction ->
            val family = GamificationPresentationIds.PROGRESS_INLINE
            val receiptId = StableRewardIds.presentationReceiptId(input.eventId, family)
            listOf(
                RewardPresentationReceiptDraft(
                    receiptId = receiptId,
                    idempotencyKey = receiptId,
                    sourceEventId = input.eventId,
                    sourceTransactionId = transaction.transactionId,
                    presentationFamily = family,
                    priority = priorityFor(tier),
                    tier = tier,
                    initialState = PresentationReceiptState.PENDING
                )
            )
        }.orEmpty()

        return RewardDecision(
            eventId = input.eventId,
            eventIdempotencyKey = input.eventIdempotencyKey,
            ruleVersion = GamificationVersions.CURRENT_RULE_VERSION,
            eligible = awardedXp > 0L,
            reasonCode = reasonCode,
            transactions = transactions,
            summary = summary,
            presentationReceipts = presentationReceipts
        )
    }

    private fun ruleFor(eventType: RewardableEventType): Rule? = when (eventType) {
        RewardableEventType.CONCEPT_MASTERED -> Rule(
            id = "reward.v1.concept_mastered",
            baseXp = 24L,
            reasonCode = "first_mastery_completed"
        )
        RewardableEventType.REVIEW_COMPLETED -> Rule(
            id = "reward.v1.review_completed",
            baseXp = 8L,
            reasonCode = "meaningful_review_completed"
        )
        RewardableEventType.PRACTICE_SESSION_COMPLETED -> Rule(
            id = "reward.v1.practice_session_completed",
            baseXp = 12L,
            reasonCode = "meaningful_practice_completed"
        )
        RewardableEventType.MISTAKE_CORRECTED -> Rule(
            id = "reward.v1.mistake_corrected",
            baseXp = 4L,
            reasonCode = "mistake_repaired"
        )
        RewardableEventType.MISTAKE_RECORDED,
        RewardableEventType.CONFUSABLE_PAIR_ATTEMPTED,
        RewardableEventType.CONFUSABLE_PAIR_RESOLVED,
        RewardableEventType.CONTEXT_APPLICATION_VERIFIED,
        RewardableEventType.DELAYED_RECALL_SUCCEEDED,
        RewardableEventType.COMEBACK_SESSION_COMPLETED,
        RewardableEventType.BOOKMARK_SAVED,
        RewardableEventType.SAVED_ITEM_REVIEWED,
        RewardableEventType.REVIEW_QUEUE_OPENED,
        RewardableEventType.REVIEW_QUEUE_CLEARED,
        RewardableEventType.OTHER -> null
    }

    private fun eligibilityDenial(input: RewardEvaluationInput, rule: Rule?): String? = when {
        rule == null -> "event_not_rewardable"
        !input.meaningful -> "action_not_meaningful"
        !input.uniqueSubjectInWindow -> "repeat_subject_not_rewarded"
        input.eventType == RewardableEventType.CONCEPT_MASTERED && !input.firstCompletion ->
            "mastery_already_rewarded"
        input.eventType == RewardableEventType.REVIEW_COMPLETED && input.completedItemCount < 1 ->
            "review_has_no_completed_items"
        input.eventType == RewardableEventType.PRACTICE_SESSION_COMPLETED &&
            input.completedItemCount < 3 -> "practice_below_minimum_items"
        input.eventType == RewardableEventType.MISTAKE_CORRECTED &&
            !input.correctedAfterMistake -> "correction_not_verified"
        else -> null
    }

    private fun tierFor(xp: Long): CelebrationTier = when {
        xp <= 0L -> CelebrationTier.NONE
        xp <= 5L -> CelebrationTier.MICRO
        xp <= 24L -> CelebrationTier.STANDARD
        xp <= 60L -> CelebrationTier.MILESTONE
        else -> CelebrationTier.SHOWPIECE
    }

    private fun priorityFor(tier: CelebrationTier): Int = when (tier) {
        CelebrationTier.NONE -> 0
        CelebrationTier.MICRO -> 10
        CelebrationTier.STANDARD -> 20
        CelebrationTier.MILESTONE -> 30
        CelebrationTier.SHOWPIECE -> 40
    }
}
