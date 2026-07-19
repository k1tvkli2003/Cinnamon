package com.cinnamon.app.domain.gamification

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GamificationCatalogTest {
    private lateinit var bundle: GamificationCatalogBundle
    private val loader = GamificationCatalogLoader()

    @Before
    fun loadStarterCatalog() {
        bundle = loader.loadValidated(
            catalogJson = assetText("gamification/catalog-v1.json"),
            copyJson = assetText("gamification/copy-en-v1.json")
        )
    }

    @Test
    fun starterCatalog_isCompleteAndValid() {
        val report = GamificationCatalogValidator.validate(bundle)

        assertTrue(report.errors.joinToString { "${it.code}:${it.path}" }, report.isValid)
        assertEquals(12, bundle.catalog.achievements.size)
        assertEquals(1, bundle.catalog.storageVersion)
        assertEquals(12, bundle.catalog.quests.size)
        assertEquals(6, bundle.catalog.progressionLevels.size)
        assertEquals(15, bundle.catalog.rewards.size)
        assertEquals(5, bundle.catalog.presentations.size)
        assertEquals(4, bundle.catalog.rarities.size)
        assertEquals(
            setOf(
                QuestCadence.DAILY,
                QuestCadence.WEEKLY,
                QuestCadence.MONTHLY,
                QuestCadence.WEEKEND,
                QuestCadence.COMEBACK
            ),
            bundle.catalog.quests.map { it.cadence }.toSet()
        )
    }

    @Test
    fun starterCatalog_hasNoXpCriterionOrUnverifiedProgress() {
        val achievementClauses = bundle.catalog.achievements.flatMap { it.criterion.clauses }
        val questClauses = bundle.catalog.quests.flatMap { it.criterion.clauses }
        val allClauses = achievementClauses + questClauses

        assertTrue(allClauses.none { it.metric == LearningMetric.XP_EARNED_LEGACY })
        assertTrue(bundle.catalog.progressionLevels.none { it.metric == LearningMetric.XP_EARNED_LEGACY })
        assertTrue(allClauses.all { it.requiresVerifiedOutcome })
    }

    @Test
    fun starterCatalog_secretIsOptionalAndHidden() {
        val secrets = bundle.catalog.achievements.filter { it.isSecret }

        assertEquals(1, secrets.size)
        assertEquals(AchievementDisplayState.HIDDEN, secrets.single().initialDisplayState)
        assertEquals(AchievementFamily.EXPLORATION, secrets.single().family)
        assertFalse(secrets.single().levels.single().rewardRefs.isEmpty())
    }

    @Test
    fun validator_rejectsDuplicateStableId() {
        val duplicate = bundle.catalog.rewards.first().copy(id = bundle.catalog.achievements.first().id)
        val invalid = bundle.copy(catalog = bundle.catalog.copy(rewards = bundle.catalog.rewards + duplicate))

        assertHasError(invalid, "duplicate_id")
    }

    @Test
    fun validator_rejectsXpAsAnyProgressCriterion() {
        val first = bundle.catalog.achievements.first()
        val invalidClause = first.criterion.clauses.first().copy(metric = LearningMetric.XP_EARNED_LEGACY)
        val invalidAchievement = first.copy(
            criterion = first.criterion.copy(clauses = listOf(invalidClause))
        )
        val invalid = bundle.copy(
            catalog = bundle.catalog.copy(
                achievements = listOf(invalidAchievement) + bundle.catalog.achievements.drop(1)
            )
        )

        assertHasError(invalid, "xp_as_criterion")
    }

    @Test
    fun validator_rejectsStorageVersionThatCannotMapToTheLedger() {
        val invalid = bundle.copy(catalog = bundle.catalog.copy(storageVersion = 2))

        assertHasError(invalid, "storage_version_mismatch")
    }

    @Test
    fun validator_rejectsMissingLocalizationAndRewardReference() {
        val first = bundle.catalog.achievements.first()
        val invalidLevel = first.levels.first().copy(rewardRefs = listOf("reward.missing"))
        val invalidAchievement = first.copy(
            titleKey = "achievement.missing.title",
            levels = listOf(invalidLevel) + first.levels.drop(1)
        )
        val invalid = bundle.copy(
            catalog = bundle.catalog.copy(
                achievements = listOf(invalidAchievement) + bundle.catalog.achievements.drop(1)
            )
        )

        val report = GamificationCatalogValidator.validate(invalid)
        assertTrue(report.errors.any { it.code == "missing_reward_ref" })
        assertTrue(report.errors.any { it.code == "missing_localization" })
        assertTrue(report.errors.any { it.code == "missing_manifest_key" })
    }

    @Test
    fun validator_rejectsBrokenProgressPlaceholders() {
        val key = "achievement.progress.unique_items"
        val invalid = bundle.copy(
            copy = bundle.copy.copy(
                strings = bundle.copy.strings + (key to "Progress is {current}")
            )
        )

        assertHasError(invalid, "invalid_progress_placeholders")
    }

    @Test
    fun validator_rejectsBrokenThresholdSecretAndExpiryContracts() {
        val firstAchievement = bundle.catalog.achievements.first()
        val descendingLevels = firstAchievement.levels.mapIndexed { index, level ->
            if (index == 1) level.copy(target = firstAchievement.levels.first().target) else level
        }
        val invalidAchievement = firstAchievement.copy(
            levels = descendingLevels,
            isSecret = true,
            initialDisplayState = AchievementDisplayState.LOCKED
        )
        val firstQuest = bundle.catalog.quests.first()
        val invalidQuest = firstQuest.copy(
            expiry = firstQuest.expiry.copy(boundary = ExpiryBoundary.LOCAL_MONTH_END),
            replacement = QuestReplacementPolicy(
                mode = ReplacementMode.REPLACE_IF_INELIGIBLE,
                group = null,
                maxReplacements = 99
            )
        )
        val invalid = bundle.copy(
            catalog = bundle.catalog.copy(
                achievements = listOf(invalidAchievement) + bundle.catalog.achievements.drop(1),
                quests = listOf(invalidQuest) + bundle.catalog.quests.drop(1)
            )
        )

        val report = GamificationCatalogValidator.validate(invalid)
        assertTrue(report.errors.any { it.code == "invalid_achievement_threshold" })
        assertTrue(report.errors.any { it.code == "invalid_secret_state" })
        assertTrue(report.errors.any { it.code == "cadence_expiry_mismatch" })
        assertTrue(report.errors.any { it.code == "invalid_replacement" })
    }

    @Test
    fun loader_rejectsUnknownFields() {
        val invalidJson = assetText("gamification/catalog-v1.json")
            .replaceFirst("\"schemaVersion\": 1,", "\"schemaVersion\": 1, \"unknownField\": true,")

        assertThrows(GamificationCatalogFormatException::class.java) {
            loader.loadValidated(invalidJson, assetText("gamification/copy-en-v1.json"))
        }
    }

    private fun assertHasError(invalid: GamificationCatalogBundle, code: String) {
        val report = GamificationCatalogValidator.validate(invalid)
        assertTrue(report.errors.joinToString { "${it.code}:${it.path}" }, report.errors.any { it.code == code })
    }

    private fun assetText(path: String): String {
        val candidates = listOf(
            File("src/main/assets/$path"),
            File("app/src/main/assets/$path")
        )
        val file = candidates.firstOrNull(File::isFile)
            ?: error("Unable to locate test asset $path from ${File(".").absolutePath}")
        return file.readText(Charsets.UTF_8)
    }
}
