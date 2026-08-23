package com.cinnamon.app.data.gamification

import com.cinnamon.app.data.local.GamificationEventEntity
import com.cinnamon.app.domain.gamification.GamificationCatalogBundle
import com.cinnamon.app.domain.gamification.GamificationCatalogLoader
import java.io.File
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuestboardProjectorTest {

    private lateinit var bundle: GamificationCatalogBundle

    @Before
    fun loadCatalog() {
        bundle = GamificationCatalogLoader().loadValidated(
            catalogJson = assetText("gamification/catalog-v1.json"),
            copyJson = assetText("gamification/copy-en-v1.json")
        )
    }

    @Test
    fun `weekly mastery uses persisted progress and immutable reward value`() {
        val candidate = weeklyCandidate()
        val instance = candidate.instance.copy(
            state = "in_progress",
            progress = 5L
        )

        val projected = QuestboardProjector.projectAssignedWeeklyQuests(
            bundle = bundle,
            instances = listOf(instance),
            nowEpochMillis = eventTime
        ).single()

        assertEquals(instance.questInstanceId, projected.instanceId)
        assertEquals("weekly", projected.cadence)
        assertEquals("Make eight memories stick", projected.title)
        assertEquals(5, projected.progress)
        assertEquals(8, projected.target)
        assertEquals(30, projected.rewardXp)
        assertTrue(!projected.claimable)
    }

    @Test
    fun `completed reward stays visible after expiry but an old claimed row leaves the board`() {
        val instance = weeklyCandidate().instance
        val afterExpiry = instance.endsAtEpochMillis + 1L
        val completed = instance.copy(
            state = "completed",
            progress = instance.target,
            completionEventId = "event-complete",
            completedAtEpochMillis = instance.endsAtEpochMillis - 1L
        )
        val claimed = completed.copy(
            state = "claimed",
            claimedAtEpochMillis = instance.endsAtEpochMillis - 1L
        )

        val completedProjection = QuestboardProjector.projectAssignedWeeklyQuests(
            bundle = bundle,
            instances = listOf(completed),
            nowEpochMillis = afterExpiry
        )
        val claimedProjection = QuestboardProjector.projectAssignedWeeklyQuests(
            bundle = bundle,
            instances = listOf(claimed),
            nowEpochMillis = afterExpiry
        )

        assertTrue(completedProjection.single().claimable)
        assertTrue(claimedProjection.isEmpty())
    }

    @Test
    fun `completed daily assignment remains visible and claimable after its window`() {
        val instance = plannedQuests().single { candidate ->
            candidate.instance.definitionId == "quest.daily.due_review"
        }.instance.copy(
            state = "completed",
            progress = 3L,
            completionEventId = "event-daily-complete",
            completedAtEpochMillis = eventTime + 1L
        )

        val projected = QuestboardProjector.projectAssignedDailyQuests(
            bundle = bundle,
            instances = listOf(instance),
            nowEpochMillis = instance.endsAtEpochMillis + 1L
        ).single()

        assertEquals("daily", projected.cadence)
        assertEquals(10, projected.rewardXp)
        assertTrue(projected.claimable)
    }

    private fun weeklyCandidate() = plannedQuests().single { candidate ->
        candidate.instance.definitionId == "quest.weekly.durable_mastery"
    }

    private fun plannedQuests() = CatalogSettlementPlanner.plan(
        bundle = bundle,
        event = GamificationEventEntity(
            eventId = "event-weekly-projection",
            actorId = "learner",
            eventType = "catalog_reconciled",
            subjectType = "gamification_catalog",
            subjectId = "1.0.0",
            occurredAtEpochMillis = eventTime,
            recordedAtEpochMillis = eventTime,
            studyDay = 19_675L,
            idempotencyKey = "event-weekly-projection",
            source = "test",
            ruleVersion = 1,
            metadataJson = "{}",
            replayOfEventId = null
        ),
        dueItemCountAtAssignment = 3,
        assignmentTimeZone = TimeZone.getTimeZone("UTC")
    ).quests

    private fun assetText(path: String): String {
        val candidates = listOf(
            File("src/main/assets/$path"),
            File("app/src/main/assets/$path")
        )
        val file = candidates.firstOrNull(File::isFile)
            ?: error("Unable to locate test asset $path from ${File(".").absolutePath}")
        return file.readText(Charsets.UTF_8)
    }

    private companion object {
        const val eventTime = 1_700_000_000_000L
    }
}
