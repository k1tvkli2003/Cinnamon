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
        dueItemCountAtAssignment: Int,
        repairCandidateCountAtAssignment: Int = 0,
        assignmentTimeZone: TimeZone = TimeZone.getDefault(),
        existingQuestInstances: List<QuestInstanceEntity> = emptyList()
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
                    definition.cadence in SUPPORTED_QUEST_CADENCES &&
                    definition.claimBehavior == ClaimBehavior.MANUAL_ONCE
            }
            .mapNotNull { definition ->
                val metric = definition.criterion.supportedSettlementMetric()
                    ?: return@mapNotNull null
                if (!definition.supportsWindowedMetric(metric)) return@mapNotNull null
                if (!definition.supportsImmutableQuestRewards(rewardsById)) return@mapNotNull null
                val effectiveRepairCandidateCount = repairCandidateCountAtAssignment.coerceAtLeast(0) +
                    if (event.eventType == RewardableEventType.MISTAKE_RECORDED.wireName) 1 else 0
                val eligible = definition.resolveAssignmentEligibility(
                    dueItemCountAtAssignment = dueItemCountAtAssignment,
                    repairCandidateCountAtAssignment = effectiveRepairCandidateCount
                )
                    ?: return@mapNotNull null
                val existing = matchingExistingQuest(
                    event = event,
                    definitionId = definition.id,
                    catalogVersion = bundle.catalog.storageVersion,
                    instances = existingQuestInstances
                )
                val window = existing?.let { (instance, criteria) ->
                    QuestAssignmentWindow(
                        evidenceStartsAtEpochMillis = criteria.evidenceStartsAtEpochMillis,
                        evidenceEndsAtEpochMillis = criteria.evidenceEndsAtEpochMillis,
                        expiresAtEpochMillis = instance.endsAtEpochMillis
                    )
                } ?: definition.assignmentWindow(event.occurredAtEpochMillis, assignmentTimeZone)
                    ?: return@mapNotNull null
                buildQuestCandidate(
                    bundle = bundle,
                    event = event,
                    definition = definition,
                    metric = metric,
                    eligible = eligible && event.recordedAtEpochMillis < window.expiresAtEpochMillis,
                    window = window,
                    assignmentTimeZoneId = existing?.second?.assignmentTimeZoneId
                        ?: assignmentTimeZone.id,
                    existingInstance = existing?.first
                )
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

    private fun buildQuestCandidate(
        bundle: GamificationCatalogBundle,
        event: GamificationEventEntity,
        definition: com.cinnamon.app.domain.gamification.QuestDefinition,
        metric: CatalogEvidenceMetric,
        eligible: Boolean,
        window: QuestAssignmentWindow,
        assignmentTimeZoneId: String,
        existingInstance: QuestInstanceEntity?
    ): CatalogQuestSettlementCandidate {
        val instanceId = StableRewardIds.questInstanceId(
            actorId = event.actorId,
            definitionId = definition.id,
            startsAtEpochMillis = window.evidenceStartsAtEpochMillis,
            catalogVersion = bundle.catalog.storageVersion
        )
        val clause = definition.criterion.clauses.single()
        val target = clause.target.toLong()
        val metricName = clause.metric.name.lowercase(Locale.ROOT)
        val aggregationName = clause.aggregation.name.lowercase(Locale.ROOT)
        val distinctByName = clause.distinctBy.name.lowercase(Locale.ROOT)
        existingInstance?.let { persisted ->
            val persistedCriteria = checkNotNull(
                QuestAssignmentContracts.decodeCriteria(persisted.criteriaJson)
            ) { "Existing quest ${persisted.questInstanceId} has an invalid criteria snapshot" }
            check(persisted.questInstanceId == instanceId) {
                "Existing quest identity no longer matches its immutable assignment window"
            }
            check(persisted.target == target && persisted.cadence == definition.cadence.name.lowercase(Locale.ROOT)) {
                "Catalog storage version changed a persisted quest contract without a version bump"
            }
            check(
                persistedCriteria.metric == metricName &&
                    persistedCriteria.aggregation == aggregationName &&
                    persistedCriteria.distinctBy == distinctByName
            ) { "Persisted quest evidence contract no longer matches its catalog definition" }
        }
        val instance = existingInstance ?: QuestInstanceEntity(
            questInstanceId = instanceId,
            actorId = event.actorId,
            definitionId = definition.id,
            catalogVersion = bundle.catalog.storageVersion,
            cadence = definition.cadence.name.lowercase(Locale.ROOT),
            startsAtEpochMillis = window.evidenceStartsAtEpochMillis,
            endsAtEpochMillis = window.expiresAtEpochMillis,
            state = QuestInstanceState.AVAILABLE.wireName,
            progress = 0L,
            target = target,
            criteriaJson = QuestAssignmentContracts.encodeCriteria(
                QuestCriteriaSnapshot(
                    metric = metricName,
                    aggregation = aggregationName,
                    distinctBy = distinctByName,
                    assignmentTimeZoneId = assignmentTimeZoneId,
                    evidenceStartsAtEpochMillis = window.evidenceStartsAtEpochMillis,
                    evidenceEndsAtEpochMillis = window.evidenceEndsAtEpochMillis
                )
            ),
            rewardJson = QuestAssignmentContracts.encodeReward(
                QuestAssignmentContracts.createRewardSnapshot(bundle, definition)
            ),
            createdAtEpochMillis = event.occurredAtEpochMillis,
            updatedAtEpochMillis = event.occurredAtEpochMillis,
            completionEventId = null,
            completedAtEpochMillis = null,
            claimedAtEpochMillis = null
        )

        return CatalogQuestSettlementCandidate(
            instance = instance,
            metric = metric,
            evidenceStartsAtEpochMillis = window.evidenceStartsAtEpochMillis,
            evidenceEndsAtEpochMillis = window.evidenceEndsAtEpochMillis,
            eligibleForAssignment = existingInstance == null && eligible
        )
    }

    private fun matchingExistingQuest(
        event: GamificationEventEntity,
        definitionId: String,
        catalogVersion: Int,
        instances: List<QuestInstanceEntity>
    ): Pair<QuestInstanceEntity, QuestCriteriaSnapshot>? {
        val relevant = instances.filter { instance ->
            instance.actorId == event.actorId &&
                instance.definitionId == definitionId &&
                instance.catalogVersion == catalogVersion &&
                instance.state in REUSABLE_QUEST_STATES
        }.map { instance ->
            instance to QuestAssignmentContracts.decodeCriteria(instance.criteriaJson)
        }
        val overlapping = relevant.mapNotNull { (instance, criteria) ->
            criteria?.takeIf { snapshot ->
                event.occurredAtEpochMillis >= snapshot.evidenceStartsAtEpochMillis &&
                    event.occurredAtEpochMillis < snapshot.evidenceEndsAtEpochMillis
            }?.let { snapshot -> instance to snapshot }
        }
        check(overlapping.size <= 1) {
            "Multiple quest instances overlap one assignment-time evidence window"
        }
        if (overlapping.isEmpty()) {
            check(
                relevant.none { (instance, criteria) ->
                    criteria == null &&
                        event.occurredAtEpochMillis >= instance.startsAtEpochMillis &&
                        event.occurredAtEpochMillis < instance.endsAtEpochMillis
                }
            ) { "An undecodable quest instance overlaps the current assignment instant" }
        }
        return overlapping.singleOrNull()
    }
}

private val SUPPORTED_QUEST_CADENCES = setOf(QuestCadence.DAILY, QuestCadence.WEEKLY)
private val REUSABLE_QUEST_STATES = setOf("available", "in_progress", "completed", "claimed")

private data class QuestAssignmentWindow(
    val evidenceStartsAtEpochMillis: Long,
    val evidenceEndsAtEpochMillis: Long,
    val expiresAtEpochMillis: Long
)

private fun com.cinnamon.app.domain.gamification.QuestDefinition.supportsWindowedMetric(
    metric: CatalogEvidenceMetric
): Boolean = when (cadence) {
    QuestCadence.DAILY -> metric in setOf(
        CatalogEvidenceMetric.DISTINCT_DUE_REVIEWS_TODAY,
        CatalogEvidenceMetric.DISTINCT_REPAIRED_SUBJECTS,
        CatalogEvidenceMetric.DISTINCT_CONFUSABLE_PAIRS,
        CatalogEvidenceMetric.DISTINCT_CONTEXT_APPLICATIONS
    )
    QuestCadence.WEEKLY -> metric in setOf(
        CatalogEvidenceMetric.MASTERED_ITEMS_AFTER_DELAY,
        CatalogEvidenceMetric.VERIFIED_REVIEW_QUEUE_CLEAR_DAYS
    )
    else -> false
}

private fun com.cinnamon.app.domain.gamification.QuestDefinition.supportsImmutableQuestRewards(
    rewardsById: Map<String, com.cinnamon.app.domain.gamification.RewardDefinition>
): Boolean = rewardRefs.isNotEmpty() && rewardRefs.all { rewardId ->
    rewardsById[rewardId]?.let { reward ->
        reward.type == RewardType.XP && reward.amount != null && reward.amount > 0
    } == true
}

private fun com.cinnamon.app.domain.gamification.QuestDefinition.resolveAssignmentEligibility(
    dueItemCountAtAssignment: Int,
    repairCandidateCountAtAssignment: Int
): Boolean? {
    val results = eligibility.clauses.map { clause ->
        val actual = when (clause.metric) {
            EligibilityMetric.ALWAYS -> 1
            EligibilityMetric.DUE_ITEM_COUNT -> dueItemCountAtAssignment.coerceAtLeast(0)
            EligibilityMetric.REPAIR_CANDIDATE_COUNT -> repairCandidateCountAtAssignment.coerceAtLeast(0)
            else -> return null
        }
        when (clause.operator) {
            ComparisonOperator.EQ -> actual == clause.value
            ComparisonOperator.GTE -> actual >= clause.value
            ComparisonOperator.LTE -> actual <= clause.value
            ComparisonOperator.IN -> return null
        }
    }
    return results.all { it }
}

private fun com.cinnamon.app.domain.gamification.QuestDefinition.assignmentWindow(
    occurredAtEpochMillis: Long,
    timeZone: TimeZone
): QuestAssignmentWindow? {
    val start = Calendar.getInstance(timeZone).apply {
        timeInMillis = occurredAtEpochMillis
        firstDayOfWeek = Calendar.MONDAY
        minimalDaysInFirstWeek = 4
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        when (cadence) {
            QuestCadence.DAILY -> Unit
            QuestCadence.WEEKLY -> {
                while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    add(Calendar.DAY_OF_MONTH, -1)
                }
            }
            else -> return null
        }
    }
    val evidenceEnd = (start.clone() as Calendar).apply {
        when (cadence) {
            QuestCadence.DAILY -> add(Calendar.DAY_OF_MONTH, 1)
            QuestCadence.WEEKLY -> add(Calendar.DAY_OF_MONTH, 7)
            else -> return null
        }
    }
    val claimEnd = (evidenceEnd.clone() as Calendar).apply {
        add(Calendar.HOUR_OF_DAY, expiry.graceHours)
    }
    return QuestAssignmentWindow(
        evidenceStartsAtEpochMillis = start.timeInMillis,
        evidenceEndsAtEpochMillis = evidenceEnd.timeInMillis,
        expiresAtEpochMillis = claimEnd.timeInMillis
    )
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
        clause.isRepairCriterion() || clause.isSingleRepairQuestCriterion() ->
            CatalogEvidenceMetric.DISTINCT_REPAIRED_SUBJECTS
        clause.isConfusablePrecisionCriterion() || clause.isDailyConfusableQuestCriterion() ->
            CatalogEvidenceMetric.DISTINCT_CONFUSABLE_PAIRS
        clause.isContextApplicationCriterion() -> CatalogEvidenceMetric.DISTINCT_CONTEXT_APPLICATIONS
        clause.isDelayedRecallCriterion() -> CatalogEvidenceMetric.DISTINCT_DELAYED_RECALLS
        clause.isGentleReturnCriterion() -> CatalogEvidenceMetric.VERIFIED_COMEBACK_SESSIONS
        clause.isSavedItemReviewCriterion() -> CatalogEvidenceMetric.DISTINCT_VERIFIED_SAVED_ITEMS
        clause.isReviewQueueClearAchievementCriterion() ||
            clause.isReviewQueueClearQuestCriterion() ->
            CatalogEvidenceMetric.VERIFIED_REVIEW_QUEUE_CLEAR_DAYS
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

private fun CriterionClause.isRepairCriterion(): Boolean {
    if (
        metric != LearningMetric.MISTAKE_REPAIRED ||
        aggregation != CriterionAggregation.COUNT_DISTINCT ||
        distinctBy != DistinctDimension.SUBJECT_ID ||
        minimumDelayHours != 0 ||
        filters.size != 2
    ) {
        return false
    }
    val filtersByField = filters.associateBy { filter -> filter.field }
    return filtersByField.size == 2 &&
        filtersByField["repair_link_present"]?.let { filter ->
            filter.operator == ComparisonOperator.EQ && filter.value == "true"
        } == true &&
        filtersByField["incorrect_attempt_precedes_correction"]?.let { filter ->
            filter.operator == ComparisonOperator.EQ && filter.value == "true"
        } == true
}

/** The daily quest is one verified repair; the DAO still revalidates temporal linkage itself. */
private fun CriterionClause.isSingleRepairQuestCriterion(): Boolean =
    metric == LearningMetric.MISTAKE_REPAIRED &&
        aggregation == CriterionAggregation.BOOLEAN &&
        distinctBy == DistinctDimension.NONE &&
        minimumDelayHours == 0 &&
        filters.singleOrNull()?.let { filter ->
            filter.field == "repair_link_present" &&
                filter.operator == ComparisonOperator.EQ &&
                filter.value == "true"
        } == true

private fun CriterionClause.isConfusablePrecisionCriterion(): Boolean =
    metric == LearningMetric.CONFUSABLE_PAIR_RESOLVED &&
        aggregation == CriterionAggregation.COUNT_DISTINCT &&
        distinctBy == DistinctDimension.CONFUSABLE_PAIR &&
        minimumDelayHours >= 24 &&
        filters.isEmpty()

/** The event-layer link makes the daily contract truthful even though the catalog sets no delay. */
private fun CriterionClause.isDailyConfusableQuestCriterion(): Boolean =
    metric == LearningMetric.CONFUSABLE_PAIR_RESOLVED &&
        aggregation == CriterionAggregation.COUNT_DISTINCT &&
        distinctBy == DistinctDimension.CONFUSABLE_PAIR &&
        minimumDelayHours == 0 &&
        filters.isEmpty()

private fun CriterionClause.isContextApplicationCriterion(): Boolean =
    metric == LearningMetric.NEW_ITEM_APPLIED_IN_CONTEXT &&
        aggregation == CriterionAggregation.COUNT_DISTINCT &&
        distinctBy == DistinctDimension.SUBJECT_ID &&
        minimumDelayHours == 0 &&
        filters.singleOrNull()?.let { filter ->
            filter.field == "application_verified" &&
                filter.operator == ComparisonOperator.EQ &&
                filter.value == "true"
        } == true

private fun CriterionClause.isDelayedRecallCriterion(): Boolean =
    metric == LearningMetric.DELAYED_RECALL_SUCCEEDED &&
        aggregation == CriterionAggregation.COUNT_DISTINCT &&
        distinctBy == DistinctDimension.SUBJECT_ID &&
        minimumDelayHours >= 72 &&
        filters.isEmpty()

private fun CriterionClause.isGentleReturnCriterion(): Boolean {
    if (
        metric != LearningMetric.COMEBACK_SESSION_COMPLETED ||
        aggregation != CriterionAggregation.BOOLEAN ||
        distinctBy != DistinctDimension.NONE ||
        minimumDelayHours != 0 ||
        filters.size != 2
    ) {
        return false
    }
    val filtersByField = filters.associateBy { filter -> filter.field }
    return filtersByField.size == 2 &&
        filtersByField["absence_days"]?.let { filter ->
            filter.operator == ComparisonOperator.GTE && filter.value.toIntOrNull() == 7
        } == true &&
        filtersByField["meaningful_actions"]?.let { filter ->
            filter.operator == ComparisonOperator.GTE && filter.value.toIntOrNull() == 3
        } == true
}

private fun CriterionClause.isSavedItemReviewCriterion(): Boolean =
    metric == LearningMetric.SAVED_ITEM_REVIEWED &&
        aggregation == CriterionAggregation.COUNT_DISTINCT &&
        distinctBy == DistinctDimension.SUBJECT_ID &&
        minimumDelayHours >= 1 &&
        filters.singleOrNull()?.let { filter ->
            filter.field == "successful_review_count" &&
            filter.operator == ComparisonOperator.GTE &&
                filter.value.toIntOrNull() == 1
        } == true

private fun CriterionClause.isReviewQueueClearAchievementCriterion(): Boolean =
    metric == LearningMetric.REVIEW_QUEUE_CLEARED &&
        aggregation == CriterionAggregation.COUNT_DISTINCT &&
        distinctBy == DistinctDimension.DAY &&
        minimumDelayHours == 0 &&
        hasExactReviewQueueStartingSizeFilter()

private fun CriterionClause.isReviewQueueClearQuestCriterion(): Boolean =
    metric == LearningMetric.REVIEW_QUEUE_CLEARED &&
        aggregation == CriterionAggregation.BOOLEAN &&
        distinctBy == DistinctDimension.NONE &&
        minimumDelayHours == 0 &&
        hasExactReviewQueueStartingSizeFilter()

private fun CriterionClause.hasExactReviewQueueStartingSizeFilter(): Boolean =
    filters.singleOrNull()?.let { filter ->
        filter.field == "starting_due_count" &&
            filter.operator == ComparisonOperator.GTE &&
            filter.value.toIntOrNull() == 5
    } == true

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
