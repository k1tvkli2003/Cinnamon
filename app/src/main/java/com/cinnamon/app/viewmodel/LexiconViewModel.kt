package com.cinnamon.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cinnamon.app.data.local.Abbreviation
import com.cinnamon.app.data.local.Confusable
import com.cinnamon.app.data.local.LexiconEntry
import com.cinnamon.app.data.local.Morpheme
import com.cinnamon.app.data.local.PhraseEntry
import com.cinnamon.app.domain.repository.LexiconRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class LexiconViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LexiconRepository.getInstance(application)

    // ── Shared search ───────────────────────────────────────────────────────
    val query = MutableStateFlow("")

    // ── Words tab filters ───────────────────────────────────────────────────
    val domainFilter = MutableStateFlow("all")   // all | clinical | general
    val levelFilter = MutableStateFlow("all")    // all | B2+ | C1 | C2

    val entries: StateFlow<List<LexiconEntry>> =
        combine(query, domainFilter, levelFilter) { q, d, l -> Triple(q, d, l) }
            .flatMapLatest { (q, d, l) -> repository.lexicon.search(q.trim(), d, l) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalCount: StateFlow<Int> = repository.lexicon.totalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), -1)

    // ── Roots tab ───────────────────────────────────────────────────────────
    val morphemeKind = MutableStateFlow("all")   // all | prefix | root | suffix

    val morphemes: StateFlow<List<Morpheme>> =
        combine(query, morphemeKind) { q, k -> q to k }
            .flatMapLatest { (q, k) -> repository.learn.morphemes(q.trim(), k) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Abbreviations tab ───────────────────────────────────────────────────
    val abbreviations: StateFlow<List<Abbreviation>> =
        query.flatMapLatest { q -> repository.learn.abbreviations(q.trim()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Phrases tab ─────────────────────────────────────────────────────────
    val phraseCategory = MutableStateFlow("all")

    val phraseCategories: StateFlow<List<String>> = repository.learn.phraseCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val phrases: StateFlow<List<PhraseEntry>> =
        combine(query, phraseCategory) { q, c -> q to c }
            .flatMapLatest { (q, c) -> repository.learn.phrases(q.trim(), c) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Mix-ups tab ─────────────────────────────────────────────────────────
    val confusables: StateFlow<List<Confusable>> =
        query.flatMapLatest { q -> repository.learn.confusables(q.trim()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Detail support ──────────────────────────────────────────────────────
    fun entry(id: Long): Flow<LexiconEntry?> = repository.lexicon.entryById(id)

    suspend fun related(entry: LexiconEntry): List<LexiconEntry> =
        repository.lexicon.relatedByTopic(entry.topic, entry.id, 6)

    fun toggleBookmark(entry: LexiconEntry) {
        viewModelScope.launch { repository.toggleBookmark(entry) }
    }

    fun startLearning(entry: LexiconEntry) {
        viewModelScope.launch { repository.startLearning(entry) }
    }
}
