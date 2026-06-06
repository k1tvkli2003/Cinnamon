package com.example.ui.screens.games

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
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.AlertRed
import com.example.ui.theme.SurgicalGreen
import com.example.viewmodel.UserProgressViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SentenceUnscrambleGame(
    onBack: () -> Unit,
    progressViewModel: UserProgressViewModel
) {
    val sentences = listOf(
        "The patient presents with acute abdominal pain",
        "Please take these medications twice a day after meals",
        "I need to schedule a follow up appointment for next week",
        "We are waiting for the laboratory results to confirm"
    )
    
    var currentSentenceIndex by remember { mutableIntStateOf(0) }
    var currentSentence by remember(currentSentenceIndex) { 
        mutableStateOf(sentences[currentSentenceIndex].split(" "))
    }
    
    var availableWords by remember(currentSentenceIndex) { 
        mutableStateOf(currentSentence.shuffled()) 
    }
    
    var builtSentence by remember { mutableStateOf(listOf<String>()) }
    var isError by remember { mutableStateOf(false) }
    var hasErrorEverOccurred by remember { mutableStateOf(false) }

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
        Column(
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
                            scope.launch {
                                builtSentence = builtSentence - word
                                availableWords = availableWords + word
                                isError = false
                                com.example.ui.util.SoundSynthesizer.playSynthesizedSound(com.example.ui.util.SoundSynthesizer.SoundType.POP)
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
                        scope.launch {
                            availableWords = availableWords - word
                            builtSentence = builtSentence + word
                            isError = false
                            com.example.ui.util.SoundSynthesizer.playSynthesizedSound(com.example.ui.util.SoundSynthesizer.SoundType.CLICK)
                        }
                    })
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    scope.launch {
                        if (builtSentence == currentSentence) {
                            progressViewModel.addPoints(25)
                            progressViewModel.incrementGeneralCare(0.02f)
                            progressViewModel.incrementPulmonology(0.015f)
                            com.example.ui.util.SoundSynthesizer.playSynthesizedSound(com.example.ui.util.SoundSynthesizer.SoundType.SUCCESS)
                            if (currentSentenceIndex < sentences.size - 1) {
                                currentSentenceIndex++
                                builtSentence = emptyList()
                            } else {
                                if (!hasErrorEverOccurred) {
                                    progressViewModel.unlockAchievement("Grammar Surgeon")
                                }
                                onBack() // Or show a win screen
                            }
                        } else {
                            isError = true
                            hasErrorEverOccurred = true
                            com.example.ui.util.SoundSynthesizer.playSynthesizedSound(com.example.ui.util.SoundSynthesizer.SoundType.SWOOSH)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Check Answer", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WordChip(word: String, isAvailable: Boolean = false, onClick: () -> Unit) {
    // Elegant spring reaction when tapped
    var isTapped by remember { mutableStateOf(false) }
    val chipScale by animateFloatAsState(
        targetValue = if (isTapped) 0.9f else 1f,
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
