package com.cinnamon.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Additive migrations only. Existing lexicon, learning, flashcard, and prefs data are untouched. */
object AppDatabaseMigrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `gamification_events` (
                    `eventId` TEXT NOT NULL,
                    `actorId` TEXT NOT NULL,
                    `eventType` TEXT NOT NULL,
                    `subjectType` TEXT NOT NULL,
                    `subjectId` TEXT NOT NULL,
                    `occurredAtEpochMillis` INTEGER NOT NULL,
                    `recordedAtEpochMillis` INTEGER NOT NULL,
                    `studyDay` INTEGER NOT NULL,
                    `idempotencyKey` TEXT NOT NULL,
                    `source` TEXT NOT NULL,
                    `ruleVersion` INTEGER NOT NULL,
                    `metadataJson` TEXT NOT NULL,
                    `replayOfEventId` TEXT,
                    PRIMARY KEY(`eventId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS `idx_gamification_events_actor_idempotency`
                ON `gamification_events` (`actorId`, `idempotencyKey`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_gamification_events_actor_occurred_at`
                ON `gamification_events` (`actorId`, `occurredAtEpochMillis`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_gamification_events_actor_study_day`
                ON `gamification_events` (`actorId`, `studyDay`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_gamification_events_type_subject`
                ON `gamification_events` (`eventType`, `subjectType`, `subjectId`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_gamification_events_replay_of`
                ON `gamification_events` (`replayOfEventId`)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reward_transactions` (
                    `transactionId` TEXT NOT NULL,
                    `eventId` TEXT NOT NULL,
                    `actorId` TEXT NOT NULL,
                    `transactionKind` TEXT NOT NULL,
                    `currency` TEXT NOT NULL,
                    `amount` INTEGER NOT NULL,
                    `ruleId` TEXT NOT NULL,
                    `ruleVersion` INTEGER NOT NULL,
                    `reasonCode` TEXT NOT NULL,
                    `createdAtEpochMillis` INTEGER NOT NULL,
                    `idempotencyKey` TEXT NOT NULL,
                    `metadataJson` TEXT NOT NULL,
                    PRIMARY KEY(`transactionId`),
                    FOREIGN KEY(`eventId`) REFERENCES `gamification_events`(`eventId`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_reward_transactions_event`
                ON `reward_transactions` (`eventId`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS `idx_reward_transactions_actor_idempotency`
                ON `reward_transactions` (`actorId`, `idempotencyKey`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS `idx_reward_transactions_event_rule_currency`
                ON `reward_transactions` (`eventId`, `ruleId`, `currency`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_reward_transactions_actor_currency_created_at`
                ON `reward_transactions` (`actorId`, `currency`, `createdAtEpochMillis`)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reward_summaries` (
                    `eventId` TEXT NOT NULL,
                    `actorId` TEXT NOT NULL,
                    `ruleVersion` INTEGER NOT NULL,
                    `xpAwarded` INTEGER NOT NULL,
                    `currencyRewardsJson` TEXT NOT NULL,
                    `questProgressJson` TEXT NOT NULL,
                    `achievementIdsJson` TEXT NOT NULL,
                    `unlockedContentJson` TEXT NOT NULL,
                    `celebrationTier` TEXT NOT NULL,
                    `summaryJson` TEXT NOT NULL,
                    `createdAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`eventId`),
                    FOREIGN KEY(`eventId`) REFERENCES `gamification_events`(`eventId`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_reward_summaries_actor_created_at`
                ON `reward_summaries` (`actorId`, `createdAtEpochMillis`)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reward_balances` (
                    `actorId` TEXT NOT NULL,
                    `currency` TEXT NOT NULL,
                    `balance` INTEGER NOT NULL,
                    `updatedAtEpochMillis` INTEGER NOT NULL,
                    `lastTransactionId` TEXT,
                    PRIMARY KEY(`actorId`, `currency`),
                    FOREIGN KEY(`lastTransactionId`) REFERENCES `reward_transactions`(`transactionId`)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_reward_balances_last_transaction`
                ON `reward_balances` (`lastTransactionId`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_reward_balances_updated_at`
                ON `reward_balances` (`updatedAtEpochMillis`)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `quest_instances` (
                    `questInstanceId` TEXT NOT NULL,
                    `actorId` TEXT NOT NULL,
                    `definitionId` TEXT NOT NULL,
                    `catalogVersion` INTEGER NOT NULL,
                    `cadence` TEXT NOT NULL,
                    `startsAtEpochMillis` INTEGER NOT NULL,
                    `endsAtEpochMillis` INTEGER NOT NULL,
                    `state` TEXT NOT NULL,
                    `progress` INTEGER NOT NULL,
                    `target` INTEGER NOT NULL,
                    `criteriaJson` TEXT NOT NULL,
                    `rewardJson` TEXT NOT NULL,
                    `createdAtEpochMillis` INTEGER NOT NULL,
                    `updatedAtEpochMillis` INTEGER NOT NULL,
                    `completionEventId` TEXT,
                    `completedAtEpochMillis` INTEGER,
                    `claimedAtEpochMillis` INTEGER,
                    PRIMARY KEY(`questInstanceId`),
                    FOREIGN KEY(`completionEventId`) REFERENCES `gamification_events`(`eventId`)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS `idx_quest_instances_actor_definition_window`
                ON `quest_instances` (`actorId`, `definitionId`, `startsAtEpochMillis`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_quest_instances_actor_state_ends_at`
                ON `quest_instances` (`actorId`, `state`, `endsAtEpochMillis`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_quest_instances_completion_event`
                ON `quest_instances` (`completionEventId`)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `achievement_unlocks` (
                    `unlockId` TEXT NOT NULL,
                    `actorId` TEXT NOT NULL,
                    `achievementId` TEXT NOT NULL,
                    `level` INTEGER NOT NULL,
                    `catalogVersion` INTEGER NOT NULL,
                    `progressJson` TEXT NOT NULL,
                    `unlockedAtEpochMillis` INTEGER NOT NULL,
                    `sourceEventId` TEXT,
                    `evidenceJson` TEXT NOT NULL,
                    PRIMARY KEY(`unlockId`),
                    FOREIGN KEY(`sourceEventId`) REFERENCES `gamification_events`(`eventId`)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS `idx_achievement_unlocks_actor_achievement_level`
                ON `achievement_unlocks` (`actorId`, `achievementId`, `level`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_achievement_unlocks_actor_unlocked_at`
                ON `achievement_unlocks` (`actorId`, `unlockedAtEpochMillis`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_achievement_unlocks_source_event`
                ON `achievement_unlocks` (`sourceEventId`)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reward_presentation_receipts` (
                    `receiptId` TEXT NOT NULL,
                    `actorId` TEXT NOT NULL,
                    `sourceEventId` TEXT NOT NULL,
                    `sourceTransactionId` TEXT,
                    `presentationFamily` TEXT NOT NULL,
                    `idempotencyKey` TEXT NOT NULL,
                    `priority` INTEGER NOT NULL,
                    `tier` TEXT NOT NULL,
                    `state` TEXT NOT NULL,
                    `immutableSummaryJson` TEXT NOT NULL,
                    `createdAtEpochMillis` INTEGER NOT NULL,
                    `updatedAtEpochMillis` INTEGER NOT NULL,
                    `expiresAtEpochMillis` INTEGER,
                    `acknowledgedAtEpochMillis` INTEGER,
                    `suppressionReason` TEXT,
                    `coalescedCount` INTEGER NOT NULL,
                    PRIMARY KEY(`receiptId`),
                    FOREIGN KEY(`sourceEventId`) REFERENCES `gamification_events`(`eventId`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`sourceTransactionId`) REFERENCES `reward_transactions`(`transactionId`)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS `idx_reward_receipts_actor_idempotency`
                ON `reward_presentation_receipts` (`actorId`, `idempotencyKey`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_reward_receipts_actor_state_priority_created_at`
                ON `reward_presentation_receipts` (`actorId`, `state`, `priority`, `createdAtEpochMillis`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_reward_receipts_source_event`
                ON `reward_presentation_receipts` (`sourceEventId`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_reward_receipts_source_transaction`
                ON `reward_presentation_receipts` (`sourceTransactionId`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_reward_receipts_expires_at`
                ON `reward_presentation_receipts` (`expiresAtEpochMillis`)
                """.trimIndent()
            )
        }
    }

    /** Adds the first durable multi-session learning journey without rewriting existing data. */
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `journey_instances` (
                    `journeyInstanceId` TEXT NOT NULL,
                    `actorId` TEXT NOT NULL,
                    `definitionId` TEXT NOT NULL,
                    `definitionVersion` INTEGER NOT NULL,
                    `state` TEXT NOT NULL,
                    `currentStageOrder` INTEGER NOT NULL,
                    `startedAtEpochMillis` INTEGER NOT NULL,
                    `updatedAtEpochMillis` INTEGER NOT NULL,
                    `completionEventId` TEXT,
                    `completedAtEpochMillis` INTEGER,
                    PRIMARY KEY(`journeyInstanceId`),
                    FOREIGN KEY(`completionEventId`) REFERENCES `gamification_events`(`eventId`)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS `idx_journey_instances_actor_definition_version`
                ON `journey_instances` (`actorId`, `definitionId`, `definitionVersion`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_journey_instances_actor_state_updated_at`
                ON `journey_instances` (`actorId`, `state`, `updatedAtEpochMillis`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_journey_instances_completion_event`
                ON `journey_instances` (`completionEventId`)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `journey_stage_progress` (
                    `stageProgressId` TEXT NOT NULL,
                    `journeyInstanceId` TEXT NOT NULL,
                    `actorId` TEXT NOT NULL,
                    `stageDefinitionId` TEXT NOT NULL,
                    `stageOrder` INTEGER NOT NULL,
                    `evidenceMetric` TEXT NOT NULL,
                    `state` TEXT NOT NULL,
                    `progress` INTEGER NOT NULL,
                    `target` INTEGER NOT NULL,
                    `rewardXp` INTEGER NOT NULL,
                    `createdAtEpochMillis` INTEGER NOT NULL,
                    `updatedAtEpochMillis` INTEGER NOT NULL,
                    `completionEventId` TEXT,
                    `completedAtEpochMillis` INTEGER,
                    PRIMARY KEY(`stageProgressId`),
                    FOREIGN KEY(`journeyInstanceId`) REFERENCES `journey_instances`(`journeyInstanceId`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`completionEventId`) REFERENCES `gamification_events`(`eventId`)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS `idx_journey_stages_instance_definition`
                ON `journey_stage_progress` (`journeyInstanceId`, `stageDefinitionId`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_journey_stages_instance_order_state`
                ON `journey_stage_progress` (`journeyInstanceId`, `stageOrder`, `state`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_journey_stages_actor_state_updated_at`
                ON `journey_stage_progress` (`actorId`, `state`, `updatedAtEpochMillis`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_journey_stages_completion_event`
                ON `journey_stage_progress` (`completionEventId`)
                """.trimIndent()
            )
        }
    }

    /** Adds one immutable route-choice contract while preserving every v3 journey row. */
    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `campaign_route_choices` (
                    `choiceId` TEXT NOT NULL,
                    `actorId` TEXT NOT NULL,
                    `campaignDefinitionId` TEXT NOT NULL,
                    `campaignDefinitionVersion` INTEGER NOT NULL,
                    `routeId` TEXT NOT NULL,
                    `journeyDefinitionId` TEXT NOT NULL,
                    `sourceEventId` TEXT NOT NULL,
                    `chosenAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`choiceId`),
                    FOREIGN KEY(`sourceEventId`) REFERENCES `gamification_events`(`eventId`)
                        ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS `idx_campaign_choices_actor_definition_version`
                ON `campaign_route_choices` (`actorId`, `campaignDefinitionId`, `campaignDefinitionVersion`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_campaign_choices_source_event`
                ON `campaign_route_choices` (`sourceEventId`)
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `idx_campaign_choices_actor_chosen_at`
                ON `campaign_route_choices` (`actorId`, `chosenAtEpochMillis`)
                """.trimIndent()
            )
        }
    }
}
