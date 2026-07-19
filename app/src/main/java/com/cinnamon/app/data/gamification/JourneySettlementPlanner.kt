package com.cinnamon.app.data.gamification

import com.cinnamon.app.data.local.CatalogEvidenceMetric
import com.cinnamon.app.data.local.GamificationEventEntity
import com.cinnamon.app.data.local.JourneyInstanceEntity
import com.cinnamon.app.data.local.JourneySettlementRequest
import com.cinnamon.app.data.local.JourneyStageProgressEntity
import com.cinnamon.app.data.local.JourneyStageSettlementCandidate
import com.cinnamon.app.data.local.RewardPresentationReceiptEntity
import com.cinnamon.app.data.local.RewardTransactionEntity
import com.cinnamon.app.domain.gamification.FoundationJourneyCatalog
import com.cinnamon.app.domain.gamification.JourneyDefinition
import com.cinnamon.app.domain.gamification.JourneyEvidenceMetric

/**
 * Builds deterministic candidates only. Room decides which single active stage can progress and
 * whether its one-time reward is inserted, keeping concurrent/replayed events safe.
 */
object JourneySettlementPlanner {
    fun plan(
        event: GamificationEventEntity,
        definition: JourneyDefinition = FoundationJourneyCatalog.definition
    ): JourneySettlementRequest {
        val journeyInstanceId = StableRewardIds.journeyInstanceId(
            actorId = event.actorId,
            definitionId = definition.id,
            definitionVersion = definition.version
        )
        val instance = JourneyInstanceEntity(
            journeyInstanceId = journeyInstanceId,
            actorId = event.actorId,
            definitionId = definition.id,
            definitionVersion = definition.version,
            state = "active",
            currentStageOrder = 1,
            startedAtEpochMillis = event.occurredAtEpochMillis,
            updatedAtEpochMillis = event.occurredAtEpochMillis,
            completionEventId = null,
            completedAtEpochMillis = null
        )
        val candidates = definition.stages.map { stage ->
            val stageProgressId = StableRewardIds.journeyStageProgressId(
                journeyInstanceId = journeyInstanceId,
                stageDefinitionId = stage.id
            )
            val ruleId = "journey.${definition.id}.stage.${stage.id}"
            val transactionId = StableRewardIds.transactionId(
                eventId = event.eventId,
                ruleId = ruleId,
                currency = RewardCurrencies.XP
            )
            val receiptId = StableRewardIds.journeyPresentationReceiptId(
                eventId = event.eventId,
                journeyDefinitionId = definition.id,
                stageDefinitionId = stage.id
            )
            JourneyStageSettlementCandidate(
                stage = JourneyStageProgressEntity(
                    stageProgressId = stageProgressId,
                    journeyInstanceId = journeyInstanceId,
                    actorId = event.actorId,
                    stageDefinitionId = stage.id,
                    stageOrder = stage.order,
                    evidenceMetric = stage.evidenceMetric.wireName,
                    state = if (stage.order == 1) "active" else "locked",
                    progress = 0L,
                    target = stage.target,
                    rewardXp = stage.rewardXp,
                    createdAtEpochMillis = event.occurredAtEpochMillis,
                    updatedAtEpochMillis = event.occurredAtEpochMillis,
                    completionEventId = null,
                    completedAtEpochMillis = null
                ),
                metric = stage.evidenceMetric.toDaoMetric(),
                rewardTransaction = RewardTransactionEntity(
                    transactionId = transactionId,
                    eventId = event.eventId,
                    actorId = event.actorId,
                    transactionKind = RewardTransactionKind.GRANT.wireName,
                    currency = RewardCurrencies.XP,
                    amount = stage.rewardXp,
                    ruleId = ruleId,
                    ruleVersion = event.ruleVersion,
                    reasonCode = "journey_stage_completed",
                    createdAtEpochMillis = event.occurredAtEpochMillis,
                    idempotencyKey = transactionId,
                    metadataJson = "{\"journeyDefinitionId\":\"${definition.id}\",\"stageDefinitionId\":\"${stage.id}\"}"
                ),
                presentationReceipt = RewardPresentationReceiptEntity(
                    receiptId = receiptId,
                    actorId = event.actorId,
                    sourceEventId = event.eventId,
                    sourceTransactionId = transactionId,
                    presentationFamily = GamificationPresentationIds.JOURNEY_STAGE_COMPLETE,
                    idempotencyKey = receiptId,
                    priority = 30,
                    tier = CelebrationTier.MILESTONE.wireName,
                    state = PresentationReceiptState.PENDING.wireName,
                    immutableSummaryJson = "{}",
                    createdAtEpochMillis = event.occurredAtEpochMillis,
                    updatedAtEpochMillis = event.occurredAtEpochMillis,
                    expiresAtEpochMillis = null,
                    acknowledgedAtEpochMillis = null,
                    suppressionReason = null,
                    coalescedCount = 1
                )
            )
        }
        return JourneySettlementRequest(instance = instance, stages = candidates)
    }
}

private fun JourneyEvidenceMetric.toDaoMetric(): CatalogEvidenceMetric = when (this) {
    JourneyEvidenceMetric.DISTINCT_REVIEWED_ITEMS ->
        CatalogEvidenceMetric.DISTINCT_REVIEWED_ITEMS
    JourneyEvidenceMetric.DISTINCT_PRACTICE_CONTENT_KINDS ->
        CatalogEvidenceMetric.DISTINCT_PRACTICE_CONTENT_KINDS
    JourneyEvidenceMetric.MASTERED_ITEMS_AFTER_DELAY ->
        CatalogEvidenceMetric.MASTERED_ITEMS_AFTER_DELAY
    JourneyEvidenceMetric.ACTIVE_STUDY_DAYS ->
        CatalogEvidenceMetric.ACTIVE_STUDY_DAYS
}
