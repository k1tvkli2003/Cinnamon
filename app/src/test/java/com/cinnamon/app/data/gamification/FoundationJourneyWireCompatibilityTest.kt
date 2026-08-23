package com.cinnamon.app.data.gamification

import com.cinnamon.app.domain.gamification.FoundationJourneyCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoundationJourneyWireCompatibilityTest {
    private val definition = FoundationJourneyCatalog.definition

    @Test
    fun `fresh Foundation identity is canonical and legacy identity is read only`() {
        val legacyId = LegacyFoundationJourneyWireCompatibility.legacyDefinitionId(definition)

        assertEquals("journey.foundation.plan", definition.id)
        assertEquals("journey.foundation.expedition", legacyId)
        assertEquals(
            listOf("journey.foundation.plan", "journey.foundation.expedition"),
            LegacyFoundationJourneyWireCompatibility.acceptedDefinitionIds(definition)
        )
    }

    @Test
    fun `legacy row projects canonical content without changing its persisted identity`() {
        val legacyId = LegacyFoundationJourneyWireCompatibility.legacyDefinitionId(definition)
        val projected = requireNotNull(
            LegacyFoundationJourneyWireCompatibility.definitionForStoredIdentity(
                definition = definition,
                storedDefinitionId = legacyId,
                storedDefinitionVersion = definition.version
            )
        )

        assertEquals(legacyId, projected.id)
        assertEquals(definition.stages, projected.stages)
        assertEquals(definition.title, projected.title)
        assertTrue(
            LegacyFoundationJourneyWireCompatibility.matchesDefinitionIdentity(
                definition,
                legacyId,
                definition.version
            )
        )
    }

    @Test
    fun `unknown id or version never enters the compatibility boundary`() {
        assertNull(
            LegacyFoundationJourneyWireCompatibility.definitionForStoredIdentity(
                definition,
                "journey.foundation.unknown",
                definition.version
            )
        )
        assertFalse(
            LegacyFoundationJourneyWireCompatibility.matchesDefinitionIdentity(
                definition,
                definition.id,
                definition.version + 1
            )
        )
    }
}
