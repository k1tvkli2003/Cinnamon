package com.cinnamon.app.data.gamification

import com.cinnamon.app.data.local.GamificationEventEntity
import com.cinnamon.app.domain.gamification.FoundationCampaignCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignSettlementPlannerTest {

    @Test
    fun `choice planner is stable and initializes only the selected route without settlement`() {
        val event = event("campaign-choice")
        val first = CampaignSettlementPlanner.planChoice(event, "route.precision-trail")
        val replay = CampaignSettlementPlanner.planChoice(event, "route.precision-trail")

        assertEquals(first, replay)
        assertEquals("route.precision-trail", first.choice?.routeId)
        assertEquals("journey.route.precision-trail", first.choice?.journeyDefinitionId)
        assertEquals(event.eventId, first.choice?.sourceEventId)
        assertEquals(3, first.selectedRouteJourney.stages.size)
        assertFalse(first.selectedRouteJourney.settleEvidenceOnThisEvent)
        assertEquals(
            FoundationCampaignCatalog.definition.prerequisiteStageDefinitionId,
            first.prerequisiteStageDefinitionId
        )
        assertEquals(
            FoundationCampaignCatalog.definition.prerequisiteJourneyDefinitionVersion,
            first.prerequisiteJourneyDefinitionVersion
        )
    }

    @Test
    fun `choice planner rejects an unknown route instead of fabricating a fallback`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            CampaignSettlementPlanner.planChoice(event("unknown-route"), "route.unknown")
        }

        assertTrue(failure.message.orEmpty().contains("Unknown campaign route"))
    }

    private fun event(id: String) = GamificationEventEntity(
        eventId = id,
        actorId = "learner",
        eventType = "campaign_route_selected",
        subjectType = "campaign_route",
        subjectId = "route.precision-trail",
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
