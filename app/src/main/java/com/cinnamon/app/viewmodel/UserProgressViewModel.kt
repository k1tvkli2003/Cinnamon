package com.cinnamon.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cinnamon.app.data.prefs.ProgressSnapshot
import com.cinnamon.app.data.prefs.ProgressStore
import com.cinnamon.app.domain.repository.LexiconRepository
import com.cinnamon.app.ui.theme.CinnamonThemes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Achievement(val title: String, val description: String, val icon: String, val unlocked: Boolean)
data class Quest(val title: String, val progress: String, val completed: Boolean)
data class Replay(val title: String, val date: String, val commentary: String)

/**
 * Learner progress. XP, streak, goal, and theme are persisted via [ProgressStore];
 * the playful gamification layer (skill tree, factions, elo, quests) stays in-memory.
 */
class UserProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val store = ProgressStore.getInstance(application)
    private val repository = LexiconRepository.getInstance(application)

    private val snapshot: StateFlow<ProgressSnapshot> = store.snapshot
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProgressSnapshot())

    // ── Persisted core ──────────────────────────────────────────────────────
    val points: StateFlow<Int> = snapshot.map { it.xp }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val streak: StateFlow<Int> = snapshot.map { it.streak }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val streakAliveToday: StateFlow<Boolean> = snapshot.map { it.streakAliveToday }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val xpToday: StateFlow<Int> = snapshot.map { it.xpToday }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val dailyGoalXp: StateFlow<Int> = snapshot.map { it.dailyGoalXp }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 80)

    val reviewedToday: StateFlow<Int> = snapshot.map { it.reviewedToday }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val weeklyXpDistribution: StateFlow<List<Int>> = snapshot.map { it.weeklyXp }
        .stateIn(viewModelScope, SharingStarted.Eagerly, List(7) { 0 })

    val selectedTheme: StateFlow<String> = snapshot.map { it.theme }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CinnamonThemes.TOASTED)

    val rank: StateFlow<String> = snapshot.map { rankFor(it.xp) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Pre-Med")

    // ── Lexicon-derived stats ───────────────────────────────────────────────
    val totalWords: StateFlow<Int> = repository.lexicon.totalCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val masteredWords: StateFlow<Int> = repository.lexicon.masteredCount()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val dueNow: StateFlow<Int> = repository.lexicon.dueCount(System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun addPoints(amount: Int) {
        viewModelScope.launch { store.addXp(amount) }
        checkAchievements(points.value + amount)
    }

    fun updateTheme(themeName: String) {
        viewModelScope.launch { store.setTheme(themeName) }
    }

    fun setDailyGoal(xp: Int) {
        viewModelScope.launch { store.setDailyGoal(xp) }
    }

    private fun rankFor(pts: Int): String = when {
        pts >= 6000 -> "Chief of Surgery"
        pts >= 3500 -> "Attending"
        pts >= 1800 -> "Senior Resident"
        pts >= 800 -> "Extern"
        pts >= 250 -> "Intern"
        else -> "Pre-Med"
    }

    // ── Playful layer (in-memory) ───────────────────────────────────────────
    private val _defibrillators = MutableStateFlow(2)
    val defibrillators = _defibrillators.asStateFlow()

    private val _cardiologyDepth = MutableStateFlow(0.35f)
    val cardiologyDepth = _cardiologyDepth.asStateFlow()

    private val _pulmonologyDepth = MutableStateFlow(0.25f)
    val pulmonologyDepth = _pulmonologyDepth.asStateFlow()

    private val _generalCareDepth = MutableStateFlow(0.30f)
    val generalCareDepth = _generalCareDepth.asStateFlow()

    fun incrementCardiology(by: Float) {
        _cardiologyDepth.value = (_cardiologyDepth.value + by).coerceIn(0f, 1f)
    }

    fun incrementPulmonology(by: Float) {
        _pulmonologyDepth.value = (_pulmonologyDepth.value + by).coerceIn(0f, 1f)
    }

    fun incrementGeneralCare(by: Float) {
        _generalCareDepth.value = (_generalCareDepth.value + by).coerceIn(0f, 1f)
    }

    private val _skillPoints = MutableStateFlow(3)
    val skillPoints = _skillPoints.asStateFlow()

    private val _unlockedSkills = MutableStateFlow(setOf("Basic Latin Roots", "Bedside Intonation"))
    val unlockedSkills = _unlockedSkills.asStateFlow()

    private val _eloRating = MutableStateFlow(1420)
    val eloRating = _eloRating.asStateFlow()

    private val _selectedFaction = MutableStateFlow("None")
    val selectedFaction = _selectedFaction.asStateFlow()

    private val _factionProgress = MutableStateFlow(7200)
    val factionProgress = _factionProgress.asStateFlow()

    private val _dailyQuests = MutableStateFlow(
        listOf(
            Quest("Use the word 'exacerbate' in a patient roleplay", "0/1", false),
            Quest("Clear your review queue", "0/1", false),
            Quest("Win a round of Cloze Clinic", "0/1", false)
        )
    )
    val dailyQuests = _dailyQuests.asStateFlow()

    private val _replays = MutableStateFlow(
        listOf(
            Replay(
                "Dr. Stu vs. SBAR Emergency Crash",
                "2 days ago",
                "Polished and precise! Commendable use of 'decompensation' instead of generic 'getting worse'. Your soft conditionals smoothed out critical updates."
            ),
            Replay(
                "Anxious Asthma Interview Replay",
                "Last week",
                "Expertly pacified patient anxiety. High rating scored for avoiding medical jargon ('dyspnea') when explaining to the layperson."
            )
        )
    )
    val replays = _replays.asStateFlow()

    private val _achievements = MutableStateFlow(
        listOf(
            Achievement("First History", "Completed an AI patient interview", "🏥", false),
            Achievement("The Empath", "Reached >90% Bedside Empathy Rating", "💗", false),
            Achievement("Grammar Surgeon", "Completed Sentence Unscramble without errors", "✂️", false),
            Achievement("Residency Master", "Accumulated over 1500 XP", "🏆", false),
            Achievement("Idiom Alchemist", "Completed a Make It Native challenge", "🔮", false),
            Achievement("Lexicon Spelunker", "Reviewed 100 words with the spaced-repetition engine", "📖", false),
            Achievement("Root Doctor", "Won Word Forge on the first attempt", "🌿", false)
        )
    )
    val achievements = _achievements.asStateFlow()

    fun unlockAchievement(title: String) {
        val current = _achievements.value.toMutableList()
        val index = current.indexOfFirst { it.title == title }
        if (index != -1 && !current[index].unlocked) {
            current[index] = current[index].copy(unlocked = true)
            _achievements.value = current
        }
    }

    private fun checkAchievements(projectedPoints: Int) {
        if (projectedPoints >= 1500) unlockAchievement("Residency Master")
    }

    fun rescueStreak(): Boolean {
        if (_defibrillators.value > 0) {
            _defibrillators.value -= 1
            return true
        }
        return false
    }

    fun buyDefibrillator(cost: Int = 300): Boolean {
        if (points.value >= cost) {
            _defibrillators.value += 1
            return true
        }
        return false
    }

    fun investSkillPoint(skillName: String): Boolean {
        if (_skillPoints.value > 0 && !_unlockedSkills.value.contains(skillName)) {
            _skillPoints.value -= 1
            _unlockedSkills.value = _unlockedSkills.value + skillName
            return true
        }
        return false
    }

    fun joinFaction(faction: String) {
        _selectedFaction.value = faction
    }

    fun increaseElo(amount: Int) {
        _eloRating.value = (_eloRating.value + amount).coerceAtLeast(100)
    }

    fun triggerWordUse(word: String) {
        if (word.lowercase() == "exacerbate") {
            val idx = _dailyQuests.value.indexOfFirst { it.title.contains("exacerbate") && !it.completed }
            if (idx != -1) completeQuest(idx)
        }
    }

    fun completeQuest(index: Int) {
        val current = _dailyQuests.value.toMutableList()
        if (index in current.indices && !current[index].completed) {
            current[index] = current[index].copy(progress = "1/1", completed = true)
            _dailyQuests.value = current
            addPoints(50)
        }
    }
}
