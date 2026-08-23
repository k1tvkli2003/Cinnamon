package com.cinnamon.app.viewmodel

import com.cinnamon.app.data.gamification.JourneySettlementPlanner
import com.cinnamon.app.data.gamification.LegacyFoundationJourneyWireCompatibility
import com.cinnamon.app.data.gamification.LegacyLearningFocusWireCompatibility
import com.cinnamon.app.data.gamification.LearningFocusCompatibilityFailure
import com.cinnamon.app.data.gamification.LearningFocusSelectionPlanner
import com.cinnamon.app.data.gamification.LearningFocusSelectionRecord
import com.cinnamon.app.data.local.GamificationEventEntity
import com.cinnamon.app.data.local.JourneyStageProgressEntity
import com.cinnamon.app.data.local.LearningFocusSelectionEntity
import com.cinnamon.app.domain.gamification.FoundationJourneyCatalog
import com.cinnamon.app.domain.gamification.FoundationLearningFocusCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningFocusProjectionTest {

    @Test
    fun `projection stays locked until the exact versioned prerequisite is complete`() {
        val foundation = foundationRows(prerequisiteCompleted = false)

        val state = projectLearningFocus(
            definition = FoundationLearningFocusCatalog.definition,
            selections = emptyList(),
            instances = listOf(foundation.first),
            stageRows = foundation.second,
            actionState = LearningFocusSelectionActionState()
        ) as LearningFocusUiState.Locked

        assertEquals(0, state.progress)
        assertEquals(3, state.target)
    }

    @Test
    fun `projection exposes two evidence-led focus options after unlock`() {
        val definition = FoundationLearningFocusCatalog.definition
        val foundation = foundationRows(prerequisiteCompleted = true)

        val state = projectLearningFocus(
            definition = definition,
            selections = emptyList(),
            instances = listOf(foundation.first),
            stageRows = foundation.second,
            actionState = LearningFocusSelectionActionState(
                savingOptionId = "learning-focus.option.language-precision"
            )
        ) as LearningFocusUiState.Choose

        assertEquals(2, state.options.size)
        assertEquals(setOf(110), state.options.map { it.totalRewardXp }.toSet())
        assertEquals(
            "learning-focus.option.language-precision",
            state.savingOptionId
        )
        assertEquals(
            listOf("practice formats", "delayed-recall items", "active days"),
            state.options.first().metrics.map(LearningFocusMetricUiModel::label)
        )
    }

    @Test
    fun `projection restores the exact legacy Foundation identity without minting it`() {
        val legacyDefinitionId = LegacyFoundationJourneyWireCompatibility
            .legacyDefinitionId(FoundationJourneyCatalog.definition)
        val foundation = foundationRows(
            prerequisiteCompleted = true,
            definitionId = legacyDefinitionId
        )

        val state = projectLearningFocus(
            definition = FoundationLearningFocusCatalog.definition,
            selections = emptyList(),
            instances = listOf(foundation.first),
            stageRows = foundation.second,
            actionState = LearningFocusSelectionActionState()
        )

        assertTrue(state is LearningFocusUiState.Choose)
        assertEquals(legacyDefinitionId, foundation.first.definitionId)
    }

    @Test
    fun `projection restores a new canonical Learning Focus selection`() {
        val definition = FoundationLearningFocusCatalog.definition
        val foundation = foundationRows(prerequisiteCompleted = true)
        val selectionRequest = LearningFocusSelectionPlanner.planSelection(
            event("select-precision", "learning_focus_selected"),
            "learning-focus.option.language-precision"
        )
        val selection = requireNotNull(selectionRequest.selection)
        val record = LegacyLearningFocusWireCompatibility.toRecord(definition, selection)
        val planInstance = requireNotNull(selectionRequest.selectedFocusProgress.instance)
        val planRows = selectionRequest.selectedFocusProgress.stages.map { it.stage }

        val state = projectLearningFocus(
            definition = definition,
            selections = listOf(record),
            instances = listOf(foundation.first, planInstance),
            stageRows = foundation.second + planRows,
            actionState = LearningFocusSelectionActionState()
        ) as LearningFocusUiState.Ready

        assertEquals("Language Precision", state.focusTitle)
        assertEquals(3, state.milestonePlan.totalStageCount)
        assertEquals(0, state.milestonePlan.completedStageCount)
        assertEquals(JourneyStageUiState.ACTIVE, state.milestonePlan.stages.first().state)
    }

    @Test
    fun `projection restores a migrated v4 selection through the bounded compatibility adapter`() {
        val definition = FoundationLearningFocusCatalog.definition
        val option = requireNotNull(
            definition.option("learning-focus.option.language-precision")
        )
        val legacyPlan = LegacyLearningFocusWireCompatibility.legacyMilestonePlan(option)
        val selectionEvent = event("legacy-select-precision", "campaign_route_selected")
        val selection = LearningFocusSelectionEntity(
            selectionId = LegacyLearningFocusWireCompatibility.legacySelectionId(
                actorId = selectionEvent.actorId,
                definition = definition
            ),
            actorId = selectionEvent.actorId,
            definitionId = LegacyLearningFocusWireCompatibility.legacyDefinitionId(definition),
            definitionVersion = definition.version,
            optionId = LegacyLearningFocusWireCompatibility.legacyOptionId(option),
            milestonePlanDefinitionId = legacyPlan.id,
            sourceEventId = selectionEvent.eventId,
            selectedAtEpochMillis = selectionEvent.occurredAtEpochMillis
        )
        val record = LegacyLearningFocusWireCompatibility.toRecord(definition, selection)
        val plan = JourneySettlementPlanner.plan(selectionEvent, legacyPlan)
        val foundation = foundationRows(prerequisiteCompleted = true)

        val state = projectLearningFocus(
            definition = definition,
            selections = listOf(record),
            instances = listOf(foundation.first, requireNotNull(plan.instance)),
            stageRows = foundation.second + plan.stages.map { candidate -> candidate.stage },
            actionState = LearningFocusSelectionActionState()
        ) as LearningFocusUiState.Ready

        assertEquals("Language Precision", state.focusTitle)
        assertEquals(requireNotNull(plan.instance).journeyInstanceId, state.milestonePlan.id)
        assertEquals(3, state.milestonePlan.totalStageCount)
    }

    @Test
    fun `projection surfaces missing ambiguous and incompatible records`() {
        val definition = FoundationLearningFocusCatalog.definition
        val foundation = foundationRows(prerequisiteCompleted = true)
        val selectionRequest = LearningFocusSelectionPlanner.planSelection(
            event("select-precision", "learning_focus_selected"),
            "learning-focus.option.language-precision"
        )
        val compatible = LegacyLearningFocusWireCompatibility.toRecord(
            definition,
            requireNotNull(selectionRequest.selection)
        ) as LearningFocusSelectionRecord.Compatible

        val missingRows = projectLearningFocus(
            definition = definition,
            selections = listOf(compatible),
            instances = listOf(foundation.first),
            stageRows = foundation.second,
            actionState = LearningFocusSelectionActionState()
        )
        val ambiguous = projectLearningFocus(
            definition = definition,
            selections = listOf(compatible, compatible.copy(selectionId = "duplicate")),
            instances = listOf(foundation.first),
            stageRows = foundation.second,
            actionState = LearningFocusSelectionActionState()
        )
        val incompatible = projectLearningFocus(
            definition = definition,
            selections = listOf(
                LearningFocusSelectionRecord.Incompatible(
                    selectionId = "incompatible",
                    reason = LearningFocusCompatibilityFailure.OPTION_OR_PLAN_IDENTITY_MISMATCH
                )
            ),
            instances = listOf(foundation.first),
            stageRows = foundation.second,
            actionState = LearningFocusSelectionActionState()
        )

        assertTrue(missingRows is LearningFocusUiState.Unavailable)
        assertTrue(ambiguous is LearningFocusUiState.Unavailable)
        assertTrue(incompatible is LearningFocusUiState.Unavailable)
    }

    @Test
    fun `projection rejects a prerequisite from another definition version`() {
        val foundation = foundationRows(prerequisiteCompleted = true)

        val state = projectLearningFocus(
            definition = FoundationLearningFocusCatalog.definition,
            selections = emptyList(),
            instances = listOf(foundation.first.copy(definitionVersion = 2)),
            stageRows = foundation.second,
            actionState = LearningFocusSelectionActionState()
        )

        assertTrue(state is LearningFocusUiState.Unavailable)
    }

    private fun foundationRows(
        prerequisiteCompleted: Boolean,
        definitionId: String = FoundationJourneyCatalog.definition.id
    ): Pair<com.cinnamon.app.data.local.JourneyInstanceEntity, List<JourneyStageProgressEntity>> {
        val definition = FoundationLearningFocusCatalog.definition
        val request = JourneySettlementPlanner.plan(
            event("foundation-bootstrap", "catalog_reconciled"),
            FoundationJourneyCatalog.definition.copy(id = definitionId)
        )
        val rows = request.stages.map { candidate ->
            val row = candidate.stage
            if (
                row.stageDefinitionId == definition.prerequisiteMilestoneDefinitionId &&
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
