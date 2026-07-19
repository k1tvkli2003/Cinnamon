package com.cinnamon.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cinnamon.app.data.gamification.GamificationRepository
import com.cinnamon.app.data.gamification.LearningActivity
import com.cinnamon.app.data.gamification.LearningEventCommand
import com.cinnamon.app.data.gamification.LearningEventSource
import com.cinnamon.app.data.gamification.PendingRewardPresentation
import com.cinnamon.app.data.gamification.RewardableEventType
import com.cinnamon.app.data.prefs.ProgressSnapshot
import com.cinnamon.app.data.prefs.ProgressStore
import com.cinnamon.app.data.startup.AppStartupCoordinator
import com.cinnamon.app.data.startup.AppStartupState
import com.cinnamon.app.data.local.CampaignRouteChoiceEntity
import com.cinnamon.app.data.local.JourneyInstanceEntity
import com.cinnamon.app.data.local.JourneyStageProgressEntity
import com.cinnamon.app.domain.gamification.AchievementFamily
import com.cinnamon.app.domain.gamification.CatalogEvidenceSnapshot
import com.cinnamon.app.domain.gamification.CatalogProgressProjector
import com.cinnamon.app.domain.gamification.CatalogProgressionLevelProjection
import com.cinnamon.app.domain.gamification.CampaignDefinition
import com.cinnamon.app.domain.gamification.CampaignRouteTone
import com.cinnamon.app.domain.gamification.FoundationCampaignCatalog
import com.cinnamon.app.domain.gamification.FoundationJourneyCatalog
import com.cinnamon.app.domain.gamification.JourneyDefinition
import com.cinnamon.app.domain.gamification.JourneyDestination
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

data class LearningRouteNode(
    val id: String,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val reached: Boolean,
    val evidenceAvailable: Boolean = true
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
    val securedXp: Int,
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

data class CampaignRouteChoiceUiModel(
    val id: String,
    val title: String,
    val tagline: String,
    val commitmentCopy: String,
    val totalRewardXp: Int,
    val tone: CampaignRouteTone
)

sealed interface CampaignUiState {
    data object Loading : CampaignUiState
    data class Locked(
        val eyebrow: String,
        val title: String,
        val message: String
    ) : CampaignUiState
    data class Choose(
        val eyebrow: String,
        val title: String,
        val description: String,
        val routes: List<CampaignRouteChoiceUiModel>,
        val savingRouteId: String?,
        val errorMessage: String?
    ) : CampaignUiState
    data class Ready(
        val campaignTitle: String,
        val routeTitle: String,
        val routeTagline: String,
        val tone: CampaignRouteTone,
        val journey: LearningJourneyUiModel
    ) : CampaignUiState
    data class Unavailable(val message: String) : CampaignUiState
}

internal data class CampaignChoiceActionState(
    val savingRouteId: String? = null,
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
 * Learner progress read model. XP and activity are derived from the immutable Room
 * ledger; DataStore keeps presentation preferences and a one-time legacy balance.
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
    private val campaignChoiceAction = MutableStateFlow(CampaignChoiceActionState())
    private var failedPracticeCommand: LearningEventCommand? = null

    private val snapshot: StateFlow<ProgressSnapshot> = store.snapshot
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProgressSnapshot())

    // ── Ledger-derived learning core ─────────────────────────────────────────
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

    val selectedFocus: StateFlow<String> = snapshot.map { it.learningFocus }
        .stateIn(viewModelScope, screenSharing, "None")

    fun selectLearningFocus(focus: String) {
        viewModelScope.launch { store.setLearningFocus(focus) }
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
        // must contribute at least three verified items before it can write a
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

    fun retryLastPracticeSession() {
        failedPracticeCommand?.let(::persistPracticeSession)
    }

    private fun persistPracticeSession(command: LearningEventCommand) {
        viewModelScope.launch {
            runCatching {
                gamification.record(command)
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
     * They never change because a checkbox was tapped and they carry no manual reward.
     */
    val dailyQuests: StateFlow<List<Quest>> by lazy {
        currentStudyDay.flatMapLatest { studyDay ->
            combine(
                gamification.distinctReviewedSubjectCountForStudyDay(studyDay),
                dueNow,
                AppStartupCoordinator.catalogBundle,
                gamification.activeQuestInstances
            ) { distinctReviewCount, dueItemCount, bundle, instances ->
                if (bundle == null) return@combine emptyList()
                val currentInstances = instances.filter { instance ->
                    ProgressStore.localEpochDay(instance.startsAtEpochMillis) == studyDay
                }.associateBy { it.definitionId }
                CatalogProgressProjector.projectEligibleDailyQuests(
                    bundle = bundle,
                    evidence = CatalogEvidenceSnapshot(
                        distinctDueReviewsToday = distinctReviewCount,
                        dueItemCountNow = dueItemCount
                    )
                ).map { quest ->
                    val persisted = currentInstances[quest.id]
                    val definition = bundle.catalog.quests.first { it.id == quest.id }
                    val rewardsById = bundle.catalog.rewards.associateBy { it.id }
                    val rewardXp = definition.rewardRefs.sumOf { rewardId ->
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
                        claimable = persisted?.state == "completed",
                        claimed = persisted?.state == "claimed",
                        rewardXp = rewardXp
                    )
                }
            }
        }.stateIn(viewModelScope, screenSharing, emptyList())
    }

    /** Catalog labels with evidence-backed progress; no unsupported criterion is unlocked. */
    val achievements: StateFlow<List<Achievement>> by lazy {
        combine(
            rhythmWeeks,
            gamification.distinctPracticeContentTypeCount,
            gamification.masteredConceptCount,
            AppStartupCoordinator.catalogBundle,
            gamification.achievementUnlocks
        ) { completedRhythmWeeks, contentKinds, masteredConcepts, bundle, unlocks ->
            if (bundle == null) return@combine emptyList()
            CatalogProgressProjector.projectAchievements(
                bundle = bundle,
                evidence = CatalogEvidenceSnapshot(
                    masteredItemsAfterDelay = masteredConcepts,
                    rhythmWeeks = completedRhythmWeeks,
                    distinctPracticeContentKinds = contentKinds
                )
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

    val learningRoute: StateFlow<List<LearningRouteNode>> by lazy {
        catalogProgressionLevels.map { levels ->
            levels.map { level ->
                LearningRouteNode(
                    id = level.id,
                    title = level.title,
                    description = level.description,
                    progress = level.progress,
                    target = level.target,
                    reached = level.thresholdReached
                )
            }
        }.stateIn(viewModelScope, screenSharing, emptyList())
    }

    /**
     * A Room-backed multi-session campaign. The projection never manufactures a stage when its
     * persistence row is absent; an inconsistent migration therefore becomes a visible recovery
     * state instead of optimistic progress.
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

    /** One immutable route choice plus the selected route's persisted Journey projection. */
    val campaignRoute: StateFlow<CampaignUiState> by lazy {
        combine(
            gamification.campaignRouteChoices,
            gamification.journeyInstances,
            gamification.journeyStages,
            AppStartupCoordinator.state,
            campaignChoiceAction
        ) { choices, instances, stages, startupState, actionState ->
            when {
                startupState != AppStartupState.Ready -> CampaignUiState.Loading
                else -> projectCampaignRoute(
                    definition = FoundationCampaignCatalog.definition,
                    choices = choices,
                    instances = instances,
                    stageRows = stages,
                    actionState = actionState
                )
            }
        }.stateIn(viewModelScope, screenSharing, CampaignUiState.Loading)
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

    fun chooseCampaignRoute(routeId: String) {
        val definition = FoundationCampaignCatalog.definition
        if (definition.route(routeId) == null || campaignChoiceAction.value.savingRouteId != null) return
        campaignChoiceAction.value = CampaignChoiceActionState(savingRouteId = routeId)
        viewModelScope.launch {
            runCatching { gamification.chooseCampaignRoute(routeId) }
                .onSuccess { campaignChoiceAction.value = CampaignChoiceActionState() }
                .onFailure { error ->
                    campaignChoiceAction.value = CampaignChoiceActionState(
                        errorMessage = when {
                            error.message?.contains("already committed", ignoreCase = true) == true ->
                                "Your saved route is already committed. No progress or XP was changed."
                            error.message?.contains("prerequisite", ignoreCase = true) == true ->
                                "Secure the first Foundation chapter before choosing a route."
                            else -> "Your route was not saved. Your Journey and XP are safe; try again."
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
): JourneyUiState = projectJourney(
    definition = FoundationJourneyCatalog.definition,
    instances = instances,
    stageRows = stageRows
)

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
            "Some saved chapters for ${definition.title} are missing. Retry setup before continuing."
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
            securedXp = completedStages.sumOf(JourneyStageUiModel::rewardXp),
            completionTitle = definition.completionTitle,
            completionDescription = definition.completionDescription,
            stages = stages
        )
    )
}

internal fun projectCampaignRoute(
    definition: CampaignDefinition,
    choices: List<CampaignRouteChoiceEntity>,
    instances: List<JourneyInstanceEntity>,
    stageRows: List<JourneyStageProgressEntity>,
    actionState: CampaignChoiceActionState
): CampaignUiState {
    val foundationInstance = instances.firstOrNull { instance ->
        instance.definitionId == definition.prerequisiteJourneyDefinitionId &&
            instance.definitionVersion == definition.prerequisiteJourneyDefinitionVersion
    } ?: return CampaignUiState.Unavailable(
        "The Foundation Journey record is missing. Retry setup before choosing a route."
    )
    val prerequisite = stageRows.firstOrNull { row ->
        row.journeyInstanceId == foundationInstance.journeyInstanceId &&
            row.stageDefinitionId == definition.prerequisiteStageDefinitionId
    } ?: return CampaignUiState.Unavailable(
        "The chapter that unlocks campaign routes is missing. Retry setup before continuing."
    )
    if (prerequisite.state != "completed") {
        return CampaignUiState.Locked(
            eyebrow = definition.eyebrow,
            title = definition.title,
            message = "Secure the first Foundation chapter to unlock both committed routes."
        )
    }

    val matchingChoices = choices.filter { choice ->
        choice.campaignDefinitionId == definition.id &&
            choice.campaignDefinitionVersion == definition.version
    }
    if (matchingChoices.size > 1) {
        return CampaignUiState.Unavailable(
            "More than one saved route was found. Your Journey and XP are untouched."
        )
    }
    val choice = matchingChoices.singleOrNull()
    if (choice == null) {
        return CampaignUiState.Choose(
            eyebrow = definition.eyebrow,
            title = definition.title,
            description = definition.description,
            routes = definition.routes.map { route ->
                CampaignRouteChoiceUiModel(
                    id = route.id,
                    title = route.title,
                    tagline = route.tagline,
                    commitmentCopy = route.commitmentCopy,
                    totalRewardXp = route.totalRewardXp.toUiXp(),
                    tone = route.tone
                )
            },
            savingRouteId = actionState.savingRouteId,
            errorMessage = actionState.errorMessage
        )
    }

    val route = definition.route(choice.routeId) ?: return CampaignUiState.Unavailable(
        "Your saved route is not present in this campaign version. No progress was changed."
    )
    if (route.journey.id != choice.journeyDefinitionId) {
        return CampaignUiState.Unavailable(
            "Your saved route and chapter record do not match. No progress was changed."
        )
    }
    return when (val journeyState = projectJourney(route.journey, instances, stageRows)) {
        JourneyUiState.Loading -> CampaignUiState.Loading
        is JourneyUiState.Unavailable -> CampaignUiState.Unavailable(journeyState.message)
        is JourneyUiState.Ready -> CampaignUiState.Ready(
            campaignTitle = definition.title,
            routeTitle = route.title,
            routeTagline = route.tagline,
            tone = route.tone,
            journey = journeyState.journey
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
        RewardableEventType.REVIEW_COMPLETED.wireName -> "Review committed"
        RewardableEventType.PRACTICE_SESSION_COMPLETED.wireName -> "Practice session completed"
        else -> "Learning activity recorded"
    }
    val rewardDetail = if (activity.xpAwarded > 0L) {
        "${activity.xpAwarded} XP settled in the ledger"
    } else {
        "Recorded without an XP grant"
    }
    return LearningActivityFeedItem(
        id = activity.eventId,
        title = title,
        detail = "$subjectLabel · $rewardDetail",
        xpAwarded = activity.xpAwarded,
        occurredAtEpochMillis = activity.occurredAtEpochMillis
    )
}
