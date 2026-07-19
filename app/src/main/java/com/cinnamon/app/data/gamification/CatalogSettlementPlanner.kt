package com.cinnamon.app.data.gamification

import com.cinnamon.app.data.local.AchievementUnlockEntity
import com.cinnamon.app.data.local.CatalogEvidenceMetric
import com.cinnamon.app.data.local.CatalogQuestSettlementCandidate
import com.cinnamon.app.data.local.CatalogSettlementRequest
import com.cinnamon.app.data.local.CatalogUnlockSettlementCandidate
import com.cinnamon.app.data.local.GamificationEventEntity
import com.cinnamon.app.data.local.QuestInstanceEntity
import com.cinnamon.app.data.local.RewardPresentationReceiptEntity
import com.cinnamon.app.data.local.RewardTransactionEntity
import com.cinnamon.app.domain.gamification.CatalogCriteria
import com.cinnamon.app.domain.gamification.CatalogLifecycleState
import com.cinnamon.app.domain.gamification.ClaimBehavior
import com.cinnamon.app.domain.gamification.ComparisonOperator
import com.cinnamon.app.domain.gamification.CriteriaMatch
import com.cinnamon.app.domain.gamification.CriterionAggregation
import com.cinnamon.app.domain.gamification.CriterionClause
import com.cinnamon.app.domain.gamification.DistinctDimension
import com.cinnamon.app.domain.gamification.EligibilityMetric
import com.cinnamon.app.domain.gamification.GamificationCatalogBundle
import com.cinnamon.app.domain.gamification.LearningMetric
import com.cinnamon.app.domain.gamification.PresentationTier
import com.cinnamon.app.domain.gamification.QuestCadence
import com.cinnamon.app.domain.gamification.RewardType
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Converts only evidence contracts that the Room ledger can prove into persistence candidates.
 * Unsupported catalog criteria remain inert instead of being approximated from XP or UI taps.
 */
object CatalogSettlementPlanner {

    fun plan(
        bundle: GamificationCatalogBundle,
        event: GamificationEventEntity,
        dueItemCountAtAssignment: Int
    ): CatalogSettlementRequest {
        val catalog = bundle.catalog
        val rewardsById = catalog.rewards.associateBy { it.id }
        val presentationsById = catalog.presentations.associateBy { it.id }

        val achievementUnlocks = catalog.achievements
            .asSequence()
            .filter { definition -> definition.lifecycle.state == CatalogLifecycleState.ACTIVE }
            .mapNotNull { definition ->
                val metric = definition.criterion.supportedSettlementMetric() ?: return@mapNotNull null
                definition to metric
            }
            .flatMap { (definition, metric) ->
                definition.levels.asSequence().map { level ->
                    buildUnlockCandidate(
                        bundle = bundle,
                        event = event,
                        catalogItemId = definition.id,
                        level = level.tier,
                        target = level.target.toLong(),
                        metric = metric,
                        rewardRefs = level.rewardRefs,
                        presentationId = definition.presentationId,
                        reasonCode = "achievement_level_unlocked",
                        rewardsById = rewardsById,
                        presentation = presentationsById[definition.presentationId]
                    )
                }
            }

        val progressionUnlocks = catalog.progressionLevels
            .asSequence()
            .filter { level ->
                level.threshold > 0 &&
                    level.rewardRefs.isNotEmpty() &&
                    level.metric == LearningMetric.UNIQUE_ITEM_MASTERED_AFTER_DELAY
            }
            .map { level ->
                buildUnlockCandidate(
                    bundle = bundle,
                    event = event,
                    catalogItemId = level.id,
                    level = 1,
                    target = level.threshold.toLong(),
                    metric = CatalogEvidenceMetric.MASTERED_ITEMS_AFTER_DELAY,
                    rewardRefs = level.rewardRefs,
                    presentationId = level.presentationId,
                    reasonCode = "progression_level_unlocked",
                    rewardsById = rewardsById,
                    presentation = presentationsById[level.presentationId]
                )
            }

        val quests = catalog.quests
            .asSequence()
            .filter { definition ->
                definition.lifecycle.state == CatalogLifecycleState.ACTIVE &&
                    definition.cadence == QuestCadence.DAILY &&
                    definition.claimBehavior == ClaimBehavior.MANUAL_ONCE
            }
            .mapNotNull { definition ->
                val metric = definition.criterion.supportedSettlementMetric()
                    ?: return@mapNotNull null
                if (metric != CatalogEvidenceMetric.DISTINCT_DUE_REVIEWS_TODAY) {
                    return@mapNotNull null
                }
                val eligible = definition.eligibility.clauses.all { clause ->
                    val actual = when (clause.metric) {
                        EligibilityMetric.ALWAYS -> 1
                        EligibilityMetric.DUE_ITEM_COUNT -> dueItemCountAtAssignment.coerceAtLeast(0)
                        else -> return@mapNotNull null
                    }
                    when (clause.operator) {
                        ComparisonOperator.EQ -> actual == clause.value
                        ComparisonOperator.GTE -> actual >= clause.value
                        ComparisonOperator.LTE -> actual <= clause.value
                        ComparisonOperator.IN -> return@mapNotNull null
                    }
                }
                buildDailyQuestCandidate(bundle, event, definition, metric, eligible)
            }
            .toList()

        return CatalogSettlementRequest(
            unlocks = (achievementUnlocks + progressionUnlocks).toList(),
            quests = quests
        )
    }

    private fun buildUnlockCandidate(
        bundle: GamificationCatalogBundle,
        event: GamificationEventEntity,
        catalogItemId: String,
        level: Int,
        target: Long,
        metric: CatalogEvidenceMetric,
        rewardRefs: List<String>,
        presentationId: String,
        reasonCode: String,
        rewardsById: Map<String, com.cinnamon.app.domain.gamification.RewardDefinition>,
        presentation: com.cinnamon.app.domain.gamification.AchievementPresentationDefinition?
    ): CatalogUnlockSettlementCandidate {
        val rewards = rewardRefs.map { rewardId ->
            checkNotNull(rewardsById[rewardId]) { "Validated catalog is missing reward $rewardId" }
        }
        val xpAmount = rewards.asSequence()
            .filter { reward -> reward.type == RewardType.XP }
            .sumOf { reward -> reward.amount?.toLong() ?: 0L }
        val markerIds = rewards.mapNotNull { reward ->
            reward.entitlementId?.takeIf { reward.type == RewardType.PROFILE_MARKER }
        }
        val ruleId = "catalog.${catalogItemId}.level.$level"
        val transactionId = if (xpAmount > 0L) {
            StableRewardIds.transactionId(event.eventId, ruleId, RewardCurrencies.XP)
        } else {
            null
        }
        val xpTransaction = transactionId?.let { id ->
            RewardTransactionEntity(
                transactionId = id,
                eventId = event.eventId,
                actorId = event.actorId,
                transactionKind = RewardTransactionKind.GRANT.wireName,
                currency = RewardCurrencies.XP,
                amount = xpAmount,
                ruleId = ruleId,
                ruleVersion = event.ruleVersion,
                reasonCode = reasonCode,
                createdAtEpochMillis = event.occurredAtEpochMillis,
                idempotencyKey = id,
                metadataJson = "{\"catalogItemId\":${jsonString(catalogItemId)},\"level\":$level}"
            )
        }
        val presentationTier = presentation?.tier ?: PresentationTier.MILESTONE
        val receiptId = StableRewardIds.catalogPresentationReceiptId(
            eventId = event.eventId,
            catalogItemId = catalogItemId,
            level = level,
            presentationFamily = presentationId
        )
        val expiresAt = presentation?.expiresAfterSeconds?.let { seconds ->
            Math.addExact(event.occurredAtEpochMillis, Math.multiplyExact(seconds.toLong(), 1_000L))
        }

        return CatalogUnlockSettlementCandidate(
            unlock = AchievementUnlockEntity(
                unlockId = StableRewardIds.catalogUnlockId(
                    actorId = event.actorId,
                    catalogItemId = catalogItemId,
                    level = level,
                    catalogVersion = bundle.catalog.storageVersion
                ),
                actorId = event.actorId,
                achievementId = catalogItemId,
                level = level,
                catalogVersion = bundle.catalog.storageVersion,
                progressJson = "{}",
                unlockedAtEpochMillis = event.occurredAtEpochMillis,
                sourceEventId = event.eventId,
                evidenceJson = "{}"
            ),
            metric = metric,
            target = target,
            xpTransaction = xpTransaction,
            unlockedContentIds = markerIds,
            presentationReceipt = RewardPresentationReceiptEntity(
                receiptId = receiptId,
                actorId = event.actorId,
                sourceEventId = event.eventId,
                sourceTransactionId = transactionId,
                presentationFamily = presentationId,
                idempotencyKey = receiptId,
                priority = presentationTier.priority(),
                tier = presentationTier.name.lowercase(Locale.ROOT),
                state = PresentationReceiptState.PENDING.wireName,
                immutableSummaryJson = "{}",
                createdAtEpochMillis = event.occurredAtEpochMillis,
                updatedAtEpochMillis = event.occurredAtEpochMillis,
                expiresAtEpochMillis = expiresAt,
                acknowledgedAtEpochMillis = null,
                suppressionReason = null,
                coalescedCount = 1
            )
        )
    }

    private fun buildDailyQuestCandidate(
        bundle: GamificationCatalogBundle,
        event: GamificationEventEntity,
        definition: com.cinnamon.app.domain.gamification.QuestDefinition,
        metric: CatalogEvidenceMetric,
        eligible: Boolean
    ): CatalogQuestSettlementCandidate {
        val zone = TimeZone.getDefault()
        val startCalendar = Calendar.getInstance(zone).apply {
            timeInMillis = event.occurredAtEpochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startsAt = startCalendar.timeInMillis
        val endsAt = (startCalendar.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.HOUR_OF_DAY, definition.expiry.graceHours)
        }.timeInMillis
        val instanceId = StableRewardIds.questInstanceId(
            actorId = event.actorId,
            definitionId = definition.id,
            startsAtEpochMillis = startsAt,
            catalogVersion = bundle.catalog.storageVersion
        )
        val target = definition.criterion.clauses.single().target.toLong()

        return CatalogQuestSettlementCandidate(
            instance = QuestInstanceEntity(
                questInstanceId = instanceId,
                actorId = event.actorId,
                definitionId = definition.id,
                catalogVersion = bundle.catalog.storageVersion,
                cadence = definition.cadence.name.lowercase(Locale.ROOT),
                startsAtEpochMillis = startsAt,
                endsAtEpochMillis = endsAt,
                state = QuestInstanceState.AVAILABLE.wireName,
                progress = 0L,
                target = target,
                criteriaJson = "{\"metric\":\"due_review_completed\",\"distinctBy\":\"subject_id\",\"timezone\":${jsonString(zone.id)}}",
                rewardJson = definition.rewardRefs.joinToString(prefix = "[", postfix = "]", transform = ::jsonString),
                createdAtEpochMillis = event.occurredAtEpochMillis,
                updatedAtEpochMillis = event.occurredAtEpochMillis,
                completionEventId = null,
                completedAtEpochMillis = null,
                claimedAtEpochMillis = null
            ),
            metric = metric,
            eligibleForAssignment = eligible
        )
    }
}

private fun CatalogCriteria.supportedSettlementMetric(): CatalogEvidenceMetric? {
    if (match != CriteriaMatch.ALL || clauses.size != 1) return null
    val clause = clauses.single()
    if (!clause.requiresVerifiedOutcome) return null
    return when {
        clause.isDistinctDueReviewCriterion() -> CatalogEvidenceMetric.DISTINCT_DUE_REVIEWS_TODAY
        clause.isDelayedMasteryCriterion() -> CatalogEvidenceMetric.MASTERED_ITEMS_AFTER_DELAY
        clause.isRhythmWeekCriterion() -> CatalogEvidenceMetric.RHYTHM_WEEKS
        clause.isContentBreadthCriterion() -> CatalogEvidenceMetric.DISTINCT_PRACTICE_CONTENT_KINDS
        else -> null
    }
}

private fun CriterionClause.isDistinctDueReviewCriterion(): Boolean =
    metric == LearningMetric.DUE_REVIEW_COMPLETED &&
        aggregation == CriterionAggregation.COUNT_DISTINCT &&
        distinctBy == DistinctDimension.SUBJECT_ID &&
        minimumDelayHours == 0 &&
        filters.isEmpty()

private fun CriterionClause.isDelayedMasteryCriterion(): Boolean =
    metric == LearningMetric.UNIQUE_ITEM_MASTERED_AFTER_DELAY &&
        aggregation == CriterionAggregation.COUNT_DISTINCT &&
        distinctBy == DistinctDimension.SUBJECT_ID &&
        minimumDelayHours > 0 &&
        filters.isEmpty()

private fun CriterionClause.isRhythmWeekCriterion(): Boolean =
    metric == LearningMetric.ACTIVE_STUDY_DAY &&
        aggregation == CriterionAggregation.COUNT_DISTINCT &&
        distinctBy == DistinctDimension.WEEK &&
        minimumDelayHours == 0 &&
        filters.singleOrNull()?.let { filter ->
            filter.field == "minimum_active_days_per_week" &&
                filter.operator == ComparisonOperator.GTE &&
                filter.value.toIntOrNull() == 3
        } == true

private fun CriterionClause.isContentBreadthCriterion(): Boolean =
    metric == LearningMetric.CONTENT_KIND_PRACTICED &&
        aggregation == CriterionAggregation.COUNT_DISTINCT &&
        distinctBy == DistinctDimension.CONTENT_KIND &&
        minimumDelayHours == 0 &&
        filters.isEmpty()

private fun PresentationTier.priority(): Int = when (this) {
    PresentationTier.MICRO -> 10
    PresentationTier.STANDARD -> 20
    PresentationTier.MILESTONE -> 30
    PresentationTier.SHOWPIECE -> 40
}

private fun jsonString(value: String): String = buildString(value.length + 2) {
    append('"')
    value.forEach { character ->
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(character)
        }
    }
    append('"')
}
