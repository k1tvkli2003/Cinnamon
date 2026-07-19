package com.cinnamon.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Immutable record of a meaningful product action.
 *
 * [idempotencyKey] is scoped to [actorId]. A replay may use a new transport or
 * request ID, but it must retain this key so it cannot grant rewards twice.
 */
@Entity(
    tableName = "gamification_events",
    indices = [
        Index(
            name = "idx_gamification_events_actor_idempotency",
            value = ["actorId", "idempotencyKey"],
            unique = true
        ),
        Index(
            name = "idx_gamification_events_actor_occurred_at",
            value = ["actorId", "occurredAtEpochMillis"]
        ),
        Index(
            name = "idx_gamification_events_actor_study_day",
            value = ["actorId", "studyDay"]
        ),
        Index(
            name = "idx_gamification_events_type_subject",
            value = ["eventType", "subjectType", "subjectId"]
        ),
        Index(
            name = "idx_gamification_events_replay_of",
            value = ["replayOfEventId"]
        )
    ]
)
data class GamificationEventEntity(
    @PrimaryKey val eventId: String,
    val actorId: String,
    val eventType: String,
    val subjectType: String,
    val subjectId: String,
    val occurredAtEpochMillis: Long,
    val recordedAtEpochMillis: Long,
    val studyDay: Long,
    val idempotencyKey: String,
    val source: String,
    val ruleVersion: Int,
    val metadataJson: String,
    val replayOfEventId: String?
)

/**
 * Append-only accounting entry. Positive amounts grant value; negative amounts
 * spend or reverse it. Never rewrite history to change a balance.
 */
@Entity(
    tableName = "reward_transactions",
    foreignKeys = [
        ForeignKey(
            entity = GamificationEventEntity::class,
            parentColumns = ["eventId"],
            childColumns = ["eventId"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(name = "idx_reward_transactions_event", value = ["eventId"]),
        Index(
            name = "idx_reward_transactions_actor_idempotency",
            value = ["actorId", "idempotencyKey"],
            unique = true
        ),
        Index(
            name = "idx_reward_transactions_event_rule_currency",
            value = ["eventId", "ruleId", "currency"],
            unique = true
        ),
        Index(
            name = "idx_reward_transactions_actor_currency_created_at",
            value = ["actorId", "currency", "createdAtEpochMillis"]
        )
    ]
)
data class RewardTransactionEntity(
    @PrimaryKey val transactionId: String,
    val eventId: String,
    val actorId: String,
    val transactionKind: String,
    val currency: String,
    val amount: Long,
    val ruleId: String,
    val ruleVersion: Int,
    val reasonCode: String,
    val createdAtEpochMillis: Long,
    val idempotencyKey: String,
    val metadataJson: String
)

/** Immutable UI-facing explanation of one evaluated event. */
@Entity(
    tableName = "reward_summaries",
    foreignKeys = [
        ForeignKey(
            entity = GamificationEventEntity::class,
            parentColumns = ["eventId"],
            childColumns = ["eventId"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            name = "idx_reward_summaries_actor_created_at",
            value = ["actorId", "createdAtEpochMillis"]
        )
    ]
)
data class RewardSummaryEntity(
    @PrimaryKey val eventId: String,
    val actorId: String,
    val ruleVersion: Int,
    val xpAwarded: Long,
    val currencyRewardsJson: String,
    val questProgressJson: String,
    val achievementIdsJson: String,
    val unlockedContentJson: String,
    val celebrationTier: String,
    val summaryJson: String,
    val createdAtEpochMillis: Long
)

/**
 * Materialized balance for fast reads. The immutable transaction ledger remains
 * authoritative and can rebuild this row.
 */
@Entity(
    tableName = "reward_balances",
    primaryKeys = ["actorId", "currency"],
    foreignKeys = [
        ForeignKey(
            entity = RewardTransactionEntity::class,
            parentColumns = ["transactionId"],
            childColumns = ["lastTransactionId"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(
            name = "idx_reward_balances_last_transaction",
            value = ["lastTransactionId"]
        ),
        Index(
            name = "idx_reward_balances_updated_at",
            value = ["updatedAtEpochMillis"]
        )
    ]
)
data class RewardBalanceEntity(
    val actorId: String,
    val currency: String,
    val balance: Long,
    val updatedAtEpochMillis: Long,
    val lastTransactionId: String?
)

/** A catalog definition materialized for a specific actor and time window. */
@Entity(
    tableName = "quest_instances",
    foreignKeys = [
        ForeignKey(
            entity = GamificationEventEntity::class,
            parentColumns = ["eventId"],
            childColumns = ["completionEventId"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(
            name = "idx_quest_instances_actor_definition_window",
            value = ["actorId", "definitionId", "startsAtEpochMillis"],
            unique = true
        ),
        Index(
            name = "idx_quest_instances_actor_state_ends_at",
            value = ["actorId", "state", "endsAtEpochMillis"]
        ),
        Index(
            name = "idx_quest_instances_completion_event",
            value = ["completionEventId"]
        )
    ]
)
data class QuestInstanceEntity(
    @PrimaryKey val questInstanceId: String,
    val actorId: String,
    val definitionId: String,
    val catalogVersion: Int,
    val cadence: String,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
    val state: String,
    val progress: Long,
    val target: Long,
    val criteriaJson: String,
    val rewardJson: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completionEventId: String?,
    val completedAtEpochMillis: Long?,
    val claimedAtEpochMillis: Long?
)

/**
 * Durable, versioned campaign progress for one learner. A journey is longer-lived than a daily
 * quest: it survives process death, app updates, and time-zone changes, and advances only from
 * immutable learning evidence committed in the same Room transaction.
 */
@Entity(
    tableName = "journey_instances",
    foreignKeys = [
        ForeignKey(
            entity = GamificationEventEntity::class,
            parentColumns = ["eventId"],
            childColumns = ["completionEventId"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(
            name = "idx_journey_instances_actor_definition_version",
            value = ["actorId", "definitionId", "definitionVersion"],
            unique = true
        ),
        Index(
            name = "idx_journey_instances_actor_state_updated_at",
            value = ["actorId", "state", "updatedAtEpochMillis"]
        ),
        Index(
            name = "idx_journey_instances_completion_event",
            value = ["completionEventId"]
        )
    ]
)
data class JourneyInstanceEntity(
    @PrimaryKey val journeyInstanceId: String,
    val actorId: String,
    val definitionId: String,
    val definitionVersion: Int,
    val state: String,
    val currentStageOrder: Int,
    val startedAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completionEventId: String?,
    val completedAtEpochMillis: Long?
)

/**
 * Materialized progress for one sequential journey stage. Rows are created together with their
 * journey, but only the active stage can absorb evidence. Completion is immutable because the
 * transition stores the event that earned it and can succeed only once.
 */
@Entity(
    tableName = "journey_stage_progress",
    foreignKeys = [
        ForeignKey(
            entity = JourneyInstanceEntity::class,
            parentColumns = ["journeyInstanceId"],
            childColumns = ["journeyInstanceId"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GamificationEventEntity::class,
            parentColumns = ["eventId"],
            childColumns = ["completionEventId"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(
            name = "idx_journey_stages_instance_definition",
            value = ["journeyInstanceId", "stageDefinitionId"],
            unique = true
        ),
        Index(
            name = "idx_journey_stages_instance_order_state",
            value = ["journeyInstanceId", "stageOrder", "state"]
        ),
        Index(
            name = "idx_journey_stages_actor_state_updated_at",
            value = ["actorId", "state", "updatedAtEpochMillis"]
        ),
        Index(
            name = "idx_journey_stages_completion_event",
            value = ["completionEventId"]
        )
    ]
)
data class JourneyStageProgressEntity(
    @PrimaryKey val stageProgressId: String,
    val journeyInstanceId: String,
    val actorId: String,
    val stageDefinitionId: String,
    val stageOrder: Int,
    val evidenceMetric: String,
    val state: String,
    val progress: Long,
    val target: Long,
    val rewardXp: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completionEventId: String?,
    val completedAtEpochMillis: Long?
)

/**
 * One immutable campaign route commitment per actor and authored campaign version. The choice is
 * an auditable event, but never a reward grant. Its selected journey definition owns all later
 * progress and value settlement.
 */
@Entity(
    tableName = "campaign_route_choices",
    foreignKeys = [
        ForeignKey(
            entity = GamificationEventEntity::class,
            parentColumns = ["eventId"],
            childColumns = ["sourceEventId"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(
            name = "idx_campaign_choices_actor_definition_version",
            value = ["actorId", "campaignDefinitionId", "campaignDefinitionVersion"],
            unique = true
        ),
        Index(name = "idx_campaign_choices_source_event", value = ["sourceEventId"]),
        Index(
            name = "idx_campaign_choices_actor_chosen_at",
            value = ["actorId", "chosenAtEpochMillis"]
        )
    ]
)
data class CampaignRouteChoiceEntity(
    @PrimaryKey val choiceId: String,
    val actorId: String,
    val campaignDefinitionId: String,
    val campaignDefinitionVersion: Int,
    val routeId: String,
    val journeyDefinitionId: String,
    val sourceEventId: String,
    val chosenAtEpochMillis: Long
)

/** One immutable unlock per actor, achievement, and catalog level. */
@Entity(
    tableName = "achievement_unlocks",
    foreignKeys = [
        ForeignKey(
            entity = GamificationEventEntity::class,
            parentColumns = ["eventId"],
            childColumns = ["sourceEventId"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(
            name = "idx_achievement_unlocks_actor_achievement_level",
            value = ["actorId", "achievementId", "level"],
            unique = true
        ),
        Index(
            name = "idx_achievement_unlocks_actor_unlocked_at",
            value = ["actorId", "unlockedAtEpochMillis"]
        ),
        Index(
            name = "idx_achievement_unlocks_source_event",
            value = ["sourceEventId"]
        )
    ]
)
data class AchievementUnlockEntity(
    @PrimaryKey val unlockId: String,
    val actorId: String,
    val achievementId: String,
    val level: Int,
    val catalogVersion: Int,
    val progressJson: String,
    val unlockedAtEpochMillis: Long,
    val sourceEventId: String?,
    val evidenceJson: String
)

/**
 * Durable queue item for a celebration or acknowledgement. It may present a
 * reward, but changing or skipping it can never grant or revoke value.
 */
@Entity(
    tableName = "reward_presentation_receipts",
    foreignKeys = [
        ForeignKey(
            entity = GamificationEventEntity::class,
            parentColumns = ["eventId"],
            childColumns = ["sourceEventId"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RewardTransactionEntity::class,
            parentColumns = ["transactionId"],
            childColumns = ["sourceTransactionId"],
            onUpdate = ForeignKey.NO_ACTION,
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(
            name = "idx_reward_receipts_actor_idempotency",
            value = ["actorId", "idempotencyKey"],
            unique = true
        ),
        Index(
            name = "idx_reward_receipts_actor_state_priority_created_at",
            value = ["actorId", "state", "priority", "createdAtEpochMillis"]
        ),
        Index(name = "idx_reward_receipts_source_event", value = ["sourceEventId"]),
        Index(
            name = "idx_reward_receipts_source_transaction",
            value = ["sourceTransactionId"]
        ),
        Index(name = "idx_reward_receipts_expires_at", value = ["expiresAtEpochMillis"])
    ]
)
data class RewardPresentationReceiptEntity(
    @PrimaryKey val receiptId: String,
    val actorId: String,
    val sourceEventId: String,
    val sourceTransactionId: String?,
    val presentationFamily: String,
    val idempotencyKey: String,
    val priority: Int,
    val tier: String,
    val state: String,
    val immutableSummaryJson: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val expiresAtEpochMillis: Long?,
    val acknowledgedAtEpochMillis: Long?,
    val suppressionReason: String?,
    val coalescedCount: Int
)
