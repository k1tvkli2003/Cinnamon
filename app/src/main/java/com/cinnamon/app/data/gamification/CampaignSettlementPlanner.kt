package com.cinnamon.app.data.gamification

import com.cinnamon.app.data.local.CampaignRouteChoiceEntity
import com.cinnamon.app.data.local.CampaignRouteChoiceRequest
import com.cinnamon.app.data.local.GamificationEventEntity
import com.cinnamon.app.domain.gamification.CampaignDefinition
import com.cinnamon.app.domain.gamification.FoundationCampaignCatalog

/** Deterministic campaign choice planning; Room remains the authority for prerequisite and lock. */
object CampaignSettlementPlanner {
    fun planChoice(
        event: GamificationEventEntity,
        routeId: String,
        definition: CampaignDefinition = FoundationCampaignCatalog.definition
    ): CampaignRouteChoiceRequest {
        val route = requireNotNull(definition.route(routeId)) {
            "Unknown campaign route: $routeId"
        }
        val choiceId = StableRewardIds.campaignRouteChoiceId(
            actorId = event.actorId,
            campaignDefinitionId = definition.id,
            campaignDefinitionVersion = definition.version
        )
        return CampaignRouteChoiceRequest(
            choice = CampaignRouteChoiceEntity(
                choiceId = choiceId,
                actorId = event.actorId,
                campaignDefinitionId = definition.id,
                campaignDefinitionVersion = definition.version,
                routeId = route.id,
                journeyDefinitionId = route.journey.id,
                sourceEventId = event.eventId,
                chosenAtEpochMillis = event.occurredAtEpochMillis
            ),
            prerequisiteJourneyDefinitionId = definition.prerequisiteJourneyDefinitionId,
            prerequisiteJourneyDefinitionVersion = definition.prerequisiteJourneyDefinitionVersion,
            prerequisiteStageDefinitionId = definition.prerequisiteStageDefinitionId,
            selectedRouteJourney = JourneySettlementPlanner.plan(event, route.journey).copy(
                settleEvidenceOnThisEvent = false
            )
        )
    }
}
