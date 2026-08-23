package com.cinnamon.app.domain.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningFocusCatalogTest {

    @Test
    fun `foundation focus offers two distinct evidence-led options with equal authored value`() {
        val definition = FoundationLearningFocusCatalog.definition

        assertEquals("stage.memory-spark", definition.prerequisiteMilestoneDefinitionId)
        assertEquals(2, definition.options.size)
        assertEquals(2, definition.options.map { it.id }.distinct().size)
        assertEquals(2, definition.options.map { it.milestonePlan.id }.distinct().size)
        assertEquals(setOf(110L), definition.options.map { it.totalRewardXp }.toSet())
        assertTrue(definition.options.all { option -> option.milestonePlan.stages.size == 3 })
        assertTrue(definition.options.flatMap { it.milestonePlan.stages }.all { milestone ->
            milestone.target > 0L && milestone.rewardXp > 0L && milestone.actionLabel.isNotBlank()
        })
    }

    @Test
    fun `each option maps exactly to its promised evidence thresholds`() {
        val definition = FoundationLearningFocusCatalog.definition
        val precision = requireNotNull(definition.option("learning-focus.option.language-precision"))
        val range = requireNotNull(definition.option("learning-focus.option.recall-range"))

        assertEquals(
            listOf(
                JourneyEvidenceMetric.DISTINCT_PRACTICE_CONTENT_KINDS to 3L,
                JourneyEvidenceMetric.MASTERED_ITEMS_AFTER_DELAY to 2L,
                JourneyEvidenceMetric.ACTIVE_STUDY_DAYS to 5L
            ),
            precision.milestonePlan.stages.map { it.evidenceMetric to it.target }
        )
        assertEquals(
            listOf(
                JourneyEvidenceMetric.DISTINCT_REVIEWED_ITEMS to 8L,
                JourneyEvidenceMetric.DISTINCT_PRACTICE_CONTENT_KINDS to 4L,
                JourneyEvidenceMetric.ACTIVE_STUDY_DAYS to 5L
            ),
            range.milestonePlan.stages.map { it.evidenceMetric to it.target }
        )
    }

    @Test
    fun `option lookup is exact and never guesses an unknown option`() {
        val definition = FoundationLearningFocusCatalog.definition

        assertNotNull(definition.option("learning-focus.option.language-precision"))
        assertNotNull(definition.option("learning-focus.option.recall-range"))
        assertNull(definition.option("learning-focus.option.unknown"))
    }

    @Test
    fun `definition rejects duplicate milestone-plan identities`() {
        val original = FoundationLearningFocusCatalog.definition
        val duplicatePlan = original.options[1].copy(milestonePlan = original.options[0].milestonePlan)

        assertThrows(IllegalArgumentException::class.java) {
            original.copy(options = listOf(original.options[0], duplicatePlan))
        }
    }

    @Test
    fun `active copy contains no legacy framing or unsupported outcome claim`() {
        val definition = FoundationLearningFocusCatalog.definition
        val activeCopy = buildList {
            add(definition.eyebrow)
            add(definition.title)
            add(definition.description)
            definition.options.forEach { option ->
                add(option.title)
                add(option.tagline)
                add(option.metricSummary)
                add(option.milestonePlan.eyebrow)
                add(option.milestonePlan.title)
                add(option.milestonePlan.description)
                add(option.milestonePlan.completionTitle)
                add(option.milestonePlan.completionDescription)
                option.milestonePlan.stages.forEach { milestone ->
                    add(milestone.title)
                    add(milestone.description)
                    add(milestone.actionLabel)
                }
            }
        }.joinToString(" ").lowercase()

        listOf(
            "campaign",
            "route",
            "expedition",
            "trail",
            "circuit",
            "clinical competence",
            "fluency",
            "complete language mastery",
            "adaptive",
            "synchron"
        ).forEach { forbidden ->
            assertTrue("Unexpected active-copy term: $forbidden", forbidden !in activeCopy)
        }
    }

}
