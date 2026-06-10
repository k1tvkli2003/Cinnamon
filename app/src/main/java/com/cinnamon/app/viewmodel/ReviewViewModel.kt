package com.cinnamon.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cinnamon.app.data.local.LexiconEntry
import com.cinnamon.app.data.prefs.ProgressStore
import com.cinnamon.app.domain.repository.LexiconRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val finished: Boolean = false
) {
    val current: LexiconEntry? get() = cards.getOrNull(index)
    val total: Int get() = cards.size
    val done: Int get() = index
}

/** Drives a single SM-2 review session over the lexicon. */
class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LexiconRepository.getInstance(application)
    private val store = ProgressStore.getInstance(application)

    private val _state = MutableStateFlow(ReviewUiState())
    val state = _state.asStateFlow()

    init {
        startSession()
    }

    fun startSession(limit: Int = 16) {
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
        val s = _state.value
        val card = s.current ?: return
        val isNewWord = card.reps == 0 && card.timesSeen == 0
        val xp = if (quality < 3) 2 else if (isNewWord) 10 else 5

        viewModelScope.launch {
            repository.grade(card, quality)
            store.addXp(xp)
            store.incrementReviewed()
        }

        val nextIndex = s.index + 1
        _state.value = s.copy(
            index = nextIndex,
            revealed = false,
            gradedAgain = s.gradedAgain + if (quality == 1) 1 else 0,
            gradedHard = s.gradedHard + if (quality == 3) 1 else 0,
            gradedGood = s.gradedGood + if (quality == 4) 1 else 0,
            gradedEasy = s.gradedEasy + if (quality == 5) 1 else 0,
            earnedXp = s.earnedXp + xp,
            finished = nextIndex >= s.cards.size
        )
    }
}
