package com.cinnamon.app.domain.gamification

data class CatalogValidationIssue(
    val code: String,
    val path: String,
    val message: String
)

data class CatalogValidationReport(
    val errors: List<CatalogValidationIssue>,
    val warnings: List<CatalogValidationIssue> = emptyList()
) {
    val isValid: Boolean get() = errors.isEmpty()
}

/** Pure validation: no clock, device, persistence, or UI state is consulted. */
object GamificationCatalogValidator {
    const val SUPPORTED_SCHEMA_VERSION = 1
    const val SUPPORTED_CRITERION_CONTRACT_VERSION = 1

    private val stableId = Regex("^[a-z][a-z0-9]*(?:[._-][a-z0-9]+)*$")
    private val semanticVersion = Regex("^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$")

    fun validate(bundle: GamificationCatalogBundle): CatalogValidationReport {
        val catalog = bundle.catalog
        val copy = bundle.copy
        val errors = mutableListOf<CatalogValidationIssue>()

        fun error(code: String, path: String, message: String) {
            errors += CatalogValidationIssue(code, path, message)
        }

        if (catalog.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            error("unsupported_schema", "schemaVersion", "Expected schema version $SUPPORTED_SCHEMA_VERSION.")
        }
        if (catalog.storageVersion <= 0) {
            error("invalid_storage_version", "storageVersion", "Persistence version must be positive.")
        }
        if (catalog.criterionContractVersion != SUPPORTED_CRITERION_CONTRACT_VERSION) {
            error(
                "unsupported_criterion_contract",
                "criterionContractVersion",
                "Expected criterion contract version $SUPPORTED_CRITERION_CONTRACT_VERSION."
            )
        }
        if (!semanticVersion.matches(catalog.catalogVersion)) {
            error("invalid_version", "catalogVersion", "Catalog version must be semantic versioning.")
        } else if (catalog.catalogVersion.substringBefore('.').toIntOrNull() != catalog.storageVersion) {
            error("storage_version_mismatch", "storageVersion", "Persistence version must match the semantic major version.")
        }
        if (catalog.manifest.version != catalog.catalogVersion) {
            error("version_mismatch", "manifest.version", "Manifest and catalog versions must match.")
        }
        catalog.manifest.previousVersion?.let { previousVersion ->
            if (!semanticVersion.matches(previousVersion) || previousVersion == catalog.catalogVersion) {
                error("invalid_previous_version", "manifest.previousVersion", "Previous version must be a different semantic version.")
            }
        }
        if (catalog.manifest.seedId.isBlank() || !stableId.matches(catalog.manifest.seedId)) {
            error("invalid_id", "manifest.seedId", "Seed ID must be stable and machine-readable.")
        }
        if (catalog.manifest.sourceAsset.isBlank()) {
            error("missing_source", "manifest.sourceAsset", "The bundled source asset must be declared.")
        }
        if (catalog.manifest.migrationNotes.isEmpty() || catalog.manifest.migrationNotes.any(String::isBlank)) {
            error("missing_migration_notes", "manifest.migrationNotes", "Migration behavior must be explicit.")
        }
        if (catalog.manifest.artRequirements.isEmpty() || catalog.manifest.testRequirements.isEmpty()) {
            error("incomplete_manifest", "manifest", "Art and test requirements must be declared.")
        }

        if (copy.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            error("unsupported_copy_schema", "copy.schemaVersion", "Copy schema must match the catalog schema.")
        }
        if (copy.catalogVersion != catalog.catalogVersion) {
            error("copy_version_mismatch", "copy.catalogVersion", "Copy and catalog versions must match.")
        }
        if (copy.locale != catalog.manifest.defaultLocale) {
            error("default_locale_mismatch", "copy.locale", "Loaded copy must be the manifest default locale.")
        }
        if (catalog.manifest.localizationAssets.isEmpty()) {
            error("missing_localization_asset", "manifest.localizationAssets", "At least one copy asset is required.")
        }
        if (copy.strings.any { (key, value) -> key.isBlank() || value.isBlank() }) {
            error("blank_localization", "copy.strings", "Localization keys and values must be non-blank.")
        }

        val allIds = buildList {
            addAll(catalog.rewards.map { it.id })
            addAll(catalog.presentations.map { it.id })
            addAll(catalog.progressionLevels.map { it.id })
            addAll(catalog.achievements.map { it.id })
            addAll(catalog.quests.map { it.id })
        }
        allIds.forEachIndexed { index, id ->
            if (!stableId.matches(id)) error("invalid_id", "ids[$index]", "Invalid stable ID: $id")
        }
        allIds.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach { id ->
            error("duplicate_id", "ids", "ID is reused: $id")
        }
        catalog.manifest.retiredIds.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach { id ->
            error("duplicate_retired_id", "manifest.retiredIds", "Retired ID is repeated: $id")
        }
        catalog.manifest.retiredIds.forEachIndexed { index, id ->
            if (!stableId.matches(id)) error("invalid_id", "manifest.retiredIds[$index]", "Invalid retired ID: $id")
            if (id in allIds) error("retired_id_reused", "manifest.retiredIds[$index]", "Retired IDs cannot be reused.")
        }

        val rarityIds = catalog.rarities.map { it.id }
        rarityIds.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach { rarity ->
            error("duplicate_rarity", "rarities", "Rarity is repeated: $rarity")
        }
        catalog.rarities.forEachIndexed { index, rarity ->
            if (rarity.distributionExpectation.isBlank() || rarity.treatmentRequirement.isBlank()) {
                error("incomplete_rarity", "rarities[$index]", "Rarity meaning and treatment must be explicit.")
            }
        }

        val rewardIds = catalog.rewards.map { it.id }.toSet()
        catalog.rewards.forEachIndexed { index, reward ->
            val path = "rewards[$index]"
            validateVersion(reward.version, "$path.version", ::error)
            if (reward.sourceRule.isBlank() || reward.economyNotes.isBlank()) {
                error("incomplete_reward", path, "Reward source and economy notes are required.")
            }
            if (reward.maxGrantsPerSource != 1) {
                error("unsafe_reward_grant", "$path.maxGrantsPerSource", "A source may grant this reward only once.")
            }
            when (reward.type) {
                RewardType.XP -> {
                    if ((reward.amount ?: 0) <= 0 || reward.entitlementId != null) {
                        error("invalid_xp_reward", path, "XP needs a positive amount and no entitlement ID.")
                    }
                }
                RewardType.PROFILE_MARKER -> {
                    if (reward.amount != null || reward.entitlementId.isNullOrBlank()) {
                        error("invalid_marker_reward", path, "Profile markers need an entitlement ID and no amount.")
                    }
                }
            }
        }

        val presentationIds = catalog.presentations.map { it.id }.toSet()
        catalog.presentations.forEachIndexed { index, presentation ->
            val path = "presentations[$index]"
            if (presentation.motionId.isBlank() || presentation.liveFields.isEmpty()) {
                error("incomplete_presentation", path, "Motion ID and live semantic fields are required.")
            }
            if (presentation.liveFields.any(String::isBlank) || presentation.liveFields.distinct().size != presentation.liveFields.size) {
                error("invalid_live_fields", "$path.liveFields", "Live fields must be unique and non-blank.")
            }
            if (
                presentation.artRequirement.isBlank() ||
                presentation.reducedMotionStrategy.isBlank() ||
                presentation.noMotionStrategy.isBlank() ||
                presentation.platformFallback.isBlank()
            ) {
                error("missing_presentation_fallback", path, "Art and accessible fallback contracts are required.")
            }
            if (presentation.expiresAfterSeconds <= 0) {
                error("invalid_presentation_expiry", "$path.expiresAfterSeconds", "Presentation receipts must expire.")
            }
        }

        var lastLevelThreshold = -1
        catalog.progressionLevels.sortedBy { it.order }.forEachIndexed { index, level ->
            val path = "progressionLevels[$index]"
            if (level.order != index + 1) error("invalid_level_order", "$path.order", "Level order must be contiguous.")
            if (level.threshold <= lastLevelThreshold) {
                error("invalid_level_threshold", "$path.threshold", "Level thresholds must strictly increase.")
            }
            lastLevelThreshold = level.threshold
            if (level.metric == LearningMetric.XP_EARNED_LEGACY) {
                error("xp_as_criterion", "$path.metric", "XP cannot define learning progression.")
            }
            validateReferences(level.rewardRefs, rewardIds, "$path.rewardRefs", "reward", ::error)
            if (level.presentationId !in presentationIds) {
                error("missing_presentation_ref", "$path.presentationId", "Unknown presentation: ${level.presentationId}")
            }
        }

        catalog.achievements.forEachIndexed { index, achievement ->
            val path = "achievements[$index]"
            validateVersion(achievement.version, "$path.version", ::error)
            validateLifecycle(achievement.lifecycle, path, ::error)
            validateCriteria(achievement.criterion, "$path.criterion", ::error)
            if (achievement.levels.isEmpty()) error("missing_levels", "$path.levels", "At least one level is required.")
            var previousTarget = 0
            achievement.levels.forEachIndexed { levelIndex, level ->
                val levelPath = "$path.levels[$levelIndex]"
                if (level.tier != levelIndex + 1 || level.visualLevel != level.tier) {
                    error("invalid_achievement_tier", levelPath, "Tier and visual level must be contiguous and equal.")
                }
                if (level.target <= previousTarget) {
                    error("invalid_achievement_threshold", "$levelPath.target", "Level targets must strictly increase.")
                }
                previousTarget = level.target
                if (level.rarity !in rarityIds) error("missing_rarity_ref", "$levelPath.rarity", "Unknown rarity.")
                validateReferences(level.rewardRefs, rewardIds, "$levelPath.rewardRefs", "reward", ::error)
            }
            achievement.criterion.clauses.firstOrNull()?.let { firstClause ->
                if (achievement.levels.firstOrNull()?.target != firstClause.target) {
                    error("criterion_level_mismatch", path, "The first level target must match the primary criterion target.")
                }
            }
            if (achievement.isSecret != (achievement.initialDisplayState == AchievementDisplayState.HIDDEN)) {
                error("invalid_secret_state", path, "Only secret achievements may start hidden, and secrets must start hidden.")
            }
            if (achievement.claimBehavior != ClaimBehavior.AUTOMATIC) {
                error("unsafe_achievement_claim", "$path.claimBehavior", "Achievements are granted automatically and idempotently.")
            }
            if (achievement.presentationId !in presentationIds) {
                error("missing_presentation_ref", "$path.presentationId", "Unknown presentation: ${achievement.presentationId}")
            }
        }

        catalog.quests.forEachIndexed { index, quest ->
            val path = "quests[$index]"
            validateVersion(quest.version, "$path.version", ::error)
            validateLifecycle(quest.lifecycle, path, ::error)
            validateCriteria(quest.criterion, "$path.criterion", ::error)
            validateQuestExpiry(quest, path, ::error)
            validateReplacement(quest.replacement, "$path.replacement", ::error)
            if (quest.eligibility.clauses.isEmpty()) {
                error("missing_eligibility", "$path.eligibility", "Quest assignment must be explicitly eligible.")
            }
            quest.eligibility.clauses.forEachIndexed { clauseIndex, clause ->
                if (clause.value < 0) error("invalid_eligibility", "$path.eligibility.clauses[$clauseIndex]", "Eligibility values cannot be negative.")
            }
            validateReferences(quest.rewardRefs, rewardIds, "$path.rewardRefs", "reward", ::error)
            if (quest.rewardRefs.isEmpty()) error("missing_reward", "$path.rewardRefs", "Quests need a transparent reward.")
            if (quest.claimBehavior != ClaimBehavior.MANUAL_ONCE) {
                error("invalid_quest_claim", "$path.claimBehavior", "Quest rewards use an idempotent one-time claim.")
            }
            if (quest.presentationId !in presentationIds) {
                error("missing_presentation_ref", "$path.presentationId", "Unknown presentation: ${quest.presentationId}")
            }
        }

        val requiredCopyKeys = buildSet {
            addAll(catalog.rarities.map { it.accessibilityLabelKey })
            addAll(catalog.rewards.map { it.explanationKey })
            catalog.progressionLevels.forEach { add(it.titleKey); add(it.descriptionKey) }
            catalog.achievements.forEach { achievement ->
                add(achievement.titleKey)
                add(achievement.descriptionKey)
                addAll(achievement.levels.map { it.progressCopyKey })
            }
            catalog.quests.forEach { quest -> add(quest.titleKey); add(quest.reasonKey) }
        }
        requiredCopyKeys.filterNot(copy.strings::containsKey).forEach { key ->
            error("missing_localization", "copy.strings", "Missing required key: $key")
        }
        // Android's ICU regex parser treats an unescaped closing brace as a
        // malformed quantifier. Escape both braces so the validation behaves
        // identically on device and on the JVM test runner.
        val placeholderPattern = Regex("\\{([A-Za-z][A-Za-z0-9_]*)\\}")
        val dynamicProgressKeys = catalog.achievements.flatMap { achievement ->
            achievement.levels.filter { it.target > 1 }.map { it.progressCopyKey }
        }.toSet()
        dynamicProgressKeys.forEach { key ->
            val placeholders = placeholderPattern.findAll(copy.strings[key].orEmpty())
                .map { it.groupValues[1] }
                .toSet()
            if (placeholders != setOf("current", "target")) {
                error("invalid_progress_placeholders", "copy.strings.$key", "Progress copy must preserve {current} and {target}.")
            }
        }
        val manifestKeys = catalog.manifest.localizationKeys.toSet()
        requiredCopyKeys.filterNot(manifestKeys::contains).forEach { key ->
            error("missing_manifest_key", "manifest.localizationKeys", "Manifest omits required key: $key")
        }
        manifestKeys.filterNot(copy.strings::containsKey).forEach { key ->
            error("missing_localization", "copy.strings", "Manifest key has no default copy: $key")
        }
        copy.strings.keys.filterNot(manifestKeys::contains).forEach { key ->
            error("unmanifested_localization", "copy.strings", "Default copy key is absent from the manifest: $key")
        }
        if (catalog.manifest.localizationKeys.size != manifestKeys.size) {
            error("duplicate_localization_key", "manifest.localizationKeys", "Localization keys must be unique.")
        }

        return CatalogValidationReport(errors = errors)
    }

    private fun validateVersion(
        version: String,
        path: String,
        error: (String, String, String) -> Unit
    ) {
        if (!semanticVersion.matches(version)) error("invalid_version", path, "Definition version must be semantic versioning.")
    }

    private fun validateLifecycle(
        lifecycle: CatalogLifecycle,
        parentPath: String,
        error: (String, String, String) -> Unit
    ) {
        validateVersion(lifecycle.introducedVersion, "$parentPath.lifecycle.introducedVersion", error)
        when (lifecycle.state) {
            CatalogLifecycleState.ACTIVE -> if (lifecycle.retiredVersion != null || lifecycle.replacementId != null) {
                error("invalid_active_lifecycle", "$parentPath.lifecycle", "Active definitions cannot declare retirement fields.")
            }
            CatalogLifecycleState.RETIRED -> if (lifecycle.retiredVersion == null) {
                error("invalid_retired_lifecycle", "$parentPath.lifecycle", "Retired definitions need a retired version.")
            } else {
                validateVersion(lifecycle.retiredVersion, "$parentPath.lifecycle.retiredVersion", error)
                lifecycle.replacementId?.let { replacementId ->
                    if (!stableId.matches(replacementId)) {
                        error("invalid_replacement_id", "$parentPath.lifecycle.replacementId", "Replacement ID must be stable.")
                    }
                }
            }
        }
    }

    private fun validateCriteria(
        criteria: CatalogCriteria,
        path: String,
        error: (String, String, String) -> Unit
    ) {
        if (criteria.clauses.isEmpty()) error("missing_criteria", "$path.clauses", "At least one learning criterion is required.")
        criteria.clauses.forEachIndexed { index, clause ->
            val clausePath = "$path.clauses[$index]"
            if (clause.metric == LearningMetric.XP_EARNED_LEGACY) {
                error("xp_as_criterion", "$clausePath.metric", "XP is feedback, not evidence of learning.")
            }
            if (clause.target <= 0) error("invalid_target", "$clausePath.target", "Criteria targets must be positive.")
            if (clause.minimumDelayHours < 0) error("invalid_delay", "$clausePath.minimumDelayHours", "Delay cannot be negative.")
            if (!clause.requiresVerifiedOutcome) {
                error("unverified_outcome", "$clausePath.requiresVerifiedOutcome", "Catalog progress requires a verified domain outcome.")
            }
            when (clause.aggregation) {
                CriterionAggregation.BOOLEAN -> {
                    if (clause.target != 1 || clause.distinctBy != DistinctDimension.NONE) {
                        error("invalid_boolean_criterion", clausePath, "Boolean criteria use target 1 and no distinct dimension.")
                    }
                }
                CriterionAggregation.COUNT_DISTINCT -> if (clause.distinctBy == DistinctDimension.NONE) {
                    error("missing_distinct_dimension", "$clausePath.distinctBy", "Distinct counts need an identity dimension.")
                }
                CriterionAggregation.COUNT -> if (clause.target > 1 && clause.distinctBy == DistinctDimension.NONE) {
                    error("farmable_criterion", clausePath, "Repeated criteria need a distinct dimension or a boolean outcome.")
                }
            }
            clause.filters.forEachIndexed { filterIndex, filter ->
                if (filter.field.isBlank() || filter.value.isBlank()) {
                    error("invalid_filter", "$clausePath.filters[$filterIndex]", "Filter fields and values must be non-blank.")
                }
            }
        }
    }

    private fun validateQuestExpiry(
        quest: QuestDefinition,
        path: String,
        error: (String, String, String) -> Unit
    ) {
        val expectedBoundary = when (quest.cadence) {
            QuestCadence.DAILY -> ExpiryBoundary.LOCAL_DAY_END
            QuestCadence.WEEKLY -> ExpiryBoundary.LOCAL_WEEK_END
            QuestCadence.MONTHLY -> ExpiryBoundary.LOCAL_MONTH_END
            QuestCadence.WEEKEND -> ExpiryBoundary.WEEKEND_END
            QuestCadence.COMEBACK -> ExpiryBoundary.ASSIGNED_WINDOW_END
        }
        if (quest.expiry.boundary != expectedBoundary) {
            error("cadence_expiry_mismatch", "$path.expiry.boundary", "${quest.cadence} quests must use $expectedBoundary.")
        }
        if (quest.expiry.graceHours !in 0..24) {
            error("invalid_expiry_grace", "$path.expiry.graceHours", "Grace must be between zero and 24 hours.")
        }
    }

    private fun validateReplacement(
        replacement: QuestReplacementPolicy,
        path: String,
        error: (String, String, String) -> Unit
    ) {
        when (replacement.mode) {
            ReplacementMode.NONE -> if (replacement.group != null || replacement.maxReplacements != 0) {
                error("invalid_replacement", path, "No-replacement policy cannot declare a group or count.")
            }
            ReplacementMode.REPLACE_IF_INELIGIBLE -> if (replacement.group.isNullOrBlank() || replacement.maxReplacements !in 1..3) {
                error("invalid_replacement", path, "Replacement needs a group and a bounded count from one to three.")
            }
        }
    }

    private fun validateReferences(
        references: List<String>,
        known: Set<String>,
        path: String,
        kind: String,
        error: (String, String, String) -> Unit
    ) {
        if (references.distinct().size != references.size) error("duplicate_reference", path, "References must be unique.")
        references.filterNot(known::contains).forEach { ref ->
            error("missing_${kind}_ref", path, "Unknown $kind reference: $ref")
        }
    }
}
