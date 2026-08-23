package com.cinnamon.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Proves catalog repair evidence is rebuilt from a valid immutable mistake → correction link. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GamificationRepairEvidenceQueryTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: GamificationDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.gamificationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `repair evidence accepts only a same-subject linked prior repair candidate`() = runBlocking {
        dao.insertEventIfAbsent(
            event(
                id = "mistake-valid",
                type = "mistake_recorded",
                subjectId = "valid-subject",
                occurredAt = 100L,
                metadataJson = "{\"repairCandidate\":true}"
            )
        )
        dao.insertEventIfAbsent(
            event(
                id = "repair-valid",
                type = "mistake_corrected",
                subjectId = "valid-subject",
                occurredAt = 200L,
                metadataJson = verifiedRepairMetadata("mistake-valid")
            )
        )
        // A duplicate correction for the same subject cannot inflate COUNT_DISTINCT evidence.
        dao.insertEventIfAbsent(
            event(
                id = "repair-valid-duplicate-subject",
                type = "mistake_corrected",
                subjectId = "valid-subject",
                occurredAt = 250L,
                metadataJson = verifiedRepairMetadata("mistake-valid")
            )
        )
        dao.insertEventIfAbsent(
            event(
                id = "repair-missing-link",
                type = "mistake_corrected",
                subjectId = "missing-link",
                occurredAt = 260L,
                metadataJson = verifiedRepairMetadata("not-in-ledger")
            )
        )
        dao.insertEventIfAbsent(
            event(
                id = "mistake-different-subject",
                type = "mistake_recorded",
                subjectId = "different-subject",
                occurredAt = 100L,
                metadataJson = "{\"repairCandidate\":true}"
            )
        )
        dao.insertEventIfAbsent(
            event(
                id = "repair-different-subject",
                type = "mistake_corrected",
                subjectId = "claimed-subject",
                occurredAt = 270L,
                metadataJson = verifiedRepairMetadata("mistake-different-subject")
            )
        )
        dao.insertEventIfAbsent(
            event(
                id = "mistake-after-correction",
                type = "mistake_recorded",
                subjectId = "late-mistake",
                occurredAt = 400L,
                metadataJson = "{\"repairCandidate\":true}"
            )
        )
        dao.insertEventIfAbsent(
            event(
                id = "repair-before-mistake",
                type = "mistake_corrected",
                subjectId = "late-mistake",
                occurredAt = 300L,
                metadataJson = verifiedRepairMetadata("mistake-after-correction")
            )
        )
        dao.insertEventIfAbsent(
            event(
                id = "repair-no-flags",
                type = "mistake_corrected",
                subjectId = "unverified-subject",
                occurredAt = 280L,
                metadataJson = "{\"repairOfEventId\":\"mistake-valid\"}"
            )
        )

        assertEquals(1, dao.observeDistinctVerifiedRepairSubjectCount(ACTOR_ID).first())
        assertEquals(1, dao.distinctVerifiedRepairSubjectCountOnce(ACTOR_ID))
        assertEquals(
            1,
            dao.distinctVerifiedRepairSubjectCountAfterOnce(
                actorId = ACTOR_ID,
                afterEpochMillis = 199L,
                throughEpochMillis = 250L
            )
        )
        assertEquals(
            1,
            dao.distinctVerifiedRepairSubjectCountInWindowOnce(
                actorId = ACTOR_ID,
                fromEpochMillis = 200L,
                untilEpochMillis = 251L
            )
        )
        assertEquals(
            0,
            dao.distinctVerifiedRepairSubjectCountInWindowOnce(
                actorId = ACTOR_ID,
                fromEpochMillis = 251L,
                untilEpochMillis = 400L
            )
        )
    }

    @Test
    fun `raw mistake evidence cannot create an active study day`() = runBlocking {
        dao.insertEventIfAbsent(
            event(
                id = "raw-mistake",
                type = "mistake_recorded",
                subjectId = "subject",
                occurredAt = 100L,
                metadataJson = "{\"repairCandidate\":true}"
            )
        )

        assertEquals(0, dao.observeActiveStudyDayCount(ACTOR_ID).first())

        dao.insertEventIfAbsent(
            event(
                id = "completed-review",
                type = "review_completed",
                subjectId = "subject",
                occurredAt = 200L
            )
        )
        assertEquals(1, dao.observeActiveStudyDayCount(ACTOR_ID).first())
    }

    @Test
    fun `open repair candidates exclude only a valid linked correction`() = runBlocking {
        dao.insertEventIfAbsent(
            event(
                id = "open-one",
                type = "mistake_recorded",
                subjectId = "one",
                occurredAt = 100L,
                metadataJson = "{\"repairCandidate\":true}"
            )
        )
        dao.insertEventIfAbsent(
            event(
                id = "open-two",
                type = "mistake_recorded",
                subjectId = "two",
                occurredAt = 110L,
                metadataJson = "{\"repairCandidate\":true}"
            )
        )
        dao.insertEventIfAbsent(event("not-a-candidate", "mistake_recorded", "three", 120L))
        // A forged correction on the same subject cannot hide an open opportunity.
        dao.insertEventIfAbsent(
            event(
                id = "forged-correction",
                type = "mistake_corrected",
                subjectId = "two",
                occurredAt = 130L,
                metadataJson = verifiedRepairMetadata("different-event")
            )
        )
        assertEquals(2, dao.unrepairedRepairCandidateCountOnce(ACTOR_ID))

        dao.insertEventIfAbsent(
            event(
                id = "verified-one",
                type = "mistake_corrected",
                subjectId = "one",
                occurredAt = 140L,
                metadataJson = verifiedRepairMetadata("open-one")
            )
        )
        assertEquals(1, dao.unrepairedRepairCandidateCountOnce(ACTOR_ID))

        dao.insertEventIfAbsent(
            event(
                id = "verified-two",
                type = "mistake_corrected",
                subjectId = "two",
                occurredAt = 150L,
                metadataJson = verifiedRepairMetadata("open-two")
            )
        )
        assertEquals(0, dao.unrepairedRepairCandidateCountOnce(ACTOR_ID))
    }

    @Test
    fun `confusable evidence requires its exact attempt and a full day delay`() = runBlocking {
        dao.insertEventIfAbsent(event("pair-one-attempt", "confusable_pair_attempted", "pair-one", 100L, "{\"confusableCandidate\":true}"))
        dao.insertEventIfAbsent(event("pair-one-early", "confusable_pair_resolved", "pair-one", 86_400_099L, confusableMetadata("pair-one-attempt")))
        dao.insertEventIfAbsent(event("pair-one-valid", "confusable_pair_resolved", "pair-one", 86_400_100L, confusableMetadata("pair-one-attempt")))
        // Same-pair replay, missing link, and a mismatched subject cannot inflate distinct evidence.
        dao.insertEventIfAbsent(event("pair-one-replay", "confusable_pair_resolved", "pair-one", 86_400_200L, confusableMetadata("pair-one-attempt")))
        dao.insertEventIfAbsent(event("pair-two-bad-link", "confusable_pair_resolved", "pair-two", 90_000_000L, confusableMetadata("absent")))
        dao.insertEventIfAbsent(event("pair-three-attempt", "confusable_pair_attempted", "pair-three", 100L, "{\"confusableCandidate\":true}"))
        dao.insertEventIfAbsent(event("pair-three-mismatch", "confusable_pair_resolved", "not-pair-three", 90_000_000L, confusableMetadata("pair-three-attempt")))

        assertEquals(1, dao.observeDistinctVerifiedConfusablePairCount(ACTOR_ID).first())
        assertEquals(1, dao.distinctVerifiedConfusablePairCountAfterOnce(ACTOR_ID, 86_400_099L, 86_400_201L))
        assertEquals(1, dao.distinctVerifiedConfusablePairCountInWindowOnce(ACTOR_ID, 86_400_100L, 86_400_201L))
        assertEquals(0, dao.latestUnresolvedConfusableAttemptBeforeOnce(ACTOR_ID, "lexicon_entry", "pair-one", 90_000_000L)?.let { 1 } ?: 0)
        assertEquals(1, dao.latestUnresolvedConfusableAttemptBeforeOnce(ACTOR_ID, "lexicon_entry", "pair-three", 90_000_000L)?.let { 1 } ?: 0)
    }

    @Test
    fun `context evidence requires its protected event type and verifier metadata`() = runBlocking {
        dao.insertEventIfAbsent(
            event(
                id = "context-valid",
                type = "context_application_verified",
                subjectId = "entry-1",
                occurredAt = 100L,
                metadataJson = "{\"applicationVerified\":true,\"applicationSource\":\"authored_cloze\"}"
            )
        )
        dao.insertEventIfAbsent(
            event(
                id = "context-duplicate-subject",
                type = "context_application_verified",
                subjectId = "entry-1",
                occurredAt = 200L,
                metadataJson = "{\"applicationVerified\":true}"
            )
        )
        dao.insertEventIfAbsent(
            event(
                id = "context-without-flag",
                type = "context_application_verified",
                subjectId = "entry-2",
                occurredAt = 300L
            )
        )
        dao.insertEventIfAbsent(
            event(
                id = "context-wrong-type",
                type = "practice_session_completed",
                subjectId = "entry-3",
                occurredAt = 400L,
                metadataJson = "{\"applicationVerified\":true}"
            )
        )

        assertEquals(1, dao.observeDistinctVerifiedContextApplicationCount(ACTOR_ID).first())
        assertEquals(1, dao.distinctVerifiedContextApplicationCountAfterOnce(ACTOR_ID, 99L, 250L))
        assertEquals(0, dao.distinctVerifiedContextApplicationCountInWindowOnce(ACTOR_ID, 250L, 500L))
    }

    @Test
    fun `delayed recall requires the exact successful review and seventy two hour boundary`() = runBlocking {
        dao.insertEventIfAbsent(event("review-before-recall", "review_completed", "delayed-entry", 100L))
        dao.insertEventIfAbsent(event("recall-early", "delayed_recall_succeeded", "delayed-entry", 259_200_099L, delayedRecallMetadata("review-before-recall")))
        dao.insertEventIfAbsent(event("recall-valid", "delayed_recall_succeeded", "delayed-entry", 259_200_100L, delayedRecallMetadata("review-before-recall")))
        dao.insertEventIfAbsent(event("recall-duplicate", "delayed_recall_succeeded", "delayed-entry", 259_200_200L, delayedRecallMetadata("review-before-recall")))
        dao.insertEventIfAbsent(event("recall-missing-link", "delayed_recall_succeeded", "other-entry", 300_000_000L, delayedRecallMetadata("missing-review")))

        assertEquals(1, dao.observeDistinctVerifiedDelayedRecallCount(ACTOR_ID).first())
        assertEquals(1, dao.distinctVerifiedDelayedRecallCountAfterOnce(ACTOR_ID, 259_200_099L, 259_200_201L))
        assertEquals("review-before-recall", dao.latestSuccessfulReviewBeforeOnce(ACTOR_ID, "lexicon_entry", "delayed-entry", 259_200_100L)?.eventId)
    }

    @Test
    fun `gentle return requires the exact prior activity and seven day boundary`() = runBlocking {
        dao.insertEventIfAbsent(event("activity-before-return", "practice_session_completed", "practice-1", 100L))
        dao.insertEventIfAbsent(event("return-early", "comeback_session_completed", "return-early", 604_800_099L, comebackMetadata("activity-before-return", 3)))
        dao.insertEventIfAbsent(event("return-valid", "comeback_session_completed", "return-valid", 604_800_100L, comebackMetadata("activity-before-return", 3)))
        dao.insertEventIfAbsent(event("return-too-small", "comeback_session_completed", "return-small", 700_000_000L, comebackMetadata("activity-before-return", 2)))
        dao.insertEventIfAbsent(event("return-missing-link", "comeback_session_completed", "return-missing", 700_000_001L, comebackMetadata("missing-activity", 3)))

        assertEquals(1, dao.observeVerifiedComebackSessionCount(ACTOR_ID).first())
        assertEquals(1, dao.verifiedComebackSessionCountAfterOnce(ACTOR_ID, 604_800_099L, 700_000_002L))
        assertEquals("activity-before-return", dao.latestMeaningfulActivityBeforeOnce(ACTOR_ID, 604_800_100L)?.eventId)
    }

    @Test
    fun `saved item review requires the exact bookmark and one hour boundary`() = runBlocking {
        dao.insertEventIfAbsent(event("bookmark-before-review", "bookmark_saved", "saved-entry", 100L))
        dao.insertEventIfAbsent(event("saved-review-early", "saved_item_reviewed", "saved-entry", 3_600_099L, savedItemReviewMetadata("bookmark-before-review")))
        dao.insertEventIfAbsent(event("saved-review-valid", "saved_item_reviewed", "saved-entry", 3_600_100L, savedItemReviewMetadata("bookmark-before-review")))
        dao.insertEventIfAbsent(event("saved-review-duplicate", "saved_item_reviewed", "saved-entry", 3_700_000L, savedItemReviewMetadata("bookmark-before-review")))
        dao.insertEventIfAbsent(event("saved-review-no-bookmark", "saved_item_reviewed", "other-saved-entry", 4_000_000L, savedItemReviewMetadata("missing-bookmark")))

        assertEquals(1, dao.observeDistinctVerifiedSavedItemCount(ACTOR_ID).first())
        assertEquals(1, dao.distinctVerifiedSavedItemCountAfterOnce(ACTOR_ID, 3_600_099L, 4_000_001L))
        assertEquals("bookmark-before-review", dao.latestBookmarkSavedBeforeOnce(ACTOR_ID, "lexicon_entry", "saved-entry", 3_600_100L)?.eventId)
    }

    private fun event(
        id: String,
        type: String,
        subjectId: String,
        occurredAt: Long,
        metadataJson: String = "{}"
    ) = GamificationEventEntity(
        eventId = id,
        actorId = ACTOR_ID,
        eventType = type,
        subjectType = "lexicon_entry",
        subjectId = subjectId,
        occurredAtEpochMillis = occurredAt,
        recordedAtEpochMillis = occurredAt,
        studyDay = 1L,
        idempotencyKey = "idempotency::$id",
        source = "test",
        ruleVersion = 1,
        metadataJson = metadataJson,
        replayOfEventId = null
    )

    private fun verifiedRepairMetadata(mistakeEventId: String): String =
        "{\"repairOfEventId\":\"$mistakeEventId\",\"repairLinkPresent\":true," +
            "\"incorrectAttemptPrecedesCorrection\":true}"

    private fun confusableMetadata(attemptEventId: String): String =
        "{\"confusableAttemptEventId\":\"$attemptEventId\",\"confusableLinkPresent\":true," +
            "\"minimumDelayHoursSatisfied\":true}"

    private fun delayedRecallMetadata(reviewEventId: String): String =
        "{\"priorReviewEventId\":\"$reviewEventId\",\"delayedRecallLinkPresent\":true," +
            "\"minimumDelayHoursSatisfied\":true}"

    private fun comebackMetadata(activityEventId: String, meaningfulActionCount: Int): String =
        "{\"priorActivityEventId\":\"$activityEventId\",\"comebackLinkPresent\":true," +
            "\"minimumAbsenceDaysSatisfied\":true,\"meaningfulActionCount\":$meaningfulActionCount}"

    private fun savedItemReviewMetadata(bookmarkEventId: String): String =
        "{\"bookmarkEventId\":\"$bookmarkEventId\",\"savedItemLinkPresent\":true," +
            "\"minimumDelayHoursSatisfied\":true,\"successfulReviewCount\":1}"

    private companion object {
        const val ACTOR_ID = "learner"
    }
}
