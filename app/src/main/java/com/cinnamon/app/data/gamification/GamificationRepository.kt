package com.cinnamon.app.data.gamification

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.cinnamon.app.data.local.AppDatabase
import com.cinnamon.app.data.local.AchievementUnlockEntity
import com.cinnamon.app.data.local.CampaignRouteChoiceEntity
import com.cinnamon.app.data.local.DailyRewardCapExceededException
import com.cinnamon.app.data.local.GamificationEventEntity
import com.cinnamon.app.data.local.LearningActivityRow
import com.cinnamon.app.data.local.JourneyInstanceEntity
import com.cinnamon.app.data.local.JourneyStageProgressEntity
import com.cinnamon.app.data.local.JourneySettlementRequest
import com.cinnamon.app.data.local.QuestInstanceEntity
import com.cinnamon.app.data.local.RewardPresentationReceiptEntity
import com.cinnamon.app.data.local.RewardSummaryEntity
import com.cinnamon.app.data.local.RewardTransactionEntity
import com.cinnamon.app.data.local.RewardWriteResult
import com.cinnamon.app.data.local.RewardWriteStatus
import com.cinnamon.app.data.local.StudyDayCurrencyTotal
import com.cinnamon.app.data.prefs.ProgressSnapshot
import com.cinnamon.app.data.prefs.ProgressStore
import com.cinnamon.app.domain.gamification.ClaimBehavior
import com.cinnamon.app.domain.gamification.FoundationCampaignCatalog
import com.cinnamon.app.domain.gamification.GamificationCatalogBundle
import com.cinnamon.app.domain.gamification.PresentationTier
import com.cinnamon.app.domain.gamification.RewardType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

enum class LearningEventSource(val wireName: String) {
    REVIEW("review"),
    PRACTICE("practice"),
    GAME("game"),
    IMPORT("import")
}

/** A non-editable account of a completed learning action, derived from Room. */
data class LearningActivity(
    val eventId: String,
    val eventType: String,
    val subjectType: String,
    val subjectId: String,
    val occurredAtEpochMillis: Long,
    val xpAwarded: Long,
    val celebrationTier: String
)

/** A pending visual acknowledgement; it cannot create, alter, or re-grant XP. */
data class PendingRewardPresentation(
    val receiptId: String,
    val presentationFamily: String,
    val xpAwarded: Long,
    val tier: String
)

data class LearningEventCommand(
    val eventType: RewardableEventType,
    val subjectType: String,
    val subjectId: String,
    val occurrenceKey: String,
    val completedItemCount: Int,
    val source: LearningEventSource,
    val correctedAfterMistake: Boolean = false,
    val occurredAtEpochMillis: Long = System.currentTimeMillis()
) {
    init {
        require(subjectType.isNotBlank()) { "subjectType must not be blank" }
        require(subjectId.isNotBlank()) { "subjectId must not be blank" }
        require(occurrenceKey.isNotBlank()) { "occurrenceKey must not be blank" }
        require(completedItemCount >= 0) { "completedItemCount must not be negative" }
        require(occurredAtEpochMillis >= 0L) { "occurredAtEpochMillis must not be negative" }
    }
}

/**
 * The only grant authority used by Android feature code. UI surfaces describe a
 * semantic action; this repository derives uniqueness, first-completion and cap
 * facts from Room before committing the immutable decision.
 */
class GamificationRepository private constructor(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.gamificationDao()
    private val lexiconDao = database.lexiconDao()
    private val catalogRepository = GamificationCatalogRepository.getInstance(context)

    val xpBalance: Flow<Long> = dao.observeRewardBalance(LOCAL_ACTOR_ID, RewardCurrencies.XP)
        .map { balance -> balance?.balance ?: 0L }

    fun xpForStudyDay(studyDay: Long): Flow<Long> = dao.observeCurrencyForStudyDay(
        actorId = LOCAL_ACTOR_ID,
        currency = RewardCurrencies.XP,
        studyDay = studyDay
    )

    fun xpTotalsSinceStudyDay(studyDay: Long): Flow<List<StudyDayCurrencyTotal>> =
        dao.observeCurrencyTotalsSinceStudyDay(
            actorId = LOCAL_ACTOR_ID,
            currency = RewardCurrencies.XP,
            fromStudyDay = studyDay
        )

    fun completedReviewCountForStudyDay(studyDay: Long): Flow<Int> =
        dao.observeCompletedReviewCountForStudyDay(LOCAL_ACTOR_ID, studyDay)

    /** Catalog due-review quests count unique reviewed subjects, never repeat events. */
    fun distinctReviewedSubjectCountForStudyDay(studyDay: Long): Flow<Int> =
        dao.observeDistinctReviewedSubjectCountForStudyDay(LOCAL_ACTOR_ID, studyDay)

    val completedReviewCount: Flow<Int> = dao.observeEventCount(
        actorId = LOCAL_ACTOR_ID,
        eventType = RewardableEventType.REVIEW_COMPLETED.wireName
    )

    /** Distinct terms whose delayed review schedule has reached real mastery evidence. */
    val masteredConceptCount: Flow<Int> = dao.observeDistinctSubjectIdCount(
        actorId = LOCAL_ACTOR_ID,
        eventType = RewardableEventType.CONCEPT_MASTERED.wireName
    )

    fun completedPracticeSessionCountForStudyDay(studyDay: Long): Flow<Int> =
        dao.observeEventCountForStudyDay(
            actorId = LOCAL_ACTOR_ID,
            eventType = RewardableEventType.PRACTICE_SESSION_COMPLETED.wireName,
            studyDay = studyDay
        )

    val distinctPracticeContentTypeCount: Flow<Int> = dao.observeDistinctSubjectTypeCount(
        actorId = LOCAL_ACTOR_ID,
        eventType = RewardableEventType.PRACTICE_SESSION_COMPLETED.wireName
    )

    fun distinctPracticeContentTypeCountForStudyDay(studyDay: Long): Flow<Int> =
        dao.observeDistinctSubjectTypeCountForStudyDay(
            actorId = LOCAL_ACTOR_ID,
            eventType = RewardableEventType.PRACTICE_SESSION_COMPLETED.wireName,
            studyDay = studyDay
        )

    val activeStudyDayCount: Flow<Int> = dao.observeActiveStudyDayCount(LOCAL_ACTOR_ID)

    val recentLearningActivity: Flow<List<LearningActivity>> = dao
        .observeRecentLearningActivity(actorId = LOCAL_ACTOR_ID, limit = 24)
        .map { rows -> rows.map(LearningActivityRow::toLearningActivity) }

    val pendingRewardPresentations: Flow<List<PendingRewardPresentation>> = dao
        .observePendingPresentationReceipts(LOCAL_ACTOR_ID, System.currentTimeMillis())
        .map { receipts -> receipts.map(RewardPresentationReceiptEntity::toPendingPresentation) }

    suspend fun acknowledgeRewardPresentation(receiptId: String): Boolean =
        dao.transitionPresentationReceipt(
            receiptId = receiptId,
            allowedCurrentStates = listOf(
                PresentationReceiptState.PENDING.wireName,
                PresentationReceiptState.READY.wireName,
                PresentationReceiptState.INTERRUPTED.wireName
            ),
            newState = PresentationReceiptState.ACKNOWLEDGED.wireName,
            updatedAtEpochMillis = System.currentTimeMillis(),
            acknowledge = true,
            suppressionReason = null
        ) == 1

    val recentStudyDays: Flow<List<Long>> = dao.observeRecentStudyDays(
        actorId = LOCAL_ACTOR_ID,
        limit = 400
    )

    val activeQuestInstances: Flow<List<QuestInstanceEntity>> =
        dao.observeActiveQuestInstances(LOCAL_ACTOR_ID)

    val journeyInstances: Flow<List<JourneyInstanceEntity>> =
        dao.observeJourneyInstances(LOCAL_ACTOR_ID)

    val journeyStages: Flow<List<JourneyStageProgressEntity>> =
        dao.observeJourneyStages(LOCAL_ACTOR_ID)

    val campaignRouteChoices: Flow<List<CampaignRouteChoiceEntity>> =
        dao.observeCampaignRouteChoices(LOCAL_ACTOR_ID)

    val achievementUnlocks: Flow<List<AchievementUnlockEntity>> =
        dao.observeAchievementUnlocks(LOCAL_ACTOR_ID)

    suspend fun record(command: LearningEventCommand): RewardWriteResult {
        val studyDay = ProgressStore.localEpochDay(command.occurredAtEpochMillis)
        val eventIdempotencyKey = StableRewardIds.eventIdempotencyKey(
            actorId = LOCAL_ACTOR_ID,
            eventType = command.eventType,
            subjectType = command.subjectType,
            subjectId = command.subjectId,
            domainOccurrenceKey = command.occurrenceKey
        )
        dao.eventByIdempotencyKey(LOCAL_ACTOR_ID, eventIdempotencyKey)?.let { existing ->
            return RewardWriteResult(
                status = com.cinnamon.app.data.local.RewardWriteStatus.DUPLICATE,
                canonicalEventId = existing.eventId,
                summary = dao.rewardSummaryForEvent(existing.eventId)
            )
        }

        var repeatSubjectDetected = false
        var capRaceDetected = false
        repeat(3) {
            val firstCompletion = !dao.hasEventForSubject(
                actorId = LOCAL_ACTOR_ID,
                eventType = command.eventType.wireName,
                subjectType = command.subjectType,
                subjectId = command.subjectId
            )
            val uniqueSubjectToday = !repeatSubjectDetected && !dao.hasEventForSubjectOnStudyDay(
                actorId = LOCAL_ACTOR_ID,
                eventType = command.eventType.wireName,
                subjectType = command.subjectType,
                subjectId = command.subjectId,
                studyDay = studyDay
            )
            val awardedToday = if (capRaceDetected) {
                RewardRuleEngine.XP_PER_REWARD_WINDOW_CAP
            } else {
                dao.cappedCurrencyForStudyDayOnce(LOCAL_ACTOR_ID, RewardCurrencies.XP, studyDay)
            }
            val rewardWindowId = when (command.eventType) {
                RewardableEventType.CONCEPT_MASTERED -> "lifetime"
                else -> "study_day:$studyDay"
            }
            val decision = RewardRuleEngine.evaluate(
                RewardEvaluationInput(
                    eventId = eventIdempotencyKey,
                    actorId = LOCAL_ACTOR_ID,
                    eventIdempotencyKey = eventIdempotencyKey,
                    eventType = command.eventType,
                    subjectType = command.subjectType,
                    subjectId = command.subjectId,
                    occurredAtEpochMillis = command.occurredAtEpochMillis,
                    rewardWindowId = rewardWindowId,
                    xpAlreadyAwardedInWindow = awardedToday,
                    meaningful = command.isMeaningful(),
                    firstCompletion = firstCompletion,
                    uniqueSubjectInWindow = uniqueSubjectToday,
                    correctedAfterMistake = command.correctedAfterMistake,
                    completedItemCount = command.completedItemCount
                )
            )

            try {
                val event = command.toEventEntity(eventIdempotencyKey, studyDay)
                val settlement = CatalogSettlementPlanner.plan(
                    bundle = catalogRepository.bundle,
                    event = event,
                    dueItemCountAtAssignment = lexiconDao.dueCountOnce(command.occurredAtEpochMillis)
                )
                val selectedCampaignJourney = selectedCampaignJourneySettlement(event)
                return dao.recordRewardAtomically(
                    event = event,
                    transactions = decision.toTransactionEntities(command.occurredAtEpochMillis),
                    summary = decision.toSummaryEntity(command.occurredAtEpochMillis),
                    presentationReceipts = decision.toReceiptEntities(command.occurredAtEpochMillis),
                    catalogSettlement = settlement,
                    journeySettlement = JourneySettlementPlanner.plan(event),
                    additionalJourneySettlements = listOfNotNull(selectedCampaignJourney)
                )
            } catch (_: SQLiteConstraintException) {
                // A concurrent attempt claimed the same subject/window reward.
                // Keep the learning event, but re-evaluate it as a zero-value repeat.
                repeatSubjectDetected = true
            } catch (_: DailyRewardCapExceededException) {
                // A concurrent event filled the daily cap after our read.
                capRaceDetected = true
            }
        }
        error("Could not settle a reward decision after concurrent retries")
    }

    /**
     * Replays current immutable evidence through the active catalog once per local day. This is a
     * migration/reconciliation event, not fabricated learning activity: every unlock still cites
     * counts derived inside the same Room transaction.
     */
    suspend fun reconcileCatalog(
        bundle: GamificationCatalogBundle,
        occurredAtEpochMillis: Long = System.currentTimeMillis()
    ): RewardWriteResult {
        val studyDay = ProgressStore.localEpochDay(occurredAtEpochMillis)
        val eventId = StableRewardIds.eventIdempotencyKey(
            actorId = LOCAL_ACTOR_ID,
            eventType = RewardableEventType.OTHER,
            subjectType = "catalog_reconciliation",
            subjectId = bundle.catalog.catalogVersion,
            domainOccurrenceKey = "catalog_reconcile_v1:$studyDay"
        )
        replayResultIfAlreadyApplied(eventId)?.let { return it }
        val event = GamificationEventEntity(
            eventId = eventId,
            actorId = LOCAL_ACTOR_ID,
            eventType = "catalog_reconciled",
            subjectType = "gamification_catalog",
            subjectId = bundle.catalog.catalogVersion,
            occurredAtEpochMillis = occurredAtEpochMillis,
            recordedAtEpochMillis = System.currentTimeMillis(),
            studyDay = studyDay,
            idempotencyKey = eventId,
            source = "startup",
            ruleVersion = GamificationVersions.CURRENT_RULE_VERSION,
            metadataJson = "{\"catalogStorageVersion\":${bundle.catalog.storageVersion}}",
            replayOfEventId = null
        )
        return dao.recordRewardAtomically(
            event = event,
            transactions = emptyList(),
            summary = RewardSummaryEntity(
                eventId = eventId,
                actorId = LOCAL_ACTOR_ID,
                ruleVersion = GamificationVersions.CURRENT_RULE_VERSION,
                xpAwarded = 0L,
                currencyRewardsJson = "[]",
                questProgressJson = "[]",
                achievementIdsJson = "[]",
                unlockedContentJson = "[]",
                celebrationTier = CelebrationTier.NONE.wireName,
                summaryJson = "{\"reasonCode\":\"catalog_reconciliation\",\"xpAwarded\":0}",
                createdAtEpochMillis = occurredAtEpochMillis
            ),
            presentationReceipts = emptyList(),
            catalogSettlement = CatalogSettlementPlanner.plan(
                bundle = bundle,
                event = event,
                dueItemCountAtAssignment = lexiconDao.dueCountOnce(occurredAtEpochMillis)
            )
        )
    }

    /**
     * Bootstraps or advances one versioned journey stage from immutable evidence. It deliberately
     * uses its own idempotency namespace: an already-reconciled catalog from an older app version
     * must not prevent the newly introduced journey tables from being initialized.
     */
    suspend fun reconcileJourney(
        occurredAtEpochMillis: Long = System.currentTimeMillis()
    ): RewardWriteResult {
        val definition = com.cinnamon.app.domain.gamification.FoundationJourneyCatalog.definition
        val studyDay = ProgressStore.localEpochDay(occurredAtEpochMillis)
        val eventId = StableRewardIds.eventIdempotencyKey(
            actorId = LOCAL_ACTOR_ID,
            eventType = RewardableEventType.OTHER,
            subjectType = "journey_reconciliation",
            subjectId = definition.id,
            domainOccurrenceKey = "journey_reconcile_v${definition.version}:$studyDay"
        )
        replayResultIfAlreadyApplied(eventId)?.let { return it }
        val event = GamificationEventEntity(
            eventId = eventId,
            actorId = LOCAL_ACTOR_ID,
            eventType = "journey_reconciled",
            subjectType = "journey_definition",
            subjectId = definition.id,
            occurredAtEpochMillis = occurredAtEpochMillis,
            recordedAtEpochMillis = System.currentTimeMillis(),
            studyDay = studyDay,
            idempotencyKey = eventId,
            source = "startup",
            ruleVersion = GamificationVersions.CURRENT_RULE_VERSION,
            metadataJson = "{\"definitionVersion\":${definition.version}}",
            replayOfEventId = null
        )
        return dao.recordRewardAtomically(
            event = event,
            transactions = emptyList(),
            summary = RewardSummaryEntity(
                eventId = eventId,
                actorId = LOCAL_ACTOR_ID,
                ruleVersion = GamificationVersions.CURRENT_RULE_VERSION,
                xpAwarded = 0L,
                currencyRewardsJson = "[]",
                questProgressJson = "[]",
                achievementIdsJson = "[]",
                unlockedContentJson = "[]",
                celebrationTier = CelebrationTier.NONE.wireName,
                summaryJson = "{\"reasonCode\":\"journey_reconciliation\",\"xpAwarded\":0}",
                createdAtEpochMillis = occurredAtEpochMillis
            ),
            presentationReceipts = emptyList(),
            journeySettlement = JourneySettlementPlanner.plan(event, definition)
        )
    }

    /**
     * Persists one irrevocable campaign route and initializes its versioned Journey in the same
     * Room transaction. The choice event grants no XP; only already-persisted evidence may move
     * the first route chapter.
     */
    suspend fun chooseCampaignRoute(
        routeId: String,
        occurredAtEpochMillis: Long = System.currentTimeMillis()
    ): RewardWriteResult {
        val definition = FoundationCampaignCatalog.definition
        val route = requireNotNull(definition.route(routeId)) { "Unknown campaign route: $routeId" }
        val studyDay = ProgressStore.localEpochDay(occurredAtEpochMillis)
        val eventId = StableRewardIds.eventIdempotencyKey(
            actorId = LOCAL_ACTOR_ID,
            eventType = RewardableEventType.OTHER,
            subjectType = "campaign_route_choice",
            subjectId = definition.id,
            domainOccurrenceKey = "campaign_v${definition.version}:${route.id}"
        )
        replayResultIfAlreadyApplied(eventId)?.let { return it }
        val event = GamificationEventEntity(
            eventId = eventId,
            actorId = LOCAL_ACTOR_ID,
            eventType = "campaign_route_selected",
            subjectType = "campaign_definition",
            subjectId = definition.id,
            occurredAtEpochMillis = occurredAtEpochMillis,
            recordedAtEpochMillis = System.currentTimeMillis(),
            studyDay = studyDay,
            idempotencyKey = eventId,
            source = "campaign",
            ruleVersion = GamificationVersions.CURRENT_RULE_VERSION,
            metadataJson = "{\"routeId\":\"${route.id}\",\"journeyDefinitionId\":\"${route.journey.id}\"}",
            replayOfEventId = null
        )
        return dao.recordRewardAtomically(
            event = event,
            transactions = emptyList(),
            summary = RewardSummaryEntity(
                eventId = eventId,
                actorId = LOCAL_ACTOR_ID,
                ruleVersion = GamificationVersions.CURRENT_RULE_VERSION,
                xpAwarded = 0L,
                currencyRewardsJson = "[]",
                questProgressJson = "[]",
                achievementIdsJson = "[]",
                unlockedContentJson = "[]",
                celebrationTier = CelebrationTier.NONE.wireName,
                summaryJson = "{\"reasonCode\":\"campaign_route_selected\",\"xpAwarded\":0}",
                createdAtEpochMillis = occurredAtEpochMillis
            ),
            presentationReceipts = emptyList(),
            campaignRouteChoice = CampaignSettlementPlanner.planChoice(
                event = event,
                routeId = route.id,
                definition = definition
            )
        )
    }

    /** One idempotent manual claim for a completed catalog quest. */
    suspend fun claimQuest(
        questInstanceId: String,
        occurredAtEpochMillis: Long = System.currentTimeMillis()
    ): RewardWriteResult {
        require(questInstanceId.isNotBlank()) { "questInstanceId must not be blank" }
        val instance = checkNotNull(dao.questInstanceById(questInstanceId)) {
            "Quest instance does not exist"
        }
        require(instance.actorId == LOCAL_ACTOR_ID) { "Quest belongs to another actor" }
        val bundle = catalogRepository.bundle
        val definition = checkNotNull(bundle.catalog.quests.firstOrNull { it.id == instance.definitionId }) {
            "Quest definition is no longer present in the validated catalog"
        }
        require(definition.claimBehavior == ClaimBehavior.MANUAL_ONCE) {
            "Quest is not manually claimable"
        }
        val rewardsById = bundle.catalog.rewards.associateBy { it.id }
        val rewards = definition.rewardRefs.map { rewardId ->
            checkNotNull(rewardsById[rewardId]) { "Quest reward $rewardId is missing" }
        }
        require(rewards.all { reward -> reward.type == RewardType.XP }) {
            "Quest claim contains a reward type without a persistence contract"
        }
        val xp = rewards.sumOf { reward -> reward.amount?.toLong() ?: 0L }
        require(xp > 0L) { "Quest claim must settle positive value" }
        val studyDay = ProgressStore.localEpochDay(occurredAtEpochMillis)
        val eventId = StableRewardIds.eventIdempotencyKey(
            actorId = LOCAL_ACTOR_ID,
            eventType = RewardableEventType.OTHER,
            subjectType = "quest_claim",
            subjectId = questInstanceId,
            domainOccurrenceKey = "manual_claim_v1"
        )
        val ruleId = "catalog.quest.${definition.id}.claim"
        val transactionId = StableRewardIds.transactionId(eventId, ruleId, RewardCurrencies.XP)
        val presentation = checkNotNull(
            bundle.catalog.presentations.firstOrNull { it.id == definition.presentationId }
        ) { "Quest presentation is missing" }
        val receiptId = StableRewardIds.catalogPresentationReceiptId(
            eventId = eventId,
            catalogItemId = definition.id,
            level = 1,
            presentationFamily = definition.presentationId
        )
        val event = GamificationEventEntity(
            eventId = eventId,
            actorId = LOCAL_ACTOR_ID,
            eventType = "quest_reward_claimed",
            subjectType = "quest_instance",
            subjectId = questInstanceId,
            occurredAtEpochMillis = occurredAtEpochMillis,
            recordedAtEpochMillis = System.currentTimeMillis(),
            studyDay = studyDay,
            idempotencyKey = eventId,
            source = "questboard",
            ruleVersion = GamificationVersions.CURRENT_RULE_VERSION,
            metadataJson = "{\"definitionId\":\"${definition.id}\"}",
            replayOfEventId = null
        )
        val transaction = RewardTransactionEntity(
            transactionId = transactionId,
            eventId = eventId,
            actorId = LOCAL_ACTOR_ID,
            transactionKind = RewardTransactionKind.GRANT.wireName,
            currency = RewardCurrencies.XP,
            amount = xp,
            ruleId = ruleId,
            ruleVersion = GamificationVersions.CURRENT_RULE_VERSION,
            reasonCode = "quest_claimed",
            createdAtEpochMillis = occurredAtEpochMillis,
            idempotencyKey = transactionId,
            metadataJson = "{\"questInstanceId\":\"$questInstanceId\"}"
        )
        return dao.recordRewardAtomically(
            event = event,
            transactions = listOf(transaction),
            summary = RewardSummaryEntity(
                eventId = eventId,
                actorId = LOCAL_ACTOR_ID,
                ruleVersion = GamificationVersions.CURRENT_RULE_VERSION,
                xpAwarded = xp,
                currencyRewardsJson = "[{\"currency\":\"xp\",\"amount\":$xp}]",
                questProgressJson = "[{\"questInstanceId\":\"$questInstanceId\",\"claimed\":true}]",
                achievementIdsJson = "[]",
                unlockedContentJson = "[]",
                celebrationTier = presentation.tier.name.lowercase(Locale.ROOT),
                summaryJson = "{\"reasonCode\":\"quest_claimed\",\"xpAwarded\":$xp}",
                createdAtEpochMillis = occurredAtEpochMillis
            ),
            presentationReceipts = listOf(
                RewardPresentationReceiptEntity(
                    receiptId = receiptId,
                    actorId = LOCAL_ACTOR_ID,
                    sourceEventId = eventId,
                    sourceTransactionId = transactionId,
                    presentationFamily = definition.presentationId,
                    idempotencyKey = receiptId,
                    priority = presentation.tier.priority(),
                    tier = presentation.tier.name.lowercase(Locale.ROOT),
                    state = PresentationReceiptState.PENDING.wireName,
                    immutableSummaryJson = "{\"questInstanceId\":\"$questInstanceId\",\"xpAwarded\":$xp}",
                    createdAtEpochMillis = occurredAtEpochMillis,
                    updatedAtEpochMillis = occurredAtEpochMillis,
                    expiresAtEpochMillis = Math.addExact(
                        occurredAtEpochMillis,
                        Math.multiplyExact(presentation.expiresAfterSeconds.toLong(), 1_000L)
                    ),
                    acknowledgedAtEpochMillis = null,
                    suppressionReason = null,
                    coalescedCount = 1
                )
            ),
            questClaimInstanceId = questInstanceId
        )
    }

    suspend fun importLegacyProgress(snapshot: ProgressSnapshot): RewardWriteResult {
        val eventId = legacyProgressEventId()
        replayResultIfAlreadyApplied(eventId)?.let { return it }
        val now = System.currentTimeMillis()
        val xp = snapshot.xp.coerceAtLeast(0).toLong()
        val transactionId = StableRewardIds.transactionId(
            eventId = eventId,
            ruleId = LEGACY_IMPORT_RULE_ID,
            currency = RewardCurrencies.XP
        )
        val transactions = if (xp > 0L) {
            listOf(
                RewardTransactionEntity(
                    transactionId = transactionId,
                    eventId = eventId,
                    actorId = LOCAL_ACTOR_ID,
                    transactionKind = RewardTransactionKind.GRANT.wireName,
                    currency = RewardCurrencies.XP,
                    amount = xp,
                    ruleId = LEGACY_IMPORT_RULE_ID,
                    ruleVersion = GamificationVersions.CURRENT_RULE_VERSION,
                    reasonCode = "legacy_opening_balance",
                    createdAtEpochMillis = now,
                    idempotencyKey = transactionId,
                    metadataJson = "{}"
                )
            )
        } else {
            emptyList()
        }
        return dao.recordRewardAtomically(
            event = GamificationEventEntity(
                eventId = eventId,
                actorId = LOCAL_ACTOR_ID,
                eventType = "legacy_progress_imported",
                subjectType = "legacy_progress",
                subjectId = "datastore_v1",
                occurredAtEpochMillis = now,
                recordedAtEpochMillis = now,
                studyDay = 0L,
                idempotencyKey = eventId,
                source = LearningEventSource.IMPORT.wireName,
                ruleVersion = GamificationVersions.CURRENT_RULE_VERSION,
                metadataJson = "{\"legacyStreak\":${snapshot.streak},\"legacyReviewedToday\":${snapshot.reviewedToday}}",
                replayOfEventId = null
            ),
            transactions = transactions,
            summary = RewardSummaryEntity(
                eventId = eventId,
                actorId = LOCAL_ACTOR_ID,
                ruleVersion = GamificationVersions.CURRENT_RULE_VERSION,
                xpAwarded = 0L,
                currencyRewardsJson = if (xp > 0L) "[{\"currency\":\"xp\",\"amount\":$xp}]" else "[]",
                questProgressJson = "[]",
                achievementIdsJson = "[]",
                unlockedContentJson = "[]",
                celebrationTier = CelebrationTier.NONE.wireName,
                summaryJson = "{\"reasonCode\":\"legacy_opening_balance\"}",
                createdAtEpochMillis = now
            ),
            presentationReceipts = emptyList()
        )
    }

    /** Avoids opening DataStore after the one-time migration event is durable. */
    suspend fun isLegacyProgressImported(): Boolean = dao.eventById(legacyProgressEventId()) != null

    private suspend fun replayResultIfAlreadyApplied(eventId: String): RewardWriteResult? {
        val existing = dao.eventById(eventId) ?: return null
        check(existing.actorId == LOCAL_ACTOR_ID && existing.idempotencyKey == eventId) {
            "eventId collision: $eventId is not the expected local idempotency event"
        }
        return RewardWriteResult(
            status = RewardWriteStatus.DUPLICATE,
            canonicalEventId = existing.eventId,
            summary = dao.rewardSummaryForEvent(existing.eventId)
        )
    }

    private suspend fun selectedCampaignJourneySettlement(
        event: GamificationEventEntity
    ): JourneySettlementRequest? {
        val definition = FoundationCampaignCatalog.definition
        val choice = dao.campaignRouteChoiceOnce(
            actorId = LOCAL_ACTOR_ID,
            campaignDefinitionId = definition.id,
            campaignDefinitionVersion = definition.version
        ) ?: return null
        val route = checkNotNull(definition.route(choice.routeId)) {
            "Saved campaign route is missing from its versioned definition"
        }
        check(route.journey.id == choice.journeyDefinitionId) {
            "Saved campaign route and journey definition diverged"
        }
        return JourneySettlementPlanner.plan(event, route.journey)
    }

    private fun legacyProgressEventId(): String = StableRewardIds.eventIdempotencyKey(
        actorId = LOCAL_ACTOR_ID,
        eventType = RewardableEventType.OTHER,
        subjectType = "legacy_progress",
        subjectId = "datastore_v1",
        domainOccurrenceKey = "opening_balance_v1"
    )

    private fun LearningEventCommand.isMeaningful(): Boolean = when (eventType) {
        RewardableEventType.CONCEPT_MASTERED -> completedItemCount >= 1
        RewardableEventType.REVIEW_COMPLETED -> completedItemCount >= 1
        RewardableEventType.PRACTICE_SESSION_COMPLETED -> completedItemCount >= 3
        RewardableEventType.MISTAKE_CORRECTED -> correctedAfterMistake
        RewardableEventType.OTHER -> false
    }

    companion object {
        const val LOCAL_ACTOR_ID: String = "local_learner"
        private const val LEGACY_IMPORT_RULE_ID = "migration.v1.legacy_opening_balance"

        @Volatile
        private var INSTANCE: GamificationRepository? = null

        fun getInstance(context: Context): GamificationRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: GamificationRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}

private fun LearningActivityRow.toLearningActivity(): LearningActivity = LearningActivity(
    eventId = eventId,
    eventType = eventType,
    subjectType = subjectType,
    subjectId = subjectId,
    occurredAtEpochMillis = occurredAtEpochMillis,
    xpAwarded = xpAwarded,
    celebrationTier = celebrationTier
)

private val xpAwardedPattern = Regex("\\\"xpAwarded\\\":(\\d+)")

private fun RewardPresentationReceiptEntity.toPendingPresentation(): PendingRewardPresentation =
    PendingRewardPresentation(
        receiptId = receiptId,
        presentationFamily = presentationFamily,
        xpAwarded = xpAwardedPattern.find(immutableSummaryJson)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
            ?: 0L,
        tier = tier
    )

private fun LearningEventCommand.toEventEntity(
    eventIdempotencyKey: String,
    studyDay: Long
): GamificationEventEntity = GamificationEventEntity(
    eventId = eventIdempotencyKey,
    actorId = GamificationRepository.LOCAL_ACTOR_ID,
    eventType = eventType.wireName,
    subjectType = subjectType,
    subjectId = subjectId,
    occurredAtEpochMillis = occurredAtEpochMillis,
    recordedAtEpochMillis = System.currentTimeMillis(),
    studyDay = studyDay,
    idempotencyKey = eventIdempotencyKey,
    source = source.wireName,
    ruleVersion = GamificationVersions.CURRENT_RULE_VERSION,
    metadataJson = "{}",
    replayOfEventId = null
)

private fun RewardDecision.toTransactionEntities(createdAtEpochMillis: Long): List<RewardTransactionEntity> =
    transactions.map { transaction ->
        RewardTransactionEntity(
            transactionId = transaction.transactionId,
            eventId = eventId,
            actorId = GamificationRepository.LOCAL_ACTOR_ID,
            transactionKind = transaction.kind.wireName,
            currency = transaction.currency,
            amount = transaction.amount,
            ruleId = transaction.ruleId,
            ruleVersion = transaction.ruleVersion,
            reasonCode = transaction.reasonCode,
            createdAtEpochMillis = createdAtEpochMillis,
            idempotencyKey = transaction.idempotencyKey,
            metadataJson = "{}"
        )
    }

private fun RewardDecision.toSummaryEntity(createdAtEpochMillis: Long): RewardSummaryEntity =
    RewardSummaryEntity(
        eventId = eventId,
        actorId = GamificationRepository.LOCAL_ACTOR_ID,
        ruleVersion = ruleVersion,
        xpAwarded = summary.xpAwarded,
        currencyRewardsJson = summary.currencyRewards.joinToString(prefix = "[", postfix = "]") { reward ->
            "{\"currency\":\"${reward.currency}\",\"amount\":${reward.amount}}"
        },
        questProgressJson = "[]",
        achievementIdsJson = "[]",
        unlockedContentJson = "[]",
        celebrationTier = summary.celebrationTier.wireName,
        summaryJson = "{\"reasonCode\":\"$reasonCode\",\"xpAwarded\":${summary.xpAwarded}}",
        createdAtEpochMillis = createdAtEpochMillis
    )

private fun RewardDecision.toReceiptEntities(createdAtEpochMillis: Long): List<RewardPresentationReceiptEntity> =
    presentationReceipts.map { receipt ->
        RewardPresentationReceiptEntity(
            receiptId = receipt.receiptId,
            actorId = GamificationRepository.LOCAL_ACTOR_ID,
            sourceEventId = receipt.sourceEventId,
            sourceTransactionId = receipt.sourceTransactionId,
            presentationFamily = receipt.presentationFamily,
            idempotencyKey = receipt.idempotencyKey,
            priority = receipt.priority,
            tier = receipt.tier.wireName,
            state = receipt.initialState.wireName,
            immutableSummaryJson = "{\"reasonCode\":\"$reasonCode\",\"xpAwarded\":${summary.xpAwarded}}",
            createdAtEpochMillis = createdAtEpochMillis,
            updatedAtEpochMillis = createdAtEpochMillis,
            expiresAtEpochMillis = null,
            acknowledgedAtEpochMillis = null,
            suppressionReason = null,
            coalescedCount = 1
        )
    }

private fun PresentationTier.priority(): Int = when (this) {
    PresentationTier.MICRO -> 10
    PresentationTier.STANDARD -> 20
    PresentationTier.MILESTONE -> 30
    PresentationTier.SHOWPIECE -> 40
}
