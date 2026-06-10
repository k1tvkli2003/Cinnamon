package com.cinnamon.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinnamon.app.data.local.LexiconEntry
import com.cinnamon.app.domain.repository.LexiconRepository
import com.cinnamon.app.ui.components.LevelBadge
import com.cinnamon.app.ui.components.SectionHeader
import com.cinnamon.app.ui.components.SpeakButton
import com.cinnamon.app.ui.components.TagPill
import com.cinnamon.app.ui.theme.IpaStyle
import com.cinnamon.app.ui.theme.Springs
import com.cinnamon.app.ui.theme.pressScale
import com.cinnamon.app.viewmodel.UserProgressViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    progressViewModel: UserProgressViewModel,
    onOpenEntry: (Long) -> Unit,
    onStartReview: () -> Unit,
    onOpenModule: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { LexiconRepository.getInstance(context) }

    val streak by progressViewModel.streak.collectAsState()
    val streakAlive by progressViewModel.streakAliveToday.collectAsState()
    val xpToday by progressViewModel.xpToday.collectAsState()
    val dailyGoal by progressViewModel.dailyGoalXp.collectAsState()
    val rank by progressViewModel.rank.collectAsState()
    val dueNow by progressViewModel.dueNow.collectAsState()
    val totalWords by progressViewModel.totalWords.collectAsState()
    val masteredWords by progressViewModel.masteredWords.collectAsState()

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
            StaggerIn(visible, 3) {
                ReviewDueCard(
                    dueNow = dueNow,
                    mastered = masteredWords,
                    total = totalWords,
                    onStartReview = onStartReview
                )
            }
        }

        item {
            StaggerIn(visible, 4) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "Sharpen with AI")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModuleTile(
                            title = "AI Patient",
                            subtitle = "Take a history",
                            icon = Icons.Rounded.Healing,
                            accent = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        ) { onOpenModule("scenario_select") }
                        ModuleTile(
                            title = "Sim Labs",
                            subtitle = "SBAR · SPIKES · ECG",
                            icon = Icons.Rounded.Science,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        ) { onOpenModule("clinical_sim_labs") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModuleTile(
                            title = "Make It Native",
                            subtitle = "Formal · casual · slang",
                            icon = Icons.Rounded.RecordVoiceOver,
                            accent = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        ) { onOpenModule("make_it_native") }
                        ModuleTile(
                            title = "Shadowing",
                            subtitle = "Rhythm & prosody",
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
private fun StaggerIn(visible: Boolean, index: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = Springs.nav()
        ) + fadeIn(tween(280 + index * 60)),
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
        Column {
            Text(
                text = "Cinnamon",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Your English, perfected daily.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(700),
        label = "goalProgress"
    )
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
