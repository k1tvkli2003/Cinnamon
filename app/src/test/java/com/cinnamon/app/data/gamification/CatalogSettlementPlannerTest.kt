package com.cinnamon.app.data.gamification

import com.cinnamon.app.data.local.CatalogEvidenceMetric
import com.cinnamon.app.data.local.GamificationEventEntity
import com.cinnamon.app.domain.gamification.GamificationCatalogBundle
import com.cinnamon.app.domain.gamification.GamificationCatalogLoader
import java.io.File
import java.util.Calendar
import java.util.TimeZone
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
                "achievement.correction.repair_loop",
                "achievement.mastery.confusable_precision",
                "achievement.application.context_builder",
                "achievement.challenge.delayed_recall",
                "achievement.comeback.gentle_return",
                "achievement.collection.learned_not_saved",
                "achievement.review.queue_resolved",
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
        assertFalse(ids.contains("achievement.exploration.etymology_trail"))
        assertEquals(33, request.unlocks.size)
        assertEquals(
            setOf(
                CatalogEvidenceMetric.MASTERED_ITEMS_AFTER_DELAY,
                CatalogEvidenceMetric.RHYTHM_WEEKS,
                CatalogEvidenceMetric.DISTINCT_PRACTICE_CONTENT_KINDS,
                CatalogEvidenceMetric.DISTINCT_REPAIRED_SUBJECTS,
                CatalogEvidenceMetric.DISTINCT_CONFUSABLE_PAIRS,
                CatalogEvidenceMetric.DISTINCT_CONTEXT_APPLICATIONS,
                CatalogEvidenceMetric.DISTINCT_DELAYED_RECALLS,
                CatalogEvidenceMetric.VERIFIED_COMEBACK_SESSIONS,
                CatalogEvidenceMetric.DISTINCT_VERIFIED_SAVED_ITEMS,
                CatalogEvidenceMetric.VERIFIED_REVIEW_QUEUE_CLEAR_DAYS
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

        val repairTierOne = request.unlocks.single {
            it.unlock.achievementId == "achievement.correction.repair_loop" &&
                it.unlock.level == 1
        }
        assertEquals(5L, repairTierOne.target)
        assertEquals(CatalogEvidenceMetric.DISTINCT_REPAIRED_SUBJECTS, repairTierOne.metric)

        val confusableTierOne = request.unlocks.single {
            it.unlock.achievementId == "achievement.mastery.confusable_precision" &&
                it.unlock.level == 1
        }
        assertEquals(5L, confusableTierOne.target)
        assertEquals(CatalogEvidenceMetric.DISTINCT_CONFUSABLE_PAIRS, confusableTierOne.metric)

        val contextTierOne = request.unlocks.single {
            it.unlock.achievementId == "achievement.application.context_builder" &&
                it.unlock.level == 1
        }
        assertEquals(5L, contextTierOne.target)
        assertEquals(CatalogEvidenceMetric.DISTINCT_CONTEXT_APPLICATIONS, contextTierOne.metric)

        val delayedRecallTierOne = request.unlocks.single {
            it.unlock.achievementId == "achievement.challenge.delayed_recall" &&
                it.unlock.level == 1
        }
        assertEquals(3L, delayedRecallTierOne.target)
        assertEquals(CatalogEvidenceMetric.DISTINCT_DELAYED_RECALLS, delayedRecallTierOne.metric)

        val gentleReturn = request.unlocks.single {
            it.unlock.achievementId == "achievement.comeback.gentle_return" && it.unlock.level == 1
        }
        assertEquals(1L, gentleReturn.target)
        assertEquals(CatalogEvidenceMetric.VERIFIED_COMEBACK_SESSIONS, gentleReturn.metric)

        val savedItemTierOne = request.unlocks.single {
            it.unlock.achievementId == "achievement.collection.learned_not_saved" && it.unlock.level == 1
        }
        assertEquals(10L, savedItemTierOne.target)
        assertEquals(CatalogEvidenceMetric.DISTINCT_VERIFIED_SAVED_ITEMS, savedItemTierOne.metric)

        val queueClearTierOne = request.unlocks.single {
            it.unlock.achievementId == "achievement.review.queue_resolved" && it.unlock.level == 1
        }
        assertEquals(1L, queueClearTierOne.target)
        assertEquals(CatalogEvidenceMetric.VERIFIED_REVIEW_QUEUE_CLEAR_DAYS, queueClearTierOne.metric)
    }

    @Test
    fun `repair achievement stays inert when its verified link contract drifts`() {
        val repair = bundle.catalog.achievements.single {
            it.id == "achievement.correction.repair_loop"
        }
        val weakened = repair.copy(
            criterion = repair.criterion.copy(
                clauses = listOf(repair.criterion.clauses.single().copy(filters = emptyList()))
            )
        )
        val changed = bundle.copy(
            catalog = bundle.catalog.copy(
                achievements = bundle.catalog.achievements.map { definition ->
                    if (definition.id == repair.id) weakened else definition
                }
            )
        )

        val ids = CatalogSettlementPlanner.plan(changed, event(), dueItemCountAtAssignment = 3)
            .unlocks
            .map { candidate -> candidate.unlock.achievementId }

        assertFalse(ids.contains(repair.id))
    }

    @Test
    fun `queue achievement stays inert when its starting size contract drifts`() {
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

        val ids = CatalogSettlementPlanner.plan(changed, event(), dueItemCountAtAssignment = 5)
            .unlocks
            .map { candidate -> candidate.unlock.achievementId }

        assertFalse(ids.contains(queueClear.id))
    }

    @Test
    fun `daily due review quest is deterministic and assignment eligibility is explicit`() {
        val eligible = CatalogSettlementPlanner.plan(
            bundle,
            event(),
            dueItemCountAtAssignment = 3,
            assignmentTimeZone = UTC
        ).quests.single { it.instance.definitionId == "quest.daily.due_review" }
        val ineligible = CatalogSettlementPlanner.plan(
            bundle,
            event(),
            dueItemCountAtAssignment = 2,
            assignmentTimeZone = UTC
        ).quests.single { it.instance.definitionId == "quest.daily.due_review" }

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

    @Test
    fun `daily repair quest activates only when an open repair candidate exists`() {
        val errorEvent = event().copy(
            eventId = "mistake-event",
            eventType = RewardableEventType.MISTAKE_RECORDED.wireName,
            metadataJson = "{\"repairCandidate\":true}"
        )
        val assigned = CatalogSettlementPlanner.plan(
            bundle = bundle,
            event = errorEvent,
            dueItemCountAtAssignment = 0,
            repairCandidateCountAtAssignment = 0,
            assignmentTimeZone = UTC
        ).quests.single { it.instance.definitionId == "quest.daily.repair_one" }
        val unavailable = CatalogSettlementPlanner.plan(
            bundle = bundle,
            event = event(),
            dueItemCountAtAssignment = 0,
            repairCandidateCountAtAssignment = 0,
            assignmentTimeZone = UTC
        ).quests.single { it.instance.definitionId == "quest.daily.repair_one" }

        assertEquals(CatalogEvidenceMetric.DISTINCT_REPAIRED_SUBJECTS, assigned.metric)
        assertEquals(1L, assigned.instance.target)
        assertTrue(assigned.eligibleForAssignment)
        assertFalse(unavailable.eligibleForAssignment)
        assertEquals(assigned.instance.questInstanceId, unavailable.instance.questInstanceId)
    }

    @Test
    fun `weekly scheduler emits only quests backed by exact windowed evidence`() {
        val quests = CatalogSettlementPlanner.plan(
            bundle = bundle,
            event = event(),
            dueItemCountAtAssignment = 3,
            assignmentTimeZone = UTC
        ).quests

        assertEquals(
            setOf(
                "quest.daily.due_review",
                "quest.daily.repair_one",
                "quest.daily.confusable_pair",
                "quest.daily.context_use",
                "quest.weekly.durable_mastery",
                "quest.weekly.queue_relief"
            ),
            quests.map { it.instance.definitionId }.toSet()
        )
        assertFalse(quests.any { it.instance.definitionId == "quest.weekly.mixed_practice" })

        val durable = quests.single { it.instance.definitionId == "quest.weekly.durable_mastery" }
        assertEquals(CatalogEvidenceMetric.MASTERED_ITEMS_AFTER_DELAY, durable.metric)
        assertEquals(8L, durable.instance.target)
        assertEquals(utcMillis(2023, Calendar.NOVEMBER, 13, 0), durable.evidenceStartsAtEpochMillis)
        assertEquals(utcMillis(2023, Calendar.NOVEMBER, 20, 0), durable.evidenceEndsAtEpochMillis)
        assertEquals(utcMillis(2023, Calendar.NOVEMBER, 20, 6), durable.instance.endsAtEpochMillis)
        assertTrue(durable.eligibleForAssignment)

        val queueRelief = quests.single { it.instance.definitionId == "quest.weekly.queue_relief" }
        assertEquals(CatalogEvidenceMetric.VERIFIED_REVIEW_QUEUE_CLEAR_DAYS, queueRelief.metric)
        assertEquals(1L, queueRelief.instance.target)
        assertFalse(queueRelief.eligibleForAssignment)

        val eligibleQueueRelief = CatalogSettlementPlanner.plan(
            bundle = bundle,
            event = event(),
            dueItemCountAtAssignment = 5,
            assignmentTimeZone = UTC
        ).quests.single { it.instance.definitionId == "quest.weekly.queue_relief" }
        assertTrue(eligibleQueueRelief.eligibleForAssignment)
    }

    @Test
    fun `quest with unsupported reward contract stays inert instead of blocking event settlement`() {
        val unsupportedQuest = bundle.catalog.quests
            .single { definition -> definition.id == "quest.weekly.durable_mastery" }
            .copy(rewardRefs = listOf("reward.marker.durable_reviewer"))
        val mutatedBundle = bundle.copy(
            catalog = bundle.catalog.copy(
                quests = bundle.catalog.quests.map { definition ->
                    if (definition.id == unsupportedQuest.id) unsupportedQuest else definition
                }
            )
        )

        val request = CatalogSettlementPlanner.plan(
            bundle = mutatedBundle,
            event = event(),
            dueItemCountAtAssignment = 3,
            assignmentTimeZone = UTC
        )

        assertEquals(
            setOf(
                "quest.daily.due_review",
                "quest.daily.repair_one",
                "quest.daily.confusable_pair",
                "quest.daily.context_use",
                "quest.weekly.queue_relief"
            ),
            request.quests.map { candidate -> candidate.instance.definitionId }.toSet()
        )
        assertTrue(request.unlocks.isNotEmpty())
    }

    @Test
    fun `weekly instance identity is stable inside a week and changes at the next boundary`() {
        val tuesday = event(occurredAtEpochMillis = utcMillis(2023, Calendar.NOVEMBER, 14, 12))
        val sunday = event(occurredAtEpochMillis = utcMillis(2023, Calendar.NOVEMBER, 19, 23))
        val nextMonday = event(occurredAtEpochMillis = utcMillis(2023, Calendar.NOVEMBER, 20, 0))

        fun durableId(at: GamificationEventEntity): String = CatalogSettlementPlanner.plan(
            bundle = bundle,
            event = at,
            dueItemCountAtAssignment = 0,
            assignmentTimeZone = UTC
        ).quests.single { it.instance.definitionId == "quest.weekly.durable_mastery" }
            .instance.questInstanceId

        assertEquals(durableId(tuesday), durableId(sunday))
        assertTrue(durableId(tuesday) != durableId(nextMonday))
    }

    @Test
    fun `assignment timezone is frozen by reusing the persisted overlapping instance`() {
        val event = event(occurredAtEpochMillis = utcMillis(2023, Calendar.NOVEMBER, 14, 12))
        val initial = CatalogSettlementPlanner.plan(
            bundle = bundle,
            event = event,
            dueItemCountAtAssignment = 3,
            assignmentTimeZone = UTC
        )
        val initialDurable = initial.quests.single {
            it.instance.definitionId == "quest.weekly.durable_mastery"
        }

        val afterTimeZoneChange = CatalogSettlementPlanner.plan(
            bundle = bundle,
            event = event.copy(eventId = "event-after-timezone-change"),
            dueItemCountAtAssignment = 3,
            assignmentTimeZone = TimeZone.getTimeZone("GMT+14:00"),
            existingQuestInstances = initial.quests.map { candidate -> candidate.instance }
        ).quests.single { it.instance.definitionId == "quest.weekly.durable_mastery" }

        assertEquals(initialDurable.instance.questInstanceId, afterTimeZoneChange.instance.questInstanceId)
        assertEquals(initialDurable.evidenceStartsAtEpochMillis, afterTimeZoneChange.evidenceStartsAtEpochMillis)
        assertEquals(initialDurable.evidenceEndsAtEpochMillis, afterTimeZoneChange.evidenceEndsAtEpochMillis)
        assertFalse(afterTimeZoneChange.eligibleForAssignment)
        assertEquals(
            "UTC",
            QuestAssignmentContracts.decodeCriteria(afterTimeZoneChange.instance.criteriaJson)
                ?.assignmentTimeZoneId
        )
    }

    @Test
    fun `late replay cannot assign an expired quest window`() {
        val occurredOnSunday = utcMillis(2023, Calendar.NOVEMBER, 19, 23)
        val recordedAfterGrace = utcMillis(2023, Calendar.NOVEMBER, 20, 7)
        val request = CatalogSettlementPlanner.plan(
            bundle = bundle,
            event = event(
                occurredAtEpochMillis = occurredOnSunday,
                recordedAtEpochMillis = recordedAfterGrace
            ),
            dueItemCountAtAssignment = 3,
            assignmentTimeZone = UTC
        )

        assertTrue(request.quests.isNotEmpty())
        assertTrue(request.quests.none { it.eligibleForAssignment })
    }

    private fun event(
        occurredAtEpochMillis: Long = 1_700_000_000_000L,
        recordedAtEpochMillis: Long = occurredAtEpochMillis
    ) = GamificationEventEntity(
        eventId = "event-settlement",
        actorId = "learner",
        eventType = "review_completed",
        subjectType = "lexicon_entry",
        subjectId = "term-a",
        occurredAtEpochMillis = occurredAtEpochMillis,
        recordedAtEpochMillis = recordedAtEpochMillis,
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

    private fun utcMillis(year: Int, month: Int, day: Int, hour: Int): Long =
        Calendar.getInstance(UTC).run {
            clear()
            set(year, month, day, hour, 0, 0)
            timeInMillis
        }

    private companion object {
        val UTC: TimeZone = TimeZone.getTimeZone("UTC")
    }
}
