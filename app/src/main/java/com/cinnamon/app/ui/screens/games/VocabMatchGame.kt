package com.cinnamon.app.ui.screens.games

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.cinnamon.app.ui.feedback.LocalCinnamonFeedbackPreferences
import com.cinnamon.app.ui.theme.NeonCyan
import com.cinnamon.app.ui.theme.SurgicalGreen
import com.cinnamon.app.ui.theme.AlertRed
import com.cinnamon.app.viewmodel.UserProgressViewModel
import kotlinx.coroutines.delay
import java.util.UUID

data class VocabCard(val id: Int, val text: String, val pairId: Int)

private const val ROUND_PAIR_COUNT = 4
private const val ROUND_DURATION_SECONDS = 90

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabMatchGame(
    onBack: () -> Unit,
    progressViewModel: UserProgressViewModel
) {
    val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { com.cinnamon.app.domain.repository.LexiconRepository.getInstance(context) }

    val fallback = listOf(
        Pair("tamponade", "fluid squeezing the heart"),
        Pair("orthopnea", "breathless lying flat"),
        Pair("insidious", "creeping in slowly"),
        Pair("equivocal", "neither clearly yes nor no")
    )
    var items by remember { mutableStateOf(fallback) }
    var cards by remember {
        mutableStateOf(
            fallback.flatMapIndexed { index, pair ->
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
    var timeLeft by remember { mutableIntStateOf(ROUND_DURATION_SECONDS) }
    var feedbackAnnouncement by remember { mutableStateOf("") }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isForeground by remember { mutableStateOf(true) }
    val practiceSessionKey = rememberSaveable { UUID.randomUUID().toString() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isForeground = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val pool = repository.lexicon.randomForGame("all", ROUND_PAIR_COUNT)
            .filter { it.plain.isNotBlank() }
            .map { it.term to it.plain }
        if (pool.size == ROUND_PAIR_COUNT) {
            items = pool
            cards = pool.flatMapIndexed { index, pair ->
                listOf(
                    VocabCard(index * 2, pair.first, index),
                    VocabCard(index * 2 + 1, pair.second, index)
                )
            }.shuffled()
            selectedFirst = null
            matchedPairs = emptySet()
            score = 0
            feedbackAnnouncement = ""
        }
    }

    LaunchedEffect(timeLeft, matchedPairs.size, isForeground) {
        if (isForeground && timeLeft > 0 && matchedPairs.size < items.size) {
            delay(1000)
            timeLeft--
        } else if (matchedPairs.size == items.size && timeLeft > 0) {
            progressViewModel.recordPracticeSession(
                subjectType = "vocabulary_match",
                subjectId = items.map { it.first.lowercase() }.sorted().joinToString(separator = "|"),
                occurrenceKey = practiceSessionKey,
                completedItemCount = items.size
            )
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
                    Text("Time: " + (if (timeLeft >= 0) timeLeft else 0) + "s", modifier = Modifier.padding(end = 16.dp), style = MaterialTheme.typography.titleMedium, color = if(timeLeft < 10) AlertRed else NeonCyan)
                    Text(matchedPairs.size.toString() + "/" + items.size, modifier = Modifier.padding(end = 16.dp), style = MaterialTheme.typography.titleMedium)
                    Text("Score: " + score, modifier = Modifier.padding(end = 16.dp), style = MaterialTheme.typography.titleMedium)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (matchedPairs.size == items.size) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                ) {
                    Text("Vocabulary Match Completed", style = MaterialTheme.typography.headlineLarge, color = SurgicalGreen)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "All " + items.size + " pairs matched. Saving one verified practice completion.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) { Text("Finish") }
                }
            }
        } else if (timeLeft == 0) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                 Column(
                     horizontalAlignment = Alignment.CenterHorizontally,
                     modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                 ) {
                    Text("Time Expired", style = MaterialTheme.typography.headlineLarge, color = AlertRed)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Feel free to try this round again at your own pace.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onBack) { Text("Back") }
                        Button(
                            onClick = {
                                cards = items.flatMapIndexed { index, pair ->
                                    listOf(
                                        VocabCard(index * 2, pair.first, index),
                                        VocabCard(index * 2 + 1, pair.second, index)
                                    )
                                }.shuffled()
                                selectedFirst = null
                                matchedPairs = emptySet()
                                score = 0
                                feedbackAnnouncement = ""
                                timeLeft = ROUND_DURATION_SECONDS
                            }
                        ) { Text("Try Again") }
                    }
                }
            }
        } else {
            val scope = rememberCoroutineScope()
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (feedbackAnnouncement.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    ) {
                        Text(
                            text = feedbackAnnouncement,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            color = when (feedbackAnnouncement) {
                                "Matched." -> SurgicalGreen
                                "Selected." -> NeonCyan
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                items(cards) { card ->
                    val isSelected = selectedFirst == card
                    val isMatched = matchedPairs.contains(card.pairId)

                    // Spring scaling card feedback (Idea 92)
                    val cardScale by animateFloatAsState(
                        targetValue = if (isSelected && !reduceMotion) 1.05f else 1f,
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
                            .semantics {
                                selected = isSelected
                                when {
                                    isMatched -> stateDescription = "Matched."
                                    isSelected -> stateDescription = "Selected."
                                }
                            }
                            .clickable(enabled = !isMatched && !isSelected) {
                                if (selectedFirst == null) {
                                    selectedFirst = card
                                    feedbackAnnouncement = "Selected."
                                    scope.launch {
                                        com.cinnamon.app.ui.util.SoundSynthesizer.playSynthesizedSound(com.cinnamon.app.ui.util.SoundSynthesizer.SoundType.CLICK)
                                    }
                                } else {
                                    val isMatch = selectedFirst!!.pairId == card.pairId
                                    selectedFirst = null
                                    if (isMatch) {
                                        matchedPairs = matchedPairs + card.pairId
                                        score += 10
                                        feedbackAnnouncement = "Matched."
                                        scope.launch {
                                            com.cinnamon.app.ui.util.SoundSynthesizer.playSynthesizedSound(com.cinnamon.app.ui.util.SoundSynthesizer.SoundType.SUCCESS)
                                        }
                                    } else {
                                        score = (score - 2).coerceAtLeast(0)
                                        feedbackAnnouncement = "Not a match. Select another card."
                                        scope.launch {
                                            com.cinnamon.app.ui.util.SoundSynthesizer.playSynthesizedSound(com.cinnamon.app.ui.util.SoundSynthesizer.SoundType.SWOOSH)
                                        }
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
}
