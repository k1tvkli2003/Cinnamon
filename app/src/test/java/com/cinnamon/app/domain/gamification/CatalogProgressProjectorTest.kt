package com.cinnamon.app.domain.gamification

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CatalogProgressProjectorTest {
    private lateinit var bundle: GamificationCatalogBundle

    @Before
    fun loadCatalog() {
        bundle = GamificationCatalogLoader().loadValidated(
            catalogJson = assetText("gamification/catalog-v1.json"),
            copyJson = assetText("gamification/copy-en-v1.json")
        )
    }

    @Test
    fun `progression threshold and title come from catalog`() {
        val foundation = bundle.catalog.progressionLevels.single { it.order == 2 }
        val changedCatalog = bundle.catalog.copy(
            progressionLevels = bundle.catalog.progressionLevels.map { level ->
                if (level.id == foundation.id) level.copy(threshold = 7) else level
            }
        )
        val changedCopy = bundle.copy.copy(
            strings = bundle.copy.strings + (foundation.titleKey to "Catalog Foundation")
        )
        val changedBundle = bundle.copy(catalog = changedCatalog, copy = changedCopy)

        val below = CatalogProgressProjector.currentProgressionLevel(
            changedBundle,
            CatalogEvidenceSnapshot(masteredItemsAfterDelay = 6)
        )
        val reached = CatalogProgressProjector.currentProgressionLevel(
            changedBundle,
            CatalogEvidenceSnapshot(masteredItemsAfterDelay = 7)
        )

        assertEquals(1, below?.order)
        assertEquals(2, reached?.order)
        assertEquals("Catalog Foundation", reached?.title)
    }

    @Test
    fun `achievement projection exposes only exact evidence contracts`() {
        val projections = CatalogProgressProjector.projectAchievements(
            bundle,
            CatalogEvidenceSnapshot(
                masteredItemsAfterDelay = 10,
                rhythmWeeks = 2,
                distinctPracticeContentKinds = 3
            )
        )

        assertEquals(
            setOf(
                "achievement.mastery.durable_memory",
                "achievement.consistency.weekly_rhythm",
                "achievement.exploration.content_breadth"
            ),
            projections.map { it.id }.toSet()
        )
        assertTrue(projections.all { it.thresholdReached })
        assertFalse(projections.any { projection ->
            projection.id == "achievement.correction.repair_loop" ||
                projection.id == "achievement.exploration.etymology_trail"
        })
    }

    @Test
    fun `daily checkpoint uses catalog definition and distinct review evidence`() {
        val eligible = CatalogProgressProjector.projectEligibleDailyQuests(
            bundle,
            CatalogEvidenceSnapshot(distinctDueReviewsToday = 2, dueItemCountNow = 3)
        )

        assertEquals(1, eligible.size)
        assertEquals("quest.daily.due_review", eligible.single().id)
        assertEquals("Review three that are ready", eligible.single().title)
        assertEquals(2, eligible.single().progress)
        assertEquals(3, eligible.single().target)
        assertFalse(eligible.single().thresholdReached)
    }

    @Test
    fun `ineligible checkpoint is hidden but completed evidence remains visible`() {
        val unavailable = CatalogProgressProjector.projectEligibleDailyQuests(
            bundle,
            CatalogEvidenceSnapshot(distinctDueReviewsToday = 0, dueItemCountNow = 2)
        )
        val completed = CatalogProgressProjector.projectEligibleDailyQuests(
            bundle,
            CatalogEvidenceSnapshot(distinctDueReviewsToday = 3, dueItemCountNow = 0)
        )

        assertTrue(unavailable.isEmpty())
        assertEquals("quest.daily.due_review", completed.single().id)
        assertTrue(completed.single().thresholdReached)
    }

    @Test
    fun `projection carries no catalog reward or claim authority`() {
        val quest = CatalogProgressProjector.projectEligibleDailyQuests(
            bundle,
            CatalogEvidenceSnapshot(distinctDueReviewsToday = 3, dueItemCountNow = 3)
        ).single()
        val achievement = CatalogProgressProjector.projectAchievements(
            bundle,
            CatalogEvidenceSnapshot(masteredItemsAfterDelay = 10)
        ).single { it.id == "achievement.mastery.durable_memory" }

        assertEquals(
            setOf("id", "title", "reason", "progress", "target", "thresholdReached"),
            quest::class.java.declaredFields.map { it.name }.filterNot { it.startsWith("$") }.toSet()
        )
        assertEquals(
            setOf("id", "family", "title", "description", "progress", "target", "thresholdReached"),
            achievement::class.java.declaredFields.map { it.name }.filterNot { it.startsWith("$") }.toSet()
        )
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
