package com.cinnamon.app.data.gamification

import com.cinnamon.app.data.local.QuestInstanceEntity
import com.cinnamon.app.domain.gamification.GamificationCatalogBundle
import com.cinnamon.app.domain.gamification.GamificationCatalogLoader
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class QuestAssignmentContractsTest {

    private lateinit var bundle: GamificationCatalogBundle

    @Before
    fun loadCatalog() {
        bundle = GamificationCatalogLoader().loadValidated(
            catalogJson = assetText("gamification/catalog-v1.json"),
            copyJson = assetText("gamification/copy-en-v1.json")
        )
    }

    @Test
    fun `assignment snapshot preserves reward value without a current catalog lookup`() {
        val definition = bundle.catalog.quests.single {
            it.id == "quest.weekly.durable_mastery"
        }
        val encoded = QuestAssignmentContracts.encodeReward(
            QuestAssignmentContracts.createRewardSnapshot(bundle, definition)
        )

        val resolved = QuestAssignmentContracts.resolveClaim(instance(rewardJson = encoded))

        assertNotNull(resolved)
        assertEquals(30L, resolved?.xpAmount)
        assertEquals("presentation.quest.complete", resolved?.presentationId)
        assertEquals("standard", resolved?.presentationTier)
        assertEquals(20, resolved?.presentationPriority)
        assertEquals(604_800, resolved?.presentationExpiresAfterSeconds)
    }

    @Test
    fun `storage version one array remains claimable at its pinned historical value`() {
        val resolved = QuestAssignmentContracts.resolveClaim(
            instance(rewardJson = "[\"reward.xp.30\"]")
        )

        assertEquals(30L, resolved?.xpAmount)
    }

    @Test
    fun `unknown legacy reward and mismatched snapshot fail closed`() {
        val definition = bundle.catalog.quests.single {
            it.id == "quest.weekly.durable_mastery"
        }
        val encoded = QuestAssignmentContracts.encodeReward(
            QuestAssignmentContracts.createRewardSnapshot(bundle, definition)
        )

        assertNull(
            QuestAssignmentContracts.resolveClaim(
                instance(rewardJson = "[\"reward.xp.999\"]")
            )
        )
        assertNull(
            QuestAssignmentContracts.resolveClaim(
                instance(rewardJson = encoded).copy(definitionId = "quest.daily.due_review")
            )
        )
    }

    @Test
    fun `legacy criteria json gains schema default and retains frozen timezone`() {
        val decoded = QuestAssignmentContracts.decodeCriteria(
            """{"metric":"unique_item_mastered_after_delay","aggregation":"count_distinct","distinctBy":"subject_id","timezone":"Asia/Tehran","evidenceStartsAtEpochMillis":1000,"evidenceEndsAtEpochMillis":2000}"""
        )

        assertEquals("Asia/Tehran", decoded?.assignmentTimeZoneId)
        assertEquals(1_000L, decoded?.evidenceStartsAtEpochMillis)
        assertEquals(2_000L, decoded?.evidenceEndsAtEpochMillis)
    }

    private fun instance(rewardJson: String) = QuestInstanceEntity(
        questInstanceId = "weekly-durable-instance",
        actorId = "learner",
        definitionId = "quest.weekly.durable_mastery",
        catalogVersion = 1,
        cadence = "weekly",
        startsAtEpochMillis = 1_000L,
        endsAtEpochMillis = 3_000L,
        state = "completed",
        progress = 8L,
        target = 8L,
        criteriaJson = "{}",
        rewardJson = rewardJson,
        createdAtEpochMillis = 1_000L,
        updatedAtEpochMillis = 2_000L,
        completionEventId = "event-complete",
        completedAtEpochMillis = 2_000L,
        claimedAtEpochMillis = null
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
