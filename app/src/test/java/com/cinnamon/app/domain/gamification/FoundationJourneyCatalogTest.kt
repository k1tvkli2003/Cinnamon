package com.cinnamon.app.domain.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FoundationJourneyCatalogTest {

    @Test
    fun `foundation plan preserves stable evidence and reward contract`() {
        val definition = FoundationJourneyCatalog.definition

        assertEquals("journey.foundation.plan", definition.id)
        assertEquals(1, definition.version)
        assertEquals(
            listOf(
                JourneyEvidenceMetric.DISTINCT_REVIEWED_ITEMS,
                JourneyEvidenceMetric.DISTINCT_PRACTICE_CONTENT_KINDS,
                JourneyEvidenceMetric.MASTERED_ITEMS_AFTER_DELAY,
                JourneyEvidenceMetric.ACTIVE_STUDY_DAYS
            ),
            definition.stages.map(JourneyStageDefinition::evidenceMetric)
        )
        assertEquals(listOf(3L, 2L, 1L, 3L), definition.stages.map(JourneyStageDefinition::target))
        assertEquals(listOf(10L, 20L, 20L, 30L), definition.stages.map(JourneyStageDefinition::rewardXp))
    }

    @Test
    fun `foundation presentation copy stays language-native`() {
        val definition = FoundationJourneyCatalog.definition
        val visibleCopy = buildList {
            add(definition.eyebrow)
            add(definition.title)
            add(definition.description)
            add(definition.completionTitle)
            add(definition.completionDescription)
            definition.stages.forEach { stage ->
                add(stage.title)
                add(stage.description)
                add(stage.actionLabel)
            }
        }.joinToString(" ").lowercase()

        listOf("campaign", "expedition", "trail", "circuit", "route", "chapter").forEach { forbidden ->
            assertFalse("Visible Foundation copy contains '$forbidden'", visibleCopy.contains(forbidden))
        }
    }
}
