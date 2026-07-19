package com.cinnamon.app.data.gamification

import com.cinnamon.app.data.local.CatalogEvidenceMetric
import com.cinnamon.app.data.local.GamificationEventEntity
import com.cinnamon.app.domain.gamification.GamificationCatalogBundle
import com.cinnamon.app.domain.gamification.GamificationCatalogLoader
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CatalogSettlementPlannerTest {

    private lateinit var bundle: GamificationCatalogBundle

    @Before
    fun loadCatalog() {
        bundle = GamificationCatalogLoader().loadValidated(
            catalogJson = assetText("gamification/catalog-v1.json"),
            copyJson = assetText("gamification/copy-en-v1.json")
        )
    }

    @Test
    fun `planner emits only catalog criteria backed by exact ledger evidence`() {
        val request = CatalogSettlementPlanner.plan(
            bundle = bundle,
            event = event(),
            dueItemCountAtAssignment = 3
        )

        val ids = request.unlocks.map { it.unlock.achievementId }.toSet()
        assertEquals(
            setOf(
                "achievement.mastery.durable_memory",
                "achievement.consistency.weekly_rhythm",
                "achievement.exploration.content_breadth",
                "level.learning.foundation",
                "level.learning.application",
                "level.learning.integration",
                "level.learning.durable",
                "level.learning.breadth"
            ),
            ids
        )
        assertFalse(ids.contains("achievement.correction.repair_loop"))
        assertFalse(ids.contains("achievement.review.queue_resolved"))
        assertFalse(ids.contains("achievement.exploration.etymology_trail"))
        assertEquals(14, request.unlocks.size)
        assertEquals(
            setOf(
                CatalogEvidenceMetric.MASTERED_ITEMS_AFTER_DELAY,
                CatalogEvidenceMetric.RHYTHM_WEEKS,
                CatalogEvidenceMetric.DISTINCT_PRACTICE_CONTENT_KINDS
            ),
            request.unlocks.map { it.metric }.toSet()
        )
    }

    @Test
    fun `planner derives immutable unlock value and presentation from catalog`() {
        val request = CatalogSettlementPlanner.plan(bundle, event(), dueItemCountAtAssignment = 3)

        val firstMasteryTier = request.unlocks.single {
            it.unlock.achievementId == "achievement.mastery.durable_memory" &&
                it.unlock.level == 1
        }
        assertEquals(10L, firstMasteryTier.target)
        assertEquals(20L, firstMasteryTier.xpTransaction?.amount)
        assertTrue(firstMasteryTier.xpTransaction?.ruleId?.startsWith("catalog.") == true)
        assertEquals(
            "presentation.achievement.level_up",
            firstMasteryTier.presentationReceipt?.presentationFamily
        )
        assertEquals("event-settlement", firstMasteryTier.unlock.sourceEventId)

        val durableProgression = request.unlocks.single {
            it.unlock.achievementId == "level.learning.durable"
        }
        assertEquals(75L, durableProgression.target)
        assertEquals(50L, durableProgression.xpTransaction?.amount)
        assertEquals(listOf("marker.durable_reviewer"), durableProgression.unlockedContentIds)

        val contentBreadthTierOne = request.unlocks.single {
            it.unlock.achievementId == "achievement.exploration.content_breadth" &&
                it.unlock.level == 1
        }
        assertEquals(CatalogEvidenceMetric.DISTINCT_PRACTICE_CONTENT_KINDS, contentBreadthTierOne.metric)
        assertNotNull(contentBreadthTierOne.presentationReceipt)
    }

    @Test
    fun `daily due review quest is deterministic and assignment eligibility is explicit`() {
        val eligible = CatalogSettlementPlanner.plan(bundle, event(), dueItemCountAtAssignment = 3)
            .quests.single()
        val ineligible = CatalogSettlementPlanner.plan(bundle, event(), dueItemCountAtAssignment = 2)
            .quests.single()

        assertEquals("quest.daily.due_review", eligible.instance.definitionId)
        assertEquals(CatalogEvidenceMetric.DISTINCT_DUE_REVIEWS_TODAY, eligible.metric)
        assertEquals(3L, eligible.instance.target)
        assertTrue(eligible.eligibleForAssignment)
        assertFalse(ineligible.eligibleForAssignment)
        assertEquals(eligible.instance.questInstanceId, ineligible.instance.questInstanceId)
        assertEquals(eligible.instance.startsAtEpochMillis, ineligible.instance.startsAtEpochMillis)
        assertEquals(eligible.instance.endsAtEpochMillis, ineligible.instance.endsAtEpochMillis)
        assertTrue(eligible.instance.endsAtEpochMillis > eligible.instance.startsAtEpochMillis)
        assertNull(eligible.instance.completionEventId)
        assertNull(eligible.instance.claimedAtEpochMillis)
    }

    private fun event() = GamificationEventEntity(
        eventId = "event-settlement",
        actorId = "learner",
        eventType = "review_completed",
        subjectType = "lexicon_entry",
        subjectId = "term-a",
        occurredAtEpochMillis = 1_700_000_000_000L,
        recordedAtEpochMillis = 1_700_000_000_000L,
        studyDay = 19_675L,
        idempotencyKey = "idempotency-settlement",
        source = "test",
        ruleVersion = 1,
        metadataJson = "{}",
        replayOfEventId = null
    )

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
