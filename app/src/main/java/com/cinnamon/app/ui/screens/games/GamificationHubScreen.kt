package com.cinnamon.app.ui.screens.games

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.cinnamon.app.ui.feedback.LocalCinnamonFeedbackPreferences
import com.cinnamon.app.ui.theme.*
import com.cinnamon.app.viewmodel.UserProgressViewModel
import com.cinnamon.app.viewmodel.LearningFocusOptionUiModel
import com.cinnamon.app.viewmodel.LearningFocusUiState
import com.cinnamon.app.viewmodel.JourneyStageUiState
import com.cinnamon.app.viewmodel.JourneyStageUiModel
import com.cinnamon.app.viewmodel.JourneyUiState
import com.cinnamon.app.domain.gamification.LearningFocusTone
import com.cinnamon.app.domain.gamification.JourneyDestination
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamificationHubScreen(
    onBack: () -> Unit,
    progressViewModel: UserProgressViewModel,
    onStartReview: () -> Unit,
    onOpenPractice: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf("Learning Plan") }
    val haptic = LocalHapticFeedback.current

    val tabs = listOf(
        "Learning Plan",
        "Quests",
        "Language Sprint",
        "Vocab Pairs",
        "Transcript Escape",
        "Study Log"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Learning Questboard", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SurgicalGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val useCompactTabs = maxWidth < 600.dp
            val selectTab: (String) -> Unit = { tab ->
                selectedTab = tab
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            if (useCompactTabs) {
                Column(modifier = Modifier.fillMaxSize()) {
                    QuestboardTabs(
                        tabs = tabs,
                        selectedTab = selectedTab,
                        compact = true,
                        onSelect = selectTab,
                        modifier = Modifier.fillMaxWidth()
                    )
                    QuestboardWorkspace(
                        selectedTab = selectedTab,
                        progressViewModel = progressViewModel,
                        onStartReview = onStartReview,
                        onOpenPractice = onOpenPractice,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    QuestboardTabs(
                        tabs = tabs,
                        selectedTab = selectedTab,
                        compact = false,
                        onSelect = selectTab,
                        modifier = Modifier
                            .width(140.dp)
                            .fillMaxHeight()
                    )
                    QuestboardWorkspace(
                        selectedTab = selectedTab,
                        progressViewModel = progressViewModel,
                        onStartReview = onStartReview,
                        onOpenPractice = onOpenPractice,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestboardTabs(
    tabs: List<String>,
    selectedTab: String,
    compact: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (compact) {
        LazyRow(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
                .selectableGroup(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs, key = { it }) { tab ->
                val isActive = selectedTab == tab
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isActive) SurgicalGreen.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface,
                    border = if (isActive) BorderStroke(1.dp, SurgicalGreen.copy(alpha = 0.7f)) else null,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = isActive,
                            role = Role.Tab,
                            onClick = { onSelect(tab) }
                        )
                ) {
                    Text(
                        text = tab,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                        color = if (isActive) SurgicalGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .verticalScroll(rememberScrollState())
                .selectableGroup(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            tabs.forEach { tab ->
                val isActive = selectedTab == tab
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = isActive,
                            role = Role.Tab,
                            onClick = { onSelect(tab) }
                        )
                        .background(if (isActive) MaterialTheme.colorScheme.background else Color.Transparent)
                        .padding(vertical = 16.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = tab,
                        color = if (isActive) SurgicalGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestboardWorkspace(
    selectedTab: String,
    progressViewModel: UserProgressViewModel,
    onStartReview: () -> Unit,
    onOpenPractice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (reduceMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                }
            },
            label = "gamificationHubTransition"
        ) { targetTab ->
            when (targetTab) {
                "Learning Plan" -> RpgSkillTreeComponent(
                    viewModel = progressViewModel,
                    onStartReview = onStartReview,
                    onOpenPractice = onOpenPractice
                )
                "Quests" -> QuestsComponent(progressViewModel)
                "Language Sprint" -> ScenarioLanguageSprintComponent(progressViewModel)
                "Vocab Pairs" -> VocabPairsComponent(progressViewModel)
                "Transcript Escape" -> TranscriptEscapeComponent(progressViewModel)
                "Study Log" -> StudyLogComponent(progressViewModel)
                else -> QuestsComponent(progressViewModel)
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// TAB: DAILY + WEEKLY QUESTS FROM PERSISTED LEARNING EVENTS
// --------------------------------------------------------------------------------------
@Composable
fun QuestsComponent(viewModel: UserProgressViewModel) {
    val points by viewModel.points.collectAsState()
    val dailyQuests by viewModel.dailyQuests.collectAsState()
    val weeklyQuests by viewModel.weeklyQuests.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val visibleQuests = dailyQuests + weeklyQuests

    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Checkpoints are projections of completed events, never tappable state.
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, SurgicalGreen.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("YOUR QUESTS", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Badge(containerColor = SurgicalGreen.copy(alpha = 0.2f)) {
                            Text("SAVED PROGRESS", color = SurgicalGreen, modifier = Modifier.padding(4.dp), fontSize = 10.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    visibleQuests.forEachIndexed { index, quest ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (quest.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (quest.completed) "Checkpoint complete" else "Checkpoint in progress",
                                tint = if (quest.completed) SurgicalGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quest.title,
                                    color = if (quest.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text("${quest.progress} of ${quest.target} complete", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                Text(quest.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
                                when {
                                    quest.claimable && quest.instanceId != null -> {
                                        FilledTonalButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.claimQuestReward(quest.instanceId)
                                            },
                                            modifier = Modifier
                                                .padding(top = 8.dp)
                                                .heightIn(min = 48.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = SurgicalGreen.copy(alpha = 0.18f),
                                                contentColor = SurgicalGreen
                                            )
                                        ) {
                                            Text(
                                                if (quest.rewardXp > 0) "CLAIM +${quest.rewardXp} XP" else "CLAIM EARNED REWARD",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    quest.claimed -> {
                                        Text(
                                            if (quest.rewardXp > 0) {
                                                "${quest.rewardXp} XP EARNED · CLAIMED ONCE"
                                            } else {
                                                "REWARD EARNED · CLAIMED ONCE"
                                            },
                                            color = SurgicalGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (index < visibleQuests.size - 1) {
                            HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // Personal progress only; no fabricated social ranking.
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("YOUR LEARNING RECORD", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current streak", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        Text("$streak days", color = SurgicalGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("XP balance", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        Text("$points XP", color = SurgicalGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Keep reward authority understandable without exposing storage jargon.
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("HOW PROGRESS WORKS", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Practice moves quests and milestones only after a completed activity is saved. Taps, retries, and guessed scores cannot create extra XP.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Activities stay predictable; Cinnamon won’t claim personalization until it can measure and explain the change.", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// TAB: FOUNDATION PLAN AND LEARNING FOCUS
// --------------------------------------------------------------------------------------
@Composable
fun RpgSkillTreeComponent(
    viewModel: UserProgressViewModel,
    onStartReview: () -> Unit,
    onOpenPractice: () -> Unit
) {
    val journeyState by viewModel.learningJourney.collectAsState()
    val learningFocusState by viewModel.learningFocus.collectAsState()
    val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (val state = journeyState) {
            JourneyUiState.Loading -> Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
                    Column {
                        Text("PREPARING YOUR LEARNING PLAN", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Loading saved milestones from this device…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            }

            is JourneyUiState.Unavailable -> Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FOUNDATION PLAN NEEDS ATTENTION", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(state.message, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp, lineHeight = 20.sp)
                    Text("Cinnamon did not reset your recorded progress or XP.", color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.76f), fontSize = 13.sp)
                    FilledTonalButton(
                        onClick = viewModel::retryStartup,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Try setup again", fontWeight = FontWeight.Bold)
                    }
                }
            }

            is JourneyUiState.Ready -> {
                val journey = state.journey
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(journey.eyebrow, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(journey.title, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        Text(journey.description, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f), fontSize = 14.sp, lineHeight = 20.sp)
                        LinearProgressIndicator(
                            progress = {
                                journey.completedStageCount.toFloat() / journey.totalStageCount.coerceAtLeast(1)
                            },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = NeonCyan,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${journey.completedStageCount}/${journey.totalStageCount} milestones complete", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${journey.earnedXp} XP earned", color = SurgicalGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (journey.isComplete) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f))
                            Text(journey.completionTitle, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text(journey.completionDescription, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f), fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                }

            // A static state accent avoids continuous GPU work and remains identical when
            // reduced motion is enabled. Completion transitions elsewhere stay event-driven.
            val dashOffset = 0f
            val activeGlowAlpha = if (reduceMotion) 0.68f else 0.78f

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                journey.stages.forEachIndexed { index, stage ->
                    val completed = stage.state == JourneyStageUiState.COMPLETED
                    val active = stage.state == JourneyStageUiState.ACTIVE
                    val glowAlpha = if (active) activeGlowAlpha else 0f
                    val stageStateLabel = when {
                        completed -> "complete"
                        active -> "in progress"
                        else -> "locked"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(if (active) 13.dp else 0.dp, RoundedCornerShape(18.dp), spotColor = NeonCyan.copy(alpha = glowAlpha))
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    active -> MaterialTheme.colorScheme.secondaryContainer
                                    completed -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = when {
                                active -> BorderStroke(1.5.dp, NeonCyan.copy(alpha = glowAlpha))
                                completed -> BorderStroke(1.dp, SurgicalGreen.copy(alpha = 0.7f))
                                else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth().semantics {
                                contentDescription =
                                    "Foundation milestone ${stage.order}, ${stage.title}, $stageStateLabel, ${stage.progress} of ${stage.target} recorded, ${stage.rewardXp} experience points"
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(if (active) NeonCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.background)
                                        .border(2.dp, if (completed) SurgicalGreen else if (active) NeonCyan else Color.DarkGray, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        completed -> Text("✓", fontSize = 18.sp)
                                        active -> Text(stage.order.toString(), fontSize = 18.sp)
                                        else -> Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(19.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("MILESTONE ${stage.order}", color = if (active) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("+${stage.rewardXp} XP", color = if (completed) SurgicalGreen else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Text(stage.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                    Text(stage.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { stage.progress.toFloat() / stage.target.coerceAtLeast(1) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(CircleShape)
                                            .semantics {
                                                progressBarRangeInfo = ProgressBarRangeInfo(
                                                    current = stage.progress.toFloat(),
                                                    range = 0f..stage.target.toFloat(),
                                                    steps = (stage.target - 1).coerceAtLeast(0)
                                                )
                                            },
                                        color = if (completed) SurgicalGreen else NeonCyan,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Text(
                                        when {
                                            completed -> "COMPLETE · ${stage.progress}/${stage.target} recorded"
                                            active -> "IN PROGRESS · ${stage.progress}/${stage.target} recorded"
                                            else -> "LOCKED · complete milestone ${stage.order - 1} first"
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (active) {
                                        FilledTonalButton(
                                            onClick = when (stage.destination) {
                                                JourneyDestination.REVIEW -> onStartReview
                                                JourneyDestination.PRACTICE -> onOpenPractice
                                                JourneyDestination.JOURNEY -> onOpenPractice
                                            },
                                            modifier = Modifier.padding(top = 8.dp).heightIn(min = 48.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = NeonCyan.copy(alpha = 0.18f),
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Text(stage.actionLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (index < journey.stages.lastIndex) {
                        val connectorReached = completed
                        val pipeColor = if (connectorReached) SurgicalGreen else Color.DarkGray
                        
                        Canvas(modifier = Modifier.width(4.dp).height(24.dp)) {
                            drawLine(
                                color = pipeColor,
                                start = Offset(size.width / 2, 0f),
                                end = Offset(size.width / 2, size.height),
                                strokeWidth = 4.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(10f, 10f),
                                    phase = if (connectorReached) -dashOffset else 0f
                                )
                            )
                        }
                    }
                    if (index == 0) {
                        LearningFocusBoard(
                            state = learningFocusState,
                            onSelectFocus = viewModel::selectLearningFocus,
                            onStartReview = onStartReview,
                            onOpenPractice = onOpenPractice,
                            onRetrySetup = viewModel::retryStartup
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun LearningFocusBoard(
    state: LearningFocusUiState,
    onSelectFocus: (String) -> Unit,
    onStartReview: () -> Unit,
    onOpenPractice: () -> Unit,
    onRetrySetup: () -> Unit
) {
    var pendingOptionId by rememberSaveable { mutableStateOf<String?>(null) }

    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    )
    Text(
        text = "LEARNING FOCUS",
        color = NeonCyan,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp
    )

    when (state) {
        LearningFocusUiState.Loading -> Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                Text("Loading your saved Learning Focus…", fontSize = 14.sp)
            }
        }

        is LearningFocusUiState.Locked -> Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription =
                        "Learning Focus locked. ${state.progress} of ${state.target} terms reviewed."
                }
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(state.eyebrow, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(state.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
                LinearProgressIndicator(
                    progress = { state.progress.toFloat() / state.target.coerceAtLeast(1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape)
                        .semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo(
                                current = state.progress.toFloat(),
                                range = 0f..state.target.toFloat(),
                                steps = (state.target - 1).coerceAtLeast(0)
                            )
                        },
                    color = NeonCyan,
                    trackColor = MaterialTheme.colorScheme.background
                )
                Text(
                    "${state.progress} of ${state.target} terms reviewed",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(
                    onClick = onStartReview,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Start review", fontWeight = FontWeight.Bold)
                }
            }
        }

        is LearningFocusUiState.Unavailable -> Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("LEARNING FOCUS NEEDS ATTENTION", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(state.message, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp, lineHeight = 20.sp)
                FilledTonalButton(
                    onClick = onRetrySetup,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Try setup again", fontWeight = FontWeight.Bold)
                }
            }
        }

        is LearningFocusUiState.Choose -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF6C3B20), MaterialTheme.colorScheme.primaryContainer, Color(0xFF134F4B))
                        )
                    )
                    .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("CHOOSE WHAT TO SHARPEN", color = NeonCyan, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    Text(state.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 23.sp)
                    Text(state.description, color = Color.White.copy(alpha = 0.86f), fontSize = 14.sp, lineHeight = 20.sp)
                    Text(
                        "Your choice is saved for this version and can’t be changed.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    "0 XP",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                "Choosing earns no reward. XP is earned only by completing recorded milestones.",
                                color = Color.White.copy(alpha = 0.86f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val sideBySide = maxWidth >= 600.dp
                if (sideBySide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        state.options.forEach { option ->
                            Box(modifier = Modifier.weight(1f)) {
                                LearningFocusOptionCard(
                                    option = option,
                                    saving = state.savingOptionId == option.id,
                                    enabled = state.savingOptionId == null,
                                    onChoose = { pendingOptionId = option.id }
                                )
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        state.options.forEach { option ->
                            LearningFocusOptionCard(
                                option = option,
                                saving = state.savingOptionId == option.id,
                                enabled = state.savingOptionId == null,
                                onChoose = { pendingOptionId = option.id }
                            )
                        }
                    }
                }
            }

            state.errorMessage?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            val pendingOption = state.options.firstOrNull { it.id == pendingOptionId }
            if (pendingOption != null) {
                AlertDialog(
                    onDismissRequest = { pendingOptionId = null },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                    title = { Text("Use ${pendingOption.title}?") },
                    text = {
                        Text(
                            "This focus is saved for this version and cannot be changed. Selecting it earns no XP; only completed milestones earn rewards."
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                pendingOptionId = null
                                onSelectFocus(pendingOption.id)
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("Use this focus", fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { pendingOptionId = null },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("Review options") }
                    }
                )
            }
        }

        is LearningFocusUiState.Ready -> LearningFocusProgressCard(
            state = state,
            onStartReview = onStartReview,
            onOpenPractice = onOpenPractice
        )
    }
}

@Composable
private fun LearningFocusOptionCard(
    option: LearningFocusOptionUiModel,
    saving: Boolean,
    enabled: Boolean,
    onChoose: () -> Unit
) {
    val precision = option.tone == LearningFocusTone.LANGUAGE_PRECISION
    val accent = if (precision) Color(0xFFF1A55B) else NeonCyan
    val icon = if (precision) Icons.Default.Tune else Icons.Default.GridView
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(46.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(option.title, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(option.tagline, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
                }
                Surface(color = accent.copy(alpha = 0.14f), shape = RoundedCornerShape(999.dp)) {
                    Text("${option.totalRewardXp} XP", modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                option.metrics.forEach { metric ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                metric.value.toString(),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                metric.label,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
            Button(
                onClick = onChoose,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF1E1613))
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF1E1613))
                    Spacer(Modifier.width(8.dp))
                    Text("Saving focus…", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                } else {
                    Text("Choose ${option.title}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun LearningFocusProgressCard(
    state: LearningFocusUiState.Ready,
    onStartReview: () -> Unit,
    onOpenPractice: () -> Unit
) {
    val journey = state.milestonePlan
    val precision = state.tone == LearningFocusTone.LANGUAGE_PRECISION
    val accent = if (precision) Color(0xFFF1A55B) else NeonCyan
    val availableXp = journey.stages.sumOf(JourneyStageUiModel::rewardXp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    if (precision) listOf(Color(0xFF6F3C20), Color(0xFF30231D))
                    else listOf(Color(0xFF155B56), Color(0xFF1E2928))
                )
            )
            .border(1.5.dp, accent.copy(alpha = 0.72f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("CURRENT FOCUS", color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    Text(state.focusTitle, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
                Surface(color = accent.copy(alpha = 0.16f), shape = RoundedCornerShape(999.dp)) {
                    Text("${journey.completedStageCount}/${journey.totalStageCount}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = accent, fontWeight = FontWeight.Bold)
                }
            }
            Text(state.focusTagline, color = Color.White.copy(alpha = 0.82f), fontSize = 14.sp, lineHeight = 20.sp)
            LinearProgressIndicator(
                progress = { journey.completedStageCount.toFloat() / journey.totalStageCount.coerceAtLeast(1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            current = journey.completedStageCount.toFloat(),
                            range = 0f..journey.totalStageCount.toFloat(),
                            steps = (journey.totalStageCount - 1).coerceAtLeast(0)
                        )
                    },
                color = accent,
                trackColor = Color.White.copy(alpha = 0.14f)
            )
            Text(
                if (journey.isComplete) journey.completionDescription
                else "${journey.earnedXp} of $availableXp XP earned · ${journey.completedStageCount} of ${journey.totalStageCount} milestones complete",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Text(
                "Saved for this version",
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 12.sp
            )
        }
    }

    journey.stages.forEach { stage ->
        LearningFocusMilestoneCard(
            stage = stage,
            accent = accent,
            onStartReview = onStartReview,
            onOpenPractice = onOpenPractice
        )
    }
}

@Composable
private fun LearningFocusMilestoneCard(
    stage: JourneyStageUiModel,
    accent: Color,
    onStartReview: () -> Unit,
    onOpenPractice: () -> Unit
) {
    val active = stage.state == JourneyStageUiState.ACTIVE
    val completed = stage.state == JourneyStageUiState.COMPLETED
    val stateLabel = when {
        completed -> "complete"
        active -> "in progress"
        else -> "locked"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (active) 6.dp else 0.dp, RoundedCornerShape(18.dp), spotColor = accent.copy(alpha = 0.45f))
            .semantics {
                contentDescription =
                    "Focus milestone ${stage.order}, ${stage.title}, $stateLabel, ${stage.progress} of ${stage.target} recorded, ${stage.rewardXp} experience points"
            },
        colors = CardDefaults.cardColors(
            containerColor = if (active) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, when { active -> accent; completed -> SurgicalGreen; else -> MaterialTheme.colorScheme.outlineVariant }),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background).border(2.dp, if (completed) SurgicalGreen else if (active) accent else Color.DarkGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                when {
                    completed -> Text("✓", fontSize = 17.sp)
                    active -> Text(stage.order.toString(), fontSize = 17.sp)
                    else -> Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("FOCUS MILESTONE ${stage.order}", color = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("+${stage.rewardXp} XP", color = if (completed) SurgicalGreen else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text(stage.title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Text(stage.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
                LinearProgressIndicator(
                    progress = { stage.progress.toFloat() / stage.target.coerceAtLeast(1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo(
                                current = stage.progress.toFloat(),
                                range = 0f..stage.target.toFloat(),
                                steps = (stage.target - 1).coerceAtLeast(0)
                            )
                        },
                    color = if (completed) SurgicalGreen else accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    when {
                        completed -> "COMPLETE · ${stage.progress}/${stage.target} recorded"
                        active -> "IN PROGRESS · ${stage.progress}/${stage.target} recorded"
                        else -> "LOCKED · complete milestone ${stage.order - 1} first"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                if (active) {
                    FilledTonalButton(
                        onClick = when (stage.destination) {
                            JourneyDestination.REVIEW -> onStartReview
                            JourneyDestination.PRACTICE, JourneyDestination.JOURNEY -> onOpenPractice
                        },
                        modifier = Modifier.padding(top = 5.dp).heightIn(min = 48.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = accent.copy(alpha = 0.18f))
                    ) {
                        Text(stage.actionLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

internal data class ScenarioLanguageSprintPrompt(
    val title: String,
    val prompt: String,
    val acceptedAnswers: List<String>,
    val cue: String
)

internal val scenarioLanguageSprintPrompts = listOf(
    ScenarioLanguageSprintPrompt(
        title = "Name the reference term",
        prompt = "In this fictional scene, the pain is described as sudden and 'ripping' through the back. Type the authored reference term.",
        acceptedAnswers = listOf("aortic dissection", "dissection"),
        cue = "Look for the two-word term in the authored reference card: aortic …"
    ),
    ScenarioLanguageSprintPrompt(
        title = "Clarify the timeline",
        prompt = "Write the clear follow-up question: 'When did the pain begin?'",
        acceptedAnswers = listOf("when did the pain begin", "when did it begin"),
        cue = "Use a short, open timing question beginning with 'When'."
    ),
    ScenarioLanguageSprintPrompt(
        title = "Signpost the next question",
        prompt = "Write this calm signpost: 'I'll ask a few questions first.'",
        acceptedAnswers = listOf("i'll ask a few questions", "i will ask a few questions", "ask a few questions first"),
        cue = "Begin with a calm first-person signpost, then mention 'a few questions'."
    )
)

internal fun matchesScenarioLanguageSprintPrompt(
    answer: String,
    prompt: ScenarioLanguageSprintPrompt
): Boolean {
    val normalizedAnswer = answer.lowercase(Locale.ROOT).trim()
    return normalizedAnswer.isNotBlank() && prompt.acceptedAnswers.any { acceptedAnswer ->
        normalizedAnswer.contains(acceptedAnswer)
    }
}

// --------------------------------------------------------------------------------------
// TAB 3: AUTHORED THREE-STEP LANGUAGE SPRINT
// --------------------------------------------------------------------------------------
@Composable
fun ScenarioLanguageSprintComponent(viewModel: UserProgressViewModel) {
    val prompts = scenarioLanguageSprintPrompts
    var timerSeconds by rememberSaveable { mutableIntStateOf(150) }
    var battleActive by rememberSaveable { mutableStateOf(false) }
    var inputAnswer by rememberSaveable { mutableStateOf("") }
    var currentPromptIndex by rememberSaveable { mutableIntStateOf(0) }
    var completedPromptCount by rememberSaveable { mutableIntStateOf(0) }
    var practiceSessionKey by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var battleStatus by remember { mutableStateOf("") }
    var completedSuccessfully by rememberSaveable { mutableStateOf(false) }
    val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion

    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    var showConfetti by remember { mutableStateOf(false) }

    if (showConfetti && !reduceMotion) {
        LaunchedEffect(Unit) {
            delay(2200)
            showConfetti = false
        }
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val colors = listOf(NeonCyan, SurgicalGreen, Color(0xFFFFC857), Color(0xFFFF8C69))
            repeat(24) { index ->
                val column = index % 6
                val row = index / 6
                val x = size.width * ((column + 0.5f) / 6f)
                val y = size.height * ((row + 1f) / 5f)
                drawCircle(color = colors[index % colors.size], radius = 7f + (index % 3) * 2f, center = Offset(x, y))
            }
        }
    }

    val pulseAlpha = if (reduceMotion) {
        0.75f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "bossPulse")
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        ).value
    }

    LaunchedEffect(battleActive) {
        if (battleActive) {
            while (timerSeconds > 0 && battleActive) {
                delay(1000)
                timerSeconds--
            }
            if (timerSeconds == 0) {
                battleActive = false
                battleStatus = "Time is up. Your progress was not recorded; restart whenever you want to complete all three authored prompts."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("THREE-STEP LANGUAGE SPRINT", color = AlertRed, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(
                "$completedPromptCount/${prompts.size} matched",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (battleActive && timerSeconds < 60) 20.dp else 0.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = AlertRed.copy(alpha = if (battleActive) pulseAlpha else 0f)
                )
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (battleActive && timerSeconds < 30) AlertRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(if (battleActive) 2.dp else 1.dp, if (battleActive) AlertRed.copy(alpha = pulseAlpha) else Color.DarkGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AUTHORED FICTIONAL LANGUAGE SCENE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        val minute = timerSeconds / 60
                        val second = timerSeconds % 60
                        val timeString = String.format(Locale.ROOT, "%02d:%02d", minute, second)
                        Text(
                            "⏰ $timeString", 
                            color = if (timerSeconds < 30) AlertRed else NeonCyan, 
                            fontWeight = FontWeight.Black, 
                            fontSize = if (timerSeconds < 30) 18.sp else 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Practice clear, calm English around a fictional chest-pain scene. This is a language prompt only—not a diagnostic, triage, or emergency-care simulator.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { completedPromptCount.toFloat() / prompts.size },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = NeonCyan,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("WHAT MAKES THIS COUNT", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "Match all three language prompts in one round. Cinnamon saves one completed practice result; retries cannot earn duplicate XP.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        if (battleActive) {
            val prompt = prompts[currentPromptIndex]
            Text(
                "STEP ${currentPromptIndex + 1} · ${prompt.title.uppercase(Locale.ROOT)}",
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                prompt.prompt,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            OutlinedTextField(
                value = inputAnswer,
                onValueChange = { inputAnswer = it },
                label = { Text("Your authored language answer") },
                placeholder = { Text("Type the reference phrase…", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AlertRed,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            )

            Button(
                onClick = {
                    focusManager.clearFocus()
                    val prompt = prompts[currentPromptIndex]
                    val matchesReference = matchesScenarioLanguageSprintPrompt(inputAnswer, prompt)
                    if (matchesReference) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val nextCount = currentPromptIndex + 1
                        completedPromptCount = nextCount
                        if (nextCount == prompts.size) {
                            battleActive = false
                            completedSuccessfully = true
                            battleStatus = "Sprint complete. All three references matched; saving one completed language-practice result."
                            showConfetti = true
                            viewModel.recordPracticeSession(
                                subjectType = "scenario_language_sprint",
                                subjectId = "fictional_chest_pain_language_v1",
                                occurrenceKey = practiceSessionKey,
                                completedItemCount = prompts.size
                            )
                        } else {
                            currentPromptIndex += 1
                            inputAnswer = ""
                            battleStatus = "Reference matched. Step ${currentPromptIndex + 1} of ${prompts.size} is ready."
                        }
                    } else {
                        completedSuccessfully = false
                        battleStatus = "Reference cue: ${prompt.cue}"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("CHECK THIS STEP", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        } else {
            Button(
                onClick = {
                    battleActive = true
                    timerSeconds = 150
                    battleStatus = ""
                    inputAnswer = ""
                    currentPromptIndex = 0
                    completedPromptCount = 0
                    completedSuccessfully = false
                    practiceSessionKey = UUID.randomUUID().toString()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(
                    if (completedSuccessfully) "REPLAY — NO EXTRA XP" else "OPEN LANGUAGE CHALLENGE",
                    color = MaterialTheme.colorScheme.onError,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }

        if (battleStatus.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (completedSuccessfully) SurgicalGreen.copy(alpha=0.1f) else AlertRed.copy(alpha=0.1f)),
                border = BorderStroke(2.dp, if (completedSuccessfully) SurgicalGreen else AlertRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = battleStatus,
                    color = if (completedSuccessfully) SurgicalGreen else AlertRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// TAB 4: VOCABULARY PAIR GAMEPLAY
// --------------------------------------------------------------------------------------
@Composable
fun VocabPairsComponent(viewModel: UserProgressViewModel) {
    var matchScore by remember { mutableIntStateOf(0) }
    var selectedId1 by remember { mutableStateOf<Int?>(null) }
    var selectedId2 by remember { mutableStateOf<Int?>(null) }
    var practiceSessionKey by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    val haptic = LocalHapticFeedback.current

    // Flashcard components paired!
    val flashcardsMaster = remember {
        mutableStateListOf(
            MatchTile(1, "Dyspnea", "Shortness of Breath", false),
            MatchTile(2, "Cephalgia", "Severe Headache", false),
            MatchTile(3, "Syncope", "Fainting Episode", false),
            MatchTile(4, "Myalgia", "Muscle Pains", false),
            MatchTile(5, "Shortness of Breath", "Dyspnea", false),
            MatchTile(6, "Severe Headache", "Cephalgia", false),
            MatchTile(7, "Fainting Episode", "Syncope", false),
            MatchTile(8, "Muscle Pains", "Myalgia", false)
        )
    }

    LaunchedEffect(selectedId1, selectedId2) {
        if (selectedId1 != null && selectedId2 != null) {
            val tile1 = flashcardsMaster.find { it.id == selectedId1 }
            val tile2 = flashcardsMaster.find { it.id == selectedId2 }

            if (tile1 != null && tile2 != null) {
                // If they align/match name to matchName
                if (tile1.matchedTerm == tile2.term) {
                    delay(400)
                    flashcardsMaster.remove(tile1)
                    flashcardsMaster.remove(tile2)
                    matchScore += 50
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } else {
                    delay(500)
                }
            }
            selectedId1 = null
            selectedId2 = null
        }
    }

    LaunchedEffect(flashcardsMaster.size, practiceSessionKey) {
        if (flashcardsMaster.isEmpty()) {
            viewModel.recordPracticeSession(
                subjectType = "flashcard_match",
                subjectId = "dyspnea|cephalgia|syncope|myalgia",
                occurrenceKey = practiceSessionKey,
                completedItemCount = 4
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("VOCAB PAIR PUZZLE", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Align medical-English terms with their plain-language descriptions to clear the board.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            Badge(containerColor = SurgicalGreen, contentColor = Color.Black) {
                Text("Board: $matchScore", fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
            }
        }

        if (flashcardsMaster.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BOARD CLEARED! Vocabulary pairs aligned.", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        matchScore = 0
                        practiceSessionKey = UUID.randomUUID().toString()
                        flashcardsMaster.addAll(
                            listOf(
                                MatchTile(1, "Dyspnea", "Shortness of Breath", false),
                                MatchTile(2, "Cephalgia", "Severe Headache", false),
                                MatchTile(3, "Syncope", "Fainting Episode", false),
                                MatchTile(4, "Myalgia", "Muscle Pains", false),
                                MatchTile(5, "Shortness of Breath", "Dyspnea", false),
                                MatchTile(6, "Severe Headache", "Cephalgia", false),
                                MatchTile(7, "Fainting Episode", "Syncope", false),
                                MatchTile(8, "Muscle Pains", "Myalgia", false)
                            ).shuffled()
                        )
                    }) {
                        Text("Reset Vocab Grid")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().height(260.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(flashcardsMaster.size) { index ->
                    val item = flashcardsMaster[index]
                    val isSelected = selectedId1 == item.id || selectedId2 == item.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                            .clickable {
                                if (selectedId1 == null) {
                                    selectedId1 = item.id
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else if (selectedId2 == null && selectedId1 != item.id) {
                                    selectedId2 = item.id
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) NeonCyan else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(1.dp, if (isSelected) Color.White else Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.term,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class MatchTile(val id: Int, val term: String, val matchedTerm: String, val matched: Boolean)

// --------------------------------------------------------------------------------------
// TAB 5: AUTHORED TRANSCRIPT ESCAPE ROOM
// --------------------------------------------------------------------------------------
internal data class TranscriptEscapeClue(
    val title: String,
    val transcript: String,
    val question: String,
    val acceptedAnswers: List<String>,
    val cue: String
)

internal val transcriptEscapeClues = listOf(
    TranscriptEscapeClue(
        title = "Unlock the sound word",
        transcript = "A fictional caller describes a high-pitched whistle while breathing out.",
        question = "What vocabulary term describes that whistling sound?",
        acceptedAnswers = listOf("wheeze", "wheezing"),
        cue = "The reference word begins with 'wheez'."
    ),
    TranscriptEscapeClue(
        title = "Unlock the plain-language phrase",
        transcript = "A speaker says: 'I am struggling to get a full breath.'",
        question = "Type the plain-language phrase for this symptom.",
        acceptedAnswers = listOf("shortness of breath", "breathlessness"),
        cue = "The phrase begins with 'shortness'."
    ),
    TranscriptEscapeClue(
        title = "Unlock the calm signpost",
        transcript = "In this fictional exchange, the speaker says: 'I'll ask one more question, then explain what happens next.'",
        question = "Type the first calm signpost from the transcript.",
        acceptedAnswers = listOf("i'll ask one more question", "i will ask one more question"),
        cue = "Start with 'I' and include 'one more question'."
    )
)

internal fun matchesTranscriptEscapeAnswer(answer: String, clue: TranscriptEscapeClue): Boolean {
    val normalizedAnswer = answer.lowercase(Locale.ROOT).trim()
    return normalizedAnswer.isNotBlank() && clue.acceptedAnswers.any { acceptedAnswer ->
        normalizedAnswer.contains(acceptedAnswer)
    }
}

@Composable
fun TranscriptEscapeComponent(viewModel: UserProgressViewModel) {
    val clues = transcriptEscapeClues
    var transcriptCueVisible by rememberSaveable { mutableStateOf(false) }
    var rawEstimate by rememberSaveable { mutableStateOf("") }
    var currentClueIndex by rememberSaveable { mutableIntStateOf(0) }
    var clearedClueCount by rememberSaveable { mutableIntStateOf(0) }
    var practiceSessionKey by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var verdictFeedback by remember { mutableStateOf("") }
    var completedSuccessfully by rememberSaveable { mutableStateOf(false) }
    val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val currentClue = clues[currentClueIndex]
    val barA: Float
    val barB: Float
    val barC: Float
    if (reduceMotion) {
        barA = 28f
        barB = 28f
        barC = 28f
    } else {
        val clueMeter = rememberInfiniteTransition(label = "clueMeter")
        barA = clueMeter.animateFloat(
            initialValue = 14f,
            targetValue = 44f,
            animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse),
            label = "clueMeterA"
        ).value
        barB = clueMeter.animateFloat(
            initialValue = 38f,
            targetValue = 16f,
            animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse),
            label = "clueMeterB"
        ).value
        barC = clueMeter.animateFloat(
            initialValue = 18f,
            targetValue = 54f,
            animationSpec = infiniteRepeatable(tween(430, easing = LinearEasing), RepeatMode.Reverse),
            label = "clueMeterC"
        ).value
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TRANSCRIPT ESCAPE ROOM", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("$clearedClueCount/${clues.size} unlocked", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text("Unlock three written language clues in one run. No audio playback, audio analysis, or clinical assessment is connected.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            border = BorderStroke(1.dp, SurgicalGreen.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("CLUE VAULT", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("STEP ${currentClueIndex + 1}: ${currentClue.title.uppercase(Locale.ROOT)}", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(barA, barB, barC, barA).forEachIndexed { index, height ->
                        if (index > 0) Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(if (transcriptCueVisible) height.dp else (12 + index * 8).dp)
                                .background(listOf(NeonCyan, SurgicalGreen, Color.Yellow, AlertRed)[index], RoundedCornerShape(99.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        transcriptCueVisible = !transcriptCueVisible
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (transcriptCueVisible) AlertRed else NeonCyan)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Transcript clue", tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (transcriptCueVisible) "HIDE WRITTEN CLUE" else "REVEAL WRITTEN CLUE", color = Color.Black)
                    }
                }
            }
        }

        if (transcriptCueVisible) {
            Text("AUTHORED TRANSCRIPT:", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(
                "\"${currentClue.transcript}\"",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                style = TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            )
        }

        OutlinedTextField(
            value = rawEstimate,
            onValueChange = { rawEstimate = it },
            label = { Text(currentClue.question) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SurgicalGreen),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        )

        Button(
            onClick = {
                focusManager.clearFocus()
                if (matchesTranscriptEscapeAnswer(rawEstimate, currentClue)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val nextCount = currentClueIndex + 1
                    clearedClueCount = nextCount
                    if (nextCount == clues.size) {
                        completedSuccessfully = true
                        verdictFeedback = "Vault opened. All three written cues matched; saving one completed language-practice result."
                        viewModel.recordPracticeSession(
                            subjectType = "authored_transcript_escape",
                            subjectId = "respiratory_language_cues_v1",
                            occurrenceKey = practiceSessionKey,
                            completedItemCount = clues.size
                        )
                    } else {
                        currentClueIndex += 1
                        rawEstimate = ""
                        transcriptCueVisible = false
                        verdictFeedback = "Clue unlocked. Step ${currentClueIndex + 1} of ${clues.size} is ready."
                    }
                } else {
                    completedSuccessfully = false
                    verdictFeedback = "Reference cue: ${currentClue.cue}"
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            enabled = rawEstimate.isNotBlank() && !completedSuccessfully
        ) {
            Text("CHECK THIS CLUE", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        if (completedSuccessfully) {
            OutlinedButton(
                onClick = {
                    transcriptCueVisible = false
                    rawEstimate = ""
                    currentClueIndex = 0
                    clearedClueCount = 0
                    practiceSessionKey = UUID.randomUUID().toString()
                    verdictFeedback = ""
                    completedSuccessfully = false
                },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, SurgicalGreen)
            ) {
                Text("REPLAY — NO EXTRA XP", color = SurgicalGreen, fontWeight = FontWeight.Bold)
            }
        }

        if (verdictFeedback.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (completedSuccessfully) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, if (completedSuccessfully) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = verdictFeedback,
                    modifier = Modifier.padding(14.dp),
                    color = if (completedSuccessfully) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// TAB 6: DURABLE LEARNING ACTIVITY LOG
// --------------------------------------------------------------------------------------
@Composable
fun StudyLogComponent(viewModel: UserProgressViewModel) {
    val activities by viewModel.recentLearningActivity.collectAsState()
    var selectedActivityId by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    val selectedActivity = activities.firstOrNull { it.id == selectedActivityId }

    Column(
        modifier = Modifier
            .fillMaxSize()
        .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("RECENT STUDY LOG", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("Only completed learning activities appear here. This build does not fabricate audio recordings, AI critiques, or clinical evaluations.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

        if (activities.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Finish a review or a full practice session to create your first saved learning entry.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        activities.forEach { activity ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedActivityId = activity.id
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                border = if (selectedActivityId == activity.id) BorderStroke(1.5.dp, NeonCyan) else null
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(activity.title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            if (activity.xpAwarded > 0L) "+${activity.xpAwarded} XP" else "No XP",
                            color = if (activity.xpAwarded > 0L) SurgicalGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(activity.detail, color = NeonCyan, fontSize = 11.sp)
                }
            }
        }

        if (selectedActivity != null) {
            val occurredAtLabel = remember(selectedActivity.occurredAtEpochMillis) {
                java.text.DateFormat.getDateTimeInstance(
                    java.text.DateFormat.MEDIUM,
                    java.text.DateFormat.SHORT,
                    Locale.getDefault()
                ).format(java.util.Date(selectedActivity.occurredAtEpochMillis))
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                border = BorderStroke(1.dp, SurgicalGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LEARNING ENTRY", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        IconButton(onClick = { selectedActivityId = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = AlertRed)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Recorded $occurredAtLabel. ${selectedActivity.detail}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
