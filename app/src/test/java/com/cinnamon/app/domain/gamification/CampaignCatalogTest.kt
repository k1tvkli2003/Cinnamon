package com.cinnamon.app.domain.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignCatalogTest {

    @Test
    fun `foundation campaign offers two distinct evidence-led routes with equal authored value`() {
        val definition = FoundationCampaignCatalog.definition

        assertEquals("journey.foundation.expedition", definition.prerequisiteJourneyDefinitionId)
        assertEquals("stage.memory-spark", definition.prerequisiteStageDefinitionId)
        assertEquals(2, definition.routes.size)
        assertEquals(2, definition.routes.map { it.id }.distinct().size)
        assertEquals(2, definition.routes.map { it.journey.id }.distinct().size)
        assertEquals(setOf(110L), definition.routes.map { it.totalRewardXp }.toSet())
        assertTrue(definition.routes.all { route -> route.journey.stages.size == 3 })
        assertTrue(definition.routes.flatMap { it.journey.stages }.all { stage ->
            stage.target > 0L && stage.rewardXp > 0L && stage.actionLabel.isNotBlank()
        })
    }

    @Test
    fun `campaign route lookup is exact and never guesses an unknown route`() {
        val definition = FoundationCampaignCatalog.definition

        assertNotNull(definition.route("route.precision-trail"))
        assertNotNull(definition.route("route.momentum-circuit"))
        assertEquals(null, definition.route("route.unknown"))
    }

    @Test
    fun `campaign definition rejects duplicate route journey identities`() {
        val original = FoundationCampaignCatalog.definition
        val duplicateJourney = original.routes[1].copy(journey = original.routes[0].journey)

        assertThrows(IllegalArgumentException::class.java) {
            original.copy(routes = listOf(original.routes[0], duplicateJourney))
        }
    }
}
