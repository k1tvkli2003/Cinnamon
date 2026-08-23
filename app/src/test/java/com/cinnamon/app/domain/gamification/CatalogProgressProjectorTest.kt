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
                distinctPracticeContentKinds = 3,
                repairedItems = 5,
                resolvedConfusablePairs = 5,
                verifiedContextApplications = 5,
                verifiedDelayedRecalls = 3,
                verifiedComebackSessions = 1,
                verifiedSavedItems = 10,
                verifiedReviewQueueClearDays = 1
            )
        )

        assertEquals(
            setOf(
                "achievement.mastery.durable_memory",
                "achievement.correction.repair_loop",
                "achievement.consistency.weekly_rhythm",
                "achievement.exploration.content_breadth",
                "achievement.mastery.confusable_precision",
                "achievement.application.context_builder",
                "achievement.challenge.delayed_recall",
                "achievement.comeback.gentle_return",
                "achievement.collection.learned_not_saved",
                "achievement.review.queue_resolved"
            ),
            projections.map { it.id }.toSet()
        )
        assertTrue(projections.all { it.thresholdReached })
        assertFalse(projections.any { projection ->
            projection.id == "achievement.exploration.etymology_trail"
        })
    }

    @Test
    fun `repair projection requires the exact two-link filter contract`() {
        val repair = bundle.catalog.achievements.single {
            it.id == "achievement.correction.repair_loop"
        }
        val changed = bundle.copy(
            catalog = bundle.catalog.copy(
                achievements = bundle.catalog.achievements.map { definition ->
                    if (definition.id == repair.id) {
                        definition.copy(
                            criterion = definition.criterion.copy(
                                clauses = listOf(
                                    definition.criterion.clauses.single().copy(
                                        filters = definition.criterion.clauses.single().filters.take(1)
                                    )
                                )
                            )
                        )
                    } else {
                        definition
                    }
                }
            )
        )

        val ids = CatalogProgressProjector.projectAchievements(
            changed,
            CatalogEvidenceSnapshot(repairedItems = 5)
        ).map { projection -> projection.id }

        assertFalse(ids.contains(repair.id))
    }

    @Test
    fun `queue projection requires the exact verified starting size contract`() {
        val queueClear = bundle.catalog.achievements.single {
            it.id == "achievement.review.queue_resolved"
        }
        val changed = bundle.copy(
            catalog = bundle.catalog.copy(
                achievements = bundle.catalog.achievements.map { definition ->
                    if (definition.id == queueClear.id) {
                        definition.copy(
                            criterion = definition.criterion.copy(
                                clauses = listOf(
                                    definition.criterion.clauses.single().copy(filters = emptyList())
                                )
                            )
                        )
                    } else {
                        definition
                    }
                }
            )
        )

        val ids = CatalogProgressProjector.projectAchievements(
            changed,
            CatalogEvidenceSnapshot(verifiedReviewQueueClearDays = 1)
        ).map { projection -> projection.id }

        assertFalse(ids.contains(queueClear.id))
    }

    @Test
    fun `daily checkpoint uses catalog definition and distinct review evidence`() {
        val eligible = CatalogProgressProjector.projectEligibleDailyQuests(
            bundle,
            CatalogEvidenceSnapshot(distinctDueReviewsToday = 2, dueItemCountNow = 3)
        )

        val dueReview = eligible.single { it.id == "quest.daily.due_review" }
        assertEquals("Review three that are ready", dueReview.title)
        assertEquals(2, dueReview.progress)
        assertEquals(3, dueReview.target)
        assertFalse(dueReview.thresholdReached)
        assertEquals(0, eligible.single { it.id == "quest.daily.confusable_pair" }.progress)
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

        assertEquals(
            setOf("quest.daily.confusable_pair", "quest.daily.context_use"),
            unavailable.map { it.id }.toSet()
        )
        val dueReview = completed.single { it.id == "quest.daily.due_review" }
        assertTrue(dueReview.thresholdReached)
    }

    @Test
    fun `projection carries no catalog reward or claim authority`() {
        val quest = CatalogProgressProjector.projectEligibleDailyQuests(
            bundle,
            CatalogEvidenceSnapshot(distinctDueReviewsToday = 3, dueItemCountNow = 3)
        ).single { it.id == "quest.daily.due_review" }
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
