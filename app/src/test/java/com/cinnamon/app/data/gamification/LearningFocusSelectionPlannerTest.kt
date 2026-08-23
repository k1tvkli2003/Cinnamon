package com.cinnamon.app.data.gamification

import com.cinnamon.app.data.local.GamificationEventEntity
import com.cinnamon.app.domain.gamification.FoundationLearningFocusCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningFocusSelectionPlannerTest {

    @Test
    fun `selection planning is stable and initializes only the selected focus without settlement`() {
        val event = event("focus-selection")
        val optionId = "learning-focus.option.language-precision"
        val first = LearningFocusSelectionPlanner.planSelection(event, optionId)
        val replay = LearningFocusSelectionPlanner.planSelection(event, optionId)
        val option = requireNotNull(FoundationLearningFocusCatalog.definition.option(optionId))

        assertEquals(first, replay)
        assertEquals(FoundationLearningFocusCatalog.definition.id, first.selection?.definitionId)
        assertEquals(option.id, first.selection?.optionId)
        assertEquals(option.milestonePlan.id, first.selection?.milestonePlanDefinitionId)
        assertEquals(event.eventId, first.selection?.sourceEventId)
        assertEquals(3, first.selectedFocusProgress.stages.size)
        assertFalse(first.selectedFocusProgress.settleEvidenceOnThisEvent)
        assertEquals(event.occurredAtEpochMillis, first.selectedFocusProgress.evidenceAfterEpochMillis)
        assertEquals(
            FoundationLearningFocusCatalog.definition.prerequisiteMilestoneDefinitionId,
            first.prerequisiteMilestoneDefinitionId
        )
    }

    @Test
    fun `new selection uses the canonical Learning Focus stable identity`() {
        val planned = LearningFocusSelectionPlanner.planSelection(
            event("stable-selection"),
            "learning-focus.option.language-precision"
        )

        assertEquals(
            StableRewardIds.learningFocusSelectionId(
                actorId = "learner",
                definitionId = FoundationLearningFocusCatalog.definition.id,
                definitionVersion = FoundationLearningFocusCatalog.definition.version
            ),
            planned.selection?.selectionId
        )
        assertTrue(planned.selection?.selectionId.orEmpty().startsWith("learning_focus_selection_"))
    }

    @Test
    fun `compatibility restores both stored options without exposing stored ids`() {
        val definition = FoundationLearningFocusCatalog.definition

        definition.options.forEach { option ->
            val legacyPlan = LegacyLearningFocusWireCompatibility.legacyMilestonePlan(option)
            val restored = LegacyLearningFocusWireCompatibility.canonicalOption(
                definition = definition,
                storedDefinitionId = LegacyLearningFocusWireCompatibility.legacyDefinitionId(definition),
                storedDefinitionVersion = definition.version,
                storedOptionId = LegacyLearningFocusWireCompatibility.legacyOptionId(option),
                storedMilestonePlanDefinitionId = legacyPlan.id
            )

            assertEquals(option, restored)
            assertTrue(option.id.startsWith("learning-focus.option."))
            assertTrue(option.milestonePlan.id.startsWith("milestone-plan.learning-focus."))
        }
    }

    @Test
    fun `record projection keeps compatible rows canonical and preserves incompatible rows`() {
        val definition = FoundationLearningFocusCatalog.definition
        val selection = requireNotNull(
            LearningFocusSelectionPlanner.planSelection(
                event("record-projection"),
                "learning-focus.option.language-precision"
            ).selection
        )

        val compatible = LegacyLearningFocusWireCompatibility.toRecord(definition, selection)
            as LearningFocusSelectionRecord.Compatible
        val incompatible = LegacyLearningFocusWireCompatibility.toRecord(
            definition,
            selection.copy(milestonePlanDefinitionId = "unknown.persisted.plan")
        ) as LearningFocusSelectionRecord.Incompatible

        assertEquals("learning-focus.definition.foundation", compatible.definitionId)
        assertEquals("learning-focus.option.language-precision", compatible.optionId)
        assertEquals(
            "milestone-plan.learning-focus.language-precision",
            compatible.milestonePlanDefinitionId
        )
        assertEquals(selection.selectionId, incompatible.selectionId)
        assertEquals(
            LearningFocusCompatibilityFailure.OPTION_OR_PLAN_IDENTITY_MISMATCH,
            incompatible.reason
        )
    }

    @Test
    fun `planner rejects an unknown option instead of fabricating a fallback`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            LearningFocusSelectionPlanner.planSelection(
                event("unknown-option"),
                "learning-focus.option.unknown"
            )
        }

        assertTrue(failure.message.orEmpty().contains("Unknown Learning Focus option"))
    }

    private fun event(id: String) = GamificationEventEntity(
        eventId = id,
        actorId = "learner",
        eventType = "learning_focus_selected",
        subjectType = "learning_focus_definition",
        subjectId = FoundationLearningFocusCatalog.definition.id,
        occurredAtEpochMillis = 1_700_000_000_000L,
        recordedAtEpochMillis = 1_700_000_000_000L,
        studyDay = 19_675L,
        idempotencyKey = "idempotency-$id",
        source = "learning_focus",
        ruleVersion = 1,
        metadataJson = "{}",
        replayOfEventId = null
    )
}
