package com.cinnamon.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

enum class RewardWriteStatus {
    APPLIED,
    DUPLICATE
}

data class RewardWriteResult(
    val status: RewardWriteStatus,
    val canonicalEventId: String,
    val summary: RewardSummaryEntity?
)

data class StudyDayCurrencyTotal(
    val studyDay: Long,
    val amount: Long
)

/** Evidence families that are backed by explicit, immutable ledger queries. */
enum class CatalogEvidenceMetric {
    DISTINCT_DUE_REVIEWS_TODAY,
    DISTINCT_REVIEWED_ITEMS,
    MASTERED_ITEMS_AFTER_DELAY,
    RHYTHM_WEEKS,
    DISTINCT_PRACTICE_CONTENT_KINDS,
    ACTIVE_STUDY_DAYS
}

/** A catalog milestone candidate. It becomes value-bearing only if its unlock row is new. */
data class CatalogUnlockSettlementCandidate(
    val unlock: AchievementUnlockEntity,
    val metric: CatalogEvidenceMetric,
    val target: Long,
    val xpTransaction: RewardTransactionEntity?,
    val unlockedContentIds: List<String>,
    val presentationReceipt: RewardPresentationReceiptEntity?
)

/** A windowed quest candidate; eligibility can assign it, but only ledger evidence progresses it. */
data class CatalogQuestSettlementCandidate(
    val instance: QuestInstanceEntity,
    val metric: CatalogEvidenceMetric,
    val eligibleForAssignment: Boolean
)

data class CatalogSettlementRequest(
    val unlocks: List<CatalogUnlockSettlementCandidate> = emptyList(),
    val quests: List<CatalogQuestSettlementCandidate> = emptyList()
)

/** One deterministic candidate per authored stage; Room selects only the current active stage. */
data class JourneyStageSettlementCandidate(
    val stage: JourneyStageProgressEntity,
    val metric: CatalogEvidenceMetric,
    val rewardTransaction: RewardTransactionEntity,
    val presentationReceipt: RewardPresentationReceiptEntity
)

data class JourneySettlementRequest(
    val instance: JourneyInstanceEntity? = null,
    val stages: List<JourneyStageSettlementCandidate> = emptyList(),
    /** False only for an initialization event that must not consume evidence or grant value. */
    val settleEvidenceOnThisEvent: Boolean = true
)

/** A route commitment and its selected Journey are committed with the same auditable event. */
data class CampaignRouteChoiceRequest(
    val choice: CampaignRouteChoiceEntity? = null,
    val prerequisiteJourneyDefinitionId: String? = null,
    val prerequisiteJourneyDefinitionVersion: Int? = null,
    val prerequisiteStageDefinitionId: String? = null,
    val selectedRouteJourney: JourneySettlementRequest = JourneySettlementRequest()
)

private data class SettledQuestProgress(
    val questInstanceId: String,
    val from: Long,
    val to: Long,
    val target: Long,
    val completed: Boolean
)

private data class CatalogSettlementResult(
    val transactions: List<RewardTransactionEntity>,
    val receipts: List<RewardPresentationReceiptEntity>,
    val unlockedItems: List<Pair<String, Int>>,
    val unlockedContentIds: List<String>,
    val questProgress: List<SettledQuestProgress>
)

private data class SettledJourneyStage(
    val journeyInstanceId: String,
    val stageDefinitionId: String,
    val stageOrder: Int,
    val from: Long,
    val to: Long,
    val target: Long,
    val completed: Boolean,
    val journeyCompleted: Boolean
)

private data class JourneySettlementResult(
    val transactions: List<RewardTransactionEntity> = emptyList(),
    val receipts: List<RewardPresentationReceiptEntity> = emptyList(),
    val stage: SettledJourneyStage? = null
)

/** Compact, read-only activity projection for learner-facing history. */
data class LearningActivityRow(
    val eventId: String,
    val eventType: String,
    val subjectType: String,
    val subjectId: String,
    val occurredAtEpochMillis: Long,
    val xpAwarded: Long,
    val celebrationTier: String
)

class DailyRewardCapExceededException : IllegalStateException("Daily XP award cap would be exceeded")

/**
 * Persistence boundary for the gamification ledger.
 *
 * There are intentionally no delete or update methods for events, transactions,
 * summaries, or achievement unlocks. Corrections belong in compensating events
 * and transactions so the audit trail remains replayable.
 */
@Dao
interface GamificationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEventIfAbsent(event: GamificationEventEntity): Long

    @Query("SELECT * FROM gamification_events WHERE eventId = :eventId LIMIT 1")
    suspend fun eventById(eventId: String): GamificationEventEntity?

    @Query(
        """
        SELECT * FROM gamification_events
        WHERE actorId = :actorId AND idempotencyKey = :idempotencyKey
        LIMIT 1
        """
    )
    suspend fun eventByIdempotencyKey(
        actorId: String,
        idempotencyKey: String
    ): GamificationEventEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRewardTransactions(transactions: List<RewardTransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRewardSummary(summary: RewardSummaryEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPresentationReceipts(receipts: List<RewardPresentationReceiptEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBalanceIfAbsent(balance: RewardBalanceEntity): Long

    @Query(
        """
        UPDATE reward_balances
        SET balance = balance + :delta,
            updatedAtEpochMillis = MAX(updatedAtEpochMillis, :updatedAtEpochMillis),
            lastTransactionId = :lastTransactionId
        WHERE actorId = :actorId AND currency = :currency
        """
    )
    suspend fun applyBalanceDelta(
        actorId: String,
        currency: String,
        delta: Long,
        updatedAtEpochMillis: Long,
        lastTransactionId: String
    ): Int

    @Query("SELECT * FROM reward_summaries WHERE eventId = :eventId LIMIT 1")
    suspend fun rewardSummaryForEvent(eventId: String): RewardSummaryEntity?

    @Query(
        """
        SELECT * FROM reward_transactions
        WHERE actorId = :actorId
        ORDER BY createdAtEpochMillis DESC, transactionId DESC
        """
    )
    fun observeRewardTransactions(actorId: String): Flow<List<RewardTransactionEntity>>

    @Query(
        """
        SELECT * FROM reward_balances
        WHERE actorId = :actorId
        ORDER BY currency ASC
        """
    )
    fun observeRewardBalances(actorId: String): Flow<List<RewardBalanceEntity>>

    @Query(
        """
        SELECT * FROM reward_balances
        WHERE actorId = :actorId AND currency = :currency
        LIMIT 1
        """
    )
    fun observeRewardBalance(actorId: String, currency: String): Flow<RewardBalanceEntity?>

    @Query(
        """
        SELECT COALESCE(SUM(reward_transactions.amount), 0)
        FROM reward_transactions
        INNER JOIN gamification_events
          ON gamification_events.eventId = reward_transactions.eventId
        WHERE reward_transactions.actorId = :actorId
          AND reward_transactions.currency = :currency
          AND reward_transactions.transactionKind = 'grant'
          AND reward_transactions.amount > 0
          AND gamification_events.studyDay = :studyDay
        """
    )
    fun observeCurrencyForStudyDay(
        actorId: String,
        currency: String,
        studyDay: Long
    ): Flow<Long>

    @Query(
        """
        SELECT gamification_events.studyDay AS studyDay,
               COALESCE(SUM(reward_transactions.amount), 0) AS amount
        FROM reward_transactions
        INNER JOIN gamification_events
          ON gamification_events.eventId = reward_transactions.eventId
        WHERE reward_transactions.actorId = :actorId
          AND reward_transactions.currency = :currency
          AND reward_transactions.transactionKind = 'grant'
          AND reward_transactions.amount > 0
          AND gamification_events.eventType != 'legacy_progress_imported'
          AND gamification_events.studyDay >= :fromStudyDay
        GROUP BY gamification_events.studyDay
        ORDER BY gamification_events.studyDay ASC
        """
    )
    fun observeCurrencyTotalsSinceStudyDay(
        actorId: String,
        currency: String,
        fromStudyDay: Long
    ): Flow<List<StudyDayCurrencyTotal>>

    @Query(
        """
        SELECT COALESCE(SUM(reward_transactions.amount), 0)
        FROM reward_transactions
        INNER JOIN gamification_events
          ON gamification_events.eventId = reward_transactions.eventId
        WHERE reward_transactions.actorId = :actorId
          AND reward_transactions.currency = :currency
          AND reward_transactions.transactionKind = 'grant'
          AND reward_transactions.amount > 0
          AND gamification_events.studyDay = :studyDay
          AND gamification_events.eventType != 'legacy_progress_imported'
        """
    )
    suspend fun currencyForStudyDayOnce(
        actorId: String,
        currency: String,
        studyDay: Long
    ): Long

    /** Farmable activity XP is capped; one-time catalog and journey milestones are exempt. */
    @Query(
        """
        SELECT COALESCE(SUM(reward_transactions.amount), 0)
        FROM reward_transactions
        INNER JOIN gamification_events
          ON gamification_events.eventId = reward_transactions.eventId
        WHERE reward_transactions.actorId = :actorId
          AND reward_transactions.currency = :currency
          AND reward_transactions.transactionKind = 'grant'
          AND reward_transactions.amount > 0
          AND reward_transactions.ruleId NOT LIKE 'catalog.%'
          AND reward_transactions.ruleId NOT LIKE 'journey.%'
          AND gamification_events.studyDay = :studyDay
        """
    )
    suspend fun cappedCurrencyForStudyDayOnce(
        actorId: String,
        currency: String,
        studyDay: Long
    ): Long

    @Query(
        """
        SELECT DISTINCT studyDay
        FROM gamification_events
        WHERE actorId = :actorId
          -- XP caps control economy; they must not erase a verified learning day.
          -- These event types are emitted only after their success gates pass.
          AND eventType IN (
              'review_completed',
              'practice_session_completed',
              'concept_mastered',
              'mistake_corrected'
          )
        ORDER BY gamification_events.studyDay DESC
        LIMIT :limit
        """
    )
    fun observeRecentStudyDays(actorId: String, limit: Int): Flow<List<Long>>

    @Query(
        """
        SELECT COUNT(*) FROM gamification_events
        WHERE actorId = :actorId
          AND eventType = 'review_completed'
          AND studyDay = :studyDay
        """
    )
    fun observeCompletedReviewCountForStudyDay(actorId: String, studyDay: Long): Flow<Int>

    @Query(
        """
        SELECT COUNT(DISTINCT subjectId) FROM gamification_events
        WHERE actorId = :actorId
          AND eventType = 'review_completed'
          AND studyDay = :studyDay
        """
    )
    fun observeDistinctReviewedSubjectCountForStudyDay(
        actorId: String,
        studyDay: Long
    ): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM gamification_events
        WHERE actorId = :actorId
          AND eventType = :eventType
          AND eventType != 'legacy_progress_imported'
        """
    )
    fun observeEventCount(actorId: String, eventType: String): Flow<Int>

    @Query(
        """
        SELECT COUNT(DISTINCT subjectId) FROM gamification_events
        WHERE actorId = :actorId
          AND eventType = :eventType
          AND eventType != 'legacy_progress_imported'
        """
    )
    fun observeDistinctSubjectIdCount(actorId: String, eventType: String): Flow<Int>

    @Query(
        """
        SELECT COUNT(DISTINCT subjectId) FROM gamification_events
        WHERE actorId = :actorId
          AND eventType = :eventType
          AND eventType != 'legacy_progress_imported'
        """
    )
    suspend fun distinctSubjectIdCountOnce(actorId: String, eventType: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM gamification_events
        WHERE actorId = :actorId
          AND eventType = :eventType
          AND studyDay = :studyDay
          AND eventType != 'legacy_progress_imported'
        """
    )
    fun observeEventCountForStudyDay(
        actorId: String,
        eventType: String,
        studyDay: Long
    ): Flow<Int>

    @Query(
        """
        SELECT COUNT(DISTINCT subjectType) FROM gamification_events
        WHERE actorId = :actorId
          AND eventType = :eventType
          AND eventType != 'legacy_progress_imported'
        """
    )
    fun observeDistinctSubjectTypeCount(actorId: String, eventType: String): Flow<Int>

    @Query(
        """
        SELECT COUNT(DISTINCT subjectType) FROM gamification_events
        WHERE actorId = :actorId
          AND eventType = :eventType
          AND eventType != 'legacy_progress_imported'
        """
    )
    suspend fun distinctSubjectTypeCountOnce(actorId: String, eventType: String): Int

    @Query(
        """
        SELECT COUNT(DISTINCT subjectType) FROM gamification_events
        WHERE actorId = :actorId
          AND eventType = :eventType
          AND studyDay = :studyDay
          AND eventType != 'legacy_progress_imported'
        """
    )
    fun observeDistinctSubjectTypeCountForStudyDay(
        actorId: String,
        eventType: String,
        studyDay: Long
    ): Flow<Int>

    @Query(
        """
        SELECT COUNT(DISTINCT studyDay) FROM gamification_events
        WHERE actorId = :actorId
          AND eventType != 'legacy_progress_imported'
          AND studyDay > 0
        """
    )
    fun observeActiveStudyDayCount(actorId: String): Flow<Int>

    @Query(
        """
        SELECT DISTINCT studyDay
        FROM gamification_events
        WHERE actorId = :actorId
          AND eventType IN (
              'review_completed',
              'practice_session_completed',
              'concept_mastered',
              'mistake_corrected'
          )
        ORDER BY studyDay DESC
        LIMIT :limit
        """
    )
    suspend fun recentStudyDaysOnce(actorId: String, limit: Int): List<Long>

    @Query(
        """
        SELECT COUNT(DISTINCT studyDay) FROM gamification_events
        WHERE actorId = :actorId
          AND eventType IN (
              'review_completed',
              'practice_session_completed',
              'concept_mastered',
              'mistake_corrected'
          )
          AND studyDay > 0
        """
    )
    suspend fun activeStudyDayCountOnce(actorId: String): Int

    @Query(
        """
        SELECT events.eventId AS eventId,
               events.eventType AS eventType,
               events.subjectType AS subjectType,
               events.subjectId AS subjectId,
               events.occurredAtEpochMillis AS occurredAtEpochMillis,
               COALESCE(summaries.xpAwarded, 0) AS xpAwarded,
               COALESCE(summaries.celebrationTier, 'none') AS celebrationTier
        FROM gamification_events AS events
        LEFT JOIN reward_summaries AS summaries ON summaries.eventId = events.eventId
        WHERE events.actorId = :actorId
          AND events.eventType NOT IN (
              'legacy_progress_imported',
              'catalog_reconciled',
              'journey_reconciled',
              'campaign_route_selected'
          )
        ORDER BY events.occurredAtEpochMillis DESC, events.eventId DESC
        LIMIT :limit
        """
    )
    fun observeRecentLearningActivity(
        actorId: String,
        limit: Int
    ): Flow<List<LearningActivityRow>>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM gamification_events
            WHERE actorId = :actorId
              AND eventType = :eventType
              AND subjectType = :subjectType
              AND subjectId = :subjectId
        )
        """
    )
    suspend fun hasEventForSubject(
        actorId: String,
        eventType: String,
        subjectType: String,
        subjectId: String
    ): Boolean

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM gamification_events
            WHERE actorId = :actorId
              AND eventType = :eventType
              AND subjectType = :subjectType
              AND subjectId = :subjectId
              AND studyDay = :studyDay
        )
        """
    )
    suspend fun hasEventForSubjectOnStudyDay(
        actorId: String,
        eventType: String,
        subjectType: String,
        subjectId: String,
        studyDay: Long
    ): Boolean

    @Query(
        """
        SELECT * FROM reward_summaries
        WHERE actorId = :actorId
        ORDER BY createdAtEpochMillis DESC
        LIMIT :limit
        """
    )
    fun observeRecentRewardSummaries(
        actorId: String,
        limit: Int
    ): Flow<List<RewardSummaryEntity>>

    /**
     * Commits the entire event result in one SQLite transaction. A duplicate
     * actor/idempotency pair returns the original result and changes no balance.
     */
    @Transaction
    suspend fun recordRewardAtomically(
        event: GamificationEventEntity,
        transactions: List<RewardTransactionEntity>,
        summary: RewardSummaryEntity,
        presentationReceipts: List<RewardPresentationReceiptEntity>,
        catalogSettlement: CatalogSettlementRequest = CatalogSettlementRequest(),
        journeySettlement: JourneySettlementRequest = JourneySettlementRequest(),
        additionalJourneySettlements: List<JourneySettlementRequest> = emptyList(),
        campaignRouteChoice: CampaignRouteChoiceRequest = CampaignRouteChoiceRequest(),
        questClaimInstanceId: String? = null
    ): RewardWriteResult {
        validateRewardCommit(event, transactions, summary, presentationReceipts)
        validateCatalogSettlement(event, catalogSettlement)
        validateCampaignRouteChoice(event, campaignRouteChoice)
        if (campaignRouteChoice.choice != null) {
            require(event.eventType == "campaign_route_selected") {
                "Campaign route choices require their dedicated event type"
            }
            require(
                transactions.isEmpty() &&
                    summary.xpAwarded == 0L &&
                    presentationReceipts.isEmpty() &&
                    catalogSettlement.unlocks.isEmpty() &&
                    catalogSettlement.quests.isEmpty() &&
                    journeySettlement.instance == null &&
                    journeySettlement.stages.isEmpty() &&
                    additionalJourneySettlements.isEmpty()
            ) { "Campaign route choice cannot share an event with rewards or other settlements" }
        }
        val journeyRequests = buildList {
            if (journeySettlement.instance != null) add(journeySettlement)
            addAll(additionalJourneySettlements)
            if (campaignRouteChoice.choice != null) add(campaignRouteChoice.selectedRouteJourney)
        }
        journeyRequests.forEach { request -> validateJourneySettlement(event, request) }
        require(
            journeyRequests.mapNotNull { request -> request.instance?.journeyInstanceId }
                .distinct().size == journeyRequests.size
        ) { "One event cannot settle the same journey definition twice" }

        val insertedRowId = insertEventIfAbsent(event)
        if (insertedRowId == -1L) {
            val byKey = eventByIdempotencyKey(event.actorId, event.idempotencyKey)
            if (byKey != null) {
                return RewardWriteResult(
                    status = RewardWriteStatus.DUPLICATE,
                    canonicalEventId = byKey.eventId,
                    summary = rewardSummaryForEvent(byKey.eventId)
                )
            }

            val byId = eventById(event.eventId)
            check(byId == null) {
                "eventId collision: ${event.eventId} is already bound to another idempotency key"
            }
            error("Event insert was ignored without a matching unique-key record")
        }

        if (questClaimInstanceId != null) {
            check(
                claimQuestOnce(
                    questInstanceId = questClaimInstanceId,
                    actorId = event.actorId,
                    claimedAtEpochMillis = event.occurredAtEpochMillis
                ) == 1
            ) { "Quest is not in a claimable completed state" }
        }

        campaignRouteChoice.choice?.let { choice ->
            val prerequisiteJourneyDefinitionId = checkNotNull(
                campaignRouteChoice.prerequisiteJourneyDefinitionId
            )
            val prerequisiteStageDefinitionId = checkNotNull(
                campaignRouteChoice.prerequisiteStageDefinitionId
            )
            val prerequisiteJourneyDefinitionVersion = checkNotNull(
                campaignRouteChoice.prerequisiteJourneyDefinitionVersion
            )
            check(
                isJourneyStageCompletedOnce(
                    actorId = event.actorId,
                    journeyDefinitionId = prerequisiteJourneyDefinitionId,
                    journeyDefinitionVersion = prerequisiteJourneyDefinitionVersion,
                    stageDefinitionId = prerequisiteStageDefinitionId
                )
            ) { "Campaign route choice is still locked by its journey prerequisite" }

            val insertedChoice = insertCampaignRouteChoiceIfAbsent(choice)
            if (insertedChoice == -1L) {
                val existing = checkNotNull(
                    campaignRouteChoiceOnce(
                        actorId = choice.actorId,
                        campaignDefinitionId = choice.campaignDefinitionId,
                        campaignDefinitionVersion = choice.campaignDefinitionVersion
                    )
                ) { "Campaign choice conflict was ignored without a canonical row" }
                check(
                    existing.choiceId == choice.choiceId &&
                        existing.routeId == choice.routeId &&
                        existing.journeyDefinitionId == choice.journeyDefinitionId
                ) { "Campaign route is already committed to another path" }
            }
        }

        journeyRequests.forEach { request ->
            request.instance?.let { instance ->
                insertJourneyInstanceIfAbsent(instance)
                if (request.stages.isNotEmpty()) {
                    insertJourneyStagesIfAbsent(request.stages.map { it.stage })
                }
            }
        }

        expireOpenQuests(event.actorId, event.occurredAtEpochMillis)
        val evidenceCache = mutableMapOf<CatalogEvidenceMetric, Long>()
        suspend fun evidence(metric: CatalogEvidenceMetric): Long {
            evidenceCache[metric]?.let { return it }
            val resolved = when (metric) {
                CatalogEvidenceMetric.DISTINCT_DUE_REVIEWS_TODAY ->
                    distinctReviewedSubjectCountForStudyDayOnce(event.actorId, event.studyDay).toLong()
                CatalogEvidenceMetric.DISTINCT_REVIEWED_ITEMS ->
                    distinctSubjectIdCountOnce(event.actorId, "review_completed").toLong()
                CatalogEvidenceMetric.MASTERED_ITEMS_AFTER_DELAY ->
                    distinctSubjectIdCountOnce(event.actorId, "concept_mastered").toLong()
                CatalogEvidenceMetric.RHYTHM_WEEKS ->
                    weeksWithAtLeastThreeStudyDays(recentStudyDaysOnce(event.actorId, 400)).toLong()
                CatalogEvidenceMetric.DISTINCT_PRACTICE_CONTENT_KINDS ->
                    distinctSubjectTypeCountOnce(event.actorId, "practice_session_completed").toLong()
                CatalogEvidenceMetric.ACTIVE_STUDY_DAYS ->
                    activeStudyDayCountOnce(event.actorId).toLong()
            }
            evidenceCache[metric] = resolved
            return resolved
        }

        val settledQuestProgress = mutableListOf<SettledQuestProgress>()
        catalogSettlement.quests.forEach { candidate ->
            if (candidate.eligibleForAssignment) {
                insertQuestInstances(listOf(candidate.instance))
            }
            val before = questInstanceById(candidate.instance.questInstanceId) ?: return@forEach
            val progress = evidence(candidate.metric).coerceAtMost(before.target)
            setQuestProgressAtLeast(
                questInstanceId = before.questInstanceId,
                actorId = event.actorId,
                progress = progress,
                updatedAtEpochMillis = event.occurredAtEpochMillis
            )
            val progressed = checkNotNull(questInstanceById(before.questInstanceId))
            val completedNow = completeQuestOnce(
                questInstanceId = progressed.questInstanceId,
                actorId = event.actorId,
                completionEventId = event.eventId,
                completedAtEpochMillis = event.occurredAtEpochMillis
            ) == 1
            val after = checkNotNull(questInstanceById(before.questInstanceId))
            if (after.progress != before.progress || completedNow) {
                settledQuestProgress += SettledQuestProgress(
                    questInstanceId = after.questInstanceId,
                    from = before.progress,
                    to = after.progress,
                    target = after.target,
                    completed = after.state == "completed" || after.state == "claimed"
                )
            }
        }

        val catalogTransactions = mutableListOf<RewardTransactionEntity>()
        val catalogReceipts = mutableListOf<RewardPresentationReceiptEntity>()
        val unlockedItems = mutableListOf<Pair<String, Int>>()
        val unlockedContentIds = linkedSetOf<String>()
        catalogSettlement.unlocks.forEach { candidate ->
            val progress = evidence(candidate.metric)
            if (progress < candidate.target) return@forEach
            val inserted = insertAchievementUnlockIfAbsent(
                candidate.unlock.copy(
                    progressJson = "{\"value\":$progress,\"target\":${candidate.target}}",
                    evidenceJson = "{\"metric\":\"${candidate.metric.name.lowercase(Locale.ROOT)}\",\"value\":$progress,\"sourceEventId\":${jsonString(event.eventId)}}"
                )
            )
            if (inserted == -1L) return@forEach

            candidate.xpTransaction?.let(catalogTransactions::add)
            candidate.presentationReceipt?.let { receipt ->
                catalogReceipts += receipt.copy(
                    immutableSummaryJson = "{\"catalogItemId\":${jsonString(candidate.unlock.achievementId)},\"level\":${candidate.unlock.level},\"progress\":$progress,\"target\":${candidate.target},\"xpAwarded\":${candidate.xpTransaction?.amount ?: 0L}}"
                )
            }
            unlockedItems += candidate.unlock.achievementId to candidate.unlock.level
            unlockedContentIds += candidate.unlockedContentIds
        }

        val settlement = CatalogSettlementResult(
            transactions = catalogTransactions,
            receipts = catalogReceipts,
            unlockedItems = unlockedItems,
            unlockedContentIds = unlockedContentIds.toList(),
            questProgress = settledQuestProgress
        )

        val journeyResults = journeyRequests.map { request ->
            if (!request.settleEvidenceOnThisEvent) {
                JourneySettlementResult()
            } else {
                this.settleActiveJourneyStage(
                    event = event,
                    request = request,
                    evidence = ::evidence
                )
            }
        }
        val allTransactions = transactions + settlement.transactions +
            journeyResults.flatMap(JourneySettlementResult::transactions)
        val allReceipts = presentationReceipts + settlement.receipts +
            journeyResults.flatMap(JourneySettlementResult::receipts)
        val settledSummary = journeyResults.fold(summary.withCatalogSettlement(settlement)) {
                current, journeyResult -> current.withJourneySettlement(journeyResult)
            }
        validateRewardCommit(event, allTransactions, settledSummary, allReceipts)

        val proposedPositiveXp = allTransactions
            .asSequence()
            .filter { transaction ->
                transaction.currency == "xp" &&
                    transaction.transactionKind == "grant" &&
                    transaction.amount > 0L &&
                    !transaction.ruleId.startsWith(CATALOG_RULE_PREFIX) &&
                    !transaction.ruleId.startsWith(JOURNEY_RULE_PREFIX)
            }
            .sumOf { it.amount }
        if (event.eventType != "legacy_progress_imported" && proposedPositiveXp > 0L) {
            val alreadyAwarded = cappedCurrencyForStudyDayOnce(
                actorId = event.actorId,
                currency = "xp",
                studyDay = event.studyDay
            )
            if (alreadyAwarded + proposedPositiveXp > MAX_XP_PER_STUDY_DAY) {
                throw DailyRewardCapExceededException()
            }
        }

        if (allTransactions.isNotEmpty()) {
            insertRewardTransactions(allTransactions)
        }
        insertRewardSummary(settledSummary)
        if (allReceipts.isNotEmpty()) {
            insertPresentationReceipts(allReceipts)
        }

        allTransactions.groupBy { it.currency }.forEach { (currency, currencyTransactions) ->
            val delta = currencyTransactions.fold(0L) { total, transaction ->
                Math.addExact(total, transaction.amount)
            }
            val latest = checkNotNull(
                currencyTransactions.maxWithOrNull(
                    compareBy<RewardTransactionEntity> { it.createdAtEpochMillis }
                        .thenBy { it.transactionId }
                )
            )
            insertBalanceIfAbsent(
                RewardBalanceEntity(
                    actorId = event.actorId,
                    currency = currency,
                    balance = 0L,
                    updatedAtEpochMillis = latest.createdAtEpochMillis,
                    lastTransactionId = null
                )
            )
            check(
                applyBalanceDelta(
                    actorId = event.actorId,
                    currency = currency,
                    delta = delta,
                    updatedAtEpochMillis = latest.createdAtEpochMillis,
                    lastTransactionId = latest.transactionId
                ) == 1
            ) { "Failed to update materialized $currency balance" }
        }

        return RewardWriteResult(
            status = RewardWriteStatus.APPLIED,
            canonicalEventId = event.eventId,
            summary = settledSummary
        )
    }

    @Query(
        """
        SELECT COUNT(DISTINCT subjectId) FROM gamification_events
        WHERE actorId = :actorId
          AND eventType = 'review_completed'
          AND studyDay = :studyDay
        """
    )
    suspend fun distinctReviewedSubjectCountForStudyDayOnce(actorId: String, studyDay: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCampaignRouteChoiceIfAbsent(choice: CampaignRouteChoiceEntity): Long

    @Query(
        """
        SELECT * FROM campaign_route_choices
        WHERE actorId = :actorId
          AND campaignDefinitionId = :campaignDefinitionId
          AND campaignDefinitionVersion = :campaignDefinitionVersion
        LIMIT 1
        """
    )
    suspend fun campaignRouteChoiceOnce(
        actorId: String,
        campaignDefinitionId: String,
        campaignDefinitionVersion: Int
    ): CampaignRouteChoiceEntity?

    @Query(
        """
        SELECT * FROM campaign_route_choices
        WHERE actorId = :actorId
        ORDER BY chosenAtEpochMillis DESC, choiceId ASC
        """
    )
    fun observeCampaignRouteChoices(actorId: String): Flow<List<CampaignRouteChoiceEntity>>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM journey_stage_progress AS stage
            INNER JOIN journey_instances AS journey
                ON journey.journeyInstanceId = stage.journeyInstanceId
            WHERE journey.actorId = :actorId
              AND journey.definitionId = :journeyDefinitionId
              AND journey.definitionVersion = :journeyDefinitionVersion
              AND stage.stageDefinitionId = :stageDefinitionId
              AND stage.state = 'completed'
        )
        """
    )
    suspend fun isJourneyStageCompletedOnce(
        actorId: String,
        journeyDefinitionId: String,
        journeyDefinitionVersion: Int,
        stageDefinitionId: String
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertJourneyInstanceIfAbsent(instance: JourneyInstanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertJourneyStagesIfAbsent(stages: List<JourneyStageProgressEntity>): List<Long>

    @Query("SELECT * FROM journey_instances WHERE journeyInstanceId = :journeyInstanceId LIMIT 1")
    suspend fun journeyInstanceById(journeyInstanceId: String): JourneyInstanceEntity?

    @Query(
        """
        SELECT * FROM journey_instances
        WHERE actorId = :actorId
        ORDER BY CASE state WHEN 'active' THEN 0 ELSE 1 END,
                 updatedAtEpochMillis DESC,
                 journeyInstanceId ASC
        """
    )
    fun observeJourneyInstances(actorId: String): Flow<List<JourneyInstanceEntity>>

    @Query(
        """
        SELECT * FROM journey_stage_progress
        WHERE actorId = :actorId
        ORDER BY journeyInstanceId ASC, stageOrder ASC
        """
    )
    fun observeJourneyStages(actorId: String): Flow<List<JourneyStageProgressEntity>>

    @Query(
        """
        SELECT * FROM journey_stage_progress
        WHERE journeyInstanceId = :journeyInstanceId AND state = 'active'
        ORDER BY stageOrder ASC
        LIMIT 1
        """
    )
    suspend fun activeJourneyStageOnce(journeyInstanceId: String): JourneyStageProgressEntity?

    @Query(
        """
        SELECT * FROM journey_stage_progress
        WHERE stageProgressId = :stageProgressId
        LIMIT 1
        """
    )
    suspend fun journeyStageById(stageProgressId: String): JourneyStageProgressEntity?

    @Query(
        """
        UPDATE journey_stage_progress
        SET progress = MAX(progress, MIN(target, :progress)),
            updatedAtEpochMillis = MAX(updatedAtEpochMillis, :updatedAtEpochMillis)
        WHERE stageProgressId = :stageProgressId
          AND actorId = :actorId
          AND state = 'active'
          AND :progress >= 0
        """
    )
    suspend fun setJourneyStageProgressAtLeast(
        stageProgressId: String,
        actorId: String,
        progress: Long,
        updatedAtEpochMillis: Long
    ): Int

    @Query(
        """
        UPDATE journey_stage_progress
        SET state = 'completed',
            progress = target,
            completionEventId = :completionEventId,
            completedAtEpochMillis = :completedAtEpochMillis,
            updatedAtEpochMillis = :completedAtEpochMillis
        WHERE stageProgressId = :stageProgressId
          AND actorId = :actorId
          AND state = 'active'
          AND progress >= target
        """
    )
    suspend fun completeJourneyStageOnce(
        stageProgressId: String,
        actorId: String,
        completionEventId: String,
        completedAtEpochMillis: Long
    ): Int

    @Query(
        """
        UPDATE journey_stage_progress
        SET state = 'active',
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE journeyInstanceId = :journeyInstanceId
          AND actorId = :actorId
          AND stageOrder = :stageOrder
          AND state = 'locked'
        """
    )
    suspend fun activateJourneyStageOnce(
        journeyInstanceId: String,
        actorId: String,
        stageOrder: Int,
        updatedAtEpochMillis: Long
    ): Int

    @Query(
        """
        UPDATE journey_instances
        SET currentStageOrder = :nextStageOrder,
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE journeyInstanceId = :journeyInstanceId
          AND actorId = :actorId
          AND state = 'active'
          AND currentStageOrder = :completedStageOrder
        """
    )
    suspend fun advanceJourneyInstanceOnce(
        journeyInstanceId: String,
        actorId: String,
        completedStageOrder: Int,
        nextStageOrder: Int,
        updatedAtEpochMillis: Long
    ): Int

    @Query(
        """
        UPDATE journey_instances
        SET state = 'completed',
            completionEventId = :completionEventId,
            completedAtEpochMillis = :completedAtEpochMillis,
            updatedAtEpochMillis = :completedAtEpochMillis
        WHERE journeyInstanceId = :journeyInstanceId
          AND actorId = :actorId
          AND state = 'active'
          AND currentStageOrder = :finalStageOrder
        """
    )
    suspend fun completeJourneyInstanceOnce(
        journeyInstanceId: String,
        actorId: String,
        finalStageOrder: Int,
        completionEventId: String,
        completedAtEpochMillis: Long
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuestInstances(instances: List<QuestInstanceEntity>): List<Long>

    @Query("SELECT * FROM quest_instances WHERE questInstanceId = :questInstanceId LIMIT 1")
    suspend fun questInstanceById(questInstanceId: String): QuestInstanceEntity?

    @Query(
        """
        SELECT * FROM quest_instances
        WHERE actorId = :actorId AND state IN ('available', 'in_progress', 'completed', 'claimed')
        ORDER BY endsAtEpochMillis ASC, questInstanceId ASC
        """
    )
    fun observeActiveQuestInstances(actorId: String): Flow<List<QuestInstanceEntity>>

    @Query(
        """
        UPDATE quest_instances
        SET progress = MAX(progress, MIN(target, :progress)),
            state = CASE
                WHEN state = 'available' AND :progress > 0 THEN 'in_progress'
                ELSE state
            END,
            updatedAtEpochMillis = MAX(updatedAtEpochMillis, :updatedAtEpochMillis)
        WHERE questInstanceId = :questInstanceId
          AND actorId = :actorId
          AND state IN ('available', 'in_progress')
          AND :progress >= 0
        """
    )
    suspend fun setQuestProgressAtLeast(
        questInstanceId: String,
        actorId: String,
        progress: Long,
        updatedAtEpochMillis: Long
    ): Int

    @Query(
        """
        UPDATE quest_instances
        SET progress = MIN(target, progress + :delta),
            state = CASE WHEN state = 'available' THEN 'in_progress' ELSE state END,
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE questInstanceId = :questInstanceId
          AND actorId = :actorId
          AND state IN ('available', 'in_progress')
          AND :delta > 0
        """
    )
    suspend fun advanceQuestProgress(
        questInstanceId: String,
        actorId: String,
        delta: Long,
        updatedAtEpochMillis: Long
    ): Int

    @Query(
        """
        UPDATE quest_instances
        SET state = 'completed',
            progress = target,
            completionEventId = :completionEventId,
            completedAtEpochMillis = :completedAtEpochMillis,
            updatedAtEpochMillis = :completedAtEpochMillis
        WHERE questInstanceId = :questInstanceId
          AND actorId = :actorId
          AND state IN ('available', 'in_progress')
          AND progress >= target
        """
    )
    suspend fun completeQuestOnce(
        questInstanceId: String,
        actorId: String,
        completionEventId: String,
        completedAtEpochMillis: Long
    ): Int

    @Query(
        """
        UPDATE quest_instances
        SET state = 'claimed',
            claimedAtEpochMillis = :claimedAtEpochMillis,
            updatedAtEpochMillis = :claimedAtEpochMillis
        WHERE questInstanceId = :questInstanceId
          AND actorId = :actorId
          AND state = 'completed'
          AND claimedAtEpochMillis IS NULL
        """
    )
    suspend fun claimQuestOnce(
        questInstanceId: String,
        actorId: String,
        claimedAtEpochMillis: Long
    ): Int

    @Query(
        """
        UPDATE quest_instances
        SET state = 'expired', updatedAtEpochMillis = :nowEpochMillis
        WHERE actorId = :actorId
          AND endsAtEpochMillis <= :nowEpochMillis
          AND state IN ('available', 'in_progress')
        """
    )
    suspend fun expireOpenQuests(actorId: String, nowEpochMillis: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievementUnlockIfAbsent(unlock: AchievementUnlockEntity): Long

    @Query(
        """
        SELECT * FROM achievement_unlocks
        WHERE actorId = :actorId
        ORDER BY unlockedAtEpochMillis DESC, achievementId ASC, level DESC
        """
    )
    fun observeAchievementUnlocks(actorId: String): Flow<List<AchievementUnlockEntity>>

    @Query(
        """
        SELECT * FROM reward_presentation_receipts
        WHERE actorId = :actorId
          AND state IN ('pending', 'ready', 'interrupted')
          AND (expiresAtEpochMillis IS NULL OR expiresAtEpochMillis > :nowEpochMillis)
        ORDER BY priority DESC, createdAtEpochMillis ASC, receiptId ASC
        """
    )
    fun observePendingPresentationReceipts(
        actorId: String,
        nowEpochMillis: Long
    ): Flow<List<RewardPresentationReceiptEntity>>

    @Query(
        """
        UPDATE reward_presentation_receipts
        SET state = :newState,
            updatedAtEpochMillis = :updatedAtEpochMillis,
            acknowledgedAtEpochMillis = CASE
                WHEN :acknowledge = 1 THEN :updatedAtEpochMillis
                ELSE acknowledgedAtEpochMillis
            END,
            suppressionReason = COALESCE(:suppressionReason, suppressionReason)
        WHERE receiptId = :receiptId AND state IN (:allowedCurrentStates)
        """
    )
    suspend fun transitionPresentationReceipt(
        receiptId: String,
        allowedCurrentStates: List<String>,
        newState: String,
        updatedAtEpochMillis: Long,
        acknowledge: Boolean,
        suppressionReason: String?
    ): Int

    @Query(
        """
        UPDATE reward_presentation_receipts
        SET state = 'expired', updatedAtEpochMillis = :nowEpochMillis
        WHERE expiresAtEpochMillis IS NOT NULL
          AND expiresAtEpochMillis <= :nowEpochMillis
          AND state IN ('pending', 'ready', 'interrupted')
        """
    )
    suspend fun expirePresentationReceipts(nowEpochMillis: Long): Int
}

private const val MAX_XP_PER_STUDY_DAY = 160L
private const val CATALOG_RULE_PREFIX = "catalog."
private const val JOURNEY_RULE_PREFIX = "journey."

/**
 * Advances at most one stage for this event. This function runs inside
 * [GamificationDao.recordRewardAtomically], so progress, reward, receipt, and balance either all
 * commit or all roll back.
 */
private suspend fun GamificationDao.settleActiveJourneyStage(
    event: GamificationEventEntity,
    request: JourneySettlementRequest,
    evidence: suspend (CatalogEvidenceMetric) -> Long
): JourneySettlementResult {
    val plannedInstance = request.instance ?: return JourneySettlementResult()
    val instance = journeyInstanceById(plannedInstance.journeyInstanceId)
        ?: error("Journey initialization did not persist")
    if (instance.state != "active") return JourneySettlementResult()

    val before = activeJourneyStageOnce(instance.journeyInstanceId)
        ?: error("Active journey has no active stage")
    check(before.stageOrder == instance.currentStageOrder) {
        "Journey stage and instance pointers diverged"
    }
    val candidate = checkNotNull(
        request.stages.singleOrNull { it.stage.stageProgressId == before.stageProgressId }
    ) { "Active journey stage is missing from the versioned settlement request" }

    val resolvedProgress = evidence(candidate.metric).coerceIn(0L, before.target)
    check(
        setJourneyStageProgressAtLeast(
            stageProgressId = before.stageProgressId,
            actorId = event.actorId,
            progress = resolvedProgress,
            updatedAtEpochMillis = event.occurredAtEpochMillis
        ) == 1
    ) { "Active journey stage could not absorb evidence" }
    val progressed = checkNotNull(journeyStageById(before.stageProgressId))
    val completedNow = completeJourneyStageOnce(
        stageProgressId = progressed.stageProgressId,
        actorId = event.actorId,
        completionEventId = event.eventId,
        completedAtEpochMillis = event.occurredAtEpochMillis
    ) == 1
    val after = checkNotNull(journeyStageById(before.stageProgressId))
    if (after.progress == before.progress && !completedNow) return JourneySettlementResult()

    var journeyCompleted = false
    if (completedNow) {
        val nextOrder = after.stageOrder + 1
        val nextCandidate = request.stages.singleOrNull { it.stage.stageOrder == nextOrder }
        if (nextCandidate == null) {
            check(
                completeJourneyInstanceOnce(
                    journeyInstanceId = instance.journeyInstanceId,
                    actorId = event.actorId,
                    finalStageOrder = after.stageOrder,
                    completionEventId = event.eventId,
                    completedAtEpochMillis = event.occurredAtEpochMillis
                ) == 1
            ) { "Final journey stage completed without closing its journey" }
            journeyCompleted = true
        } else {
            check(
                activateJourneyStageOnce(
                    journeyInstanceId = instance.journeyInstanceId,
                    actorId = event.actorId,
                    stageOrder = nextOrder,
                    updatedAtEpochMillis = event.occurredAtEpochMillis
                ) == 1
            ) { "Next journey stage could not be activated" }
            check(
                advanceJourneyInstanceOnce(
                    journeyInstanceId = instance.journeyInstanceId,
                    actorId = event.actorId,
                    completedStageOrder = after.stageOrder,
                    nextStageOrder = nextOrder,
                    updatedAtEpochMillis = event.occurredAtEpochMillis
                ) == 1
            ) { "Journey pointer could not advance to the next stage" }
        }
    }

    val settledStage = SettledJourneyStage(
        journeyInstanceId = after.journeyInstanceId,
        stageDefinitionId = after.stageDefinitionId,
        stageOrder = after.stageOrder,
        from = before.progress,
        to = after.progress,
        target = after.target,
        completed = completedNow,
        journeyCompleted = journeyCompleted
    )
    if (!completedNow) return JourneySettlementResult(stage = settledStage)

    val receipt = candidate.presentationReceipt.copy(
        immutableSummaryJson = "{\"journeyInstanceId\":${jsonString(after.journeyInstanceId)}," +
            "\"stageDefinitionId\":${jsonString(after.stageDefinitionId)}," +
            "\"stageOrder\":${after.stageOrder},\"xpAwarded\":${after.rewardXp}," +
            "\"journeyCompleted\":$journeyCompleted}"
    )
    return JourneySettlementResult(
        transactions = listOf(candidate.rewardTransaction),
        receipts = listOf(receipt),
        stage = settledStage
    )
}

private fun validateRewardCommit(
    event: GamificationEventEntity,
    transactions: List<RewardTransactionEntity>,
    summary: RewardSummaryEntity,
    presentationReceipts: List<RewardPresentationReceiptEntity>
) {
    require(event.eventId.isNotBlank()) { "eventId must not be blank" }
    require(event.actorId.isNotBlank()) { "actorId must not be blank" }
    require(event.idempotencyKey.isNotBlank()) { "event idempotencyKey must not be blank" }
    require(event.ruleVersion > 0) { "ruleVersion must be positive" }
    require(event.occurredAtEpochMillis >= 0L) { "occurredAtEpochMillis must not be negative" }
    require(event.recordedAtEpochMillis >= 0L) { "recordedAtEpochMillis must not be negative" }
    require(event.studyDay >= 0L) { "studyDay must not be negative" }
    require(summary.eventId == event.eventId && summary.actorId == event.actorId) {
        "Reward summary must belong to the committed event and actor"
    }
    require(summary.ruleVersion == event.ruleVersion) {
        "Reward summary and event must use the same rule version"
    }
    require(transactions.map { it.transactionId }.toSet().size == transactions.size) {
        "Duplicate transaction IDs in one commit"
    }
    require(presentationReceipts.map { it.receiptId }.toSet().size == presentationReceipts.size) {
        "Duplicate presentation receipt IDs in one commit"
    }

    val transactionIds = transactions.mapTo(mutableSetOf()) { it.transactionId }
    transactions.forEach { transaction ->
        require(transaction.eventId == event.eventId && transaction.actorId == event.actorId) {
            "Every reward transaction must belong to the committed event and actor"
        }
        require(transaction.ruleVersion == event.ruleVersion) {
            "Reward transaction and event must use the same rule version"
        }
        require(transaction.amount != 0L) { "Zero-value ledger transactions are not allowed" }
        require(transaction.currency.isNotBlank()) { "Reward currency must not be blank" }
        require(transaction.idempotencyKey.isNotBlank()) {
            "Reward transaction idempotencyKey must not be blank"
        }
    }

    if (event.eventType != "legacy_progress_imported") {
        val settledXp = transactions.asSequence()
            .filter { transaction ->
                transaction.currency == "xp" &&
                    transaction.transactionKind == "grant" &&
                    transaction.amount > 0L
            }
            .sumOf(RewardTransactionEntity::amount)
        require(summary.xpAwarded == settledXp) {
            "Reward summary XP must equal the committed positive XP grants"
        }
    }

    presentationReceipts.forEach { receipt ->
        require(receipt.sourceEventId == event.eventId && receipt.actorId == event.actorId) {
            "Every presentation receipt must belong to the committed event and actor"
        }
        require(receipt.idempotencyKey.isNotBlank()) {
            "Presentation receipt idempotencyKey must not be blank"
        }
        require(receipt.coalescedCount > 0) { "coalescedCount must be positive" }
        require(
            receipt.sourceTransactionId == null || receipt.sourceTransactionId in transactionIds
        ) {
            "Presentation receipt references a transaction outside this event commit"
        }
    }
}

private fun validateCatalogSettlement(
    event: GamificationEventEntity,
    request: CatalogSettlementRequest
) {
    require(request.unlocks.map { it.unlock.unlockId }.toSet().size == request.unlocks.size) {
        "Duplicate catalog unlock IDs in one settlement"
    }
    require(request.quests.map { it.instance.questInstanceId }.toSet().size == request.quests.size) {
        "Duplicate quest instance IDs in one settlement"
    }
    request.unlocks.forEach { candidate ->
        require(candidate.unlock.actorId == event.actorId) {
            "Catalog unlock must belong to the committed actor"
        }
        require(candidate.unlock.sourceEventId == event.eventId) {
            "Catalog unlock must cite the committed event"
        }
        require(candidate.target >= 0L) { "Catalog unlock target must not be negative" }
        candidate.xpTransaction?.let { transaction ->
            require(transaction.eventId == event.eventId && transaction.actorId == event.actorId) {
                "Catalog transaction must belong to the committed event and actor"
            }
            require(transaction.ruleId.startsWith(CATALOG_RULE_PREFIX)) {
                "Catalog transaction rule IDs must use the catalog namespace"
            }
            require(transaction.currency == "xp" && transaction.amount > 0L) {
                "Catalog XP transactions must be positive XP grants"
            }
        }
        candidate.presentationReceipt?.let { receipt ->
            require(receipt.sourceEventId == event.eventId && receipt.actorId == event.actorId) {
                "Catalog presentation must belong to the committed event and actor"
            }
            require(
                receipt.sourceTransactionId == null ||
                    receipt.sourceTransactionId == candidate.xpTransaction?.transactionId
            ) { "Catalog presentation references an unrelated transaction" }
        }
    }
    request.quests.forEach { candidate ->
        require(candidate.instance.actorId == event.actorId) {
            "Quest instance must belong to the committed actor"
        }
        require(candidate.instance.target > 0L) { "Quest target must be positive" }
        require(candidate.instance.startsAtEpochMillis < candidate.instance.endsAtEpochMillis) {
            "Quest window must have a positive duration"
        }
    }
}

private fun validateCampaignRouteChoice(
    event: GamificationEventEntity,
    request: CampaignRouteChoiceRequest
) {
    val choice = request.choice
    if (choice == null) {
        require(request.prerequisiteJourneyDefinitionId == null) {
            "Campaign prerequisite requires a route choice"
        }
        require(request.prerequisiteStageDefinitionId == null) {
            "Campaign prerequisite stage requires a route choice"
        }
        require(request.prerequisiteJourneyDefinitionVersion == null) {
            "Campaign prerequisite version requires a route choice"
        }
        require(request.selectedRouteJourney.instance == null && request.selectedRouteJourney.stages.isEmpty()) {
            "Selected route journey requires a route choice"
        }
        return
    }

    require(choice.actorId == event.actorId && choice.sourceEventId == event.eventId) {
        "Campaign choice must belong to the committed event and actor"
    }
    require(
        choice.choiceId.isNotBlank() &&
            choice.campaignDefinitionId.isNotBlank() &&
            choice.campaignDefinitionVersion > 0 &&
            choice.routeId.isNotBlank() &&
            choice.journeyDefinitionId.isNotBlank()
    ) { "Campaign choice identifiers and version must be complete" }
    require(choice.chosenAtEpochMillis == event.occurredAtEpochMillis) {
        "Campaign choice timestamp must match its event"
    }
    require(
        !request.prerequisiteJourneyDefinitionId.isNullOrBlank() &&
            request.prerequisiteJourneyDefinitionVersion != null &&
            request.prerequisiteJourneyDefinitionVersion > 0 &&
            !request.prerequisiteStageDefinitionId.isNullOrBlank()
    ) { "Campaign choice requires an authored journey prerequisite" }

    val routeJourney = checkNotNull(request.selectedRouteJourney.instance) {
        "Campaign choice requires its selected route journey"
    }
    require(routeJourney.actorId == choice.actorId) {
        "Campaign route journey belongs to another actor"
    }
    require(routeJourney.definitionId == choice.journeyDefinitionId) {
        "Campaign choice and route journey definitions diverged"
    }
    require(!request.selectedRouteJourney.settleEvidenceOnThisEvent) {
        "Campaign route selection may initialize its journey but cannot settle evidence"
    }
}

private fun validateJourneySettlement(
    event: GamificationEventEntity,
    request: JourneySettlementRequest
) {
    val instance = request.instance
    if (instance == null) {
        require(request.stages.isEmpty()) { "Journey stages require a journey instance" }
        return
    }
    require(instance.actorId == event.actorId) {
        "Journey instance must belong to the committed actor"
    }
    require(instance.definitionVersion > 0 && instance.currentStageOrder == 1) {
        "New journey instances must start at a positive version and stage one"
    }
    require(instance.state == "active") { "New journey instance must start active" }
    require(request.stages.isNotEmpty()) { "Journey instance requires authored stages" }
    require(request.stages.map { it.stage.stageProgressId }.distinct().size == request.stages.size) {
        "Duplicate journey stage IDs in one settlement"
    }
    require(request.stages.map { it.stage.stageDefinitionId }.distinct().size == request.stages.size) {
        "Duplicate journey stage definitions in one settlement"
    }
    require(request.stages.map { it.stage.stageOrder } == (1..request.stages.size).toList()) {
        "Journey stage order must be contiguous and start at one"
    }

    request.stages.forEachIndexed { index, candidate ->
        val stage = candidate.stage
        require(stage.actorId == event.actorId && stage.journeyInstanceId == instance.journeyInstanceId) {
            "Journey stage must belong to the committed actor and journey"
        }
        require(stage.target > 0L && stage.rewardXp > 0L) {
            "Journey stage target and reward must be positive"
        }
        require(stage.progress == 0L && stage.completionEventId == null) {
            "Journey settlement candidates must be pristine definitions"
        }
        require(stage.state == if (index == 0) "active" else "locked") {
            "Only the first newly authored journey stage may start active"
        }
        require(stage.evidenceMetric == candidate.metric.name.lowercase(Locale.ROOT)) {
            "Journey evidence metric wire value differs from its settlement metric"
        }

        val transaction = candidate.rewardTransaction
        require(transaction.eventId == event.eventId && transaction.actorId == event.actorId) {
            "Journey transaction must belong to the committed event and actor"
        }
        require(transaction.ruleId.startsWith(JOURNEY_RULE_PREFIX)) {
            "Journey transaction rule IDs must use the journey namespace"
        }
        require(
            transaction.currency == "xp" &&
                transaction.transactionKind == "grant" &&
                transaction.amount == stage.rewardXp
        ) { "Journey transaction must grant exactly the authored stage XP" }

        val receipt = candidate.presentationReceipt
        require(receipt.sourceEventId == event.eventId && receipt.actorId == event.actorId) {
            "Journey presentation must belong to the committed event and actor"
        }
        require(receipt.sourceTransactionId == transaction.transactionId) {
            "Journey presentation must reference its stage transaction"
        }
        require(receipt.presentationFamily.startsWith("presentation.journey.")) {
            "Journey presentation must use the journey family"
        }
    }
}

private fun RewardSummaryEntity.withCatalogSettlement(
    settlement: CatalogSettlementResult
): RewardSummaryEntity {
    if (
        settlement.transactions.isEmpty() &&
        settlement.receipts.isEmpty() &&
        settlement.unlockedItems.isEmpty() &&
        settlement.unlockedContentIds.isEmpty() &&
        settlement.questProgress.isEmpty()
    ) {
        return this
    }

    val additionalXp = settlement.transactions.asSequence()
        .filter { transaction ->
            transaction.currency == "xp" &&
                transaction.transactionKind == "grant" &&
                transaction.amount > 0L
        }
        .sumOf(RewardTransactionEntity::amount)
    val totalXp = Math.addExact(xpAwarded, additionalXp)
    val questJson = settlement.questProgress.joinToString(prefix = "[", postfix = "]") { progress ->
        "{\"questInstanceId\":${jsonString(progress.questInstanceId)},\"from\":${progress.from},\"to\":${progress.to},\"target\":${progress.target},\"completed\":${progress.completed}}"
    }
    val achievementJson = settlement.unlockedItems.joinToString(prefix = "[", postfix = "]") { (id, level) ->
        "{\"id\":${jsonString(id)},\"level\":$level}"
    }
    val contentJson = settlement.unlockedContentIds.joinToString(prefix = "[", postfix = "]", transform = ::jsonString)
    val settledTier = when {
        settlement.unlockedItems.isNotEmpty() -> maxCelebrationTier(celebrationTier, "milestone")
        settlement.questProgress.any(SettledQuestProgress::completed) ->
            maxCelebrationTier(celebrationTier, "standard")
        else -> celebrationTier
    }

    return copy(
        xpAwarded = totalXp,
        currencyRewardsJson = if (totalXp > 0L) {
            "[{\"currency\":\"xp\",\"amount\":$totalXp}]"
        } else {
            currencyRewardsJson
        },
        questProgressJson = questJson,
        achievementIdsJson = achievementJson,
        unlockedContentJson = contentJson,
        celebrationTier = settledTier,
        summaryJson = "{\"xpAwarded\":$totalXp,\"catalogUnlockCount\":${settlement.unlockedItems.size},\"questProgressCount\":${settlement.questProgress.size}}"
    )
}

private fun RewardSummaryEntity.withJourneySettlement(
    settlement: JourneySettlementResult
): RewardSummaryEntity {
    val stage = settlement.stage ?: return this
    val additionalXp = settlement.transactions.asSequence()
        .filter { transaction ->
            transaction.currency == "xp" &&
                transaction.transactionKind == "grant" &&
                transaction.amount > 0L
        }
        .sumOf(RewardTransactionEntity::amount)
    val totalXp = Math.addExact(xpAwarded, additionalXp)
    val tier = if (stage.completed) {
        maxCelebrationTier(celebrationTier, "milestone")
    } else {
        celebrationTier
    }
    return copy(
        xpAwarded = totalXp,
        currencyRewardsJson = if (totalXp > 0L) {
            "[{\"currency\":\"xp\",\"amount\":$totalXp}]"
        } else {
            currencyRewardsJson
        },
        celebrationTier = tier,
        summaryJson = "{\"xpAwarded\":$totalXp,\"journey\":{" +
            "\"journeyInstanceId\":${jsonString(stage.journeyInstanceId)}," +
            "\"stageDefinitionId\":${jsonString(stage.stageDefinitionId)}," +
            "\"stageOrder\":${stage.stageOrder},\"from\":${stage.from}," +
            "\"to\":${stage.to},\"target\":${stage.target}," +
            "\"completed\":${stage.completed}," +
            "\"journeyCompleted\":${stage.journeyCompleted}}," +
            "\"base\":$summaryJson}"
    )
}

private fun maxCelebrationTier(first: String, second: String): String {
    val order = listOf("none", "micro", "standard", "milestone", "showpiece")
    return if (order.indexOf(first) >= order.indexOf(second)) first else second
}

private fun weeksWithAtLeastThreeStudyDays(studyDays: List<Long>): Int = studyDays
    .distinct()
    .groupBy(::isoWeekKey)
    .count { (_, days) -> days.size >= 3 }

private fun isoWeekKey(epochDay: Long): String {
    val calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT).apply {
        firstDayOfWeek = Calendar.MONDAY
        minimalDaysInFirstWeek = 4
        timeInMillis = Math.multiplyExact(epochDay, 86_400_000L)
    }
    return "${calendar.weekYear}-${calendar.get(Calendar.WEEK_OF_YEAR)}"
}

private fun jsonString(value: String): String = buildString(value.length + 2) {
    append('"')
    value.forEach { character ->
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (character.code < 0x20) {
                append("\\u")
                append(character.code.toString(16).padStart(4, '0'))
            } else {
                append(character)
            }
        }
    }
    append('"')
}
