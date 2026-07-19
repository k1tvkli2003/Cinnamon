package com.cinnamon.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cinnamon.app.data.local.LexiconEntry
import com.cinnamon.app.domain.repository.LexiconRepository
import com.cinnamon.app.domain.repository.StaleReviewAttemptException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ReviewSubmissionError {
    CARD_CHANGED,
    STORAGE_UNAVAILABLE
}

data class ReviewUiState(
    val loading: Boolean = true,
    val cards: List<LexiconEntry> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val gradedAgain: Int = 0,
    val gradedHard: Int = 0,
    val gradedGood: Int = 0,
    val gradedEasy: Int = 0,
    val earnedXp: Int = 0,
    val submitting: Boolean = false,
    val submissionError: ReviewSubmissionError? = null,
    val finished: Boolean = false
) {
    val current: LexiconEntry? get() = cards.getOrNull(index)
    val total: Int get() = cards.size
    val done: Int get() = index
}

/** Drives a single SM-2 review session over the lexicon. */
class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LexiconRepository.getInstance(application)

    private data class PendingReviewAttempt(
        val cardId: Long,
        val quality: Int,
        val occurrenceKey: String,
        val occurredAtEpochMillis: Long
    )

    private var pendingAttempt: PendingReviewAttempt? = null

    private val _state = MutableStateFlow(ReviewUiState())
    val state = _state.asStateFlow()

    init {
        startSession()
    }

    fun startSession(limit: Int = 16) {
        pendingAttempt = null
        _state.value = ReviewUiState(loading = true)
        viewModelScope.launch {
            val cards = repository.buildReviewSession(limit)
            _state.value = ReviewUiState(
                loading = false,
                cards = cards,
                finished = cards.isEmpty()
            )
        }
    }

    fun reveal() {
        _state.value = _state.value.copy(revealed = true)
    }

    /** quality: 1 = Again, 3 = Hard, 4 = Good, 5 = Easy */
    fun grade(quality: Int) {
        require(quality in setOf(1, 3, 4, 5)) { "Unsupported review quality: $quality" }
        val s = _state.value
        if (s.submitting) return
        val card = s.current ?: return
        val attempt = pendingAttempt?.takeIf { it.cardId == card.id } ?: PendingReviewAttempt(
            cardId = card.id,
            quality = quality,
            occurrenceKey = card.stableReviewAttemptKey(quality),
            occurredAtEpochMillis = System.currentTimeMillis()
        ).also { pendingAttempt = it }
        submit(card, attempt)
    }

    /** Replays the exact logical attempt after a transient storage failure. */
    fun retryGrade() {
        val s = _state.value
        if (s.submitting) return
        val card = s.current ?: return
        val attempt = pendingAttempt?.takeIf { it.cardId == card.id } ?: return
        submit(card, attempt)
    }

    private fun submit(card: LexiconEntry, attempt: PendingReviewAttempt) {
        _state.value = _state.value.copy(submitting = true, submissionError = null)
        viewModelScope.launch {
            runCatching {
                repository.commitReview(
                    expectedEntry = card,
                    quality = attempt.quality,
                    occurrenceKey = attempt.occurrenceKey,
                    occurredAtEpochMillis = attempt.occurredAtEpochMillis
                )
            }.onSuccess { result ->
                val committedState = _state.value
                if (committedState.current?.id != attempt.cardId) return@onSuccess
                pendingAttempt = null
                val nextIndex = committedState.index + 1
                _state.value = committedState.copy(
                    index = nextIndex,
                    revealed = false,
                    gradedAgain = committedState.gradedAgain + if (attempt.quality == 1) 1 else 0,
                    gradedHard = committedState.gradedHard + if (attempt.quality == 3) 1 else 0,
                    gradedGood = committedState.gradedGood + if (attempt.quality == 4) 1 else 0,
                    gradedEasy = committedState.gradedEasy + if (attempt.quality == 5) 1 else 0,
                    earnedXp = committedState.earnedXp + result.xpAwarded,
                    submitting = false,
                    submissionError = null,
                    finished = nextIndex >= committedState.cards.size
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    submitting = false,
                    submissionError = if (error is StaleReviewAttemptException) {
                        ReviewSubmissionError.CARD_CHANGED
                    } else {
                        ReviewSubmissionError.STORAGE_UNAVAILABLE
                    }
                )
            }
        }
    }
}

/**
 * The schedule snapshot is the logical attempt version. Two UI sessions that
 * loaded the same card therefore converge on one event; after a successful
 * commit, the incremented schedule produces a different key for a future due
 * review.
 */
private fun LexiconEntry.stableReviewAttemptKey(quality: Int): String =
    "srs_review_v1:$id:$timesSeen:$reps:$dueAt:${intervalDays.toBits()}:${easeFactor.toBits()}:$quality"
