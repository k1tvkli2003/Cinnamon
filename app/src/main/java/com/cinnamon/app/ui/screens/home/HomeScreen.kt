package com.cinnamon.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinnamon.app.R
import com.cinnamon.app.data.local.LexiconEntry
import com.cinnamon.app.data.seed.LexiconSeedState
import com.cinnamon.app.data.seed.LexiconSeeder
import com.cinnamon.app.data.startup.AppStartupCoordinator
import com.cinnamon.app.data.startup.AppStartupState
import com.cinnamon.app.domain.repository.LexiconRepository
import com.cinnamon.app.domain.gamification.JourneyDestination
import com.cinnamon.app.ui.components.LevelBadge
import com.cinnamon.app.ui.components.SectionHeader
import com.cinnamon.app.ui.components.SpeakButton
import com.cinnamon.app.ui.components.TagPill
import com.cinnamon.app.ui.feedback.LocalCinnamonFeedbackPreferences
import com.cinnamon.app.ui.theme.IpaStyle
import com.cinnamon.app.ui.theme.Springs
import com.cinnamon.app.ui.theme.pressScale
import com.cinnamon.app.viewmodel.UserProgressViewModel
import com.cinnamon.app.viewmodel.Quest
import com.cinnamon.app.viewmodel.JourneyStageUiModel
import com.cinnamon.app.viewmodel.JourneyStageUiState
import com.cinnamon.app.viewmodel.JourneyUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    progressViewModel: UserProgressViewModel,
    onOpenEntry: (Long) -> Unit,
    onStartReview: () -> Unit,
    onOpenModule: (String) -> Unit
) {
    val context = LocalContext.current
    val seedState by LexiconSeeder.state.collectAsState()
    val startupState by AppStartupCoordinator.state.collectAsState()
    val startupReady = startupState == AppStartupState.Ready && seedState == LexiconSeedState.Ready

    if (!startupReady) {
        val scope = rememberCoroutineScope()
        StartupHomeScreen(
            startupState = startupState,
            seedState = seedState,
            onRetry = {
                scope.launch {
                    AppStartupCoordinator.prepare(context.applicationContext)
                }
            }
        )
        return
    }

    val repository = remember { LexiconRepository.getInstance(context) }

    val streak by progressViewModel.streak.collectAsState()
    val streakAlive by progressViewModel.streakAliveToday.collectAsState()
    val xpToday by progressViewModel.xpToday.collectAsState()
    val dailyGoal by progressViewModel.dailyGoalXp.collectAsState()
    val rank by progressViewModel.rank.collectAsState()
    val dueNow by progressViewModel.dueNow.collectAsState()
    val totalWords by progressViewModel.totalWords.collectAsState()
    val masteredWords by progressViewModel.masteredWords.collectAsState()
    val dailyQuests by progressViewModel.dailyQuests.collectAsState()
    val journeyState by progressViewModel.learningJourney.collectAsState()

    val wordOfTheDay by repository.wordOfTheDay().collectAsState(initial = null)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(60)
        visible = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            StaggerIn(visible, 0) {
                HomeHeader(streak = streak, streakAlive = streakAlive)
            }
        }

        item {
            StaggerIn(visible, 1) {
                DailyGoalCard(xpToday = xpToday, dailyGoal = dailyGoal, rank = rank)
            }
        }

        item {
            StaggerIn(visible, 2) {
                MissionPulseCard(
                    quest = dailyQuests.firstOrNull(),
                    dueNow = dueNow,
                    onReview = onStartReview,
                    onBrowse = { onOpenModule("lexicon") },
                    onClaim = progressViewModel::claimQuestReward
                )
            }
        }

        item {
            StaggerIn(visible, 3) {
                JourneyPulseCard(
                    state = journeyState,
                    onReview = onStartReview,
                    onPractice = { onOpenModule("practice") },
                    onOpenJourney = { onOpenModule("gamification_hub") },
                    onRetry = progressViewModel::retryStartup
                )
            }
        }

        item {
            StaggerIn(visible, 4) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "Word of the day")
                    WordOfTheDayCard(
                        entry = wordOfTheDay,
                        totalWords = totalWords,
                        onOpen = onOpenEntry
                    )
                }
            }
        }

        item {
            StaggerIn(visible, 5) {
                ReviewDueCard(
                    dueNow = dueNow,
                    mastered = masteredWords,
                    total = totalWords,
                    onStartReview = onStartReview
                )
            }
        }

        item {
            StaggerIn(visible, 6) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "Practice studio")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModuleTile(
                            title = "Dialogue Practice",
                            subtitle = "Guided fictional prompts",
                            icon = Icons.Rounded.Healing,
                            accent = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        ) { onOpenModule("scenario_select") }
                        ModuleTile(
                            title = "Language Labs",
                            subtitle = "Authored terminology exercises",
                            icon = Icons.Rounded.Science,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        ) { onOpenModule("clinical_sim_labs") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModuleTile(
                            title = "Register Studio",
                            subtitle = "Compare authored styles",
                            icon = Icons.Rounded.RecordVoiceOver,
                            accent = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        ) { onOpenModule("make_it_native") }
                        ModuleTile(
                            title = "Speech Rhythm",
                            subtitle = "Visual pacing guides",
                            icon = Icons.Rounded.GraphicEq,
                            accent = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        ) { onOpenModule("native_fluency_playground/Shadowing & Rhythm") }
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyPulseCard(
    state: JourneyUiState,
    onReview: () -> Unit,
    onPractice: () -> Unit,
    onOpenJourney: () -> Unit,
    onRetry: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.34f))
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(
                    colors = listOf(
                        colors.primaryContainer,
                        colors.secondaryContainer.copy(alpha = 0.92f),
                        colors.surface
                    )
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 34.dp, y = (-38).dp)
                    .size(138.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.08f))
            )
            when (state) {
                JourneyUiState.Loading -> Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 3.dp,
                        color = colors.primary
                    )
                    Column {
                        Text(
                            "CHARTING YOUR JOURNEY",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onPrimaryContainer.copy(alpha = 0.72f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Restoring your saved learning plan…",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is JourneyUiState.Unavailable -> Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "JOURNEY NEEDS ATTENTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.error,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = onRetry,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Retry journey setup", fontWeight = FontWeight.Bold)
                    }
                }

                is JourneyUiState.Ready -> {
                    val journey = state.journey
                    val active = journey.activeStage
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(colors.primary.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.School,
                                        contentDescription = null,
                                        tint = colors.primary
                                    )
                                }
                                Column {
                                    Text(
                                        journey.eyebrow,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onPrimaryContainer.copy(alpha = 0.68f)
                                    )
                                    Text(
                                        journey.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            TagPill(
                                text = "${journey.completedStageCount}/${journey.totalStageCount}",
                                container = colors.primary.copy(alpha = 0.14f),
                                content = colors.onPrimaryContainer
                            )
                        }

                        JourneyCheckpointRail(journey.stages)

                        if (journey.isComplete) {
                            Text(
                                journey.completionTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                journey.completionDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${journey.earnedXp} XP earned",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                FilledTonalButton(
                                    onClick = onOpenJourney,
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("View milestones")
                                }
                            }
                        } else if (active != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "MILESTONE ${active.order}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.primary
                                )
                                TagPill(
                                    text = "+${active.rewardXp} XP",
                                    container = colors.tertiary.copy(alpha = 0.14f),
                                    content = colors.onSurface
                                )
                            }
                            Text(
                                active.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                active.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                            LinearProgressIndicator(
                                progress = {
                                    (active.progress.toFloat() / active.target.coerceAtLeast(1))
                                        .coerceIn(0f, 1f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(9.dp)
                                    .clip(CircleShape)
                                    .semantics {
                                        progressBarRangeInfo = ProgressBarRangeInfo(
                                            current = active.progress.toFloat(),
                                            range = 0f..active.target.toFloat(),
                                            steps = (active.target - 1).coerceAtLeast(0)
                                        )
                                    },
                                color = colors.primary,
                                trackColor = colors.onSurface.copy(alpha = 0.1f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${active.progress} / ${active.target} recorded",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = when (active.destination) {
                                        JourneyDestination.REVIEW -> onReview
                                        JourneyDestination.PRACTICE -> onPractice
                                        JourneyDestination.JOURNEY -> onOpenJourney
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text(active.actionLabel, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyCheckpointRail(stages: List<JourneyStageUiModel>) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stages.forEachIndexed { index, stage ->
            val markerColor = when (stage.state) {
                JourneyStageUiState.COMPLETED -> colors.tertiary
                JourneyStageUiState.ACTIVE -> colors.primary
                JourneyStageUiState.LOCKED -> colors.onSurface.copy(alpha = 0.2f)
            }
            Box(
                modifier = Modifier
                    .size(if (stage.state == JourneyStageUiState.ACTIVE) 32.dp else 28.dp)
                    .clip(CircleShape)
                    .background(markerColor)
                    .semantics(mergeDescendants = true) {
                        val stateLabel = when (stage.state) {
                            JourneyStageUiState.COMPLETED -> "complete"
                            JourneyStageUiState.ACTIVE -> "in progress"
                            JourneyStageUiState.LOCKED -> "locked"
                        }
                        contentDescription =
                            "Foundation milestone ${stage.order}, ${stage.title}, $stateLabel"
                    },
                contentAlignment = Alignment.Center
            ) {
                when (stage.state) {
                    JourneyStageUiState.COMPLETED -> Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = colors.onTertiary,
                        modifier = Modifier.size(17.dp)
                    )
                    JourneyStageUiState.ACTIVE -> Text(
                        stage.order.toString(),
                        color = colors.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    JourneyStageUiState.LOCKED -> Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = colors.surface,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (index < stages.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 3.dp,
                    color = if (stage.state == JourneyStageUiState.COMPLETED) {
                        colors.tertiary.copy(alpha = 0.68f)
                    } else {
                        colors.onSurface.copy(alpha = 0.1f)
                    }
                )
            }
        }
    }
}

@Composable
private fun MissionPulseCard(
    quest: Quest?,
    dueNow: Int,
    onReview: () -> Unit,
    onBrowse: () -> Unit,
    onClaim: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val target = quest?.target ?: 3
    val progress = quest?.progress ?: 0
    val progressFraction = (progress.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
    val copy = missionPulseCopy(quest = quest, dueNow = dueNow)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = colors.tertiaryContainer,
        contentColor = colors.onTertiaryContainer,
        border = BorderStroke(1.dp, colors.tertiary.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.tertiary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = colors.onTertiaryContainer,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    Column {
                        Text(
                            "TODAY’S MISSION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onTertiaryContainer.copy(alpha = 0.72f)
                        )
                        Text(
                            copy.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (quest != null && quest.rewardXp > 0) {
                    TagPill(
                        text = "+${quest.rewardXp} XP",
                        container = colors.tertiary.copy(alpha = 0.16f),
                        content = colors.onTertiaryContainer
                    )
                }
            }
            Text(
                copy.detail,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onTertiaryContainer.copy(alpha = 0.82f)
            )
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(CircleShape)
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            current = progress.toFloat(),
                            range = 0f..target.toFloat(),
                            steps = (target - 1).coerceAtLeast(0)
                        )
                    },
                color = colors.tertiary,
                trackColor = colors.onTertiaryContainer.copy(alpha = 0.12f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$progress / $target recorded",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                when {
                    quest?.claimable == true && quest.instanceId != null -> Button(
                        onClick = { onClaim(quest.instanceId) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.tertiary,
                            contentColor = colors.onTertiary
                        )
                    ) {
                        Text("Claim +${quest.rewardXp} XP", fontWeight = FontWeight.Bold)
                    }
                    quest?.claimed == true -> Text(
                        "Reward claimed",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.tertiary
                    )
                    quest != null -> Button(
                        onClick = onReview,
                        enabled = dueNow > 0,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(if (dueNow > 0) "Continue reviews" else "Awaiting due words")
                    }
                    else -> FilledTonalButton(
                        onClick = onBrowse,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Choose words", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * A truthful first frame: no stale zero counts or tappable learning destinations are
 * presented while the local lexicon and reward ledger are still being checked.
 */
@Composable
private fun StartupHomeScreen(
    startupState: AppStartupState,
    seedState: LexiconSeedState,
    onRetry: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            StartupCheckpointCard(
                startupState = startupState,
                seedState = seedState,
                onRetry = onRetry
            )
        }
    }
}

@Composable
internal fun StartupCheckpointCard(
    startupState: AppStartupState,
    seedState: LexiconSeedState,
    onRetry: () -> Unit
) {
    val presentation = startupCheckpointPresentation(startupState, seedState)
    val foreground = if (presentation.isFailure) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                stateDescription = presentation.stateDescription
            },
        shape = RoundedCornerShape(24.dp),
        color = if (presentation.isFailure) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        border = BorderStroke(1.dp, foreground.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = foreground.copy(alpha = 0.10f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (presentation.isFailure) {
                            Icon(
                                imageVector = Icons.Rounded.Healing,
                                contentDescription = null,
                                tint = foreground
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(25.dp),
                                strokeWidth = 3.dp,
                                color = foreground
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = presentation.eyebrow,
                        style = MaterialTheme.typography.labelSmall,
                        color = foreground.copy(alpha = 0.74f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = presentation.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = foreground,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = presentation.body,
                style = MaterialTheme.typography.bodyMedium,
                color = foreground
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = foreground.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = foreground
                    )
                    Text(
                        text = presentation.protectionNote,
                        style = MaterialTheme.typography.labelMedium,
                        color = foreground
                    )
                }
            }

            presentation.actionLabel?.let { label ->
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(label, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StaggerIn(visible: Boolean, index: Int, content: @Composable () -> Unit) {
    val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion
    AnimatedVisibility(
        visible = visible,
        enter = if (reduceMotion) {
            EnterTransition.None
        } else {
            slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = Springs.nav()
            ) + fadeIn(tween(280 + index * 60))
        },
    ) {
        content()
    }
}

@Composable
private fun HomeHeader(streak: Int, streakAlive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = "Cinnamon roll mark",
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Cinnamon",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Ready for one focused win?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        val flameColor = if (streakAlive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(flameColor.copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.LocalFireDepartment,
                contentDescription = "Streak",
                tint = flameColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "$streak",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = flameColor
            )
        }
    }
}

@Composable
private fun DailyGoalCard(xpToday: Int, dailyGoal: Int, rank: String) {
    val progress = (xpToday.toFloat() / dailyGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion
    val animatedProgress = if (reduceMotion) {
        progress
    } else {
        animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(700),
            label = "goalProgress"
        ).value
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's goal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$xpToday / $dailyGoal XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TagPill(
                    text = rank,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            if (progress >= 1f) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Goal reached — the kitchen smells great. 🤎",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun WordOfTheDayCard(
    entry: LexiconEntry?,
    totalWords: Int,
    onOpen: (Long) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = entry != null
            ) { entry?.let { onOpen(it.id) } },
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
    ) {
        Box(
            Modifier.background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                )
            )
        ) {
            if (entry == null) {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        text = if (totalWords == 0) "Brewing the lexicon…" else "Loading…",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "The dictionary is being prepared for its first opening.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(Modifier.padding(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = entry.term,
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (entry.ipa.isNotBlank()) {
                                Text(
                                    text = entry.ipa,
                                    style = IpaStyle,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        SpeakButton(text = entry.term)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LevelBadge(entry.level)
                        TagPill(entry.topic)
                        if (entry.pos.isNotBlank()) {
                            Text(
                                text = entry.pos,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = entry.definition,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewDueCard(
    dueNow: Int,
    mastered: Int,
    total: Int,
    onStartReview: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Style,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (dueNow > 0) "$dueNow words due" else "Queue is clear",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "$mastered of $total mastered",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            Button(
                onClick = onStartReview,
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text(if (dueNow > 0) "Review" else "Learn new", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ModuleTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
