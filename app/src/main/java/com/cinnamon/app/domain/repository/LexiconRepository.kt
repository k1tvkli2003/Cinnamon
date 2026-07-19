package com.cinnamon.app.domain.repository

import android.content.Context
import androidx.room.withTransaction
import com.cinnamon.app.data.gamification.GamificationRepository
import com.cinnamon.app.data.gamification.LearningEventCommand
import com.cinnamon.app.data.gamification.LearningEventSource
import com.cinnamon.app.data.gamification.RewardableEventType
import com.cinnamon.app.data.local.AppDatabase
import com.cinnamon.app.data.local.LexiconEntry
import com.cinnamon.app.data.local.RewardWriteStatus
import com.cinnamon.app.data.prefs.ProgressStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlin.math.max

/** A single cloze round: the example sentence with the term blanked out + 4 options. */
data class ClozeRound(
    val entry: LexiconEntry,
    val blankedSentence: String,
    val options: List<String>
)

data class ReviewCommitResult(
    val entry: LexiconEntry,
    val xpAwarded: Int,
    val wasAlreadyCommitted: Boolean
)

class StaleReviewAttemptException : IllegalStateException(
    "This review card changed before the attempt could be committed"
)

class LexiconRepository private constructor(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val gamification = GamificationRepository.getInstance(context)
    val lexicon = db.lexiconDao()
    val learn = db.learnDao()

    // ── SM-2 spaced repetition ──────────────────────────────────────────────

    /**
     * Commits the semantic review event, its reward decision, and the SM-2
     * schedule mutation in one Room transaction. [occurrenceKey] must identify
     * the logical attempt rather than a network/UI invocation so retries are
     * idempotent.
     */
    suspend fun commitReview(
        expectedEntry: LexiconEntry,
        quality: Int,
        occurrenceKey: String,
        occurredAtEpochMillis: Long
    ): ReviewCommitResult {
        require(quality in setOf(1, 3, 4, 5)) { "Unsupported review quality: $quality" }
        require(occurrenceKey.isNotBlank()) { "occurrenceKey must not be blank" }
        require(occurredAtEpochMillis >= 0L) { "occurredAtEpochMillis must not be negative" }

        return db.withTransaction {
            val current = checkNotNull(lexicon.entryByIdOnce(expectedEntry.id)) {
                "Review entry ${expectedEntry.id} no longer exists"
            }
            // "Again" is a scheduling retry, not a successful recall. It must
            // not enter the completed-review ledger because that ledger drives
            // XP, daily quests, streaks, and achievement evidence.
            val rewardWrite = if (isSuccessfulReviewQuality(quality)) {
                gamification.record(
                    LearningEventCommand(
                        eventType = RewardableEventType.REVIEW_COMPLETED,
                        subjectType = REVIEW_SUBJECT_TYPE,
                        subjectId = current.id.toString(),
                        occurrenceKey = occurrenceKey,
                        completedItemCount = 1,
                        source = LearningEventSource.REVIEW,
                        occurredAtEpochMillis = occurredAtEpochMillis
                    )
                )
            } else {
                null
            }

            if (rewardWrite?.status == RewardWriteStatus.DUPLICATE) {
                return@withTransaction ReviewCommitResult(
                    entry = current,
                    xpAwarded = rewardWrite.summary?.xpAwarded?.toSafeInt() ?: 0,
                    wasAlreadyCommitted = true
                )
            }

            if (!current.hasSameReviewScheduleAs(expectedEntry)) {
                throw StaleReviewAttemptException()
            }

            val updated = scheduleReview(current, quality, occurredAtEpochMillis)
            lexicon.update(updated)
            val masteryWrite = if (
                isConceptMasteryPromotion(
                    quality = quality,
                    previousReps = current.reps,
                    updatedReps = updated.reps,
                    updatedIntervalDays = updated.intervalDays
                )
            ) {
                gamification.record(
                    LearningEventCommand(
                        eventType = RewardableEventType.CONCEPT_MASTERED,
                        subjectType = REVIEW_SUBJECT_TYPE,
                        subjectId = current.id.toString(),
                        occurrenceKey = "$occurrenceKey:delayed_mastery_v1",
                        completedItemCount = 1,
                        source = LearningEventSource.REVIEW,
                        occurredAtEpochMillis = occurredAtEpochMillis
                    )
                )
            } else {
                null
            }
            ReviewCommitResult(
                entry = updated,
                xpAwarded = (rewardWrite?.summary?.xpAwarded ?: 0L)
                    .plus(masteryWrite?.summary?.xpAwarded ?: 0L)
                    .toSafeInt(),
                wasAlreadyCommitted = false
            )
        }
    }

    private fun scheduleReview(entry: LexiconEntry, quality: Int, now: Long): LexiconEntry {
        val updated = if (quality < 3) {
            entry.copy(
                reps = 0,
                intervalDays = 0f,
                dueAt = now + TEN_MINUTES_MS,
                timesSeen = entry.timesSeen + 1
            )
        } else {
            val newReps = entry.reps + 1
            val newEase = max(
                1.3f,
                entry.easeFactor + 0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f)
            )
            val newInterval = when (newReps) {
                1 -> 1f
                2 -> 3f
                else -> (entry.intervalDays * newEase).coerceAtLeast(4f)
            }
            entry.copy(
                reps = newReps,
                easeFactor = newEase,
                intervalDays = newInterval,
                dueAt = now + (newInterval * DAY_MS).toLong(),
                timesSeen = entry.timesSeen + 1
            )
        }
        return updated
    }

    /** Due cards first, then brand-new words fill the rest of the session. */
    suspend fun buildReviewSession(limit: Int = 16): List<LexiconEntry> {
        val due = lexicon.dueEntries(System.currentTimeMillis(), limit)
        val remaining = limit - due.size
        val fresh = if (remaining > 0) lexicon.freshEntries(remaining) else emptyList()
        return due.shuffled() + fresh.shuffled()
    }

    /** Pull a word into today's queue without touching its learning state. */
    suspend fun startLearning(entry: LexiconEntry) {
        if (entry.dueAt == 0L) {
            lexicon.update(entry.copy(dueAt = System.currentTimeMillis()))
        }
    }

    suspend fun toggleBookmark(entry: LexiconEntry) {
        lexicon.update(entry.copy(isBookmarked = !entry.isBookmarked))
    }

    // ── Daily word ──────────────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    fun wordOfTheDay(): Flow<LexiconEntry?> = lexicon.totalCount().flatMapLatest { count ->
        if (count == 0) {
            flowOf(null)
        } else {
            val index = ((ProgressStore.localEpochDay() * 31) % count).toInt()
            lexicon.entryAtOffset(index)
        }
    }

    // ── Game material ───────────────────────────────────────────────────────

    suspend fun clozeRounds(count: Int): List<ClozeRound> {
        val candidates = lexicon.clozeCandidates(count * 2)
        return candidates.mapNotNull { entry ->
            val regex = Regex(Regex.escape(entry.term), RegexOption.IGNORE_CASE)
            if (!regex.containsMatchIn(entry.exampleClinical)) return@mapNotNull null
            val blanked = regex.replace(entry.exampleClinical, "_______")
            val distractors = lexicon.distractors(entry.topic, entry.term, 3).map { it.term }
            if (distractors.size < 3) return@mapNotNull null
            ClozeRound(
                entry = entry,
                blankedSentence = blanked,
                options = (distractors + entry.term).shuffled()
            )
        }.take(count)
    }

    companion object {
        private const val TEN_MINUTES_MS = 10 * 60 * 1000L
        private const val DAY_MS = 86_400_000L
        private const val REVIEW_SUBJECT_TYPE = "lexicon_entry"

        @Volatile
        private var INSTANCE: LexiconRepository? = null

        fun getInstance(context: Context): LexiconRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LexiconRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}

private fun LexiconEntry.hasSameReviewScheduleAs(other: LexiconEntry): Boolean =
    id == other.id &&
        reps == other.reps &&
        timesSeen == other.timesSeen &&
        dueAt == other.dueAt &&
        intervalDays.compareTo(other.intervalDays) == 0 &&
        easeFactor.compareTo(other.easeFactor) == 0

/** Only a successful self-assessed recall is progress evidence. */
internal fun isSuccessfulReviewQuality(quality: Int): Boolean = quality in setOf(3, 4, 5)

/**
 * Third successful repetition creates a four-day-or-longer interval under this
 * SM-2 policy. That is the first point at which a term can truthfully count as
 * durable, delayed mastery rather than an XP-derived level.
 */
internal fun isConceptMasteryPromotion(
    quality: Int,
    previousReps: Int,
    updatedReps: Int,
    updatedIntervalDays: Float
): Boolean =
    isSuccessfulReviewQuality(quality) &&
        previousReps < MIN_REPS_FOR_DELAYED_MASTERY &&
        updatedReps >= MIN_REPS_FOR_DELAYED_MASTERY &&
        updatedIntervalDays >= MIN_DELAYED_MASTERY_INTERVAL_DAYS

private const val MIN_REPS_FOR_DELAYED_MASTERY = 3
private const val MIN_DELAYED_MASTERY_INTERVAL_DAYS = 4f

private fun Long.toSafeInt(): Int = coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
