package com.example.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Achievement(val title: String, val description: String, val icon: String, val unlocked: Boolean)
data class Quest(val title: String, val progress: String, val completed: Boolean)
data class Replay(val title: String, val date: String, val commentary: String)

class UserProgressViewModel : ViewModel() {
    private val _points = MutableStateFlow(1250)
    val points = _points.asStateFlow()

    private val _streak = MutableStateFlow(12)
    val streak = _streak.asStateFlow()

    // Clinical rank representation (Idea 41)
    private val _rank = MutableStateFlow("Resident")
    val rank = _rank.asStateFlow()

    // Streak Rescue Defibrillators available (Idea 49)
    private val _defibrillators = MutableStateFlow(2)
    val defibrillators = _defibrillators.asStateFlow()

    // Dynamic stats for charts (Idea 48)
    private val _weeklyXpDistribution = MutableStateFlow(listOf(120, 240, 80, 190, 310, 150, 420))
    val weeklyXpDistribution = _weeklyXpDistribution.asStateFlow()

    // Real, functional clinical knowledge coverage depths
    private val _cardiologyDepth = MutableStateFlow(0.82f)
    val cardiologyDepth = _cardiologyDepth.asStateFlow()

    private val _pulmonologyDepth = MutableStateFlow(0.65f)
    val pulmonologyDepth = _pulmonologyDepth.asStateFlow()

    private val _generalCareDepth = MutableStateFlow(0.48f)
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

    // Dynamic User Theming (Idea 60) and True AMOLED Dark Mode (Idea 59)
    private val _selectedTheme = MutableStateFlow("Cyberpunk Neon")
    val selectedTheme = _selectedTheme.asStateFlow()

    fun updateTheme(themeName: String) {
        _selectedTheme.value = themeName
    }

    // RPG Skill Tree Progress (Idea 42)
    private val _skillPoints = MutableStateFlow(3)
    val skillPoints = _skillPoints.asStateFlow()

    private val _unlockedSkills = MutableStateFlow(setOf("Basic Latin Roots", "Bedside Intonation"))
    val unlockedSkills = _unlockedSkills.asStateFlow()

    // MMR/Elo Adaptive Difficulty rating (Idea 51)
    private val _eloRating = MutableStateFlow(1420)
    val eloRating = _eloRating.asStateFlow()

    // Faction/Guild representation (Idea 55)
    private val _selectedFaction = MutableStateFlow("None")
    val selectedFaction = _selectedFaction.asStateFlow()

    private val _factionProgress = MutableStateFlow(7200) // Total points contributed by faction members
    val factionProgress = _factionProgress.asStateFlow()

    // Daily Quests (Idea 47)
    private val _dailyQuests = MutableStateFlow(
        listOf(
            Quest("Use the word 'exacerbate' in a patient roleplay", "0/1", false),
            Quest("Complete 20 flashcards in Match-3", "12/20", false),
            Quest("Unlock a C2 Synonym in the Escalator", "1/1", true)
        )
    )
    val dailyQuests = _dailyQuests.asStateFlow()

    // Roleplay Replays (Idea 54)
    private val _replays = MutableStateFlow(
        listOf(
            Replay("Dr. Stu vs. SBAR Emergency Crash", "2 days ago", "Polished and precise! Commendable use of 'decompensation' instead of generic 'getting worse'. Your soft conditionals smoothed out critical updates."),
            Replay("Anxious Asthma Interview Replay", "Last week", "Expertly pacified patient anxiety. High rating scored for avoiding medical jargons ('dyspnea') when explaining to the layperson.")
        )
    )
    val replays = _replays.asStateFlow()

    private val _achievements = MutableStateFlow(
        listOf(
            Achievement("First History", "Completed an AI patient interview", "🏥", true),
            Achievement("The Empath", "Reached >90% Bedside Empathy Rating", "💗", true),
            Achievement("Grammar Surgeon", "Completed Sentence Unscramble without errors", "✂️", false),
            Achievement("Residency Master", "Accumulated over 1500 points", "🏆", false),
            Achievement("Idiom Alchemist", "Completed Make It Native challenge", "🔮", true)
        )
    )
    val achievements = _achievements.asStateFlow()

    fun addPoints(amount: Int) {
        _points.value += amount
        updateRankIfNeeded()
        checkAchievements()
        val currentDist = _weeklyXpDistribution.value.toMutableList()
        if (currentDist.isNotEmpty()) {
            val lastIdx = currentDist.lastIndex
            currentDist[lastIdx] = currentDist[lastIdx] + amount
            _weeklyXpDistribution.value = currentDist
        }
    }

    fun unlockAchievement(title: String) {
        val current = _achievements.value.toMutableList()
        val index = current.indexOfFirst { it.title == title }
        if (index != -1 && !current[index].unlocked) {
            current[index] = current[index].copy(unlocked = true)
            _achievements.value = current
        }
    }

    private fun checkAchievements() {
        val current = _achievements.value.toMutableList()
        val pts = _points.value
        val residencyMasterIndex = current.indexOfFirst { it.title == "Residency Master" }
        if (residencyMasterIndex != -1 && pts >= 1500) {
            if (!current[residencyMasterIndex].unlocked) {
                current[residencyMasterIndex] = current[residencyMasterIndex].copy(unlocked = true)
                _achievements.value = current
            }
        }
    }

    fun rescueStreak(): Boolean {
        if (_defibrillators.value > 0) {
            _defibrillators.value -= 1
            _streak.value += 1 // Safeguard the streak or increment it
            return true
        }
        return false
    }

    fun buyDefibrillator(cost: Int = 300): Boolean {
        if (_points.value >= cost) {
            _points.value -= cost
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
            if (idx != -1) {
                completeQuest(idx)
            }
        }
    }

    fun completeQuest(index: Int) {
        val current = _dailyQuests.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(progress = "1/1", completed = true)
            _dailyQuests.value = current
            addPoints(50)
        }
    }

    private fun updateRankIfNeeded() {
        val pts = _points.value
        _rank.value = when {
            pts >= 2500 -> "Chief of Surgery"
            pts >= 1800 -> "Attending"
            pts >= 1200 -> "Resident"
            pts >= 600 -> "Intern"
            else -> "Pre-Med"
        }
    }
}
