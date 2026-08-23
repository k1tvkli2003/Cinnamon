package com.cinnamon.app.data.gamification

import com.cinnamon.app.data.local.GamificationEventEntity
import com.cinnamon.app.data.local.LearningFocusSelectionEntity
import com.cinnamon.app.data.local.LearningFocusSelectionRequest
import com.cinnamon.app.domain.gamification.FoundationLearningFocusCatalog
import com.cinnamon.app.domain.gamification.JourneyDefinition
import com.cinnamon.app.domain.gamification.LearningFocusDefinition
import com.cinnamon.app.domain.gamification.LearningFocusOption

enum class LearningFocusCompatibilityFailure {
    DEFINITION_IDENTITY_MISMATCH,
    OPTION_OR_PLAN_IDENTITY_MISMATCH,
    MULTIPLE_SELECTIONS
}

class LearningFocusRecordIncompatibleException(
    val selectionId: String,
    val reason: LearningFocusCompatibilityFailure
) : IllegalStateException("The saved Learning Focus record is incompatible with this catalog")

sealed interface LearningFocusSelectionRecord {
    val selectionId: String

    data class Compatible(
        override val selectionId: String,
        val actorId: String,
        val definitionId: String,
        val definitionVersion: Int,
        val optionId: String,
        val milestonePlanDefinitionId: String,
        val persistedMilestonePlanDefinitionId: String,
        val sourceEventId: String,
        val selectedAtEpochMillis: Long
    ) : LearningFocusSelectionRecord

    data class Incompatible(
        override val selectionId: String,
        val reason: LearningFocusCompatibilityFailure
    ) : LearningFocusSelectionRecord
}

/** Deterministic, zero-XP selection planning; Room remains the prerequisite and lock authority. */
object LearningFocusSelectionPlanner {
    fun planSelection(
        event: GamificationEventEntity,
        optionId: String,
        definition: LearningFocusDefinition = FoundationLearningFocusCatalog.definition
    ): LearningFocusSelectionRequest {
        val option = requireNotNull(definition.option(optionId)) {
            "Unknown Learning Focus option: $optionId"
        }
        return LearningFocusSelectionRequest(
            selection = LearningFocusSelectionEntity(
                selectionId = StableRewardIds.learningFocusSelectionId(
                    actorId = event.actorId,
                    definitionId = definition.id,
                    definitionVersion = definition.version
                ),
                actorId = event.actorId,
                definitionId = definition.id,
                definitionVersion = definition.version,
                optionId = option.id,
                milestonePlanDefinitionId = option.milestonePlan.id,
                sourceEventId = event.eventId,
                selectedAtEpochMillis = event.occurredAtEpochMillis
            ),
            prerequisiteDefinitionId = definition.prerequisiteDefinitionId,
            prerequisiteDefinitionVersion = definition.prerequisiteDefinitionVersion,
            prerequisiteMilestoneDefinitionId = definition.prerequisiteMilestoneDefinitionId,
            selectedFocusProgress = JourneySettlementPlanner.plan(event, option.milestonePlan).copy(
                settleEvidenceOnThisEvent = false,
                evidenceAfterEpochMillis = event.occurredAtEpochMillis
            )
        )
    }
}

/**
 * The only active-code boundary allowed to know frozen v4 identifiers. New product APIs use
 * canonical Learning Focus identities; persistence remains compatible with immutable v4 history.
 */
internal object LegacyLearningFocusWireCompatibility {
    private const val LEGACY_DEFINITION_ID = "campaign.foundation-route-choice"
    private const val LEGACY_LANGUAGE_PRECISION_OPTION_ID = "route.precision-trail"
    private const val LEGACY_RECALL_RANGE_OPTION_ID = "route.momentum-circuit"
    private const val LEGACY_LANGUAGE_PRECISION_PLAN_ID = "journey.route.precision-trail"
    private const val LEGACY_RECALL_RANGE_PLAN_ID = "journey.route.momentum-circuit"
    private const val LEGACY_SELECTION_ID_PREFIX = "campaign_choice"

    private val legacyPlanStageIdsByCanonicalId = mapOf(
        "milestone.language-precision.practice-kinds" to "stage.switch-the-rules",
        "milestone.language-precision.delayed-mastery" to "stage.prove-it-later",
        "milestone.language-precision.active-days" to "stage.hold-the-line",
        "milestone.recall-range.reviewed-items" to "stage.widen-recall",
        "milestone.recall-range.practice-kinds" to "stage.carry-meaning",
        "milestone.recall-range.active-days" to "stage.make-momentum-stick"
    )

    fun legacyDefinitionId(definition: LearningFocusDefinition): String {
        require(definition.id == FoundationLearningFocusCatalog.definition.id) {
            "No frozen storage identity exists for Learning Focus definition ${definition.id}"
        }
        return LEGACY_DEFINITION_ID
    }

    fun legacySelectionId(actorId: String, definition: LearningFocusDefinition): String =
        StableRewardIds.compatibilityStableId(
            LEGACY_SELECTION_ID_PREFIX,
            actorId,
            legacyDefinitionId(definition),
            definition.version.toString()
        )

    fun legacyOptionId(option: LearningFocusOption): String = when (option.id) {
        "learning-focus.option.language-precision" -> LEGACY_LANGUAGE_PRECISION_OPTION_ID
        "learning-focus.option.recall-range" -> LEGACY_RECALL_RANGE_OPTION_ID
        else -> error("No frozen storage identity exists for Learning Focus option ${option.id}")
    }

    fun legacyMilestonePlan(option: LearningFocusOption): JourneyDefinition {
        val legacyPlanId = when (option.id) {
            "learning-focus.option.language-precision" -> LEGACY_LANGUAGE_PRECISION_PLAN_ID
            "learning-focus.option.recall-range" -> LEGACY_RECALL_RANGE_PLAN_ID
            else -> error("No frozen storage identity exists for Learning Focus option ${option.id}")
        }
        return option.milestonePlan.copy(
            id = legacyPlanId,
            stages = option.milestonePlan.stages.map { milestone ->
                milestone.copy(
                    id = checkNotNull(legacyPlanStageIdsByCanonicalId[milestone.id]) {
                        "No frozen storage identity exists for milestone ${milestone.id}"
                    }
                )
            }
        )
    }

    fun milestonePlanForSelection(
        option: LearningFocusOption,
        persistedMilestonePlanDefinitionId: String
    ): JourneyDefinition = when (persistedMilestonePlanDefinitionId) {
        option.milestonePlan.id -> option.milestonePlan
        legacyMilestonePlan(option).id -> legacyMilestonePlan(option)
        else -> error(
            "No compatible milestone plan exists for $persistedMilestonePlanDefinitionId"
        )
    }

    fun matchesDefinitionIdentity(
        definition: LearningFocusDefinition,
        storedDefinitionId: String,
        storedDefinitionVersion: Int
    ): Boolean = storedDefinitionVersion == definition.version &&
        (storedDefinitionId == LEGACY_DEFINITION_ID || storedDefinitionId == definition.id)

    fun canonicalOption(
        definition: LearningFocusDefinition,
        storedDefinitionId: String,
        storedDefinitionVersion: Int,
        storedOptionId: String,
        storedMilestonePlanDefinitionId: String
    ): LearningFocusOption? {
        if (!matchesDefinitionIdentity(definition, storedDefinitionId, storedDefinitionVersion)) {
            return null
        }
        val canonicalOptionId = when (storedOptionId) {
            LEGACY_LANGUAGE_PRECISION_OPTION_ID,
            "learning-focus.option.language-precision" -> "learning-focus.option.language-precision"
            LEGACY_RECALL_RANGE_OPTION_ID,
            "learning-focus.option.recall-range" -> "learning-focus.option.recall-range"
            else -> return null
        }
        val option = definition.option(canonicalOptionId) ?: return null
        val acceptedPlanIds = setOf(
            option.milestonePlan.id,
            legacyMilestonePlan(option).id
        )
        return option.takeIf { storedMilestonePlanDefinitionId in acceptedPlanIds }
    }

    fun compatibilityFailure(
        definition: LearningFocusDefinition,
        selection: LearningFocusSelectionEntity
    ): LearningFocusCompatibilityFailure = if (
        matchesDefinitionIdentity(
            definition,
            selection.definitionId,
            selection.definitionVersion
        )
    ) {
        LearningFocusCompatibilityFailure.OPTION_OR_PLAN_IDENTITY_MISMATCH
    } else {
        LearningFocusCompatibilityFailure.DEFINITION_IDENTITY_MISMATCH
    }

    fun toRecord(
        definition: LearningFocusDefinition,
        selection: LearningFocusSelectionEntity
    ): LearningFocusSelectionRecord {
        val option = canonicalOption(
            definition = definition,
            storedDefinitionId = selection.definitionId,
            storedDefinitionVersion = selection.definitionVersion,
            storedOptionId = selection.optionId,
            storedMilestonePlanDefinitionId = selection.milestonePlanDefinitionId
        ) ?: return LearningFocusSelectionRecord.Incompatible(
            selectionId = selection.selectionId,
            reason = compatibilityFailure(definition, selection)
        )
        return LearningFocusSelectionRecord.Compatible(
            selectionId = selection.selectionId,
            actorId = selection.actorId,
            definitionId = definition.id,
            definitionVersion = definition.version,
            optionId = option.id,
            milestonePlanDefinitionId = option.milestonePlan.id,
            persistedMilestonePlanDefinitionId = selection.milestonePlanDefinitionId,
            sourceEventId = selection.sourceEventId,
            selectedAtEpochMillis = selection.selectedAtEpochMillis
        )
    }
}
