package com.cinnamon.app.viewmodel

import com.cinnamon.app.data.gamification.CampaignSettlementPlanner
import com.cinnamon.app.data.gamification.JourneySettlementPlanner
import com.cinnamon.app.data.local.GamificationEventEntity
import com.cinnamon.app.data.local.JourneyStageProgressEntity
import com.cinnamon.app.domain.gamification.FoundationCampaignCatalog
import com.cinnamon.app.domain.gamification.FoundationJourneyCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignProjectionTest {

    @Test
    fun `projection stays locked until the exact versioned prerequisite is complete`() {
        val foundation = foundationRows(prerequisiteCompleted = false)

        val state = projectCampaignRoute(
            definition = FoundationCampaignCatalog.definition,
            choices = emptyList(),
            instances = listOf(foundation.first),
            stageRows = foundation.second,
            actionState = CampaignChoiceActionState()
        )

        assertTrue(state is CampaignUiState.Locked)
    }

    @Test
    fun `projection exposes both balanced authored routes after unlock`() {
        val foundation = foundationRows(prerequisiteCompleted = true)

        val state = projectCampaignRoute(
            definition = FoundationCampaignCatalog.definition,
            choices = emptyList(),
            instances = listOf(foundation.first),
            stageRows = foundation.second,
            actionState = CampaignChoiceActionState(savingRouteId = "route.precision-trail")
        ) as CampaignUiState.Choose

        assertEquals(2, state.routes.size)
        assertEquals(setOf(110), state.routes.map { it.totalRewardXp }.toSet())
        assertEquals("route.precision-trail", state.savingRouteId)
    }

    @Test
    fun `projection restores the selected persisted route and its real chapter rows`() {
        val foundation = foundationRows(prerequisiteCompleted = true)
        val choiceRequest = CampaignSettlementPlanner.planChoice(
            event("select-precision", "campaign_route_selected"),
            "route.precision-trail"
        )
        val routeInstance = requireNotNull(choiceRequest.selectedRouteJourney.instance)
        val routeRows = choiceRequest.selectedRouteJourney.stages.map { it.stage }

        val state = projectCampaignRoute(
            definition = FoundationCampaignCatalog.definition,
            choices = listOf(requireNotNull(choiceRequest.choice)),
            instances = listOf(foundation.first, routeInstance),
            stageRows = foundation.second + routeRows,
            actionState = CampaignChoiceActionState()
        ) as CampaignUiState.Ready

        assertEquals("Precision Trail", state.routeTitle)
        assertEquals(3, state.journey.totalStageCount)
        assertEquals(0, state.journey.completedStageCount)
        assertEquals(JourneyStageUiState.ACTIVE, state.journey.stages.first().state)
    }

    @Test
    fun `projection shows recovery instead of fabricating missing or ambiguous persistence`() {
        val foundation = foundationRows(prerequisiteCompleted = true)
        val precision = CampaignSettlementPlanner.planChoice(
            event("select-precision", "campaign_route_selected"),
            "route.precision-trail"
        )
        val momentum = CampaignSettlementPlanner.planChoice(
            event("select-momentum", "campaign_route_selected"),
            "route.momentum-circuit"
        )

        val missingRows = projectCampaignRoute(
            definition = FoundationCampaignCatalog.definition,
            choices = listOf(requireNotNull(precision.choice)),
            instances = listOf(foundation.first),
            stageRows = foundation.second,
            actionState = CampaignChoiceActionState()
        )
        val ambiguous = projectCampaignRoute(
            definition = FoundationCampaignCatalog.definition,
            choices = listOf(requireNotNull(precision.choice), requireNotNull(momentum.choice)),
            instances = listOf(foundation.first),
            stageRows = foundation.second,
            actionState = CampaignChoiceActionState()
        )

        assertTrue(missingRows is CampaignUiState.Unavailable)
        assertTrue(ambiguous is CampaignUiState.Unavailable)
    }

    @Test
    fun `projection rejects a prerequisite from another definition version`() {
        val foundation = foundationRows(prerequisiteCompleted = true)

        val state = projectCampaignRoute(
            definition = FoundationCampaignCatalog.definition,
            choices = emptyList(),
            instances = listOf(foundation.first.copy(definitionVersion = 2)),
            stageRows = foundation.second,
            actionState = CampaignChoiceActionState()
        )

        assertTrue(state is CampaignUiState.Unavailable)
    }

    private fun foundationRows(
        prerequisiteCompleted: Boolean
    ): Pair<com.cinnamon.app.data.local.JourneyInstanceEntity, List<JourneyStageProgressEntity>> {
        val request = JourneySettlementPlanner.plan(
            event("foundation-bootstrap", "catalog_reconciled"),
            FoundationJourneyCatalog.definition
        )
        val rows = request.stages.map { candidate ->
            val row = candidate.stage
            if (row.stageDefinitionId == FoundationCampaignCatalog.definition.prerequisiteStageDefinitionId &&
                prerequisiteCompleted
            ) {
                row.copy(state = "completed", progress = row.target)
            } else {
                row
            }
        }
        return requireNotNull(request.instance) to rows
    }

    private fun event(id: String, type: String) = GamificationEventEntity(
        eventId = id,
        actorId = "local-user",
        eventType = type,
        subjectType = "test",
        subjectId = id,
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
