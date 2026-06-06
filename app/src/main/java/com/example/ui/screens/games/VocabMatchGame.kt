package com.example.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurgicalGreen
import com.example.ui.theme.AlertRed
import com.example.viewmodel.UserProgressViewModel
import kotlinx.coroutines.delay

data class VocabCard(val id: Int, val text: String, val pairId: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabMatchGame(
    onBack: () -> Unit,
    progressViewModel: UserProgressViewModel
) {
    val items = listOf(
        Pair("Myocardial Infarction", "Heart Attack"),
        Pair("Hypertension", "High Blood Pressure"),
        Pair("Syncope", "Fainting"),
        Pair("Dyspnea", "Shortness of Breath"),
        Pair("Hemorrhage", "Bleeding"),
        Pair("Renal", "Kidney")
    )
    
    var cards by remember { 
        mutableStateOf(
            items.flatMapIndexed { index, pair -> 
                listOf(
                    VocabCard(index * 2, pair.first, index),
                    VocabCard(index * 2 + 1, pair.second, index)
                )
            }.shuffled()
        )
    }

    var selectedFirst by remember { mutableStateOf<VocabCard?>(null) }
    var matchedPairs by remember { mutableStateOf(setOf<Int>()) }
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(60) }

    LaunchedEffect(timeLeft, matchedPairs.size) {
        if (timeLeft > 0 && matchedPairs.size < items.size) {
            delay(1000)
            timeLeft--
        } else if (matchedPairs.size == items.size && timeLeft > 0) {
            progressViewModel.addPoints(50 + timeLeft) // Bonus points
            timeLeft = -1 // Stop timer
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vocab Match") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text("Time: ${if(timeLeft>=0) timeLeft else 0}s", modifier = Modifier.padding(end = 16.dp), style = MaterialTheme.typography.titleMedium, color = if(timeLeft < 10) AlertRed else NeonCyan)
                    Text("Pts: $score", modifier = Modifier.padding(end = 16.dp), style = MaterialTheme.typography.titleMedium)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (matchedPairs.size == items.size) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("You Win!", style = MaterialTheme.typography.headlineLarge, color = SurgicalGreen)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) { Text("Finish") }
                }
            }
        } else if (timeLeft == 0) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Time's Up!", style = MaterialTheme.typography.headlineLarge, color = AlertRed)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("Back") }
                }
            }
        } else {
            val scope = rememberCoroutineScope()
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(padding).fillMaxSize()
            ) {
                items(cards) { card ->
                    val isSelected = selectedFirst == card
                    val isMatched = matchedPairs.contains(card.pairId)

                    // Spring scaling card feedback (Idea 92)
                    val cardScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "cardScale"
                    )
                    
                    Box(
                        modifier = Modifier
                            .aspectRatio(1.5f)
                            .graphicsLayer {
                                scaleX = cardScale
                                scaleY = cardScale
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when {
                                    isMatched -> SurgicalGreen.copy(alpha = 0.12f)
                                    isSelected -> NeonCyan.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = when {
                                    isMatched -> SurgicalGreen.copy(alpha = 0.45f)
                                    isSelected -> NeonCyan
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(enabled = !isMatched && !isSelected) {
                                scope.launch {
                                    if (selectedFirst == null) {
                                        selectedFirst = card
                                        com.example.ui.util.SoundSynthesizer.playSynthesizedSound(com.example.ui.util.SoundSynthesizer.SoundType.CLICK)
                                    } else {
                                        if (selectedFirst!!.pairId == card.pairId) {
                                            matchedPairs = matchedPairs + card.pairId
                                            score += 10
                                            progressViewModel.addPoints(10)
                                            progressViewModel.incrementCardiology(0.015f)
                                            progressViewModel.incrementGeneralCare(0.01f)
                                            com.example.ui.util.SoundSynthesizer.playSynthesizedSound(com.example.ui.util.SoundSynthesizer.SoundType.SUCCESS)
                                        } else {
                                            score = (score - 2).coerceAtLeast(0)
                                            com.example.ui.util.SoundSynthesizer.playSynthesizedSound(com.example.ui.util.SoundSynthesizer.SoundType.SWOOSH)
                                        }
                                        selectedFirst = null
                                    }
                                }
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = card.text,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = when {
                                    isMatched -> SurgicalGreen
                                    isSelected -> NeonCyan
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (isMatched) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "✓ MATCHED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SurgicalGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
