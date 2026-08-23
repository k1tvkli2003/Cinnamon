package com.cinnamon.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LearningFocusPersistenceContractTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: GamificationDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.gamificationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `evidence at selection millisecond is excluded and next millisecond is included`() =
        runBlocking {
            val selectedAt = 1_700_000_000_000L
            dao.insertEventIfAbsent(
                event(
                    id = "review-at-selection",
                    type = "review_completed",
                    subjectType = "lexicon_entry",
                    subjectId = "excluded-review",
                    occurredAt = selectedAt
                )
            )
            dao.insertEventIfAbsent(
                event(
                    id = "review-after-selection",
                    type = "review_completed",
                    subjectType = "lexicon_entry",
                    subjectId = "included-review",
                    occurredAt = selectedAt + 1L
                )
            )
            dao.insertEventIfAbsent(
                event(
                    id = "practice-at-selection",
                    type = "practice_session_completed",
                    subjectType = "excluded-format",
                    subjectId = "excluded-session",
                    occurredAt = selectedAt
                )
            )
            dao.insertEventIfAbsent(
                event(
                    id = "practice-after-selection",
                    type = "practice_session_completed",
                    subjectType = "included-format",
                    subjectId = "included-session",
                    occurredAt = selectedAt + 1L
                )
            )
            dao.insertEventIfAbsent(
                event(
                    id = "exact-only-activity",
                    actorId = "exact-only-learner",
                    type = "review_completed",
                    subjectType = "lexicon_entry",
                    subjectId = "excluded-only",
                    occurredAt = selectedAt
                )
            )

            assertEquals(
                1,
                dao.distinctSubjectIdCountAfterOnce(
                    actorId = "learner",
                    eventType = "review_completed",
                    afterEpochMillis = selectedAt,
                    throughEpochMillis = selectedAt + 1L
                )
            )
            assertEquals(
                1,
                dao.distinctSubjectTypeCountAfterOnce(
                    actorId = "learner",
                    eventType = "practice_session_completed",
                    afterEpochMillis = selectedAt,
                    throughEpochMillis = selectedAt + 1L
                )
            )
            assertEquals(
                1,
                dao.distinctReviewedSubjectCountForStudyDayAfterOnce(
                    actorId = "learner",
                    studyDay = 19_675L,
                    afterEpochMillis = selectedAt,
                    throughEpochMillis = selectedAt + 1L
                )
            )
            assertEquals(
                1,
                dao.activeStudyDayCountAfterOnce(
                    actorId = "learner",
                    afterEpochMillis = selectedAt,
                    throughEpochMillis = selectedAt + 1L
                )
            )
            assertEquals(
                0,
                dao.activeStudyDayCountAfterOnce(
                    actorId = "exact-only-learner",
                    afterEpochMillis = selectedAt,
                    throughEpochMillis = selectedAt + 1L
                )
            )
        }

    @Test
    fun `migrated same-option replay creates no event while another option is a typed conflict`() =
        runBlocking {
            val historicalEvent = event(
                id = "historical-selection",
                type = "campaign_route_selected",
                subjectType = "campaign_route_choice",
                subjectId = "route.precision-trail",
                occurredAt = 1_700_000_000_000L
            )
            dao.insertEventIfAbsent(historicalEvent)
            dao.insertLearningFocusSelectionIfAbsent(
                LearningFocusSelectionEntity(
                    selectionId = "campaign_choice::learner::v1",
                    actorId = "learner",
                    definitionId = "campaign.foundation-route-choice",
                    definitionVersion = 1,
                    optionId = "route.precision-trail",
                    milestonePlanDefinitionId = "journey.route.precision-trail",
                    sourceEventId = historicalEvent.eventId,
                    selectedAtEpochMillis = historicalEvent.occurredAtEpochMillis
                )
            )

            val replayEvent = learningFocusEvent(
                id = "same-option-replay",
                optionId = "route.precision-trail",
                occurredAt = historicalEvent.occurredAtEpochMillis + 1_000L
            )
            val replay = dao.recordRewardAtomically(
                event = replayEvent,
                transactions = emptyList(),
                summary = emptySummary(replayEvent),
                presentationReceipts = emptyList(),
                learningFocusSelection = selectionRequest(
                    replayEvent,
                    optionId = "route.precision-trail",
                    planDefinitionId = "journey.route.precision-trail"
                )
            )

            assertEquals(RewardWriteStatus.DUPLICATE, replay.status)
            assertEquals(historicalEvent.eventId, replay.canonicalEventId)
            assertNull(dao.eventById(replayEvent.eventId))
            assertEquals(1, dao.observeLearningFocusSelections("learner").first().size)

            val conflictEvent = learningFocusEvent(
                id = "different-option-conflict",
                optionId = "route.momentum-circuit",
                occurredAt = historicalEvent.occurredAtEpochMillis + 2_000L
            )
            val conflict = assertThrows(LearningFocusAlreadySelectedException::class.java) {
                runBlocking {
                    dao.recordRewardAtomically(
                        event = conflictEvent,
                        transactions = emptyList(),
                        summary = emptySummary(conflictEvent),
                        presentationReceipts = emptyList(),
                        learningFocusSelection = selectionRequest(
                            conflictEvent,
                            optionId = "route.momentum-circuit",
                            planDefinitionId = "journey.route.momentum-circuit"
                        )
                    )
                }
            }

            assertEquals("route.precision-trail", conflict.existingOptionId)
            assertEquals("route.momentum-circuit", conflict.requestedOptionId)
            assertNull(dao.eventById(conflictEvent.eventId))
            assertEquals(1, dao.observeLearningFocusSelections("learner").first().size)
        }

    private fun selectionRequest(
        event: GamificationEventEntity,
        optionId: String,
        planDefinitionId: String
    ): LearningFocusSelectionRequest {
        val transactionId = "transaction::${event.eventId}"
        val progress = JourneySettlementRequest(
            instance = JourneyInstanceEntity(
                journeyInstanceId = "instance::${event.eventId}",
                actorId = event.actorId,
                definitionId = planDefinitionId,
                definitionVersion = 1,
                state = "active",
                currentStageOrder = 1,
                startedAtEpochMillis = event.occurredAtEpochMillis,
                updatedAtEpochMillis = event.occurredAtEpochMillis,
                completionEventId = null,
                completedAtEpochMillis = null
            ),
            stages = listOf(
                JourneyStageSettlementCandidate(
                    stage = JourneyStageProgressEntity(
                        stageProgressId = "stage::${event.eventId}",
                        journeyInstanceId = "instance::${event.eventId}",
                        actorId = event.actorId,
                        stageDefinitionId = "milestone.test",
                        stageOrder = 1,
                        evidenceMetric = "distinct_reviewed_items",
                        state = "active",
                        progress = 0L,
                        target = 1L,
                        rewardXp = 1L,
                        createdAtEpochMillis = event.occurredAtEpochMillis,
                        updatedAtEpochMillis = event.occurredAtEpochMillis,
                        completionEventId = null,
                        completedAtEpochMillis = null
                    ),
                    metric = CatalogEvidenceMetric.DISTINCT_REVIEWED_ITEMS,
                    rewardTransaction = RewardTransactionEntity(
                        transactionId = transactionId,
                        eventId = event.eventId,
                        actorId = event.actorId,
                        transactionKind = "grant",
                        currency = "xp",
                        amount = 1L,
                        ruleId = "journey.test.milestone",
                        ruleVersion = 1,
                        reasonCode = "test",
                        createdAtEpochMillis = event.occurredAtEpochMillis,
                        idempotencyKey = transactionId,
                        metadataJson = "{}"
                    ),
                    presentationReceipt = RewardPresentationReceiptEntity(
                        receiptId = "receipt::${event.eventId}",
                        actorId = event.actorId,
                        sourceEventId = event.eventId,
                        sourceTransactionId = transactionId,
                        presentationFamily = "presentation.journey.test",
                        idempotencyKey = "receipt::${event.eventId}",
                        priority = 1,
                        tier = "micro",
                        state = "pending",
                        immutableSummaryJson = "{}",
                        createdAtEpochMillis = event.occurredAtEpochMillis,
                        updatedAtEpochMillis = event.occurredAtEpochMillis,
                        expiresAtEpochMillis = null,
                        acknowledgedAtEpochMillis = null,
                        suppressionReason = null,
                        coalescedCount = 1
                    )
                )
            ),
            settleEvidenceOnThisEvent = false,
            evidenceAfterEpochMillis = event.occurredAtEpochMillis
        )
        return LearningFocusSelectionRequest(
            selection = LearningFocusSelectionEntity(
                selectionId = "selection::${event.eventId}",
                actorId = event.actorId,
                definitionId = "campaign.foundation-route-choice",
                definitionVersion = 1,
                optionId = optionId,
                milestonePlanDefinitionId = planDefinitionId,
                sourceEventId = event.eventId,
                selectedAtEpochMillis = event.occurredAtEpochMillis
            ),
            prerequisiteDefinitionId = "journey.foundation",
            prerequisiteDefinitionVersion = 1,
            prerequisiteMilestoneDefinitionId = "stage.memory-spark",
            selectedFocusProgress = progress
        )
    }

    private fun learningFocusEvent(
        id: String,
        optionId: String,
        occurredAt: Long
    ): GamificationEventEntity = event(
        id = id,
        type = "learning_focus_selected",
        subjectType = "learning_focus_selection",
        subjectId = optionId,
        occurredAt = occurredAt
    )

    private fun event(
        id: String,
        actorId: String = "learner",
        type: String,
        subjectType: String,
        subjectId: String,
        occurredAt: Long
    ) = GamificationEventEntity(
        eventId = "event::$id",
        actorId = actorId,
        eventType = type,
        subjectType = subjectType,
        subjectId = subjectId,
        occurredAtEpochMillis = occurredAt,
        recordedAtEpochMillis = occurredAt,
        studyDay = 19_675L,
        idempotencyKey = "idempotency::$id",
        source = "test",
        ruleVersion = 1,
        metadataJson = "{}",
        replayOfEventId = null
    )

    private fun emptySummary(event: GamificationEventEntity) = RewardSummaryEntity(
        eventId = event.eventId,
        actorId = event.actorId,
        ruleVersion = 1,
        xpAwarded = 0L,
        currencyRewardsJson = "[]",
        questProgressJson = "[]",
        achievementIdsJson = "[]",
        unlockedContentJson = "[]",
        celebrationTier = "micro",
        summaryJson = "{}",
        createdAtEpochMillis = event.occurredAtEpochMillis
    )
}
