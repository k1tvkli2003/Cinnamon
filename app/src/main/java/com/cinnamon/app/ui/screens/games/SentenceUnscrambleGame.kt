package com.cinnamon.app.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.cinnamon.app.ui.feedback.LocalCinnamonFeedbackPreferences
import com.cinnamon.app.ui.theme.NeonCyan
import com.cinnamon.app.ui.theme.AlertRed
import com.cinnamon.app.ui.theme.SurgicalGreen
import com.cinnamon.app.viewmodel.UserProgressViewModel
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SentenceUnscrambleGame(
    onBack: () -> Unit,
    progressViewModel: UserProgressViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { com.cinnamon.app.domain.repository.LexiconRepository.getInstance(context) }

    val fallback = listOf(
        "The attending asked me to justify my leading differential",
        "Her argument was cogent enough to silence every lingering doubt",
        "Trend his lactate overnight before escalating toward aggressive resuscitation",
        "Only later did we grasp the full ramifications involved"
    )
    var sentences by rememberSaveable { mutableStateOf(fallback) }
    LaunchedEffect(Unit) {
        val loaded = repository.learn.randomSentences(12).map { it.text }
        if (loaded.isNotEmpty()) sentences = loaded
    }

    var currentSentenceIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentSentence by remember(currentSentenceIndex, sentences) {
        mutableStateOf(sentences[currentSentenceIndex.coerceIn(0, sentences.lastIndex)].split(" "))
    }

    var availableWords by rememberSaveable(currentSentenceIndex, sentences) {
        mutableStateOf(currentSentence.shuffled())
    }
    
    var builtSentence by rememberSaveable { mutableStateOf(listOf<String>()) }
    var isError by rememberSaveable { mutableStateOf(false) }
    var feedbackMessage by rememberSaveable { mutableStateOf("") }
    var completed by rememberSaveable { mutableStateOf(false) }
    val practiceSessionKey = rememberSaveable { UUID.randomUUID().toString() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unscramble") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (completed) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                ) {
                    Text(
                        "Practice Round Complete",
                        style = MaterialTheme.typography.headlineLarge,
                        color = SurgicalGreen
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "You reviewed ${sentences.size} sentences in this session.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onBack) { Text("Finish") }
                }
            }
        } else Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Text("Build the sentence:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            val scope = rememberCoroutineScope()
            // Built Sentence Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isError) AlertRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(1.dp, if (isError) AlertRed.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    builtSentence.forEach { word ->
                        WordChip(word = word, isAvailable = false, onClick = {
                            builtSentence = builtSentence - word
                            availableWords = availableWords + word
                            isError = false
                            feedbackMessage = ""
                            scope.launch {
                                com.cinnamon.app.ui.util.SoundSynthesizer.playSynthesizedSound(com.cinnamon.app.ui.util.SoundSynthesizer.SoundType.POP)
                            }
                        })
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Available words:", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableWords.forEach { word ->
                    WordChip(word = word, isAvailable = true, onClick = {
                        availableWords = availableWords - word
                        builtSentence = builtSentence + word
                        isError = false
                        feedbackMessage = ""
                        scope.launch {
                            com.cinnamon.app.ui.util.SoundSynthesizer.playSynthesizedSound(com.cinnamon.app.ui.util.SoundSynthesizer.SoundType.CLICK)
                        }
                    })
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = when {
                    feedbackMessage.isNotEmpty() -> feedbackMessage
                    builtSentence.size < currentSentence.size -> "Place all the words to complete the sentence."
                    else -> ""
                },
                color = if (isError) AlertRed else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )

            Button(
                onClick = {
                    if (builtSentence == currentSentence) {
                        scope.launch {
                            com.cinnamon.app.ui.util.SoundSynthesizer.playSynthesizedSound(com.cinnamon.app.ui.util.SoundSynthesizer.SoundType.SUCCESS)
                        }
                        if (currentSentenceIndex < sentences.size - 1) {
                            feedbackMessage = "Correct. Sentence ${currentSentenceIndex + 1} of ${sentences.size} completed."
                            currentSentenceIndex++
                            builtSentence = emptyList()
                        } else {
                            progressViewModel.recordPracticeSession(
                                subjectType = "sentence_unscramble",
                                subjectId = sentences.sorted().joinToString(separator = "|"),
                                occurrenceKey = practiceSessionKey,
                                completedItemCount = sentences.size
                            )
                            completed = true
                            feedbackMessage = ""
                        }
                    } else {
                        isError = true
                        feedbackMessage = "That order is not quite right. Try rearranging the words."
                        scope.launch {
                            com.cinnamon.app.ui.util.SoundSynthesizer.playSynthesizedSound(com.cinnamon.app.ui.util.SoundSynthesizer.SoundType.SWOOSH)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = builtSentence.size == currentSentence.size,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Check Answer", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WordChip(word: String, isAvailable: Boolean = false, onClick: () -> Unit) {
    val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion
    // Elegant spring reaction when tapped
    var isTapped by remember { mutableStateOf(false) }
    val chipScale by animateFloatAsState(
        targetValue = if (isTapped && !reduceMotion) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        finishedListener = { isTapped = false },
        label = "chipScale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = chipScale
                scaleY = chipScale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isAvailable) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                } else {
                    SurgicalGreen.copy(alpha = 0.2f)
                }
            )
            .border(
                width = 1.dp,
                color = if (isAvailable) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else SurgicalGreen,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { 
                isTapped = true
                onClick() 
            }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = word, 
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (isAvailable) MaterialTheme.colorScheme.primary else SurgicalGreen
        )
    }
}
