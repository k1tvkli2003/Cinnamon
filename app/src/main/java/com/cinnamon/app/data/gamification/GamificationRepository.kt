package com.cinnamon.app.data.gamification

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.cinnamon.app.data.local.AppDatabase
import com.cinnamon.app.data.local.AchievementUnlockEntity
import com.cinnamon.app.data.local.DailyRewardCapExceededException
import com.cinnamon.app.data.local.GamificationEventEntity
import com.cinnamon.app.data.local.LearningActivityRow
import com.cinnamon.app.data.local.LearningFocusAlreadySelectedException
import com.cinnamon.app.data.local.LearningFocusSelectionEntity
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
import com.cinnamon.app.domain.gamification.FoundationLearningFocusCatalog
import com.cinnamon.app.domain.gamification.GamificationCatalogBundle
import com.cinnamon.app.domain.gamification.JourneyDefinition
import com.cinnamon.app.domain.gamification.LearningFocusDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
    val occurredAtEpochMillis: Long = System.currentTimeMillis()
) {
    init {
        require(subjectType.isNotBlank()) { "subjectType must not be blank" }
        require(subjectId.isNotBlank()) { "subjectId must not be blank" }
        require(occurrenceKey.isNotBlank()) { "occurrenceKey must not be blank" }
        require(completedItemCount >= 0) { "completedItemCount must not be negative" }
        require(occurredAtEpochMillis >= 0L) { "occurredAtEpochMillis must not be negative" }
        if (eventType == RewardableEventType.MISTAKE_RECORDED) {
            require(source == LearningEventSource.REVIEW && completedItemCount == 1) {
                "Mistake evidence must come from one committed review attempt"
            }
        }
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

    /** Unique review subjects with a Room-verified mistake → later correction chain. */
    val repairedSubjectCount: Flow<Int> =
        dao.observeDistinctVerifiedRepairSubjectCount(LOCAL_ACTOR_ID)

    /** Pairs whose distinction survived a real 24-hour delayed return. */
    val resolvedConfusablePairCount: Flow<Int> =
        dao.observeDistinctVerifiedConfusablePairCount(LOCAL_ACTOR_ID)

    /** First verified use of a real bundled lexicon entry in an authored context. */
    val verifiedContextApplicationCount: Flow<Int> =
        dao.observeDistinctVerifiedContextApplicationCount(LOCAL_ACTOR_ID)

    /** Items recalled successfully after a Room-verified three-day interval. */
    val delayedRecallCount: Flow<Int> =
        dao.observeDistinctVerifiedDelayedRecallCount(LOCAL_ACTOR_ID)

    /** One substantial return after a real seven-day learner absence. */
    val verifiedComebackSessionCount: Flow<Int> =
        dao.observeVerifiedComebackSessionCount(LOCAL_ACTOR_ID)

    /** Distinct bookmarked terms whose later successful review is ledger-verified. */
    val verifiedSavedItemCount: Flow<Int> =
        dao.observeDistinctVerifiedSavedItemCount(LOCAL_ACTOR_ID)

    /** Distinct local days where a Room-snapshotted queue of five or more reached zero. */
    val verifiedReviewQueueClearDayCount: Flow<Int> =
        dao.observeVerifiedReviewQueueClearDayCount(LOCAL_ACTOR_ID)

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

    val learningFocusSelections: Flow<List<LearningFocusSelectionRecord>> =
        dao.observeLearningFocusSelections(LOCAL_ACTOR_ID).map { selections ->
            val definition = FoundationLearningFocusCatalog.definition
            selections.map { selection ->
                LegacyLearningFocusWireCompatibility.toRecord(definition, selection)
            }
        }

    val achievementUnlocks: Flow<List<AchievementUnlockEntity>> =
        dao.observeAchievementUnlocks(LOCAL_ACTOR_ID)

    suspend fun record(command: LearningEventCommand): RewardWriteResult {
        require(
                command.eventType != RewardableEventType.MISTAKE_CORRECTED &&
                command.eventType != RewardableEventType.CONFUSABLE_PAIR_RESOLVED &&
                command.eventType != RewardableEventType.CONTEXT_APPLICATION_VERIFIED &&
                command.eventType != RewardableEventType.DELAYED_RECALL_SUCCEEDED &&
                command.eventType != RewardableEventType.COMEBACK_SESSION_COMPLETED &&
                command.eventType != RewardableEventType.SAVED_ITEM_REVIEWED &&
                command.eventType != RewardableEventType.REVIEW_QUEUE_OPENED &&
                command.eventType != RewardableEventType.REVIEW_QUEUE_CLEARED
        ) {
            "Verified outcomes must be derived from their persisted learning evidence"
        }
        return recordInternal(command, repairOfEventId = null)
    }

    /** The bookmark must correspond to a currently persisted local lexicon item. */
    suspend fun recordBookmarkSaved(
        lexiconEntryId: Long,
        occurrenceKey: String,
        occurredAtEpochMillis: Long
    ): RewardWriteResult? = database.withTransaction {
        val entry = lexiconDao.entryByIdOnce(lexiconEntryId) ?: return@withTransaction null
        if (!entry.isBookmarked) return@withTransaction null
        recordInternal(
            command = LearningEventCommand(
                eventType = RewardableEventType.BOOKMARK_SAVED,
                subjectType = CONTEXT_SUBJECT_TYPE,
                subjectId = entry.id.toString(),
                occurrenceKey = occurrenceKey,
                completedItemCount = 0,
                source = LearningEventSource.PRACTICE,
                occurredAtEpochMillis = occurredAtEpochMillis
            ),
            repairOfEventId = null
        )
    }

    /**
     * Produces one saved-item outcome only after a real bookmark has remained long enough and the
     * same currently saved lexicon entry receives a successful review.
     */
    suspend fun recordVerifiedSavedItemReview(
        lexiconEntryId: Long,
        occurrenceKey: String,
        occurredAtEpochMillis: Long
    ): RewardWriteResult? = database.withTransaction {
        val entry = lexiconDao.entryByIdOnce(lexiconEntryId) ?: return@withTransaction null
        if (!entry.isBookmarked || dao.hasEventForSubject(
                actorId = LOCAL_ACTOR_ID,
                eventType = RewardableEventType.SAVED_ITEM_REVIEWED.wireName,
                subjectType = CONTEXT_SUBJECT_TYPE,
                subjectId = entry.id.toString()
            )
        ) return@withTransaction null
        val bookmark = dao.latestBookmarkSavedBeforeOnce(
            actorId = LOCAL_ACTOR_ID,
            subjectType = CONTEXT_SUBJECT_TYPE,
            subjectId = entry.id.toString(),
            beforeEpochMillis = occurredAtEpochMillis
        ) ?: return@withTransaction null
        if (occurredAtEpochMillis < bookmark.occurredAtEpochMillis + SAVED_ITEM_DELAY_MILLIS) {
            return@withTransaction null
        }
        recordInternal(
            command = LearningEventCommand(
                eventType = RewardableEventType.SAVED_ITEM_REVIEWED,
                subjectType = CONTEXT_SUBJECT_TYPE,
                subjectId = entry.id.toString(),
                occurrenceKey = occurrenceKey,
                completedItemCount = 1,
                source = LearningEventSource.REVIEW,
                occurredAtEpochMillis = occurredAtEpochMillis
            ),
            repairOfEventId = bookmark.eventId
        )
    }

    /**
     * Captures the authoritative due count when the learner enters Review. One stable opening per
     * local day keeps retries and multiple UI instances from producing ledger noise.
     */
    suspend fun recordReviewQueueOpened(
        occurredAtEpochMillis: Long = System.currentTimeMillis()
    ): RewardWriteResult? = database.withTransaction {
        val startingDueCount = lexiconDao.dueCountOnce(occurredAtEpochMillis)
        if (startingDueCount < MIN_REVIEW_QUEUE_SIZE) return@withTransaction null
        val studyDay = ProgressStore.localEpochDay(occurredAtEpochMillis)
        recordInternal(
            command = LearningEventCommand(
                eventType = RewardableEventType.REVIEW_QUEUE_OPENED,
                subjectType = REVIEW_QUEUE_SUBJECT_TYPE,
                subjectId = studyDay.toString(),
                occurrenceKey = "review_queue_open_v1:$studyDay",
                completedItemCount = 0,
                source = LearningEventSource.REVIEW,
                occurredAtEpochMillis = occurredAtEpochMillis
            ),
            repairOfEventId = null,
            reviewQueueStartingDueCount = startingDueCount
        )
    }

    /**
     * Emits a queue-clear outcome only after Room proves both ends of the relationship: the
     * same-day opening captured at least five due items and the current due count is exactly zero.
     */
    suspend fun recordVerifiedReviewQueueCleared(
        occurredAtEpochMillis: Long
    ): RewardWriteResult? = database.withTransaction {
        val finalDueCount = lexiconDao.dueCountOnce(occurredAtEpochMillis)
        if (finalDueCount != 0) return@withTransaction null
        val studyDay = ProgressStore.localEpochDay(occurredAtEpochMillis)
        val opening = dao.verifiedReviewQueueOpeningForStudyDayOnce(
            actorId = LOCAL_ACTOR_ID,
            studyDay = studyDay,
            throughEpochMillis = occurredAtEpochMillis
        ) ?: return@withTransaction null
        if (opening.startingDueCount < MIN_REVIEW_QUEUE_SIZE) return@withTransaction null

        recordInternal(
            command = LearningEventCommand(
                eventType = RewardableEventType.REVIEW_QUEUE_CLEARED,
                subjectType = REVIEW_QUEUE_SUBJECT_TYPE,
                subjectId = studyDay.toString(),
                occurrenceKey = "review_queue_clear_v1:$studyDay",
                completedItemCount = opening.startingDueCount,
                source = LearningEventSource.REVIEW,
                occurredAtEpochMillis = occurredAtEpochMillis
            ),
            repairOfEventId = opening.eventId,
            reviewQueueStartingDueCount = opening.startingDueCount,
            reviewQueueFinalDueCount = finalDueCount
        )
    }

    /**
     * Persists a normal, idempotent practice session and only then derives a Gentle Return outcome
     * from the immutable prior-activity ledger. New learners and ordinary next-day sessions stay
     * ordinary; a second activity after the return cannot re-trigger the same absence window.
     */
    suspend fun recordPracticeSessionWithComeback(
        command: LearningEventCommand
    ): RewardWriteResult = database.withTransaction {
        require(command.eventType == RewardableEventType.PRACTICE_SESSION_COMPLETED) {
            "Comeback derivation is only valid for a completed practice session"
        }
        require(command.source == LearningEventSource.GAME && command.completedItemCount >= 3) {
            "Comeback derivation requires a verified three-action game session"
        }
        val priorActivity = dao.latestMeaningfulActivityBeforeOnce(
            actorId = LOCAL_ACTOR_ID,
            beforeEpochMillis = command.occurredAtEpochMillis
        )
        val practiceWrite = recordInternal(command, repairOfEventId = null)
        if (
            practiceWrite.status == RewardWriteStatus.DUPLICATE ||
            priorActivity == null ||
            command.occurredAtEpochMillis < priorActivity.occurredAtEpochMillis + COMEBACK_ABSENCE_MILLIS
        ) {
            return@withTransaction practiceWrite
        }
        recordInternal(
            command = LearningEventCommand(
                eventType = RewardableEventType.COMEBACK_SESSION_COMPLETED,
                subjectType = COMEBACK_SUBJECT_TYPE,
                subjectId = command.occurrenceKey,
                occurrenceKey = "${command.occurrenceKey}:comeback_v1",
                completedItemCount = command.completedItemCount,
                source = LearningEventSource.GAME,
                occurredAtEpochMillis = command.occurredAtEpochMillis
            ),
            repairOfEventId = priorActivity.eventId,
            comebackMeaningfulActionCount = command.completedItemCount
        )
        practiceWrite
    }

    /**
     * Records one correction only when Room can link it to an earlier, still-unrepaired mistake for
     * the same subject. The query and write share one transaction, so concurrent retries cannot
     * turn one mistake into multiple repair evidence events.
     */
    suspend fun recordVerifiedMistakeCorrection(
        subjectType: String,
        subjectId: String,
        occurrenceKey: String,
        source: LearningEventSource,
        occurredAtEpochMillis: Long
    ): RewardWriteResult? = database.withTransaction {
        val mistake = dao.latestUnrepairedMistakeBeforeOnce(
            actorId = LOCAL_ACTOR_ID,
            subjectType = subjectType,
            subjectId = subjectId,
            beforeEpochMillis = occurredAtEpochMillis
        ) ?: return@withTransaction null

        recordInternal(
            command = LearningEventCommand(
                eventType = RewardableEventType.MISTAKE_CORRECTED,
                subjectType = subjectType,
                subjectId = subjectId,
                occurrenceKey = occurrenceKey,
                completedItemCount = 1,
                source = source,
                occurredAtEpochMillis = occurredAtEpochMillis
            ),
            repairOfEventId = mistake.eventId
        )
    }

    /**
     * A first correct discrimination opens a pair-specific recall challenge. A later correct
     * answer can resolve it only after 24 hours and only by linking the immutable first attempt.
     * Repeated taps before that boundary create no new evidence or reward surface.
     */
    suspend fun recordConfusablePairSuccess(
        confusablePairId: String,
        occurrenceKey: String,
        occurredAtEpochMillis: Long
    ): RewardWriteResult? = database.withTransaction {
        val attempt = dao.latestUnresolvedConfusableAttemptBeforeOnce(
            actorId = LOCAL_ACTOR_ID,
            subjectType = CONFUSABLE_SUBJECT_TYPE,
            subjectId = confusablePairId,
            beforeEpochMillis = occurredAtEpochMillis
        )
        if (attempt == null) {
            return@withTransaction recordInternal(
                command = LearningEventCommand(
                    eventType = RewardableEventType.CONFUSABLE_PAIR_ATTEMPTED,
                    subjectType = CONFUSABLE_SUBJECT_TYPE,
                    subjectId = confusablePairId,
                    occurrenceKey = occurrenceKey,
                    completedItemCount = 1,
                    source = LearningEventSource.GAME,
                    occurredAtEpochMillis = occurredAtEpochMillis
                ),
                repairOfEventId = null
            )
        }
        if (occurredAtEpochMillis < attempt.occurredAtEpochMillis + CONFUSABLE_DELAY_MILLIS) {
            return@withTransaction null
        }
        recordInternal(
            command = LearningEventCommand(
                eventType = RewardableEventType.CONFUSABLE_PAIR_RESOLVED,
                subjectType = CONFUSABLE_SUBJECT_TYPE,
                subjectId = confusablePairId,
                occurrenceKey = occurrenceKey,
                completedItemCount = 1,
                source = LearningEventSource.GAME,
                occurredAtEpochMillis = occurredAtEpochMillis
            ),
            repairOfEventId = attempt.eventId
        )
    }

    /**
     * The Cloze surface may ask for this event only after it has evaluated its authored answer.
     * Room confirms that the cited item is a bundled lexicon entry; generic callers cannot forge
     * a context application through [record].
     */
    suspend fun recordVerifiedContextApplication(
        lexiconEntryId: Long,
        occurrenceKey: String,
        occurredAtEpochMillis: Long = System.currentTimeMillis()
    ): RewardWriteResult? = database.withTransaction {
        val entry = lexiconDao.entryByIdOnce(lexiconEntryId) ?: return@withTransaction null
        recordInternal(
            command = LearningEventCommand(
                eventType = RewardableEventType.CONTEXT_APPLICATION_VERIFIED,
                subjectType = CONTEXT_SUBJECT_TYPE,
                subjectId = entry.id.toString(),
                occurrenceKey = occurrenceKey,
                completedItemCount = 1,
                source = LearningEventSource.GAME,
                occurredAtEpochMillis = occurredAtEpochMillis
            ),
            repairOfEventId = null,
            contextApplicationVerified = true
        )
    }

    /**
     * Builds one durable-recall evidence event only from a prior committed successful review of
     * the same lexicon entry. A client timestamp or a handcrafted metadata flag is insufficient.
     */
    suspend fun recordVerifiedDelayedRecall(
        lexiconEntryId: Long,
        occurrenceKey: String,
        occurredAtEpochMillis: Long
    ): RewardWriteResult? = database.withTransaction {
        val entry = lexiconDao.entryByIdOnce(lexiconEntryId) ?: return@withTransaction null
        if (dao.hasEventForSubject(
                actorId = LOCAL_ACTOR_ID,
                eventType = RewardableEventType.DELAYED_RECALL_SUCCEEDED.wireName,
                subjectType = CONTEXT_SUBJECT_TYPE,
                subjectId = entry.id.toString()
            )
        ) return@withTransaction null
        val priorReview = dao.latestSuccessfulReviewBeforeOnce(
            actorId = LOCAL_ACTOR_ID,
            subjectType = CONTEXT_SUBJECT_TYPE,
            subjectId = entry.id.toString(),
            beforeEpochMillis = occurredAtEpochMillis
        ) ?: return@withTransaction null
        if (occurredAtEpochMillis < priorReview.occurredAtEpochMillis + DELAYED_RECALL_MILLIS) {
            return@withTransaction null
        }
        recordInternal(
            command = LearningEventCommand(
                eventType = RewardableEventType.DELAYED_RECALL_SUCCEEDED,
                subjectType = CONTEXT_SUBJECT_TYPE,
                subjectId = entry.id.toString(),
                occurrenceKey = occurrenceKey,
                completedItemCount = 1,
                source = LearningEventSource.REVIEW,
                occurredAtEpochMillis = occurredAtEpochMillis
            ),
            repairOfEventId = priorReview.eventId
        )
    }

    private suspend fun recordInternal(
        command: LearningEventCommand,
        repairOfEventId: String?,
        contextApplicationVerified: Boolean = false,
        comebackMeaningfulActionCount: Int = 0,
        reviewQueueStartingDueCount: Int = 0,
        reviewQueueFinalDueCount: Int = -1
    ): RewardWriteResult {
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
                    meaningful = command.isMeaningful(
                        hasVerifiedRepairLink = repairOfEventId != null,
                        reviewQueueStartingDueCount = reviewQueueStartingDueCount,
                        reviewQueueFinalDueCount = reviewQueueFinalDueCount
                    ),
                    firstCompletion = firstCompletion,
                    uniqueSubjectInWindow = uniqueSubjectToday,
                    correctedAfterMistake =
                        command.eventType == RewardableEventType.MISTAKE_CORRECTED &&
                            repairOfEventId != null,
                    completedItemCount = command.completedItemCount
                )
            )

            try {
                val event = command.toEventEntity(
                    eventIdempotencyKey = eventIdempotencyKey,
                    studyDay = studyDay,
                    repairOfEventId = repairOfEventId,
                    contextApplicationVerified = contextApplicationVerified,
                    comebackMeaningfulActionCount = comebackMeaningfulActionCount,
                    reviewQueueStartingDueCount = reviewQueueStartingDueCount,
                    reviewQueueFinalDueCount = reviewQueueFinalDueCount
                )
                val settlement = CatalogSettlementPlanner.plan(
                    bundle = catalogRepository.bundle,
                    event = event,
                    dueItemCountAtAssignment = lexiconDao.dueCountOnce(command.occurredAtEpochMillis),
                    repairCandidateCountAtAssignment = dao.unrepairedRepairCandidateCountOnce(LOCAL_ACTOR_ID),
                    existingQuestInstances = dao.activeQuestInstancesOnce(LOCAL_ACTOR_ID)
                )
                val selectedLearningFocusProgress = selectedLearningFocusSettlement(event)
                return dao.recordRewardAtomically(
                    event = event,
                    transactions = decision.toTransactionEntities(command.occurredAtEpochMillis),
                    summary = decision.toSummaryEntity(command.occurredAtEpochMillis),
                    presentationReceipts = decision.toReceiptEntities(command.occurredAtEpochMillis),
                    catalogSettlement = settlement,
                    journeySettlement = JourneySettlementPlanner.plan(event),
                    additionalJourneySettlements = listOfNotNull(selectedLearningFocusProgress)
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
            domainOccurrenceKey = "catalog_reconcile_v2:$studyDay"
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
                dueItemCountAtAssignment = lexiconDao.dueCountOnce(occurredAtEpochMillis),
                repairCandidateCountAtAssignment = dao.unrepairedRepairCandidateCountOnce(LOCAL_ACTOR_ID),
                existingQuestInstances = dao.activeQuestInstancesOnce(LOCAL_ACTOR_ID)
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
        val definition = foundationJourneyDefinitionForPersistence()
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
     * Persists one immutable Learning Focus and initializes only its milestone plan in the same
     * Room transaction. Selection grants no XP; later milestones use post-selection evidence.
     */
    suspend fun selectLearningFocus(
        optionId: String,
        occurredAtEpochMillis: Long = System.currentTimeMillis()
    ): RewardWriteResult {
        val definition = FoundationLearningFocusCatalog.definition
        val option = requireNotNull(definition.option(optionId)) {
            "Unknown Learning Focus option: $optionId"
        }
        learningFocusSelectionOnce(definition)?.let { existing ->
            val existingOption = LegacyLearningFocusWireCompatibility.canonicalOption(
                definition = definition,
                storedDefinitionId = existing.definitionId,
                storedDefinitionVersion = existing.definitionVersion,
                storedOptionId = existing.optionId,
                storedMilestonePlanDefinitionId = existing.milestonePlanDefinitionId
            ) ?: throw LearningFocusRecordIncompatibleException(
                selectionId = existing.selectionId,
                reason = LegacyLearningFocusWireCompatibility.compatibilityFailure(definition, existing)
            )
            if (existingOption.id != option.id) {
                throw LearningFocusAlreadySelectedException(
                    existingOptionId = existingOption.id,
                    requestedOptionId = option.id
                )
            }
            return RewardWriteResult(
                status = RewardWriteStatus.DUPLICATE,
                canonicalEventId = existing.sourceEventId,
                summary = dao.rewardSummaryForEvent(existing.sourceEventId)
            )
        }
        val studyDay = ProgressStore.localEpochDay(occurredAtEpochMillis)
        val eventId = StableRewardIds.eventIdempotencyKey(
            actorId = LOCAL_ACTOR_ID,
            eventType = RewardableEventType.OTHER,
            subjectType = "learning_focus_selection",
            subjectId = definition.id,
            domainOccurrenceKey = "learning_focus_v${definition.version}:${option.id}"
        )
        replayResultIfAlreadyApplied(eventId)?.let { return it }
        val event = GamificationEventEntity(
            eventId = eventId,
            actorId = LOCAL_ACTOR_ID,
            eventType = "learning_focus_selected",
            subjectType = "learning_focus_definition",
            subjectId = definition.id,
            occurredAtEpochMillis = occurredAtEpochMillis,
            recordedAtEpochMillis = System.currentTimeMillis(),
            studyDay = studyDay,
            idempotencyKey = eventId,
            source = "learning_focus",
            ruleVersion = GamificationVersions.CURRENT_RULE_VERSION,
            metadataJson = "{\"optionId\":\"${option.id}\"," +
                "\"milestonePlanDefinitionId\":\"${option.milestonePlan.id}\"}",
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
                summaryJson = "{\"reasonCode\":\"learning_focus_selected\",\"xpAwarded\":0}",
                createdAtEpochMillis = occurredAtEpochMillis
            ),
            presentationReceipts = emptyList(),
            learningFocusSelection = LearningFocusSelectionPlanner.planSelection(
                event = event,
                optionId = option.id,
                definition = definition
            ).copy(
                prerequisiteDefinitionId = foundationJourneyDefinitionForPersistence().id
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
        val claim = checkNotNull(QuestAssignmentContracts.resolveClaim(instance)) {
            "Quest assignment has no valid immutable reward contract"
        }
        val xp = claim.xpAmount
        val studyDay = ProgressStore.localEpochDay(occurredAtEpochMillis)
        val eventId = StableRewardIds.eventIdempotencyKey(
            actorId = LOCAL_ACTOR_ID,
            eventType = RewardableEventType.OTHER,
            subjectType = "quest_claim",
            subjectId = questInstanceId,
            domainOccurrenceKey = "manual_claim_v1"
        )
        val ruleId = "catalog.quest.${instance.definitionId}.claim"
        val transactionId = StableRewardIds.transactionId(eventId, ruleId, RewardCurrencies.XP)
        val receiptId = StableRewardIds.catalogPresentationReceiptId(
            eventId = eventId,
            catalogItemId = instance.definitionId,
            level = 1,
            presentationFamily = claim.presentationId
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
            metadataJson = "{\"definitionId\":\"${instance.definitionId}\"}",
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
                celebrationTier = claim.presentationTier,
                summaryJson = "{\"reasonCode\":\"quest_claimed\",\"xpAwarded\":$xp}",
                createdAtEpochMillis = occurredAtEpochMillis
            ),
            presentationReceipts = listOf(
                RewardPresentationReceiptEntity(
                    receiptId = receiptId,
                    actorId = LOCAL_ACTOR_ID,
                    sourceEventId = eventId,
                    sourceTransactionId = transactionId,
                    presentationFamily = claim.presentationId,
                    idempotencyKey = receiptId,
                    priority = claim.presentationPriority,
                    tier = claim.presentationTier,
                    state = PresentationReceiptState.PENDING.wireName,
                    immutableSummaryJson = "{\"questInstanceId\":\"$questInstanceId\",\"xpAwarded\":$xp}",
                    createdAtEpochMillis = occurredAtEpochMillis,
                    updatedAtEpochMillis = occurredAtEpochMillis,
                    expiresAtEpochMillis = Math.addExact(
                        occurredAtEpochMillis,
                        Math.multiplyExact(claim.presentationExpiresAfterSeconds.toLong(), 1_000L)
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

    private suspend fun selectedLearningFocusSettlement(
        event: GamificationEventEntity
    ): JourneySettlementRequest? {
        val definition = FoundationLearningFocusCatalog.definition
        val selection = learningFocusSelectionOnce(definition) ?: return null
        val option = LegacyLearningFocusWireCompatibility.canonicalOption(
            definition = definition,
            storedDefinitionId = selection.definitionId,
            storedDefinitionVersion = selection.definitionVersion,
            storedOptionId = selection.optionId,
            storedMilestonePlanDefinitionId = selection.milestonePlanDefinitionId
        ) ?: throw LearningFocusRecordIncompatibleException(
            selectionId = selection.selectionId,
            reason = LegacyLearningFocusWireCompatibility.compatibilityFailure(definition, selection)
        )
        val persistedPlan = LegacyLearningFocusWireCompatibility.milestonePlanForSelection(
            option = option,
            persistedMilestonePlanDefinitionId = selection.milestonePlanDefinitionId
        )
        return JourneySettlementPlanner.plan(event, persistedPlan).copy(
            evidenceAfterEpochMillis = selection.selectedAtEpochMillis
        )
    }

    /** Reads both accepted identities but never guesses when more than one durable row exists. */
    private suspend fun learningFocusSelectionOnce(
        definition: LearningFocusDefinition
    ): LearningFocusSelectionEntity? {
        val legacyDefinitionId = LegacyLearningFocusWireCompatibility.legacyDefinitionId(definition)
        val acceptedDefinitionIds = listOf(legacyDefinitionId, definition.id).distinct()
        val matches = acceptedDefinitionIds.mapNotNull { definitionId ->
            dao.learningFocusSelectionOnce(
                actorId = LOCAL_ACTOR_ID,
                definitionId = definitionId,
                definitionVersion = definition.version
            )
        }.distinctBy(LearningFocusSelectionEntity::selectionId)
        if (matches.size > 1) {
            throw LearningFocusRecordIncompatibleException(
                selectionId = matches.first().selectionId,
                reason = LearningFocusCompatibilityFailure.MULTIPLE_SELECTIONS
            )
        }
        return matches.singleOrNull()
    }

    /** Reuses one exact legacy identity when present; otherwise fresh installs mint canonical v5. */
    private suspend fun foundationJourneyDefinitionForPersistence(): JourneyDefinition {
        val definition =
            com.cinnamon.app.domain.gamification.FoundationJourneyCatalog.definition
        val matches = LegacyFoundationJourneyWireCompatibility
            .acceptedDefinitionIds(definition)
            .flatMap { definitionId ->
                dao.journeyInstancesForDefinitionOnce(
                    actorId = LOCAL_ACTOR_ID,
                    definitionId = definitionId,
                    definitionVersion = definition.version
                )
            }
            .distinctBy(JourneyInstanceEntity::journeyInstanceId)
        if (matches.size > 1) {
            throw FoundationJourneyRecordIncompatibleException(
                persistedInstanceIds = matches.mapTo(linkedSetOf()) {
                    it.journeyInstanceId
                }
            )
        }
        val persisted = matches.singleOrNull() ?: return definition
        return checkNotNull(
            LegacyFoundationJourneyWireCompatibility.definitionForStoredIdentity(
                definition = definition,
                storedDefinitionId = persisted.definitionId,
                storedDefinitionVersion = persisted.definitionVersion
            )
        ) { "The persisted Foundation plan identity was accepted but could not be projected" }
    }

    private fun legacyProgressEventId(): String = StableRewardIds.eventIdempotencyKey(
        actorId = LOCAL_ACTOR_ID,
        eventType = RewardableEventType.OTHER,
        subjectType = "legacy_progress",
        subjectId = "datastore_v1",
        domainOccurrenceKey = "opening_balance_v1"
    )

    private fun LearningEventCommand.isMeaningful(
        hasVerifiedRepairLink: Boolean,
        reviewQueueStartingDueCount: Int,
        reviewQueueFinalDueCount: Int
    ): Boolean = when (eventType) {
        RewardableEventType.CONCEPT_MASTERED -> completedItemCount >= 1
        RewardableEventType.REVIEW_COMPLETED -> completedItemCount >= 1
        RewardableEventType.PRACTICE_SESSION_COMPLETED -> completedItemCount >= 3
        RewardableEventType.MISTAKE_RECORDED -> completedItemCount == 1 && source == LearningEventSource.REVIEW
        RewardableEventType.MISTAKE_CORRECTED -> hasVerifiedRepairLink
        RewardableEventType.CONFUSABLE_PAIR_ATTEMPTED -> false
        RewardableEventType.CONFUSABLE_PAIR_RESOLVED -> hasVerifiedRepairLink
        RewardableEventType.CONTEXT_APPLICATION_VERIFIED -> hasVerifiedRepairLink
        RewardableEventType.DELAYED_RECALL_SUCCEEDED -> hasVerifiedRepairLink
        RewardableEventType.COMEBACK_SESSION_COMPLETED -> hasVerifiedRepairLink && completedItemCount >= 3
        RewardableEventType.BOOKMARK_SAVED -> false
        RewardableEventType.SAVED_ITEM_REVIEWED -> hasVerifiedRepairLink && completedItemCount >= 1
        RewardableEventType.REVIEW_QUEUE_OPENED -> false
        RewardableEventType.REVIEW_QUEUE_CLEARED ->
            hasVerifiedRepairLink &&
                reviewQueueStartingDueCount >= MIN_REVIEW_QUEUE_SIZE &&
                reviewQueueFinalDueCount == 0
        RewardableEventType.OTHER -> false
    }

    companion object {
        const val LOCAL_ACTOR_ID: String = "local_learner"
        const val CONFUSABLE_SUBJECT_TYPE: String = "confusable_pair"
        const val CONTEXT_SUBJECT_TYPE: String = "lexicon_entry"
        const val COMEBACK_SUBJECT_TYPE: String = "return_session"
        const val REVIEW_QUEUE_SUBJECT_TYPE: String = "review_queue"
        const val MIN_REVIEW_QUEUE_SIZE: Int = 5
        const val DELAYED_RECALL_MILLIS: Long = 72L * 60L * 60L * 1000L
        const val SAVED_ITEM_DELAY_MILLIS: Long = 60L * 60L * 1000L
        const val CONFUSABLE_DELAY_MILLIS: Long = 24L * 60L * 60L * 1000L
        const val COMEBACK_ABSENCE_MILLIS: Long = 7L * 24L * 60L * 60L * 1000L
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
    studyDay: Long,
    repairOfEventId: String?,
    contextApplicationVerified: Boolean = false,
    comebackMeaningfulActionCount: Int = 0,
    reviewQueueStartingDueCount: Int = 0,
    reviewQueueFinalDueCount: Int = -1
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
    metadataJson = when {
        eventType == RewardableEventType.CONFUSABLE_PAIR_RESOLVED && repairOfEventId != null ->
            "{\"confusableAttemptEventId\":${repairOfEventId.toJsonString()}," +
                "\"confusableLinkPresent\":true,\"minimumDelayHoursSatisfied\":true}"
        eventType == RewardableEventType.MISTAKE_CORRECTED && repairOfEventId != null ->
            "{\"repairOfEventId\":${repairOfEventId.toJsonString()}," +
                "\"repairLinkPresent\":true,\"incorrectAttemptPrecedesCorrection\":true}"
        eventType == RewardableEventType.MISTAKE_RECORDED -> "{\"repairCandidate\":true}"
        eventType == RewardableEventType.CONFUSABLE_PAIR_ATTEMPTED -> "{\"confusableCandidate\":true}"
        eventType == RewardableEventType.CONTEXT_APPLICATION_VERIFIED && contextApplicationVerified ->
            "{\"applicationVerified\":true,\"applicationSource\":\"authored_cloze\"}"
        eventType == RewardableEventType.DELAYED_RECALL_SUCCEEDED && repairOfEventId != null ->
            "{\"priorReviewEventId\":${repairOfEventId.toJsonString()}," +
                "\"delayedRecallLinkPresent\":true,\"minimumDelayHoursSatisfied\":true}"
        eventType == RewardableEventType.COMEBACK_SESSION_COMPLETED && repairOfEventId != null &&
            comebackMeaningfulActionCount >= 3 ->
            "{\"priorActivityEventId\":${repairOfEventId.toJsonString()}," +
                "\"comebackLinkPresent\":true,\"minimumAbsenceDaysSatisfied\":true," +
                "\"meaningfulActionCount\":$comebackMeaningfulActionCount}"
        eventType == RewardableEventType.BOOKMARK_SAVED -> "{\"bookmarkRecorded\":true}"
        eventType == RewardableEventType.SAVED_ITEM_REVIEWED && repairOfEventId != null ->
            "{\"bookmarkEventId\":${repairOfEventId.toJsonString()}," +
                "\"savedItemLinkPresent\":true,\"minimumDelayHoursSatisfied\":true," +
                "\"successfulReviewCount\":1}"
        eventType == RewardableEventType.REVIEW_QUEUE_OPENED &&
            reviewQueueStartingDueCount >= GamificationRepository.MIN_REVIEW_QUEUE_SIZE ->
            "{\"queueOpenVerified\":true,\"startingDueCount\":$reviewQueueStartingDueCount}"
        eventType == RewardableEventType.REVIEW_QUEUE_CLEARED && repairOfEventId != null &&
            reviewQueueStartingDueCount >= GamificationRepository.MIN_REVIEW_QUEUE_SIZE &&
            reviewQueueFinalDueCount == 0 ->
            "{\"reviewQueueOpeningEventId\":${repairOfEventId.toJsonString()}," +
                "\"queueClearLinkPresent\":true,\"startingDueCount\":$reviewQueueStartingDueCount," +
                "\"finalDueCount\":$reviewQueueFinalDueCount}"
        else -> "{}"
    },
    replayOfEventId = null
)

private fun String.toJsonString(): String = buildString(length + 2) {
    append('"')
    this@toJsonString.forEach { character ->
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
