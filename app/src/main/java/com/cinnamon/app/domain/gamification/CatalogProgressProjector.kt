package com.cinnamon.app.domain.gamification

/**
 * Evidence that can be reconstructed truthfully from the current immutable event ledger.
 *
 * This read model deliberately contains no reward, claim, or unlock state. Reaching a threshold in
 * one of these projections is evidence for the UI; it is never authority to grant catalog rewards.
 */
data class CatalogEvidenceSnapshot(
    val distinctDueReviewsToday: Int = 0,
    val dueItemCountNow: Int = 0,
    val masteredItemsAfterDelay: Int = 0,
    val rhythmWeeks: Int = 0,
    val distinctPracticeContentKinds: Int = 0
)

data class CatalogQuestProjection(
    val id: String,
    val title: String,
    val reason: String,
    val progress: Int,
    val target: Int,
    val thresholdReached: Boolean
)

data class CatalogAchievementProjection(
    val id: String,
    val family: AchievementFamily,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val thresholdReached: Boolean
)

data class CatalogProgressionLevelProjection(
    val id: String,
    val order: Int,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val thresholdReached: Boolean
)

/**
 * Converts a validated catalog plus ledger-derived evidence into read-only UI state.
 *
 * Support is intentionally allow-listed by the exact criterion contract. A newly added catalog
 * metric stays invisible until a real persisted signal and a tested resolver are added here.
 */
object CatalogProgressProjector {

    fun projectEligibleDailyQuests(
        bundle: GamificationCatalogBundle,
        evidence: CatalogEvidenceSnapshot
    ): List<CatalogQuestProjection> = bundle.catalog.quests
        .asSequence()
        .filter { definition ->
            definition.lifecycle.state == CatalogLifecycleState.ACTIVE &&
                definition.cadence == QuestCadence.DAILY
        }
        .mapNotNull { definition ->
            val progress = resolveSingleCriterion(definition.criterion, evidence) ?: return@mapNotNull null
            val eligible = resolveEligibility(definition.eligibility, evidence) ?: return@mapNotNull null

            // With no persisted quest assignment yet, retain an in-progress/completed checkpoint
            // after today's evidence exists, but do not invent a claim or reward state.
            if (!eligible && progress <= 0) return@mapNotNull null

            val target = definition.criterion.clauses.single().target
            CatalogQuestProjection(
                id = definition.id,
                title = bundle.copy.required(definition.titleKey),
                reason = bundle.copy.required(definition.reasonKey),
                progress = progress.coerceIn(0, target),
                target = target,
                thresholdReached = progress >= target
            )
        }
        .toList()

    fun projectAchievements(
        bundle: GamificationCatalogBundle,
        evidence: CatalogEvidenceSnapshot
    ): List<CatalogAchievementProjection> = bundle.catalog.achievements
        .asSequence()
        .filter { definition ->
            definition.lifecycle.state == CatalogLifecycleState.ACTIVE && !definition.isSecret
        }
        .mapNotNull { definition ->
            val progress = resolveSingleCriterion(definition.criterion, evidence) ?: return@mapNotNull null
            val firstLevel = definition.levels.minByOrNull(AchievementLevelDefinition::tier)
                ?: return@mapNotNull null
            CatalogAchievementProjection(
                id = definition.id,
                family = definition.family,
                title = bundle.copy.required(definition.titleKey),
                description = bundle.copy.required(definition.descriptionKey),
                progress = progress.coerceIn(0, firstLevel.target),
                target = firstLevel.target,
                thresholdReached = progress >= firstLevel.target
            )
        }
        .toList()

    fun projectProgressionLevels(
        bundle: GamificationCatalogBundle,
        evidence: CatalogEvidenceSnapshot
    ): List<CatalogProgressionLevelProjection> = bundle.catalog.progressionLevels
        .asSequence()
        .sortedBy(ProgressionLevelDefinition::order)
        .mapNotNull { definition ->
            val progress = resolveProgressionMetric(definition.metric, evidence) ?: return@mapNotNull null
            CatalogProgressionLevelProjection(
                id = definition.id,
                order = definition.order,
                title = bundle.copy.required(definition.titleKey),
                description = bundle.copy.required(definition.descriptionKey),
                progress = progress.coerceIn(0, definition.threshold.coerceAtLeast(0)),
                target = definition.threshold,
                thresholdReached = progress >= definition.threshold
            )
        }
        .toList()

    fun currentProgressionLevel(
        bundle: GamificationCatalogBundle,
        evidence: CatalogEvidenceSnapshot
    ): CatalogProgressionLevelProjection? = projectProgressionLevels(bundle, evidence)
        .lastOrNull(CatalogProgressionLevelProjection::thresholdReached)

    private fun resolveSingleCriterion(
        criteria: CatalogCriteria,
        evidence: CatalogEvidenceSnapshot
    ): Int? {
        if (criteria.match != CriteriaMatch.ALL || criteria.clauses.size != 1) return null
        val clause = criteria.clauses.single()
        if (!clause.requiresVerifiedOutcome) return null

        return when {
            clause.isDistinctDueReviewCriterion() -> evidence.distinctDueReviewsToday
            clause.isDelayedMasteryCriterion() -> evidence.masteredItemsAfterDelay
            clause.isRhythmWeekCriterion() -> evidence.rhythmWeeks
            clause.isContentBreadthCriterion() -> evidence.distinctPracticeContentKinds
            else -> null
        }?.coerceAtLeast(0)
    }

    private fun resolveProgressionMetric(
        metric: LearningMetric,
        evidence: CatalogEvidenceSnapshot
    ): Int? = when (metric) {
        LearningMetric.UNIQUE_ITEM_MASTERED_AFTER_DELAY -> evidence.masteredItemsAfterDelay.coerceAtLeast(0)
        else -> null
    }

    private fun resolveEligibility(
        eligibility: QuestEligibility,
        evidence: CatalogEvidenceSnapshot
    ): Boolean? {
        val results = eligibility.clauses.map { clause ->
            val actual = when (clause.metric) {
                EligibilityMetric.ALWAYS -> 1
                EligibilityMetric.DUE_ITEM_COUNT -> evidence.dueItemCountNow.coerceAtLeast(0)
                // Current content-kind evidence describes completed practice, not the separate
                // available-content inventory required by this eligibility contract.
                else -> return null
            }
            compare(actual, clause.operator, clause.value) ?: return null
        }
        return results.all { it }
    }

    private fun compare(actual: Int, operator: ComparisonOperator, expected: Int): Boolean? = when (operator) {
        ComparisonOperator.EQ -> actual == expected
        ComparisonOperator.GTE -> actual >= expected
        ComparisonOperator.LTE -> actual <= expected
        ComparisonOperator.IN -> null
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

private fun GamificationCopyCatalog.required(key: String): String =
    strings[key] ?: error("Validated gamification copy is missing key: $key")
