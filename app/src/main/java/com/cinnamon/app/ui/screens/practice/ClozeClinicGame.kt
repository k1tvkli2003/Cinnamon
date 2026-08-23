package com.cinnamon.app.ui.screens.practice

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.SpeakerNotes
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.cinnamon.app.domain.repository.ClozeRound
import com.cinnamon.app.domain.repository.LexiconRepository
import com.cinnamon.app.ui.components.EmptyState
import com.cinnamon.app.ui.theme.SuccessMint
import com.cinnamon.app.viewmodel.UserProgressViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClozeClinicGame(
    onBack: () -> Unit,
    progressViewModel: UserProgressViewModel
) {
    val context = LocalContext.current
    val repository = remember { LexiconRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    var rounds by remember { mutableStateOf<List<ClozeRound>?>(null) }
    var index by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<String?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var practiceSessionKey by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }

    LaunchedEffect(Unit) {
        rounds = repository.clozeRounds(10)
    }

    LaunchedEffect(rounds, index, correctCount, practiceSessionKey) {
        val completedRounds = rounds
        if (
            completedRounds != null &&
            completedRounds.isNotEmpty() &&
            index >= completedRounds.size &&
            isRewardEligibleClozeRun(correctCount, completedRounds.size)
        ) {
            progressViewModel.recordPracticeSession(
                subjectType = "cloze_clinic",
                subjectId = completedRounds.map { it.entry.id }.sorted().joinToString(separator = "|"),
                occurrenceKey = practiceSessionKey,
                completedItemCount = correctCount
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloze Clinic") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, "Close") } },
                actions = {
                    rounds?.let {
                        if (it.isNotEmpty() && index < it.size) {
                            Text(
                                "${index + 1} / ${it.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val data = rounds
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                data == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                data.isEmpty() -> EmptyState(
                    icon = Icons.AutoMirrored.Rounded.SpeakerNotes,
                    title = "Lexicon still brewing",
                    message = "Cloze Clinic needs the dictionary loaded. Give it a moment after first launch and try again."
                )
                index >= data.size -> ClozeSummary(
                    score = score,
                    correct = correctCount,
                    total = data.size,
                    rewardEligible = isRewardEligibleClozeRun(correctCount, data.size),
                    onAgain = {
                    scope.launch {
                        rounds = repository.clozeRounds(10)
                        index = 0; chosen = null; score = 0; correctCount = 0
                        practiceSessionKey = UUID.randomUUID().toString()
                    }
                }, onDone = onBack)
                else -> {
                    val round = data[index]
                    Column(
                        Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Which word fits the blank?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                round.blankedSentence,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = MaterialTheme.typography.headlineSmall.fontSize,
                                modifier = Modifier.padding(20.dp)
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        round.options.forEach { option ->
                            val isChosen = chosen == option
                            val isAnswer = option == round.entry.term
                            val revealed = chosen != null
                            val target = when {
                                !revealed -> MaterialTheme.colorScheme.surface
                                isAnswer -> SuccessMint.copy(alpha = 0.18f)
                                isChosen -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val bg by animateColorAsState(target, tween(220), label = "optionBg")
                            val borderColor = when {
                                !revealed -> MaterialTheme.colorScheme.outline
                                isAnswer -> SuccessMint
                                isChosen -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outline
                            }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(15.dp))
                                    .semantics {
                                        selected = isChosen
                                        if (revealed) {
                                            when {
                                                isAnswer -> stateDescription = "Correct answer."
                                                isChosen -> stateDescription = "Selected incorrect option. Correct answer is ${round.entry.term}."
                                            }
                                        }
                                    }
                                    .clickable(enabled = !revealed) {
                                        chosen = option
                                        if (isAnswer) {
                                            score += 15; correctCount++
                                            // This is a first-choice success inside a real authored sentence.
                                            // The ViewModel cannot mint XP; it asks the ledger to verify the entry.
                                            progressViewModel.recordVerifiedContextApplication(
                                                lexiconEntryId = round.entry.id,
                                                occurrenceKey = "$practiceSessionKey:${round.entry.id}"
                                            )
                                        }
                                    },
                                shape = RoundedCornerShape(15.dp),
                                color = bg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Text(
                                    option,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        if (chosen != null) {
                            val answeredCorrectly = chosen == round.entry.term
                            Surface(
                                shape = RoundedCornerShape(15.dp),
                                color = if (answeredCorrectly) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                            ) {
                                Text(
                                    text = if (answeredCorrectly) {
                                        "Correct."
                                    } else {
                                        "Not quite. The correct term is ${round.entry.term}. ${round.entry.plain}"
                                    },
                                    modifier = Modifier.padding(14.dp),
                                    color = if (answeredCorrectly) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = { index++; chosen = null },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(15.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(if (index == data.size - 1) "Finish" else "Next", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A completion needs verified correct answers before it becomes reward evidence. */
internal fun isRewardEligibleClozeRun(correctCount: Int, totalRounds: Int): Boolean =
    totalRounds > 0 && correctCount in 3..totalRounds

@Composable
private fun ClozeSummary(
    score: Int,
    correct: Int,
    total: Int,
    rewardEligible: Boolean,
    onAgain: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🤎", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text("$correct / $total correct", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text("Practice score: $score", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (!rewardEligible) {
            Spacer(Modifier.height(10.dp))
            Text(
                "This round did not count as a completed practice session. You can try it again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onAgain,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) { Text("Play again", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}
