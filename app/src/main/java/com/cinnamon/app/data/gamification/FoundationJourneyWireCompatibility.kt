package com.cinnamon.app.data.gamification

import com.cinnamon.app.domain.gamification.JourneyDefinition

/**
 * The only active-code boundary allowed to recognize the Foundation plan's pre-v5 wire identity.
 * Fresh rows always use the canonical catalog id; an existing row keeps its exact persisted id so
 * stable instance, stage, transaction, and receipt identities are never forked during upgrade.
 */
internal object LegacyFoundationJourneyWireCompatibility {
    private const val LEGACY_DEFINITION_ID = "journey.foundation.expedition"

    fun acceptedDefinitionIds(definition: JourneyDefinition): List<String> =
        listOf(definition.id, LEGACY_DEFINITION_ID).distinct()

    fun legacyDefinitionId(definition: JourneyDefinition): String {
        require(definition.id != LEGACY_DEFINITION_ID) {
            "The canonical Foundation definition must not mint the legacy wire identity"
        }
        return LEGACY_DEFINITION_ID
    }

    fun matchesDefinitionIdentity(
        definition: JourneyDefinition,
        storedDefinitionId: String,
        storedDefinitionVersion: Int
    ): Boolean =
        storedDefinitionVersion == definition.version &&
            storedDefinitionId in acceptedDefinitionIds(definition)

    fun definitionForStoredIdentity(
        definition: JourneyDefinition,
        storedDefinitionId: String,
        storedDefinitionVersion: Int
    ): JourneyDefinition? {
        if (!matchesDefinitionIdentity(definition, storedDefinitionId, storedDefinitionVersion)) {
            return null
        }
        return if (storedDefinitionId == definition.id) {
            definition
        } else {
            definition.copy(id = storedDefinitionId)
        }
    }
}

internal class FoundationJourneyRecordIncompatibleException(
    val persistedInstanceIds: Set<String>
) : IllegalStateException("More than one compatible Foundation plan instance is persisted")
