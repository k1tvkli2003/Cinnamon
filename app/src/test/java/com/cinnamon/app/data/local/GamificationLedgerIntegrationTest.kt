package com.cinnamon.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cinnamon.app.data.gamification.CampaignSettlementPlanner
import com.cinnamon.app.data.gamification.JourneySettlementPlanner
import com.cinnamon.app.domain.gamification.FoundationCampaignCatalog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Exercises the real Room transaction boundary, not only pure reward rules. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GamificationLedgerIntegrationTest {

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
    fun `a replayed event keeps one immutable transaction and one balance delta`() = runBlocking {
        val first = commit(eventKey = "review-1", xp = 8L)
        val replay = commit(eventKey = "review-1", xp = 8L)

        assertEquals(RewardWriteStatus.APPLIED, first.status)
        assertEquals(RewardWriteStatus.DUPLICATE, replay.status)
        assertEquals("event-review-1", replay.canonicalEventId)
        assertEquals(
            8L,
            dao.observeRewardBalance("learner", "xp").first()?.balance
        )
        assertEquals(
            1,
            dao.observeRewardTransactions("learner").first().size
        )
    }

    @Test
    fun `daily cap rollback preserves prior ledger state`() = runBlocking {
        commit(eventKey = "near-cap", xp = 159L)

        assertThrows(DailyRewardCapExceededException::class.java) {
            runBlocking { commit(eventKey = "over-cap", xp = 2L) }
        }

        assertEquals(
            159L,
            dao.observeRewardBalance("learner", "xp").first()?.balance
        )
        assertNull(dao.eventById("event-over-cap"))
        assertTrue(dao.observeRewardTransactions("learner").first().all { it.amount == 159L })
    }

    @Test
    fun `activity projections join the committed event with its immutable reward summary`() = runBlocking {
        commit(
            eventKey = "vocabulary-match",
            xp = 12L,
            eventType = "practice_session_completed",
            subjectType = "vocabulary_match"
        )
        commit(
            eventKey = "cloze-clinic",
            xp = 12L,
            eventType = "practice_session_completed",
            subjectType = "cloze_clinic"
        )

        val activities = dao.observeRecentLearningActivity("learner", limit = 8).first()

        assertEquals(2, activities.size)
        assertEquals("event-vocabulary-match", activities.first().eventId)
        assertEquals("practice_session_completed", activities.first().eventType)
        assertEquals("vocabulary_match", activities.first().subjectType)
        assertEquals(12L, activities.first().xpAwarded)
        assertEquals("micro", activities.first().celebrationTier)
        assertEquals(
            2,
            dao.observeDistinctSubjectTypeCount("learner", "practice_session_completed").first()
        )
    }

    @Test
    fun `acknowledging a reward presentation cannot change its settled balance`() = runBlocking {
        commit(eventKey = "presentation", xp = 12L, includePresentationReceipt = true)

        val receipt = dao.observePendingPresentationReceipts("learner", System.currentTimeMillis()).first().single()
        assertEquals("pending", receipt.state)
        assertEquals(12L, dao.observeRewardBalance("learner", "xp").first()?.balance)

        assertEquals(
            1,
            dao.transitionPresentationReceipt(
                receiptId = receipt.receiptId,
                allowedCurrentStates = listOf("pending"),
                newState = "acknowledged",
                updatedAtEpochMillis = System.currentTimeMillis(),
                acknowledge = true,
                suppressionReason = null
            )
        )

        assertTrue(dao.observePendingPresentationReceipts("learner", System.currentTimeMillis()).first().isEmpty())
        assertEquals(12L, dao.observeRewardBalance("learner", "xp").first()?.balance)
    }

    @Test
    fun `a verified completion keeps its study day after the XP cap is reached`() = runBlocking {
        commit(
            eventKey = "capped-practice",
            xp = 0L,
            eventType = "practice_session_completed",
            subjectType = "vocabulary_match"
        )

        assertEquals(
            listOf(19_675L),
            dao.observeRecentStudyDays("learner", limit = 8).first()
        )
    }

    @Test
    fun `daily due-review evidence counts distinct subjects rather than repeat events`() = runBlocking {
        commit(eventKey = "review-a-1", xp = 0L, subjectId = "term-a")
        commit(eventKey = "review-a-2", xp = 0L, subjectId = "term-a")
        commit(eventKey = "review-b-1", xp = 0L, subjectId = "term-b")

        assertEquals(
            2,
            dao.observeDistinctReviewedSubjectCountForStudyDay("learner", 19_675L).first()
        )
        assertEquals(
            3,
            dao.observeCompletedReviewCountForStudyDay("learner", 19_675L).first()
        )
    }

    @Test
    fun `catalog unlock reward summary receipt and balance commit as one unit`() = runBlocking {
        val event = event(
            eventKey = "mastery-threshold",
            eventType = "concept_mastered",
            subjectId = "term-mastered"
        )
        val candidate = catalogUnlockCandidate(event, unlockId = "unlock-mastery-1")

        val result = commitCatalog(
            event = event,
            settlement = CatalogSettlementRequest(unlocks = listOf(candidate))
        )

        assertEquals(RewardWriteStatus.APPLIED, result.status)
        assertEquals(20L, result.summary?.xpAwarded)
        assertTrue(result.summary?.achievementIdsJson?.contains("achievement.test.mastery") == true)
        assertEquals(20L, dao.observeRewardBalance("learner", "xp").first()?.balance)
        assertEquals(1, dao.observeRewardTransactions("learner").first().size)
        val unlock = dao.observeAchievementUnlocks("learner").first().single()
        assertEquals("event-mastery-threshold", unlock.sourceEventId)
        assertTrue(unlock.evidenceJson.contains("mastered_items_after_delay"))
        val receipt = dao.observePendingPresentationReceipts("learner", Long.MIN_VALUE).first().single()
        assertEquals("receipt-unlock-mastery-1", receipt.receiptId)
        assertEquals("transaction-unlock-mastery-1", receipt.sourceTransactionId)
    }

    @Test
    fun `catalog ledger collision rolls back event unlock and reward together`() = runBlocking {
        commit(eventKey = "existing-transaction", xp = 5L)
        val event = event(
            eventKey = "atomic-rollback",
            eventType = "concept_mastered",
            subjectId = "term-rollback"
        )
        val candidate = catalogUnlockCandidate(
            event = event,
            unlockId = "unlock-rollback",
            transactionId = "transaction-existing-transaction"
        )

        val failure = runCatching {
            commitCatalog(
                event = event,
                settlement = CatalogSettlementRequest(unlocks = listOf(candidate))
            )
        }.exceptionOrNull()

        assertNotNull(failure)
        assertNull(dao.eventById("event-atomic-rollback"))
        assertTrue(dao.observeAchievementUnlocks("learner").first().isEmpty())
        assertEquals(5L, dao.observeRewardBalance("learner", "xp").first()?.balance)
        assertEquals(1, dao.observeRewardTransactions("learner").first().size)
    }

    @Test
    fun `a reached catalog unlock cannot grant its reward twice on later events`() = runBlocking {
        val firstEvent = event(
            eventKey = "first-unlock",
            eventType = "concept_mastered",
            subjectId = "term-a"
        )
        val laterEvent = event(
            eventKey = "later-evidence",
            eventType = "concept_mastered",
            subjectId = "term-b"
        )

        val first = commitCatalog(
            event = firstEvent,
            settlement = CatalogSettlementRequest(
                unlocks = listOf(catalogUnlockCandidate(firstEvent, unlockId = "unlock-stable"))
            )
        )
        val later = commitCatalog(
            event = laterEvent,
            settlement = CatalogSettlementRequest(
                unlocks = listOf(catalogUnlockCandidate(laterEvent, unlockId = "unlock-stable"))
            )
        )

        assertEquals(20L, first.summary?.xpAwarded)
        assertEquals(0L, later.summary?.xpAwarded)
        assertEquals("[]", later.summary?.achievementIdsJson)
        assertEquals(20L, dao.observeRewardBalance("learner", "xp").first()?.balance)
        assertEquals(1, dao.observeRewardTransactions("learner").first().size)
        assertEquals(1, dao.observeAchievementUnlocks("learner").first().size)
        assertEquals(1, dao.observePendingPresentationReceipts("learner", Long.MIN_VALUE).first().size)
    }

    @Test
    fun `quest progress counts distinct reviewed subjects and completes once`() = runBlocking {
        val progressSummaries = listOf(
            event("quest-a-1", subjectId = "term-a"),
            event("quest-a-2", subjectId = "term-a"),
            event("quest-b", subjectId = "term-b"),
            event("quest-c", subjectId = "term-c")
        ).map { event ->
            commitCatalog(
                event = event,
                settlement = CatalogSettlementRequest(quests = listOf(dailyQuestCandidate(event)))
            ).summary
        }

        val quest = checkNotNull(dao.questInstanceById("quest-daily-due-review"))
        assertEquals(3L, quest.progress)
        assertEquals("completed", quest.state)
        assertEquals("event-quest-c", quest.completionEventId)
        assertTrue(progressSummaries[0]?.questProgressJson?.contains("\"to\":1") == true)
        assertEquals("[]", progressSummaries[1]?.questProgressJson)
        assertTrue(progressSummaries[2]?.questProgressJson?.contains("\"to\":2") == true)
        assertTrue(progressSummaries[3]?.questProgressJson?.contains("\"completed\":true") == true)
        assertEquals(1, dao.observeActiveQuestInstances("learner").first().size)
    }

    @Test
    fun `completed quest claim is idempotent and a second claim event rolls back`() = runBlocking {
        listOf("term-a", "term-b", "term-c").forEachIndexed { index, subjectId ->
            val evidenceEvent = event("claim-evidence-$index", subjectId = subjectId)
            commitCatalog(
                event = evidenceEvent,
                settlement = CatalogSettlementRequest(quests = listOf(dailyQuestCandidate(evidenceEvent)))
            )
        }

        val claimEvent = event(
            eventKey = "quest-claim",
            eventType = "quest_reward_claimed",
            subjectType = "quest_instance",
            subjectId = "quest-daily-due-review"
        )
        val first = commitQuestClaim(claimEvent)
        val replay = commitQuestClaim(claimEvent)
        val forgedEvent = event(
            eventKey = "quest-claim-forged",
            eventType = "quest_reward_claimed",
            subjectType = "quest_instance",
            subjectId = "quest-daily-due-review"
        )

        val rejected = runCatching { commitQuestClaim(forgedEvent) }.exceptionOrNull()

        assertEquals(RewardWriteStatus.APPLIED, first.status)
        assertEquals(RewardWriteStatus.DUPLICATE, replay.status)
        assertNotNull(rejected)
        assertEquals("claimed", dao.questInstanceById("quest-daily-due-review")?.state)
        assertEquals(10L, dao.observeRewardBalance("learner", "xp").first()?.balance)
        assertEquals(1, dao.observeRewardTransactions("learner").first().size)
        assertNull(dao.eventById("event-quest-claim-forged"))
    }

    @Test
    fun `journey counts distinct reviews and settles its first stage exactly once`() = runBlocking {
        val first = event("journey-review-a", subjectId = "term-a")
        val repeated = event("journey-review-a-repeat", subjectId = "term-a")
        val second = event("journey-review-b", subjectId = "term-b")
        val third = event("journey-review-c", subjectId = "term-c")

        commitJourney(first)
        commitJourney(repeated)
        commitJourney(second)
        val completed = commitJourney(third)
        val replay = commitJourney(third)

        val instance = dao.observeJourneyInstances("learner").first().single()
        val stages = dao.observeJourneyStages("learner").first()
        assertEquals("active", instance.state)
        assertEquals(2, instance.currentStageOrder)
        assertEquals("completed", stages[0].state)
        assertEquals(3L, stages[0].progress)
        assertEquals("event-journey-review-c", stages[0].completionEventId)
        assertEquals("active", stages[1].state)
        assertEquals(0L, stages[1].progress)
        assertEquals(10L, completed.summary?.xpAwarded)
        assertTrue(completed.summary?.summaryJson?.contains("stage.memory-spark") == true)
        assertEquals(RewardWriteStatus.DUPLICATE, replay.status)
        assertEquals(10L, dao.observeRewardBalance("learner", "xp").first()?.balance)
        assertEquals(
            1,
            dao.observeRewardTransactions("learner").first()
                .count { it.ruleId.startsWith("journey.") }
        )
    }

    @Test
    fun `locked journey stage ignores early evidence and only one stage can complete per event`() = runBlocking {
        // Seed all evidence without a journey request, as an existing installation might have it.
        listOf("term-a", "term-b", "term-c").forEachIndexed { index, subjectId ->
            commit(eventKey = "seed-review-$index", xp = 0L, subjectId = subjectId)
        }
        commit(
            eventKey = "seed-practice-a",
            xp = 0L,
            eventType = "practice_session_completed",
            subjectType = "vocabulary_match"
        )
        commit(
            eventKey = "seed-practice-b",
            xp = 0L,
            eventType = "practice_session_completed",
            subjectType = "cloze_clinic"
        )
        commit(
            eventKey = "seed-mastery",
            xp = 0L,
            eventType = "concept_mastered",
            subjectId = "term-a"
        )

        val reconcile = event(
            eventKey = "journey-reconcile",
            eventType = "catalog_reconciled",
            subjectType = "journey",
            subjectId = "foundation"
        )
        commitJourney(reconcile)

        val after = dao.observeJourneyStages("learner").first()
        assertEquals("completed", after[0].state)
        assertEquals("active", after[1].state)
        assertEquals(0L, after[1].progress)
        assertEquals("locked", after[2].state)
        assertEquals(0L, after[2].progress)
        assertEquals(10L, dao.observeRewardBalance("learner", "xp").first()?.balance)
    }

    @Test
    fun `four-stage journey completes sequentially and banks the authored XP`() = runBlocking {
        listOf("term-a", "term-b", "term-c").forEachIndexed { index, subjectId ->
            commitJourney(event("route-review-$index", subjectId = subjectId))
        }
        commitJourney(
            event(
                eventKey = "route-practice-a",
                eventType = "practice_session_completed",
                subjectType = "vocabulary_match"
            )
        )
        commitJourney(
            event(
                eventKey = "route-practice-b",
                eventType = "practice_session_completed",
                subjectType = "cloze_clinic"
            )
        )
        commitJourney(
            event(
                eventKey = "route-mastery",
                eventType = "concept_mastered",
                subjectId = "term-a"
            )
        )
        commitJourney(event("route-day-two", subjectId = "term-d", studyDay = 19_676L))
        val final = commitJourney(event("route-day-three", subjectId = "term-e", studyDay = 19_677L))

        val instance = dao.observeJourneyInstances("learner").first().single()
        val stages = dao.observeJourneyStages("learner").first()
        assertEquals("completed", instance.state)
        assertEquals("event-route-day-three", instance.completionEventId)
        assertTrue(stages.all { it.state == "completed" })
        assertEquals(listOf(10L, 20L, 20L, 30L), stages.map { it.rewardXp })
        assertEquals(30L, final.summary?.xpAwarded)
        assertTrue(final.summary?.summaryJson?.contains("\"journeyCompleted\":true") == true)
        assertEquals(80L, dao.observeRewardBalance("learner", "xp").first()?.balance)
        assertEquals(
            4,
            dao.observeRewardTransactions("learner").first()
                .count { it.ruleId.startsWith("journey.") }
        )
    }

    @Test
    fun `journey milestone XP is exempt from the farmable daily cap`() = runBlocking {
        commit(eventKey = "cap-opening-review", xp = 160L, subjectId = "term-a")
        commitJourney(event("cap-journey-review-b", subjectId = "term-b"))
        val milestone = commitJourney(event("cap-journey-review-c", subjectId = "term-c"))

        assertEquals(10L, milestone.summary?.xpAwarded)
        assertEquals(170L, dao.observeRewardBalance("learner", "xp").first()?.balance)
    }

    @Test
    fun `journey transaction collision rolls back initialization progress and event`() = runBlocking {
        commit(eventKey = "existing-journey-transaction", xp = 5L)
        val journeyEvent = event("journey-atomic-rollback", subjectId = "term-new")
        val planned = JourneySettlementPlanner.plan(journeyEvent)
        val first = planned.stages.first()
        val colliding = first.copy(
            rewardTransaction = first.rewardTransaction.copy(
                transactionId = "transaction-existing-journey-transaction",
                idempotencyKey = "transaction-existing-journey-transaction"
            ),
            presentationReceipt = first.presentationReceipt.copy(
                sourceTransactionId = "transaction-existing-journey-transaction"
            )
        )
        // Three distinct reviews are needed so this event attempts the colliding stage reward.
        commit(eventKey = "rollback-seed-a", xp = 0L, subjectId = "term-a")
        commit(eventKey = "rollback-seed-b", xp = 0L, subjectId = "term-b")

        val failure = runCatching {
            dao.recordRewardAtomically(
                event = journeyEvent,
                transactions = emptyList(),
                summary = emptySummary(journeyEvent),
                presentationReceipts = emptyList(),
                journeySettlement = planned.copy(
                    stages = listOf(colliding) + planned.stages.drop(1)
                )
            )
        }.exceptionOrNull()

        assertNotNull(failure)
        assertNull(dao.eventById(journeyEvent.eventId))
        assertTrue(dao.observeJourneyInstances("learner").first().isEmpty())
        assertTrue(dao.observeJourneyStages("learner").first().isEmpty())
        assertEquals(5L, dao.observeRewardBalance("learner", "xp").first()?.balance)
    }

    @Test
    fun `campaign choice stays locked and rolls back every row before its journey prerequisite`() = runBlocking {
        val choiceEvent = campaignChoiceEvent(
            eventKey = "locked-campaign-choice",
            routeId = "route.precision-trail"
        )

        val failure = runCatching {
            commitCampaignChoice(choiceEvent, "route.precision-trail")
        }.exceptionOrNull()

        assertNotNull(failure)
        assertNull(dao.eventById(choiceEvent.eventId))
        assertTrue(dao.observeCampaignRouteChoices("learner").first().isEmpty())
        assertTrue(dao.observeJourneyInstances("learner").first().isEmpty())
        assertTrue(dao.observeJourneyStages("learner").first().isEmpty())
        assertTrue(dao.observeRewardTransactions("learner").first().isEmpty())
    }

    @Test
    fun `campaign choice atomically persists one route without granting selection XP`() = runBlocking {
        unlockCampaignChoice()
        // Eligible historic evidence must not turn the selection tap into a reward event.
        listOf("vocabulary_match", "cloze_clinic", "rapid_recall").forEachIndexed { index, kind ->
            commit(
                eventKey = "historic-practice-$index",
                xp = 0L,
                eventType = "practice_session_completed",
                subjectType = kind
            )
        }
        val balanceBefore = dao.observeRewardBalance("learner", "xp").first()?.balance
        val choiceEvent = campaignChoiceEvent(
            eventKey = "choose-precision",
            routeId = "route.precision-trail"
        )

        val result = commitCampaignChoice(choiceEvent, "route.precision-trail")

        val choice = dao.observeCampaignRouteChoices("learner").first().single()
        val journeys = dao.observeJourneyInstances("learner").first()
        val routeStages = dao.observeJourneyStages("learner").first()
            .filter { stage -> stage.journeyInstanceId == choice.journeyDefinitionId.toJourneyInstanceId() }
        assertEquals(RewardWriteStatus.APPLIED, result.status)
        assertEquals(0L, result.summary?.xpAwarded)
        assertEquals(balanceBefore, dao.observeRewardBalance("learner", "xp").first()?.balance)
        assertEquals("route.precision-trail", choice.routeId)
        assertEquals("journey.route.precision-trail", choice.journeyDefinitionId)
        assertEquals(2, journeys.size)
        assertTrue(journeys.none { it.definitionId == "journey.route.momentum-circuit" })
        assertEquals(listOf("active", "locked", "locked"), routeStages.map { it.state })
        assertEquals(listOf(0L, 0L, 0L), routeStages.map { it.progress })
        assertTrue(
            dao.observeRewardTransactions("learner").first()
                .none { it.eventId == choiceEvent.eventId }
        )
    }

    @Test
    fun `campaign choice replay is stable while a conflicting route rolls back`() = runBlocking {
        unlockCampaignChoice()
        val precisionEvent = campaignChoiceEvent(
            eventKey = "campaign-precision",
            routeId = "route.precision-trail"
        )
        val first = commitCampaignChoice(precisionEvent, "route.precision-trail")
        val replay = commitCampaignChoice(precisionEvent, "route.precision-trail")
        val conflictEvent = campaignChoiceEvent(
            eventKey = "campaign-momentum-conflict",
            routeId = "route.momentum-circuit"
        )

        val conflict = runCatching {
            commitCampaignChoice(conflictEvent, "route.momentum-circuit")
        }.exceptionOrNull()

        assertEquals(RewardWriteStatus.APPLIED, first.status)
        assertEquals(RewardWriteStatus.DUPLICATE, replay.status)
        assertNotNull(conflict)
        assertNull(dao.eventById(conflictEvent.eventId))
        assertEquals(
            listOf("route.precision-trail"),
            dao.observeCampaignRouteChoices("learner").first().map { it.routeId }
        )
        assertEquals(
            1,
            dao.observeJourneyInstances("learner").first()
                .count { it.definitionId.startsWith("journey.route.") }
        )
    }

    @Test
    fun `selected campaign route progresses from learning while the unselected route never materializes`() = runBlocking {
        unlockCampaignChoice()
        commitCampaignChoice(
            campaignChoiceEvent("choose-route-for-progress", "route.precision-trail"),
            "route.precision-trail"
        )
        val route = requireNotNull(
            FoundationCampaignCatalog.definition.route("route.precision-trail")
        )
        listOf("vocabulary_match", "cloze_clinic", "rapid_recall").forEachIndexed { index, kind ->
            val learningEvent = event(
                eventKey = "campaign-practice-$index",
                eventType = "practice_session_completed",
                subjectType = kind,
                subjectId = "session-$index"
            )
            dao.recordRewardAtomically(
                event = learningEvent,
                transactions = emptyList(),
                summary = emptySummary(learningEvent),
                presentationReceipts = emptyList(),
                journeySettlement = JourneySettlementPlanner.plan(learningEvent),
                additionalJourneySettlements = listOf(
                    JourneySettlementPlanner.plan(learningEvent, route.journey)
                )
            )
        }

        val instances = dao.observeJourneyInstances("learner").first()
        val precision = instances.single { it.definitionId == "journey.route.precision-trail" }
        val stages = dao.observeJourneyStages("learner").first()
            .filter { it.journeyInstanceId == precision.journeyInstanceId }
        assertEquals(2, precision.currentStageOrder)
        assertEquals("completed", stages[0].state)
        assertEquals(3L, stages[0].progress)
        assertEquals("active", stages[1].state)
        assertTrue(instances.none { it.definitionId == "journey.route.momentum-circuit" })
        assertEquals(
            25L,
            dao.observeRewardTransactions("learner").first()
                .filter { it.ruleId.startsWith("journey.journey.route.precision-trail") }
                .sumOf { it.amount }
        )
    }

    private fun event(
        eventKey: String,
        eventType: String = "review_completed",
        subjectType: String = "lexicon_entry",
        subjectId: String = eventKey,
        studyDay: Long = 19_675L
    ): GamificationEventEntity {
        val now = 1_700_000_000_000L
        return GamificationEventEntity(
            eventId = "event-$eventKey",
            actorId = "learner",
            eventType = eventType,
            subjectType = subjectType,
            subjectId = subjectId,
            occurredAtEpochMillis = now,
            recordedAtEpochMillis = now,
            studyDay = studyDay,
            idempotencyKey = "idempotency-$eventKey",
            source = "test",
            ruleVersion = 1,
            metadataJson = "{}",
            replayOfEventId = null
        )
    }

    private fun campaignChoiceEvent(
        eventKey: String,
        routeId: String
    ): GamificationEventEntity = event(
        eventKey = eventKey,
        eventType = "campaign_route_selected",
        subjectType = "campaign_route",
        subjectId = routeId
    )

    private suspend fun unlockCampaignChoice() {
        listOf("unlock-term-a", "unlock-term-b", "unlock-term-c").forEachIndexed { index, term ->
            commitJourney(event("campaign-unlock-$index", subjectId = term))
        }
    }

    private suspend fun commitCampaignChoice(
        event: GamificationEventEntity,
        routeId: String
    ): RewardWriteResult = dao.recordRewardAtomically(
        event = event,
        transactions = emptyList(),
        summary = emptySummary(event),
        presentationReceipts = emptyList(),
        campaignRouteChoice = CampaignSettlementPlanner.planChoice(event, routeId)
    )

    private fun String.toJourneyInstanceId(): String =
        com.cinnamon.app.data.gamification.StableRewardIds.journeyInstanceId(
            actorId = "learner",
            definitionId = this,
            definitionVersion = 1
        )

    private fun catalogUnlockCandidate(
        event: GamificationEventEntity,
        unlockId: String,
        transactionId: String = "transaction-$unlockId"
    ) = CatalogUnlockSettlementCandidate(
        unlock = AchievementUnlockEntity(
            unlockId = unlockId,
            actorId = event.actorId,
            achievementId = "achievement.test.mastery",
            level = 1,
            catalogVersion = 1,
            progressJson = "{}",
            unlockedAtEpochMillis = event.occurredAtEpochMillis,
            sourceEventId = event.eventId,
            evidenceJson = "{}"
        ),
        metric = CatalogEvidenceMetric.MASTERED_ITEMS_AFTER_DELAY,
        target = 1L,
        xpTransaction = RewardTransactionEntity(
            transactionId = transactionId,
            eventId = event.eventId,
            actorId = event.actorId,
            transactionKind = "grant",
            currency = "xp",
            amount = 20L,
            ruleId = "catalog.achievement.test.mastery.level.1",
            ruleVersion = 1,
            reasonCode = "achievement_level_unlocked",
            createdAtEpochMillis = event.occurredAtEpochMillis,
            idempotencyKey = transactionId,
            metadataJson = "{}"
        ),
        unlockedContentIds = emptyList(),
        presentationReceipt = RewardPresentationReceiptEntity(
            receiptId = "receipt-$unlockId",
            actorId = event.actorId,
            sourceEventId = event.eventId,
            sourceTransactionId = transactionId,
            presentationFamily = "presentation.achievement.level_up",
            idempotencyKey = "receipt-$unlockId",
            priority = 30,
            tier = "milestone",
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

    private fun dailyQuestCandidate(event: GamificationEventEntity) = CatalogQuestSettlementCandidate(
        instance = QuestInstanceEntity(
            questInstanceId = "quest-daily-due-review",
            actorId = event.actorId,
            definitionId = "quest.daily.due_review",
            catalogVersion = 1,
            cadence = "daily",
            startsAtEpochMillis = event.occurredAtEpochMillis - 1_000L,
            endsAtEpochMillis = event.occurredAtEpochMillis + 86_400_000L,
            state = "available",
            progress = 0L,
            target = 3L,
            criteriaJson = "{}",
            rewardJson = "[\"reward.xp.10\"]",
            createdAtEpochMillis = event.occurredAtEpochMillis,
            updatedAtEpochMillis = event.occurredAtEpochMillis,
            completionEventId = null,
            completedAtEpochMillis = null,
            claimedAtEpochMillis = null
        ),
        metric = CatalogEvidenceMetric.DISTINCT_DUE_REVIEWS_TODAY,
        eligibleForAssignment = true
    )

    private suspend fun commitCatalog(
        event: GamificationEventEntity,
        settlement: CatalogSettlementRequest
    ): RewardWriteResult = dao.recordRewardAtomically(
        event = event,
        transactions = emptyList(),
        summary = emptySummary(event),
        presentationReceipts = emptyList(),
        catalogSettlement = settlement
    )

    private suspend fun commitJourney(event: GamificationEventEntity): RewardWriteResult =
        dao.recordRewardAtomically(
            event = event,
            transactions = emptyList(),
            summary = emptySummary(event),
            presentationReceipts = emptyList(),
            journeySettlement = JourneySettlementPlanner.plan(event)
        )

    private suspend fun commitQuestClaim(event: GamificationEventEntity): RewardWriteResult {
        val transactionId = "transaction-${event.eventId}"
        return dao.recordRewardAtomically(
            event = event,
            transactions = listOf(
                RewardTransactionEntity(
                    transactionId = transactionId,
                    eventId = event.eventId,
                    actorId = event.actorId,
                    transactionKind = "grant",
                    currency = "xp",
                    amount = 10L,
                    ruleId = "catalog.quest.daily.due_review.claim",
                    ruleVersion = 1,
                    reasonCode = "quest_claimed",
                    createdAtEpochMillis = event.occurredAtEpochMillis,
                    idempotencyKey = transactionId,
                    metadataJson = "{}"
                )
            ),
            summary = emptySummary(event).copy(
                xpAwarded = 10L,
                currencyRewardsJson = "[{\"currency\":\"xp\",\"amount\":10}]",
                questProgressJson = "[{\"questInstanceId\":\"quest-daily-due-review\",\"claimed\":true}]"
            ),
            presentationReceipts = emptyList(),
            questClaimInstanceId = "quest-daily-due-review"
        )
    }

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

    private suspend fun commit(
        eventKey: String,
        xp: Long,
        eventType: String = "review_completed",
        subjectType: String = "lexicon_entry",
        subjectId: String = eventKey,
        includePresentationReceipt: Boolean = false
    ): RewardWriteResult {
        val now = 1_700_000_000_000L
        val eventId = "event-$eventKey"
        val transactionId = "transaction-$eventKey"
        return dao.recordRewardAtomically(
            event = GamificationEventEntity(
                eventId = eventId,
                actorId = "learner",
                eventType = eventType,
                subjectType = subjectType,
                subjectId = subjectId,
                occurredAtEpochMillis = now,
                recordedAtEpochMillis = now,
                studyDay = 19_675L,
                idempotencyKey = "idempotency-$eventKey",
                source = "test",
                ruleVersion = 1,
                metadataJson = "{}",
                replayOfEventId = null
            ),
            transactions = if (xp > 0L) {
                listOf(
                    RewardTransactionEntity(
                        transactionId = transactionId,
                        eventId = eventId,
                        actorId = "learner",
                        transactionKind = "grant",
                        currency = "xp",
                        amount = xp,
                        ruleId = "test.reward",
                        ruleVersion = 1,
                        reasonCode = "test_reward",
                        createdAtEpochMillis = now,
                        idempotencyKey = transactionId,
                        metadataJson = "{}"
                    )
                )
            } else {
                emptyList()
            },
            summary = RewardSummaryEntity(
                eventId = eventId,
                actorId = "learner",
                ruleVersion = 1,
                xpAwarded = xp,
                currencyRewardsJson = "[]",
                questProgressJson = "[]",
                achievementIdsJson = "[]",
                unlockedContentJson = "[]",
                celebrationTier = "micro",
                summaryJson = "{}",
                createdAtEpochMillis = now
            ),
            presentationReceipts = if (includePresentationReceipt) {
                listOf(
                    RewardPresentationReceiptEntity(
                        receiptId = "receipt-$eventKey",
                        actorId = "learner",
                        sourceEventId = eventId,
                        sourceTransactionId = transactionId,
                        presentationFamily = "presentation.progress.inline",
                        idempotencyKey = "receipt-$eventKey",
                        priority = 1,
                        tier = "micro",
                        state = "pending",
                        immutableSummaryJson = "{\"xpAwarded\":$xp}",
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                        expiresAtEpochMillis = null,
                        acknowledgedAtEpochMillis = null,
                        suppressionReason = null,
                        coalescedCount = 1
                    )
                )
            } else {
                emptyList()
            }
        )
    }
}
