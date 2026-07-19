package com.cinnamon.app.domain.gamification

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Immutable, versioned content consumed by the reward engine.
 *
 * Catalog definitions describe what can progress and what should be presented. They never grant a
 * reward by themselves; only an idempotent reward transaction may do that.
 */
@JsonClass(generateAdapter = true)
data class GamificationCatalog(
    val schemaVersion: Int,
    /** Compact persistence version used by the Room ledger; catalogVersion remains semantic. */
    val storageVersion: Int,
    val catalogVersion: String,
    val criterionContractVersion: Int,
    val manifest: CatalogSeedManifest,
    val rarities: List<BadgeRarityDefinition>,
    val rewards: List<RewardDefinition>,
    val presentations: List<AchievementPresentationDefinition>,
    val progressionLevels: List<ProgressionLevelDefinition>,
    val achievements: List<AchievementDefinition>,
    val quests: List<QuestDefinition>
)

@JsonClass(generateAdapter = true)
data class CatalogSeedManifest(
    val seedId: String,
    val sourceAsset: String,
    val version: String,
    val previousVersion: String? = null,
    val migrationNotes: List<String>,
    val defaultLocale: String,
    val localizationAssets: List<String>,
    val localizationKeys: List<String>,
    val artRequirements: List<String>,
    val testRequirements: List<String>,
    val retiredIds: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GamificationCopyCatalog(
    val schemaVersion: Int,
    val catalogVersion: String,
    val locale: String,
    val strings: Map<String, String>
)

data class GamificationCatalogBundle(
    val catalog: GamificationCatalog,
    val copy: GamificationCopyCatalog
)

@JsonClass(generateAdapter = true)
data class BadgeRarityDefinition(
    val id: BadgeRarity,
    val accessibilityLabelKey: String,
    val distributionExpectation: String,
    val treatmentRequirement: String
)

enum class BadgeRarity {
    @Json(name = "common") COMMON,
    @Json(name = "notable") NOTABLE,
    @Json(name = "rare") RARE,
    @Json(name = "exceptional") EXCEPTIONAL
}

@JsonClass(generateAdapter = true)
data class RewardDefinition(
    val id: String,
    val version: String,
    val type: RewardType,
    val amount: Int? = null,
    val entitlementId: String? = null,
    val explanationKey: String,
    val sourceRule: String,
    val claimBehavior: ClaimBehavior,
    val economyNotes: String,
    val maxGrantsPerSource: Int = 1
)

enum class RewardType {
    @Json(name = "xp") XP,
    @Json(name = "profile_marker") PROFILE_MARKER
}

enum class ClaimBehavior {
    @Json(name = "automatic") AUTOMATIC,
    @Json(name = "manual_once") MANUAL_ONCE
}

@JsonClass(generateAdapter = true)
data class AchievementPresentationDefinition(
    val id: String,
    val tier: PresentationTier,
    val motionId: String,
    val liveFields: List<String>,
    val requiresMascot: Boolean,
    val requiresAudio: Boolean,
    val artRequirement: String,
    val reducedMotionStrategy: String,
    val noMotionStrategy: String,
    val replayPolicy: ReplayPolicy,
    val expiresAfterSeconds: Int,
    val platformFallback: String,
    val clinicalModePolicy: ClinicalModePolicy
)

enum class PresentationTier {
    @Json(name = "micro") MICRO,
    @Json(name = "standard") STANDARD,
    @Json(name = "milestone") MILESTONE,
    @Json(name = "showpiece") SHOWPIECE
}

enum class ReplayPolicy {
    @Json(name = "never") NEVER,
    @Json(name = "profile_history_only") PROFILE_HISTORY_ONLY
}

enum class ClinicalModePolicy {
    @Json(name = "allow") ALLOW,
    @Json(name = "defer_until_session_exit") DEFER_UNTIL_SESSION_EXIT,
    @Json(name = "suppress") SUPPRESS
}

@JsonClass(generateAdapter = true)
data class ProgressionLevelDefinition(
    val id: String,
    val order: Int,
    val titleKey: String,
    val descriptionKey: String,
    val metric: LearningMetric,
    val threshold: Int,
    val rewardRefs: List<String> = emptyList(),
    val presentationId: String
)

@JsonClass(generateAdapter = true)
data class AchievementDefinition(
    val id: String,
    val version: String,
    val family: AchievementFamily,
    val titleKey: String,
    val descriptionKey: String,
    val criterion: CatalogCriteria,
    val levels: List<AchievementLevelDefinition>,
    val isSecret: Boolean,
    val initialDisplayState: AchievementDisplayState,
    val claimBehavior: ClaimBehavior,
    val presentationId: String,
    val lifecycle: CatalogLifecycle
)

enum class AchievementFamily {
    @Json(name = "mastery") MASTERY,
    @Json(name = "consistency") CONSISTENCY,
    @Json(name = "correction") CORRECTION,
    @Json(name = "exploration") EXPLORATION,
    @Json(name = "challenge") CHALLENGE,
    @Json(name = "collection") COLLECTION,
    @Json(name = "comeback") COMEBACK,
    @Json(name = "reflection") REFLECTION,
    @Json(name = "application") APPLICATION
}

enum class AchievementDisplayState {
    @Json(name = "locked") LOCKED,
    @Json(name = "visible") VISIBLE,
    @Json(name = "hidden") HIDDEN
}

@JsonClass(generateAdapter = true)
data class AchievementLevelDefinition(
    val tier: Int,
    /** Target for the primary criterion clause at this tier; additional clauses remain gates. */
    val target: Int,
    val rarity: BadgeRarity,
    val visualLevel: Int,
    val progressCopyKey: String,
    val rewardRefs: List<String>
)

@JsonClass(generateAdapter = true)
data class QuestDefinition(
    val id: String,
    val version: String,
    val cadence: QuestCadence,
    val titleKey: String,
    val reasonKey: String,
    val criterion: CatalogCriteria,
    val eligibility: QuestEligibility,
    val expiry: QuestExpiryDefinition,
    val replacement: QuestReplacementPolicy,
    val rewardRefs: List<String>,
    val claimBehavior: ClaimBehavior,
    val progressVisibility: ProgressVisibility,
    val presentationId: String,
    val lifecycle: CatalogLifecycle
)

enum class QuestCadence {
    @Json(name = "daily") DAILY,
    @Json(name = "weekly") WEEKLY,
    @Json(name = "monthly") MONTHLY,
    @Json(name = "weekend") WEEKEND,
    @Json(name = "comeback") COMEBACK
}

enum class ProgressVisibility {
    @Json(name = "visible") VISIBLE,
    @Json(name = "hidden_until_complete") HIDDEN_UNTIL_COMPLETE
}

@JsonClass(generateAdapter = true)
data class QuestEligibility(
    val clauses: List<EligibilityClause> = emptyList()
)

@JsonClass(generateAdapter = true)
data class EligibilityClause(
    val metric: EligibilityMetric,
    val operator: ComparisonOperator,
    val value: Int
)

enum class EligibilityMetric {
    @Json(name = "always") ALWAYS,
    @Json(name = "due_item_count") DUE_ITEM_COUNT,
    @Json(name = "repair_candidate_count") REPAIR_CANDIDATE_COUNT,
    @Json(name = "absence_days") ABSENCE_DAYS,
    @Json(name = "available_content_kind_count") AVAILABLE_CONTENT_KIND_COUNT
}

@JsonClass(generateAdapter = true)
data class QuestExpiryDefinition(
    val boundary: ExpiryBoundary,
    val graceHours: Int,
    val timezonePolicy: TimezonePolicy
)

enum class ExpiryBoundary {
    @Json(name = "local_day_end") LOCAL_DAY_END,
    @Json(name = "local_week_end") LOCAL_WEEK_END,
    @Json(name = "local_month_end") LOCAL_MONTH_END,
    @Json(name = "weekend_end") WEEKEND_END,
    @Json(name = "assigned_window_end") ASSIGNED_WINDOW_END
}

enum class TimezonePolicy {
    @Json(name = "user_iana_at_assignment") USER_IANA_AT_ASSIGNMENT
}

@JsonClass(generateAdapter = true)
data class QuestReplacementPolicy(
    val mode: ReplacementMode,
    val group: String? = null,
    val maxReplacements: Int = 0
)

enum class ReplacementMode {
    @Json(name = "none") NONE,
    @Json(name = "replace_if_ineligible") REPLACE_IF_INELIGIBLE
}

@JsonClass(generateAdapter = true)
data class CatalogCriteria(
    val match: CriteriaMatch,
    val clauses: List<CriterionClause>
)

enum class CriteriaMatch {
    @Json(name = "all") ALL,
    @Json(name = "any") ANY
}

@JsonClass(generateAdapter = true)
data class CriterionClause(
    val metric: LearningMetric,
    val target: Int,
    val aggregation: CriterionAggregation,
    val distinctBy: DistinctDimension,
    val minimumDelayHours: Int = 0,
    val filters: List<CriterionFilter> = emptyList(),
    val requiresVerifiedOutcome: Boolean = true
)

/**
 * XP_EARNED_LEGACY is intentionally parseable only so the validator can reject an old catalog with
 * a precise migration error. It must never appear in a valid catalog.
 */
enum class LearningMetric {
    @Json(name = "due_review_completed") DUE_REVIEW_COMPLETED,
    @Json(name = "mistake_repaired") MISTAKE_REPAIRED,
    @Json(name = "unique_item_mastered_after_delay") UNIQUE_ITEM_MASTERED_AFTER_DELAY,
    @Json(name = "content_kind_practiced") CONTENT_KIND_PRACTICED,
    @Json(name = "review_queue_cleared") REVIEW_QUEUE_CLEARED,
    @Json(name = "confusable_pair_resolved") CONFUSABLE_PAIR_RESOLVED,
    @Json(name = "new_item_applied_in_context") NEW_ITEM_APPLIED_IN_CONTEXT,
    @Json(name = "delayed_recall_succeeded") DELAYED_RECALL_SUCCEEDED,
    @Json(name = "comeback_session_completed") COMEBACK_SESSION_COMPLETED,
    @Json(name = "confidence_calibrated") CONFIDENCE_CALIBRATED,
    @Json(name = "saved_item_reviewed") SAVED_ITEM_REVIEWED,
    @Json(name = "morpheme_connection_mastered") MORPHEME_CONNECTION_MASTERED,
    @Json(name = "active_study_day") ACTIVE_STUDY_DAY,
    @Json(name = "xp_earned") XP_EARNED_LEGACY
}

enum class CriterionAggregation {
    @Json(name = "count") COUNT,
    @Json(name = "count_distinct") COUNT_DISTINCT,
    @Json(name = "boolean") BOOLEAN
}

enum class DistinctDimension {
    @Json(name = "none") NONE,
    @Json(name = "subject_id") SUBJECT_ID,
    @Json(name = "day") DAY,
    @Json(name = "week") WEEK,
    @Json(name = "content_kind") CONTENT_KIND,
    @Json(name = "session_id") SESSION_ID,
    @Json(name = "morpheme_family") MORPHEME_FAMILY,
    @Json(name = "confusable_pair") CONFUSABLE_PAIR
}

@JsonClass(generateAdapter = true)
data class CriterionFilter(
    val field: String,
    val operator: ComparisonOperator,
    val value: String
)

enum class ComparisonOperator {
    @Json(name = "eq") EQ,
    @Json(name = "gte") GTE,
    @Json(name = "lte") LTE,
    @Json(name = "in") IN
}

@JsonClass(generateAdapter = true)
data class CatalogLifecycle(
    val state: CatalogLifecycleState,
    val introducedVersion: String,
    val retiredVersion: String? = null,
    val replacementId: String? = null
)

enum class CatalogLifecycleState {
    @Json(name = "active") ACTIVE,
    @Json(name = "retired") RETIRED
}
