package com.cinnamon.app.data.gamification

import com.cinnamon.app.data.local.GamificationEventEntity
import com.cinnamon.app.domain.gamification.FoundationJourneyCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneySettlementPlannerTest {

    @Test
    fun `planner authors one active stage followed by ordered locked stages`() {
        val request = JourneySettlementPlanner.plan(event("event-a"))

        assertEquals("active", request.instance?.state)
        assertEquals(1, request.instance?.currentStageOrder)
        assertEquals(listOf(1, 2, 3, 4), request.stages.map { it.stage.stageOrder })
        assertEquals(listOf("active", "locked", "locked", "locked"), request.stages.map { it.stage.state })
        assertEquals(listOf(10L, 20L, 20L, 30L), request.stages.map { it.stage.rewardXp })
        assertEquals(
            FoundationJourneyCatalog.definition.stages.map { it.id },
            request.stages.map { it.stage.stageDefinitionId }
        )
    }

    @Test
    fun `planner creates stable auditable reward and receipt identities`() {
        val first = JourneySettlementPlanner.plan(event("event-stable"))
        val replay = JourneySettlementPlanner.plan(event("event-stable"))

        assertEquals(first, replay)
        first.stages.forEach { candidate ->
            assertTrue(candidate.rewardTransaction.ruleId.startsWith("journey."))
            assertEquals(candidate.stage.rewardXp, candidate.rewardTransaction.amount)
            assertEquals(
                candidate.rewardTransaction.transactionId,
                candidate.presentationReceipt.sourceTransactionId
            )
            assertEquals(
                GamificationPresentationIds.JOURNEY_STAGE_COMPLETE,
                candidate.presentationReceipt.presentationFamily
            )
        }
    }

    private fun event(id: String) = GamificationEventEntity(
        eventId = id,
        actorId = "learner",
        eventType = "review_completed",
        subjectType = "lexicon_entry",
        subjectId = "term-a",
        occurredAtEpochMillis = 1_700_000_000_000L,
        recordedAtEpochMillis = 1_700_000_000_000L,
        studyDay = 19_675L,
        idempotencyKey = "idempotency-$id",
        source = "test",
        ruleVersion = 1,
        metadataJson = "{}",
        replayOfEventId = null
    )
}
