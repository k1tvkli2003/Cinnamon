package com.cinnamon.app.ui.screens.practice

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cinnamon.app.data.local.LexiconEntry
import com.cinnamon.app.ui.components.EmptyState
import com.cinnamon.app.ui.components.LevelBadge
import com.cinnamon.app.ui.components.SpeakButton
import com.cinnamon.app.ui.components.TagPill
import com.cinnamon.app.ui.theme.HoneyGold
import com.cinnamon.app.ui.theme.IpaStyle
import com.cinnamon.app.ui.theme.SuccessMint
import com.cinnamon.app.viewmodel.ReviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSessionScreen(
    onBack: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    viewModel: ReviewViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    if (!state.finished && state.total > 0) {
                        Text(
                            "${state.done} / ${state.total}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                state.finished -> SessionSummary(
                    earnedXp = state.earnedXp,
                    reviewed = state.total,
                    again = state.gradedAgain,
                    good = state.gradedGood + state.gradedEasy,
                    onAgain = { viewModel.startSession() },
                    onDone = onBack,
                    empty = state.total == 0
                )
                else -> {
                    val card = state.current
                    if (card != null) {
                        Column(Modifier.fillMaxSize()) {
                            LinearProgressIndicator(
                                progress = { if (state.total == 0) 0f else state.done.toFloat() / state.total },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            ReviewCard(
                                entry = card,
                                revealed = state.revealed,
                                submitting = state.submitting,
                                submissionError = state.submissionError,
                                onReveal = { viewModel.reveal() },
                                onGrade = { viewModel.grade(it) },
                                onRetry = { viewModel.retryGrade() },
                                onRefreshQueue = { viewModel.startSession() },
                                onOpenEntry = { onOpenEntry(card.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    entry: LexiconEntry,
    revealed: Boolean,
    submitting: Boolean,
    submissionError: com.cinnamon.app.viewmodel.ReviewSubmissionError?,
    onReveal: () -> Unit,
    onGrade: (Int) -> Unit,
    onRetry: () -> Unit,
    onRefreshQueue: () -> Unit,
    onOpenEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(20.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable(enabled = !revealed && !submitting) { onReveal() },
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
        ) {
            AnimatedContent(
                targetState = revealed,
                transitionSpec = { fadeIn(tween(240)).togetherWith(fadeOut(tween(120))) },
                label = "reviewFlip"
            ) { isRevealed ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LevelBadge(entry.level)
                            TagPill(entry.topic)
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = entry.term,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        if (entry.ipa.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Text(entry.ipa, style = IpaStyle, color = MaterialTheme.colorScheme.secondary)
                                SpeakButton(text = entry.term)
                            }
                        }

                        if (!isRevealed) {
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "Recall the meaning, then tap to flip",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Spacer(Modifier.height(20.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Spacer(Modifier.height(20.dp))
                            Text(
                                entry.definition,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            if (entry.exampleClinical.isNotBlank()) {
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    entry.exampleClinical,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            TextButton(onClick = onOpenEntry) { Text("See full entry") }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (!revealed) {
            Button(
                onClick = onReveal,
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Flip", fontWeight = FontWeight.Bold)
            }
        } else {
            if (submissionError != null) {
                ReviewSubmissionNotice(
                    error = submissionError,
                    onRetry = onRetry,
                    onRefreshQueue = onRefreshQueue
                )
                Spacer(Modifier.height(12.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradeButton("Again", MaterialTheme.colorScheme.error, Modifier.weight(1f), enabled = !submitting) { onGrade(1) }
                GradeButton("Hard", HoneyGold, Modifier.weight(1f), enabled = !submitting) { onGrade(3) }
                GradeButton("Good", MaterialTheme.colorScheme.secondary, Modifier.weight(1f), enabled = !submitting) { onGrade(4) }
                GradeButton("Easy", SuccessMint, Modifier.weight(1f), enabled = !submitting) { onGrade(5) }
            }
            if (submitting) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ReviewSubmissionNotice(
    error: com.cinnamon.app.viewmodel.ReviewSubmissionError,
    onRetry: () -> Unit,
    onRefreshQueue: () -> Unit
) {
    val isStale = error == com.cinnamon.app.viewmodel.ReviewSubmissionError.CARD_CHANGED
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.56f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isStale) "This card changed in another review session." else "Your answer has not been saved yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (isStale) "Refresh the queue before grading again." else "Retry uses the same logical answer, so it cannot award XP twice.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.84f)
                )
            }
            TextButton(onClick = if (isStale) onRefreshQueue else onRetry) {
                Text(if (isStale) "Refresh" else "Retry")
            }
        }
    }
}

@Composable
private fun GradeButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(15.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = color.copy(alpha = if (enabled) 0.16f else 0.06f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = color.copy(alpha = if (enabled) 1f else 0.45f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SessionSummary(
    earnedXp: Int,
    reviewed: Int,
    again: Int,
    good: Int,
    onAgain: () -> Unit,
    onDone: () -> Unit,
    empty: Boolean
) {
    if (empty) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState(
                    icon = Icons.Rounded.Done,
                    title = "Nothing due right now",
                    message = "Your review queue is clear. Open the Lexicon and add a few words to start a deck."
                )
                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Back") }
            }
        }
        return
    }

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("🤎", style = MaterialTheme.typography.displayMedium)
        }
        Spacer(Modifier.height(24.dp))
        Text("Session complete", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text("+$earnedXp XP earned", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryStat("$reviewed", "reviewed", MaterialTheme.colorScheme.secondary)
            SummaryStat("$good", "got it", SuccessMint)
            SummaryStat("$again", "to revisit", MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onAgain,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) { Text("Another round", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done for now") }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
