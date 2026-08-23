package com.cinnamon.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cinnamon.app.data.gamification.GamificationRepository
import com.cinnamon.app.data.gamification.LegacyLearningFocusWireCompatibility
import com.cinnamon.app.data.gamification.LegacyFoundationJourneyWireCompatibility
import com.cinnamon.app.data.gamification.LearningActivity
import com.cinnamon.app.data.gamification.LearningEventCommand
import com.cinnamon.app.data.gamification.LearningEventSource
import com.cinnamon.app.data.gamification.LearningFocusRecordIncompatibleException
import com.cinnamon.app.data.gamification.LearningFocusSelectionRecord
import com.cinnamon.app.data.gamification.PendingRewardPresentation
import com.cinnamon.app.data.gamification.QuestAssignmentContracts
import com.cinnamon.app.data.gamification.QuestboardProjector
import com.cinnamon.app.data.gamification.RewardableEventType
import com.cinnamon.app.data.prefs.ProgressSnapshot
import com.cinnamon.app.data.prefs.ProgressStore
import com.cinnamon.app.data.startup.AppStartupCoordinator
import com.cinnamon.app.data.startup.AppStartupState
import com.cinnamon.app.data.local.JourneyInstanceEntity
import com.cinnamon.app.data.local.JourneyStageProgressEntity
import com.cinnamon.app.data.local.LearningFocusAlreadySelectedException
import com.cinnamon.app.domain.gamification.AchievementFamily
import com.cinnamon.app.domain.gamification.CatalogEvidenceSnapshot
import com.cinnamon.app.domain.gamification.CatalogProgressProjector
import com.cinnamon.app.domain.gamification.CatalogProgressionLevelProjection
import com.cinnamon.app.domain.gamification.FoundationLearningFocusCatalog
import com.cinnamon.app.domain.gamification.FoundationJourneyCatalog
import com.cinnamon.app.domain.gamification.JourneyDefinition
import com.cinnamon.app.domain.gamification.JourneyDestination
import com.cinnamon.app.domain.gamification.JourneyEvidenceMetric
import com.cinnamon.app.domain.gamification.LearningFocusDefinition
import com.cinnamon.app.domain.gamification.LearningFocusTone
import com.cinnamon.app.domain.gamification.RewardType
import com.cinnamon.app.domain.repository.LexiconRepository
import com.cinnamon.app.ui.theme.CinnamonThemes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val progress: Int,
    val target: Int,
    val reached: Boolean
)

data class Quest(
    val id: String,
    val title: String,
    val progress: Int,
    val target: Int,
    val completed: Boolean,
    val detail: String,
    val instanceId: String? = null,
    val claimable: Boolean = false,
    val claimed: Boolean = false,
    val rewardXp: Int = 0
)

/** Keeps the six-signal daily board type-safe without relying on Flow's vararg combine overload. */
private data class DailyQuestInputs(
    val distinctReviewCount: Int,
    val dueItemCount: Int,
    val bundle: com.cinnamon.app.domain.gamification.GamificationCatalogBundle?,
    val instances: List<com.cinnamon.app.data.local.QuestInstanceEntity>,
    val resolvedPairs: Int
)

enum class JourneyStageUiState {
    LOCKED,
    ACTIVE,
    COMPLETED
}

data class JourneyStageUiModel(
    val id: String,
    val order: Int,
    val title: String,
    val description: String,
    val state: JourneyStageUiState,
    val progress: Int,
    val target: Int,
    val rewardXp: Int,
    val destination: JourneyDestination,
    val actionLabel: String
)

data class LearningJourneyUiModel(
    val id: String,
    val eyebrow: String,
    val title: String,
    val description: String,
    val state: String,
    val completedStageCount: Int,
    val totalStageCount: Int,
    val earnedXp: Int,
    val completionTitle: String,
    val completionDescription: String,
    val stages: List<JourneyStageUiModel>
) {
    val activeStage: JourneyStageUiModel?
        get() = stages.firstOrNull { it.state == JourneyStageUiState.ACTIVE }

    val isComplete: Boolean
        get() = state == "completed"
}

sealed interface JourneyUiState {
    data object Loading : JourneyUiState
    data class Ready(val journey: LearningJourneyUiModel) : JourneyUiState
    data class Unavailable(val message: String) : JourneyUiState
}

data class LearningFocusMetricUiModel(
    val value: Int,
    val label: String
)

data class LearningFocusOptionUiModel(
    val id: String,
    val title: String,
    val tagline: String,
    val metrics: List<LearningFocusMetricUiModel>,
    val totalRewardXp: Int,
    val tone: LearningFocusTone
)

sealed interface LearningFocusUiState {
    data object Loading : LearningFocusUiState
    data class Locked(
        val eyebrow: String,
        val title: String,
        val message: String,
        val progress: Int,
        val target: Int
    ) : LearningFocusUiState
    data class Choose(
        val eyebrow: String,
        val title: String,
        val description: String,
        val options: List<LearningFocusOptionUiModel>,
        val savingOptionId: String?,
        val errorMessage: String?
    ) : LearningFocusUiState
    data class Ready(
        val focusTitle: String,
        val focusTagline: String,
        val tone: LearningFocusTone,
        val milestonePlan: LearningJourneyUiModel
    ) : LearningFocusUiState
    data class Unavailable(val message: String) : LearningFocusUiState
}

internal data class LearningFocusSelectionActionState(
    val savingOptionId: String? = null,
    val errorMessage: String? = null
)

data class LearningActivityFeedItem(
    val id: String,
    val title: String,
    val detail: String,
    val xpAwarded: Long,
    val occurredAtEpochMillis: Long
)

data class ProgressErrorEvent(
    val message: String,
    val retryable: Boolean
)

/**
 * Learner progress read model. XP and activity are derived from immutable Room events;
 * DataStore keeps presentation preferences and a one-time legacy balance.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val store = ProgressStore.getInstance(application)
    private val repository by lazy { LexiconRepository.getInstance(application) }
    private val gamification by lazy { GamificationRepository.getInstance(application) }
    private val screenSharing = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000)
    private val currentStudyDay: StateFlow<Long> = ProgressStore.observeLocalEpochDay()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProgressStore.localEpochDay())
    private val _progressErrorEvents = MutableSharedFlow<ProgressErrorEvent>(extraBufferCapacity = 1)
    val progressErrorEvents = _progressErrorEvents.asSharedFlow()
    private val learningFocusSelectionAction = MutableStateFlow(LearningFocusSelectionActionState())
    private var failedPracticeCommand: LearningEventCommand? = null

    private val snapshot: StateFlow<ProgressSnapshot> = store.snapshot
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProgressSnapshot())

    // ── Persisted learning core ──────────────────────────────────────────────
    val points: StateFlow<Int> by lazy {
        gamification.xpBalance.map { it.toUiXp() }
            .stateIn(viewModelScope, screenSharing, 0)
    }

    val streak: StateFlow<Int> by lazy {
        combine(gamification.recentStudyDays, currentStudyDay) { studyDays, studyDay ->
            consecutiveStudyDayCount(studyDays, studyDay)
        }
            .stateIn(viewModelScope, screenSharing, 0)
    }

    val streakAliveToday: StateFlow<Boolean> by lazy {
        combine(gamification.recentStudyDays, currentStudyDay) { studyDays, studyDay ->
            studyDay in studyDays
        }
            .stateIn(viewModelScope, screenSharing, false)
    }

    val xpToday: StateFlow<Int> by lazy {
        currentStudyDay.flatMapLatest { studyDay -> gamification.xpForStudyDay(studyDay) }
            .map { it.toUiXp() }
            .stateIn(viewModelScope, screenSharing, 0)
    }

    val dailyGoalXp: StateFlow<Int> = snapshot.map { it.dailyGoalXp }
        .stateIn(viewModelScope, screenSharing, 80)

    val reviewedToday: StateFlow<Int> by lazy {
        currentStudyDay.flatMapLatest { studyDay ->
            gamification.completedReviewCountForStudyDay(studyDay)
        }
            .stateIn(viewModelScope, screenSharing, 0)
    }

    val weeklyXpDistribution: StateFlow<List<Int>> by lazy {
        currentStudyDay.flatMapLatest { studyDay ->
            gamification.xpTotalsSinceStudyDay(studyDay - 6)
            .map { totals ->
                val byDay = totals.associate { it.studyDay to it.amount }
                (6 downTo 0).map { offset -> (byDay[studyDay - offset] ?: 0L).toUiXp() }
            }
        }
            .stateIn(viewModelScope, screenSharing, List(7) { 0 })
    }

    val selectedTheme: StateFlow<String> = snapshot.map { it.theme }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CinnamonThemes.TOASTED)

    val soundEffectsEnabled: StateFlow<Boolean> = snapshot.map { it.soundEffectsEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val hapticFeedbackEnabled: StateFlow<Boolean> = snapshot.map { it.hapticFeedbackEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val reduceMotion: StateFlow<Boolean> = snapshot.map { it.reduceMotion }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Levels follow the validated catalog and delayed mastery—not a spendable XP balance. */
    private val catalogProgressionLevels: StateFlow<List<CatalogProgressionLevelProjection>> by lazy {
        combine(
            gamification.masteredConceptCount,
            AppStartupCoordinator.catalogBundle
        ) { masteredConcepts, bundle ->
            if (bundle == null) {
                emptyList()
            } else {
                CatalogProgressProjector.projectProgressionLevels(
                    bundle = bundle,
                    evidence = CatalogEvidenceSnapshot(masteredItemsAfterDelay = masteredConcepts)
                )
            }
        }.stateIn(viewModelScope, screenSharing, emptyList())
    }

    val progressionLevel: StateFlow<Int> by lazy {
        catalogProgressionLevels
            .map { levels -> levels.lastOrNull { it.thresholdReached }?.order ?: 1 }
            .stateIn(viewModelScope, screenSharing, 1)
    }

    val rank: StateFlow<String> by lazy {
        catalogProgressionLevels
            .map { levels -> levels.lastOrNull { it.thresholdReached }?.title.orEmpty() }
            .stateIn(viewModelScope, screenSharing, "")
    }

    // ── Lexicon-derived stats ───────────────────────────────────────────────
    val totalWords: StateFlow<Int> by lazy {
        repository.lexicon.totalCount()
            .stateIn(viewModelScope, screenSharing, 0)
    }

    val masteredWords: StateFlow<Int> by lazy {
        repository.lexicon.masteredCount()
            .stateIn(viewModelScope, screenSharing, 0)
    }

    val dueNow: StateFlow<Int> by lazy {
        repository.lexicon.dueCount(System.currentTimeMillis())
            .stateIn(viewModelScope, screenSharing, 0)
    }

    fun updateTheme(themeName: String) {
        viewModelScope.launch { store.setTheme(themeName) }
    }

    fun setDailyGoal(xp: Int) {
        viewModelScope.launch { store.setDailyGoal(xp) }
    }

    fun setSoundEffectsEnabled(enabled: Boolean) {
        viewModelScope.launch { store.setSoundEffectsEnabled(enabled) }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch { store.setHapticFeedbackEnabled(enabled) }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { store.setReduceMotion(enabled) }
    }

    fun retryStartup() {
        viewModelScope.launch {
            AppStartupCoordinator.prepare(getApplication())
        }
    }

    /**
     * Records one completed, verifiable practice session. UI must supply a
     * stable [occurrenceKey] for the logical session; tapping a button is not
     * itself a reward authority.
     */
    fun recordPracticeSession(
        subjectType: String,
        subjectId: String,
        occurrenceKey: String,
        completedItemCount: Int
    ) {
        // A finished screen alone is not learning evidence. Every current game
        // must contribute at least three completed items before it can write a
        // completed-practice event, affect a quest, or request XP.
        if (completedItemCount < MIN_MEANINGFUL_PRACTICE_ITEMS) return
        persistPracticeSession(
            LearningEventCommand(
                eventType = RewardableEventType.PRACTICE_SESSION_COMPLETED,
                subjectType = subjectType,
                subjectId = subjectId,
                occurrenceKey = occurrenceKey,
                completedItemCount = completedItemCount,
                source = LearningEventSource.GAME
            )
        )
    }

    /** The game may report a correct first-choice, but the repository owns delayed validation. */
    fun recordConfusablePairSuccess(confusablePairId: String, occurrenceKey: String) {
        viewModelScope.launch {
            runCatching {
                gamification.recordConfusablePairSuccess(
                    confusablePairId = confusablePairId,
                    occurrenceKey = occurrenceKey,
                    occurredAtEpochMillis = System.currentTimeMillis()
                )
            }
        }
    }

    /** The answer surface supplies only its verified entry ID; ledger authority remains private. */
    fun recordVerifiedContextApplication(lexiconEntryId: Long, occurrenceKey: String) {
        viewModelScope.launch {
            runCatching {
                gamification.recordVerifiedContextApplication(
                    lexiconEntryId = lexiconEntryId,
                    occurrenceKey = occurrenceKey
                )
            }
        }
    }

    fun retryLastPracticeSession() {
        failedPracticeCommand?.let(::persistPracticeSession)
    }

    private fun persistPracticeSession(command: LearningEventCommand) {
        viewModelScope.launch {
            runCatching {
                gamification.recordPracticeSessionWithComeback(command)
            }.onSuccess {
                if (failedPracticeCommand == command) failedPracticeCommand = null
            }.onFailure {
                failedPracticeCommand = command
                _progressErrorEvents.emit(
                    ProgressErrorEvent(
                        message = "This completed practice session could not be saved. Retry uses the same session and cannot award XP twice.",
                        retryable = true
                    )
                )
            }
        }
    }

    private val rhythmWeeks: StateFlow<Int> by lazy {
        gamification.recentStudyDays
            .map(::weeksWithAtLeastThreeStudyDays)
            .stateIn(viewModelScope, screenSharing, 0)
    }

    /**
     * Visible daily checkpoints are read-only projections of persisted actions.
     * They never change because a checkbox was tapped. Completed unclaimed assignments remain
     * visible even after their evidence window closes.
     */
    val dailyQuests: StateFlow<List<Quest>> by lazy {
        currentStudyDay.flatMapLatest { studyDay ->
            combine(
                gamification.distinctReviewedSubjectCountForStudyDay(studyDay),
                dueNow,
                AppStartupCoordinator.catalogBundle,
                gamification.activeQuestInstances,
                gamification.resolvedConfusablePairCount
            ) { distinctReviewCount, dueItemCount, bundle, instances, resolvedPairs ->
                DailyQuestInputs(
                    distinctReviewCount = distinctReviewCount,
                    dueItemCount = dueItemCount,
                    bundle = bundle,
                    instances = instances,
                    resolvedPairs = resolvedPairs
                )
            }.combine(gamification.verifiedContextApplicationCount) { inputs, contextApplications ->
                val (distinctReviewCount, dueItemCount, bundle, instances, resolvedPairs) = inputs
                if (bundle == null) return@combine emptyList()
                val now = System.currentTimeMillis()
                val currentInstances = instances.asSequence()
                    .filter { instance -> instance.cadence == "daily" }
                    .mapNotNull { instance ->
                        val criteria = QuestAssignmentContracts.decodeCriteria(instance.criteriaJson)
                            ?: return@mapNotNull null
                        instance.takeIf {
                            now >= criteria.evidenceStartsAtEpochMillis &&
                                now < criteria.evidenceEndsAtEpochMillis
                        }
                    }
                    .groupBy { instance -> instance.definitionId }
                    .mapValues { (_, matching) -> matching.maxBy { instance -> instance.startsAtEpochMillis } }
                val current = CatalogProgressProjector.projectEligibleDailyQuests(
                    bundle = bundle,
                    evidence = CatalogEvidenceSnapshot(
                        distinctDueReviewsToday = distinctReviewCount,
                        dueItemCountNow = dueItemCount,
                        resolvedConfusablePairs = resolvedPairs,
                        verifiedContextApplications = contextApplications
                    )
                ).map { quest ->
                    val persisted = currentInstances[quest.id]
                    val definition = bundle.catalog.quests.first { it.id == quest.id }
                    val rewardsById = bundle.catalog.rewards.associateBy { it.id }
                    val persistedClaim = persisted?.let(QuestAssignmentContracts::resolveClaim)
                    val rewardXp = persistedClaim?.xpAmount
                            ?.coerceAtMost(Int.MAX_VALUE.toLong())
                            ?.toInt()
                        ?: definition.rewardRefs.sumOf { rewardId ->
                        rewardsById[rewardId]
                            ?.takeIf { reward -> reward.type == RewardType.XP }
                            ?.amount
                            ?: 0
                    }
                    Quest(
                        id = quest.id,
                        title = quest.title,
                        progress = (persisted?.progress?.toInt() ?: quest.progress)
                            .coerceIn(0, quest.target),
                        target = quest.target,
                        completed = persisted?.state in setOf("completed", "claimed") ||
                            quest.thresholdReached,
                        detail = quest.reason,
                        instanceId = persisted?.questInstanceId,
                        claimable = persisted?.state == "completed" && persistedClaim != null,
                        claimed = persisted?.state == "claimed",
                        rewardXp = rewardXp
                    )
                }
                val currentInstanceIds = current.mapNotNull { quest -> quest.instanceId }.toSet()
                val currentWindowInstanceIds = currentInstances.values
                    .map { instance -> instance.questInstanceId }
                    .toSet()
                val outstandingOrCurrent = QuestboardProjector.projectAssignedDailyQuests(
                    bundle = bundle,
                    instances = instances,
                    nowEpochMillis = now
                ).asSequence()
                    .filter { quest ->
                        quest.instanceId !in currentInstanceIds &&
                            (quest.claimable || quest.instanceId in currentWindowInstanceIds)
                    }
                    .sortedBy { quest -> if (quest.instanceId in currentWindowInstanceIds) 0 else 1 }
                    .map { quest ->
                        Quest(
                            id = quest.instanceId,
                            title = quest.title,
                            progress = quest.progress,
                            target = quest.target,
                            completed = quest.completed,
                            detail = quest.reason,
                            instanceId = quest.instanceId,
                            claimable = quest.claimable,
                            claimed = quest.claimed,
                            rewardXp = quest.rewardXp
                        )
                    }
                    .toList()
                current + outstandingOrCurrent
            }
        }.stateIn(viewModelScope, screenSharing, emptyList())
    }

    /** Assigned weekly quests use persisted windows and rewards; UI projection has no grant authority. */
    val weeklyQuests: StateFlow<List<Quest>> by lazy {
        combine(
            AppStartupCoordinator.catalogBundle,
            gamification.activeQuestInstances
        ) { bundle, instances ->
            if (bundle == null) return@combine emptyList()
            QuestboardProjector.projectAssignedWeeklyQuests(
                bundle = bundle,
                instances = instances,
                nowEpochMillis = System.currentTimeMillis()
            ).map { quest ->
                Quest(
                    id = quest.definitionId,
                    title = quest.title,
                    progress = quest.progress,
                    target = quest.target,
                    completed = quest.completed,
                    detail = quest.reason,
                    instanceId = quest.instanceId,
                    claimable = quest.claimable,
                    claimed = quest.claimed,
                    rewardXp = quest.rewardXp
                )
            }
        }.stateIn(viewModelScope, screenSharing, emptyList())
    }

    private val catalogAchievementEvidence: StateFlow<CatalogEvidenceSnapshot> by lazy {
        combine(
            rhythmWeeks,
            gamification.distinctPracticeContentTypeCount,
            gamification.masteredConceptCount,
            gamification.repairedSubjectCount,
            gamification.resolvedConfusablePairCount
        ) { completedRhythmWeeks, contentKinds, masteredConcepts, repairedItems, resolvedPairs ->
            CatalogEvidenceSnapshot(
                masteredItemsAfterDelay = masteredConcepts,
                rhythmWeeks = completedRhythmWeeks,
                distinctPracticeContentKinds = contentKinds,
                repairedItems = repairedItems,
                resolvedConfusablePairs = resolvedPairs
            )
        }.combine(gamification.verifiedContextApplicationCount) { evidence, contextApplications ->
            evidence.copy(verifiedContextApplications = contextApplications)
        }.combine(gamification.delayedRecallCount) { evidence, delayedRecalls ->
            evidence.copy(verifiedDelayedRecalls = delayedRecalls)
        }.combine(gamification.verifiedComebackSessionCount) { evidence, comebackSessions ->
            evidence.copy(verifiedComebackSessions = comebackSessions)
        }.combine(gamification.verifiedSavedItemCount) { evidence, savedItems ->
            evidence.copy(verifiedSavedItems = savedItems)
        }.combine(gamification.verifiedReviewQueueClearDayCount) { evidence, clearDays ->
            evidence.copy(verifiedReviewQueueClearDays = clearDays)
        }.stateIn(viewModelScope, screenSharing, CatalogEvidenceSnapshot())
    }

    /** Catalog labels with evidence-backed progress; no unsupported criterion is unlocked. */
    val achievements: StateFlow<List<Achievement>> by lazy {
        combine(
            catalogAchievementEvidence,
            AppStartupCoordinator.catalogBundle,
            gamification.achievementUnlocks
        ) { evidence, bundle, unlocks ->
            if (bundle == null) return@combine emptyList()
            CatalogProgressProjector.projectAchievements(
                bundle = bundle,
                evidence = evidence
            ).map { achievement ->
                Achievement(
                    id = achievement.id,
                    title = achievement.title,
                    description = achievement.description,
                    icon = achievementIcon(achievement.family),
                    progress = achievement.progress,
                    target = achievement.target,
                    reached = unlocks.any { unlock ->
                        unlock.achievementId == achievement.id && unlock.level >= 1
                    }
                )
            }
        }.stateIn(viewModelScope, screenSharing, emptyList())
    }

    /**
     * Room-backed Foundation progress. Missing persisted rows become a visible recovery state;
     * the projection never manufactures optimistic milestone progress.
     */
    val learningJourney: StateFlow<JourneyUiState> by lazy {
        combine(
            gamification.journeyInstances,
            gamification.journeyStages,
            AppStartupCoordinator.state
        ) { instances, stages, startupState ->
            when {
                startupState != AppStartupState.Ready -> JourneyUiState.Loading
                else -> projectFoundationJourney(instances, stages)
            }
        }.stateIn(viewModelScope, screenSharing, JourneyUiState.Loading)
    }

    /** One immutable Learning Focus plus the selected milestone plan's persisted projection. */
    val learningFocus: StateFlow<LearningFocusUiState> by lazy {
        combine(
            gamification.learningFocusSelections,
            gamification.journeyInstances,
            gamification.journeyStages,
            AppStartupCoordinator.state,
            learningFocusSelectionAction
        ) { selections, instances, stages, startupState, actionState ->
            when {
                startupState != AppStartupState.Ready -> LearningFocusUiState.Loading
                else -> projectLearningFocus(
                    definition = FoundationLearningFocusCatalog.definition,
                    selections = selections,
                    instances = instances,
                    stageRows = stages,
                    actionState = actionState
                )
            }
        }.stateIn(viewModelScope, screenSharing, LearningFocusUiState.Loading)
    }

    val recentLearningActivity: StateFlow<List<LearningActivityFeedItem>> by lazy {
        gamification.recentLearningActivity
            .map { activity -> activity.map(::activityFeedItem) }
            .stateIn(viewModelScope, screenSharing, emptyList())
    }

    val pendingRewardPresentation: StateFlow<PendingRewardPresentation?> by lazy {
        gamification.pendingRewardPresentations
            .map { receipts -> receipts.firstOrNull() }
            .stateIn(viewModelScope, screenSharing, null)
    }

    fun acknowledgeRewardPresentation(receiptId: String) {
        viewModelScope.launch { gamification.acknowledgeRewardPresentation(receiptId) }
    }

    fun claimQuestReward(questInstanceId: String) {
        if (questInstanceId.isBlank()) return
        viewModelScope.launch {
            runCatching { gamification.claimQuest(questInstanceId) }
                .onFailure {
                    _progressErrorEvents.emit(
                        ProgressErrorEvent(
                            message = "Quest progress is safe. The reward was not claimed; try again.",
                            retryable = true
                        )
                    )
                }
        }
    }

    fun selectLearningFocus(optionId: String) {
        val definition = FoundationLearningFocusCatalog.definition
        if (
            definition.option(optionId) == null ||
            learningFocusSelectionAction.value.savingOptionId != null
        ) return
        learningFocusSelectionAction.value =
            LearningFocusSelectionActionState(savingOptionId = optionId)
        viewModelScope.launch {
            runCatching { gamification.selectLearningFocus(optionId) }
                .onSuccess {
                    learningFocusSelectionAction.value = LearningFocusSelectionActionState()
                }
                .onFailure { error ->
                    learningFocusSelectionAction.value = LearningFocusSelectionActionState(
                        errorMessage = when {
                            error is LearningFocusAlreadySelectedException ->
                                "A different Learning Focus is already saved. No new selection was written."
                            error is LearningFocusRecordIncompatibleException ->
                                "Your saved Learning Focus does not match this app version. Cinnamon did not reset your progress or XP."
                            error.message?.contains("prerequisite", ignoreCase = true) == true ->
                                "Complete Foundation milestone 1 before choosing a Learning Focus."
                            else ->
                                "We couldn’t save this Learning Focus. Nothing changed; try again."
                        }
                    )
                }
        }
    }

}

private const val MIN_MEANINGFUL_PRACTICE_ITEMS = 3

private fun achievementIcon(family: AchievementFamily): String = when (family) {
    AchievementFamily.MASTERY -> "◈"
    AchievementFamily.CONSISTENCY -> "✦"
    AchievementFamily.CORRECTION -> "↻"
    AchievementFamily.EXPLORATION -> "⌁"
    AchievementFamily.CHALLENGE -> "◆"
    AchievementFamily.COLLECTION -> "▣"
    AchievementFamily.COMEBACK -> "↗"
    AchievementFamily.REFLECTION -> "◎"
    AchievementFamily.APPLICATION -> "◇"
}

private fun Long.toUiXp(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

private fun projectFoundationJourney(
    instances: List<JourneyInstanceEntity>,
    stageRows: List<JourneyStageProgressEntity>
): JourneyUiState {
    val definition = FoundationJourneyCatalog.definition
    val matches = instances.filter { instance ->
        LegacyFoundationJourneyWireCompatibility.matchesDefinitionIdentity(
            definition = definition,
            storedDefinitionId = instance.definitionId,
            storedDefinitionVersion = instance.definitionVersion
        )
    }
    if (matches.size > 1) {
        return JourneyUiState.Unavailable(
            "We found more than one saved Foundation plan. Cinnamon did not reset your progress or XP."
        )
    }
    val persistedDefinition = matches.singleOrNull()?.let { instance ->
        LegacyFoundationJourneyWireCompatibility.definitionForStoredIdentity(
            definition = definition,
            storedDefinitionId = instance.definitionId,
            storedDefinitionVersion = instance.definitionVersion
        )
    } ?: definition
    return projectJourney(
        definition = persistedDefinition,
        instances = instances,
        stageRows = stageRows
    )
}

private fun projectJourney(
    definition: JourneyDefinition,
    instances: List<JourneyInstanceEntity>,
    stageRows: List<JourneyStageProgressEntity>
): JourneyUiState {
    val instance = instances.firstOrNull { candidate ->
        candidate.definitionId == definition.id &&
            candidate.definitionVersion == definition.version
    } ?: return JourneyUiState.Unavailable(
        "We could not restore ${definition.title} on this device. Retry setup before continuing."
    )
    val persistedStages = stageRows
        .filter { row -> row.journeyInstanceId == instance.journeyInstanceId }
        .associateBy(JourneyStageProgressEntity::stageDefinitionId)
    if (persistedStages.keys != definition.stages.mapTo(linkedSetOf()) { it.id }) {
        return JourneyUiState.Unavailable(
            "Some saved milestones for ${definition.title} are missing. Try setup again before continuing."
        )
    }

    val stages = definition.stages.map { stage ->
        val row = checkNotNull(persistedStages[stage.id])
        val state = when (row.state) {
            "completed" -> JourneyStageUiState.COMPLETED
            "active" -> JourneyStageUiState.ACTIVE
            else -> JourneyStageUiState.LOCKED
        }
        JourneyStageUiModel(
            id = stage.id,
            order = stage.order,
            title = stage.title,
            description = stage.description,
            state = state,
            progress = row.progress.coerceIn(0L, row.target).toUiXp(),
            target = row.target.toUiXp(),
            rewardXp = row.rewardXp.toUiXp(),
            destination = stage.destination,
            actionLabel = stage.actionLabel
        )
    }
    val completedStages = stages.filter { it.state == JourneyStageUiState.COMPLETED }
    return JourneyUiState.Ready(
        LearningJourneyUiModel(
            id = instance.journeyInstanceId,
            eyebrow = definition.eyebrow,
            title = definition.title,
            description = definition.description,
            state = instance.state,
            completedStageCount = completedStages.size,
            totalStageCount = stages.size,
            earnedXp = completedStages.sumOf(JourneyStageUiModel::rewardXp),
            completionTitle = definition.completionTitle,
            completionDescription = definition.completionDescription,
            stages = stages
        )
    )
}

internal fun projectLearningFocus(
    definition: LearningFocusDefinition,
    selections: List<LearningFocusSelectionRecord>,
    instances: List<JourneyInstanceEntity>,
    stageRows: List<JourneyStageProgressEntity>,
    actionState: LearningFocusSelectionActionState
): LearningFocusUiState {
    val canonicalFoundation = FoundationJourneyCatalog.definition
    val foundationInstances = instances.filter { instance ->
        LegacyFoundationJourneyWireCompatibility.matchesDefinitionIdentity(
            definition = canonicalFoundation,
            storedDefinitionId = instance.definitionId,
            storedDefinitionVersion = instance.definitionVersion
        ) && definition.prerequisiteDefinitionId == canonicalFoundation.id &&
            definition.prerequisiteDefinitionVersion == canonicalFoundation.version
    }
    if (foundationInstances.size > 1) {
        return LearningFocusUiState.Unavailable(
            "We found more than one saved Foundation plan. Cinnamon did not reset your progress or XP."
        )
    }
    val foundationInstance = foundationInstances.singleOrNull()
        ?: return LearningFocusUiState.Unavailable(
        "We couldn’t find the saved Foundation plan. Try setup again before choosing a focus."
    )
    val prerequisite = stageRows.firstOrNull { row ->
        row.journeyInstanceId == foundationInstance.journeyInstanceId &&
            row.stageDefinitionId == definition.prerequisiteMilestoneDefinitionId
    } ?: return LearningFocusUiState.Unavailable(
        "We couldn’t find Foundation milestone 1. Cinnamon did not reset your recorded progress or XP."
    )
    if (prerequisite.state != "completed") {
        return LearningFocusUiState.Locked(
            eyebrow = definition.eyebrow,
            title = "Unlock your Learning Focus",
            message = "Review three different terms to unlock two focus options.",
            progress = prerequisite.progress.coerceAtLeast(0L).toUiXp(),
            target = prerequisite.target.coerceAtLeast(1L).toUiXp()
        )
    }

    if (selections.size > 1) {
        return LearningFocusUiState.Unavailable(
            "We found more than one saved Learning Focus. Cinnamon did not reset your progress or XP."
        )
    }
    val selection = selections.singleOrNull()
    if (selection == null) {
        return LearningFocusUiState.Choose(
            eyebrow = definition.eyebrow,
            title = definition.title,
            description = definition.description,
            options = definition.options.map { option ->
                LearningFocusOptionUiModel(
                    id = option.id,
                    title = option.title,
                    tagline = option.tagline,
                    metrics = option.milestonePlan.stages.map { milestone ->
                        LearningFocusMetricUiModel(
                            value = milestone.target.toUiXp(),
                            label = when (milestone.evidenceMetric) {
                                JourneyEvidenceMetric.DISTINCT_REVIEWED_ITEMS -> "reviewed items"
                                JourneyEvidenceMetric.DISTINCT_PRACTICE_CONTENT_KINDS -> "practice formats"
                                JourneyEvidenceMetric.MASTERED_ITEMS_AFTER_DELAY -> "delayed-recall items"
                                JourneyEvidenceMetric.ACTIVE_STUDY_DAYS -> "active days"
                            }
                        )
                    },
                    totalRewardXp = option.totalRewardXp.toUiXp(),
                    tone = option.tone
                )
            },
            savingOptionId = actionState.savingOptionId,
            errorMessage = actionState.errorMessage
        )
    }

    if (selection is LearningFocusSelectionRecord.Incompatible) {
        return LearningFocusUiState.Unavailable(
            "Your saved Learning Focus does not match this app version. Cinnamon did not reset your progress or XP."
        )
    }
    selection as LearningFocusSelectionRecord.Compatible
    val option = definition.option(selection.optionId) ?: return LearningFocusUiState.Unavailable(
        "Your saved Learning Focus option is unavailable in this app version. Cinnamon did not reset your progress or XP."
    )
    if (selection.milestonePlanDefinitionId != option.milestonePlan.id) {
        return LearningFocusUiState.Unavailable(
            "Your saved Learning Focus and milestone plan do not match. Cinnamon did not reset your progress or XP."
        )
    }
    val persistedMilestonePlan = LegacyLearningFocusWireCompatibility.milestonePlanForSelection(
        option = option,
        persistedMilestonePlanDefinitionId = selection.persistedMilestonePlanDefinitionId
    )
    return when (val planState = projectJourney(persistedMilestonePlan, instances, stageRows)) {
        JourneyUiState.Loading -> LearningFocusUiState.Loading
        is JourneyUiState.Unavailable -> LearningFocusUiState.Unavailable(planState.message)
        is JourneyUiState.Ready -> LearningFocusUiState.Ready(
            focusTitle = option.title,
            focusTagline = option.tagline,
            tone = option.tone,
            milestonePlan = planState.journey
        )
    }
}

private fun consecutiveStudyDayCount(studyDays: List<Long>, today: Long): Int {
    val daySet = studyDays.toSet()
    var cursor = when {
        today in daySet -> today
        today - 1L in daySet -> today - 1L
        else -> return 0
    }
    var count = 0
    while (cursor in daySet) {
        count += 1
        cursor -= 1L
    }
    return count
}

internal fun weeksWithAtLeastThreeStudyDays(studyDays: List<Long>): Int = studyDays
    .distinct()
    .groupBy(::isoWeekKey)
    .count { (_, days) -> days.size >= 3 }

/** Groups local epoch-day ordinals by ISO week without applying a second timezone offset. */
private fun isoWeekKey(epochDay: Long): String {
    val calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT).apply {
        firstDayOfWeek = Calendar.MONDAY
        minimalDaysInFirstWeek = 4
        timeInMillis = Math.multiplyExact(epochDay, 86_400_000L)
    }
    return "${calendar.weekYear}-${calendar.get(Calendar.WEEK_OF_YEAR)}"
}

private fun activityFeedItem(activity: LearningActivity): LearningActivityFeedItem {
    val subjectLabel = when (activity.subjectType) {
        "vocabulary_match" -> "Vocabulary Match"
        "sentence_unscramble" -> "Sentence Unscramble"
        "cloze_clinic" -> "Cloze Clinic"
        "flashcard_match" -> "Flashcard Match"
        "scenario_language_sprint" -> "Scenario language sprint"
        "authored_transcript_escape" -> "Transcript escape"
        "lexicon_entry" -> "Lexicon review"
        else -> activity.subjectType.replace('_', ' ').replaceFirstChar(Char::uppercase)
    }
    val title = when (activity.eventType) {
        RewardableEventType.REVIEW_COMPLETED.wireName -> "Review completed"
        RewardableEventType.PRACTICE_SESSION_COMPLETED.wireName -> "Practice session completed"
        RewardableEventType.MISTAKE_CORRECTED.wireName -> "Mistake repaired"
        RewardableEventType.CONFUSABLE_PAIR_RESOLVED.wireName -> "Confusable pair retained"
        RewardableEventType.CONTEXT_APPLICATION_VERIFIED.wireName -> "Context use verified"
        RewardableEventType.DELAYED_RECALL_SUCCEEDED.wireName -> "Delayed recall retained"
        RewardableEventType.COMEBACK_SESSION_COMPLETED.wireName -> "Welcome back - momentum restored"
        RewardableEventType.SAVED_ITEM_REVIEWED.wireName -> "Saved item revisited"
        else -> "Learning activity recorded"
    }
    val rewardDetail = if (activity.xpAwarded > 0L) {
        "${activity.xpAwarded} XP earned"
    } else {
        "Completed without an XP reward"
    }
    return LearningActivityFeedItem(
        id = activity.eventId,
        title = title,
        detail = "$subjectLabel · $rewardDetail",
        xpAwarded = activity.xpAwarded,
        occurredAtEpochMillis = activity.occurredAtEpochMillis
    )
}
