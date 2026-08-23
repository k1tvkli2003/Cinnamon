package com.cinnamon.app.data.gamification

import java.security.MessageDigest

object GamificationVersions {
    const val CURRENT_RULE_VERSION: Int = 1
    const val CURRENT_CATALOG_VERSION: Int = 1
}

object RewardCurrencies {
    const val XP: String = "xp"
}

/** IDs are catalog contracts, not ad-hoc UI animation names. */
object GamificationPresentationIds {
    const val PROGRESS_INLINE: String = "presentation.progress.inline"
    const val JOURNEY_STAGE_COMPLETE: String = "presentation.journey.stage_complete"
}

enum class RewardableEventType(val wireName: String) {
    CONCEPT_MASTERED("concept_mastered"),
    REVIEW_COMPLETED("review_completed"),
    PRACTICE_SESSION_COMPLETED("practice_session_completed"),
    MISTAKE_RECORDED("mistake_recorded"),
    MISTAKE_CORRECTED("mistake_corrected"),
    CONFUSABLE_PAIR_ATTEMPTED("confusable_pair_attempted"),
    CONFUSABLE_PAIR_RESOLVED("confusable_pair_resolved"),
    CONTEXT_APPLICATION_VERIFIED("context_application_verified"),
    DELAYED_RECALL_SUCCEEDED("delayed_recall_succeeded"),
    COMEBACK_SESSION_COMPLETED("comeback_session_completed"),
    BOOKMARK_SAVED("bookmark_saved"),
    SAVED_ITEM_REVIEWED("saved_item_reviewed"),
    REVIEW_QUEUE_OPENED("review_queue_opened"),
    REVIEW_QUEUE_CLEARED("review_queue_cleared"),
    OTHER("other")
}

enum class RewardTransactionKind(val wireName: String) {
    GRANT("grant"),
    SPEND("spend"),
    REPAIR("repair"),
    REFUND("refund"),
    REVERSAL("reversal")
}

enum class CelebrationTier(val wireName: String) {
    NONE("none"),
    MICRO("micro"),
    STANDARD("standard"),
    MILESTONE("milestone"),
    SHOWPIECE("showpiece")
}

enum class QuestInstanceState(val wireName: String) {
    AVAILABLE("available"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    CLAIMED("claimed"),
    EXPIRED("expired")
}

enum class PresentationReceiptState(val wireName: String) {
    PENDING("pending"),
    READY("ready"),
    PLAYING("playing"),
    INTERRUPTED("interrupted"),
    ACKNOWLEDGED("acknowledged"),
    SKIPPED("skipped"),
    EXPIRED("expired");

    fun canTransitionTo(next: PresentationReceiptState): Boolean = when (this) {
        PENDING -> next in setOf(READY, ACKNOWLEDGED, SKIPPED, EXPIRED)
        READY -> next in setOf(PLAYING, ACKNOWLEDGED, SKIPPED, EXPIRED)
        PLAYING -> next in setOf(INTERRUPTED, ACKNOWLEDGED, SKIPPED)
        INTERRUPTED -> next in setOf(READY, PLAYING, ACKNOWLEDGED, SKIPPED, EXPIRED)
        ACKNOWLEDGED, SKIPPED, EXPIRED -> false
    }

    companion object {
        fun fromWireName(value: String): PresentationReceiptState? =
            entries.firstOrNull { it.wireName == value }
    }
}

/**
 * All mutable/time-zone state is supplied by the caller. This keeps rule
 * evaluation deterministic and makes local/offline replay testable.
 */
data class RewardEvaluationInput(
    val eventId: String,
    val actorId: String,
    val eventIdempotencyKey: String,
    val eventType: RewardableEventType,
    val subjectType: String,
    val subjectId: String,
    val occurredAtEpochMillis: Long,
    val rewardWindowId: String,
    val xpAlreadyAwardedInWindow: Long,
    val meaningful: Boolean,
    val firstCompletion: Boolean,
    val uniqueSubjectInWindow: Boolean,
    val correctedAfterMistake: Boolean,
    val completedItemCount: Int
) {
    init {
        require(eventId.isNotBlank()) { "eventId must not be blank" }
        require(actorId.isNotBlank()) { "actorId must not be blank" }
        require(eventIdempotencyKey.isNotBlank()) { "eventIdempotencyKey must not be blank" }
        require(subjectType.isNotBlank()) { "subjectType must not be blank" }
        require(subjectId.isNotBlank()) { "subjectId must not be blank" }
        require(rewardWindowId.isNotBlank()) { "rewardWindowId must not be blank" }
        require(occurredAtEpochMillis >= 0L) { "occurredAtEpochMillis must not be negative" }
        require(xpAlreadyAwardedInWindow >= 0L) {
            "xpAlreadyAwardedInWindow must not be negative"
        }
        require(completedItemCount >= 0) { "completedItemCount must not be negative" }
    }
}

data class RewardTransactionDraft(
    val transactionId: String,
    val idempotencyKey: String,
    val ruleId: String,
    val ruleVersion: Int,
    val kind: RewardTransactionKind,
    val currency: String,
    val amount: Long,
    val reasonCode: String
) {
    init {
        require(transactionId.isNotBlank()) { "transactionId must not be blank" }
        require(idempotencyKey.isNotBlank()) { "idempotencyKey must not be blank" }
        require(ruleId.isNotBlank()) { "ruleId must not be blank" }
        require(ruleVersion > 0) { "ruleVersion must be positive" }
        require(currency.isNotBlank()) { "currency must not be blank" }
        require(amount != 0L) { "A ledger transaction must change a balance" }
        require(reasonCode.isNotBlank()) { "reasonCode must not be blank" }
    }
}

data class CurrencyRewardDraft(
    val currency: String,
    val amount: Long
)

data class QuestProgressDraft(
    val questInstanceId: String,
    val from: Long,
    val to: Long,
    val target: Long,
    val completed: Boolean
)

data class AchievementUnlockDraft(
    val achievementId: String,
    val level: Int,
    val catalogVersion: Int
)

/** A truthful, immutable value snapshot suitable for recap and restored UI. */
data class RewardSummaryDraft(
    val eventId: String,
    val ruleVersion: Int,
    val xpAwarded: Long,
    val currencyRewards: List<CurrencyRewardDraft>,
    val questProgress: List<QuestProgressDraft>,
    val achievementUnlocks: List<AchievementUnlockDraft>,
    val unlockedContentIds: List<String>,
    val celebrationTier: CelebrationTier,
    val reasonCode: String
)

/**
 * A presentation acknowledgement, never a grant authority. Skipping it only
 * changes receipt state; [RewardTransactionDraft] already contains the value.
 */
data class RewardPresentationReceiptDraft(
    val receiptId: String,
    val idempotencyKey: String,
    val sourceEventId: String,
    val sourceTransactionId: String?,
    val presentationFamily: String,
    val priority: Int,
    val tier: CelebrationTier,
    val initialState: PresentationReceiptState
)

data class RewardDecision(
    val eventId: String,
    val eventIdempotencyKey: String,
    val ruleVersion: Int,
    val eligible: Boolean,
    val reasonCode: String,
    val transactions: List<RewardTransactionDraft>,
    val summary: RewardSummaryDraft,
    val presentationReceipts: List<RewardPresentationReceiptDraft>
)

/** Stable IDs must be reproduced on retries, offline replay, and process restore. */
object StableRewardIds {
    fun eventIdempotencyKey(
        actorId: String,
        eventType: RewardableEventType,
        subjectType: String,
        subjectId: String,
        domainOccurrenceKey: String
    ): String = stableId(
        prefix = "event",
        actorId,
        eventType.wireName,
        subjectType,
        subjectId,
        domainOccurrenceKey
    )

    fun transactionId(eventId: String, ruleId: String, currency: String): String =
        stableId("transaction", eventId, ruleId, currency)

    fun rewardClaimTransactionId(
        actorId: String,
        ruleId: String,
        currency: String,
        rewardWindowId: String,
        subjectType: String,
        subjectId: String
    ): String = stableId(
        "transaction",
        actorId,
        ruleId,
        currency,
        rewardWindowId,
        subjectType,
        subjectId
    )

    fun presentationReceiptId(eventId: String, presentationFamily: String): String =
        stableId("receipt", eventId, presentationFamily)

    fun catalogUnlockId(
        actorId: String,
        catalogItemId: String,
        level: Int,
        catalogVersion: Int
    ): String = stableId(
        "unlock",
        actorId,
        catalogItemId,
        level.toString(),
        catalogVersion.toString()
    )

    fun questInstanceId(
        actorId: String,
        definitionId: String,
        startsAtEpochMillis: Long,
        catalogVersion: Int
    ): String = stableId(
        "quest",
        actorId,
        definitionId,
        startsAtEpochMillis.toString(),
        catalogVersion.toString()
    )

    fun catalogPresentationReceiptId(
        eventId: String,
        catalogItemId: String,
        level: Int,
        presentationFamily: String
    ): String = stableId(
        "receipt",
        eventId,
        catalogItemId,
        level.toString(),
        presentationFamily
    )

    fun journeyInstanceId(
        actorId: String,
        definitionId: String,
        definitionVersion: Int
    ): String = stableId(
        "journey",
        actorId,
        definitionId,
        definitionVersion.toString()
    )

    fun journeyStageProgressId(
        journeyInstanceId: String,
        stageDefinitionId: String
    ): String = stableId("journey_stage", journeyInstanceId, stageDefinitionId)

    fun learningFocusSelectionId(
        actorId: String,
        definitionId: String,
        definitionVersion: Int
    ): String = stableId(
        "learning_focus_selection",
        actorId,
        definitionId,
        definitionVersion.toString()
    )

    fun journeyPresentationReceiptId(
        eventId: String,
        journeyDefinitionId: String,
        stageDefinitionId: String
    ): String = stableId(
        "receipt",
        eventId,
        journeyDefinitionId,
        stageDefinitionId,
        GamificationPresentationIds.JOURNEY_STAGE_COMPLETE
    )

    /** Only bounded compatibility adapters may use a frozen historical ID namespace. */
    internal fun compatibilityStableId(prefix: String, vararg parts: String): String =
        stableId(prefix, *parts)

    private fun stableId(prefix: String, vararg parts: String): String {
        require(parts.all { it.isNotBlank() }) { "Stable ID parts must not be blank" }
        val canonical = parts.joinToString(separator = "\u001F") { it.trim() }
        return "${prefix}_${sha256(canonical)}"
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        val digits = "0123456789abcdef"
        return buildString(bytes.size * 2) {
            bytes.forEach { byte ->
                val unsigned = byte.toInt() and 0xff
                append(digits[unsigned ushr 4])
                append(digits[unsigned and 0x0f])
            }
        }
    }
}
