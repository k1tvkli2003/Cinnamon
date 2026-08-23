package com.cinnamon.app.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cinnamon.app.data.local.Confusable
import com.cinnamon.app.domain.repository.LexiconRepository
import com.cinnamon.app.ui.theme.AlertRed
import com.cinnamon.app.ui.theme.NeonCyan
import com.cinnamon.app.ui.theme.SurgicalGreen
import com.cinnamon.app.viewmodel.UserProgressViewModel
import kotlinx.coroutines.delay
import java.util.UUID

private const val ROUND_PAIR_COUNT = 4
private const val ROUND_DURATION_SECONDS = 90

/**
 * A first-choice clinical language game. It deliberately makes the distinction memorable instead
 * of rewarding frantic matching: a pair becomes durable only when the learner returns tomorrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabMatchGame(
    onBack: () -> Unit,
    progressViewModel: UserProgressViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { LexiconRepository.getInstance(context) }
    val fallback = remember {
        listOf(
            Confusable(a = "palpation", b = "palpitation", howToTell = "One is an examination; the other is a heartbeat sensation.", exampleA = "Palpation found tenderness.", exampleB = "She reported palpitations."),
            Confusable(a = "affect", b = "effect", howToTell = "Affect is usually the verb; effect is usually the result.", exampleA = "The treatment may affect sleep.", exampleB = "The effect was immediate."),
            Confusable(a = "stationary", b = "stationery", howToTell = "Stationary means still; stationery means writing supplies.", exampleA = "The patient remained stationary.", exampleB = "The chart used hospital stationery."),
            Confusable(a = "complement", b = "compliment", howToTell = "Complement completes; compliment praises.", exampleA = "The brace complements therapy.", exampleB = "The consultant gave a compliment.")
        )
    }
    var pairs by remember { mutableStateOf(fallback) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var correctCount by rememberSaveable { mutableIntStateOf(0) }
    var timeLeft by rememberSaveable { mutableIntStateOf(ROUND_DURATION_SECONDS) }
    var selectedTerm by rememberSaveable { mutableStateOf<String?>(null) }
    var feedback by rememberSaveable { mutableStateOf("") }
    val sessionKey = rememberSaveable { UUID.randomUUID().toString() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val seeded = repository.learn.randomConfusables(ROUND_PAIR_COUNT)
        if (seeded.size == ROUND_PAIR_COUNT) pairs = seeded
    }
    LaunchedEffect(index, selectedTerm, timeLeft) {
        if (index < pairs.size && selectedTerm == null && timeLeft > 0) {
            delay(1_000)
            timeLeft -= 1
        }
    }

    val finished = index >= pairs.size
    val pair = pairs.getOrNull(index)
    val answerIsA = index % 2 == 0
    val prompt = pair?.let { if (answerIsA) it.exampleA else it.exampleB }.orEmpty()
    val correctAnswer = pair?.let { if (answerIsA) it.a else it.b }.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confusable Precision") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    Text("${(timeLeft.coerceAtLeast(0))}s", color = if (timeLeft < 10) AlertRed else NeonCyan, modifier = Modifier.padding(end = 10.dp))
                    Text("${index.coerceAtMost(pairs.size)}/${pairs.size}", modifier = Modifier.padding(end = 16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                finished -> CompletionCard(correctCount, pairs.size, onBack, progressViewModel, pairs, sessionKey)
                timeLeft <= 0 -> TimeExpiredCard(onBack)
                pair != null -> PairRound(
                    pair = pair,
                    prompt = prompt,
                    correctAnswer = correctAnswer,
                    selectedTerm = selectedTerm,
                    feedback = feedback,
                    onChoose = { chosen ->
                        if (selectedTerm != null) return@PairRound
                        selectedTerm = chosen
                        val correct = chosen == correctAnswer
                        if (correct) {
                            correctCount += 1
                            feedback = "دقیق بود. ${pair.howToTell}"
                            if (pair.id > 0L) progressViewModel.recordConfusablePairSuccess(pair.id.toString(), "$sessionKey:${pair.id}")
                        } else {
                            feedback = "این بار نه. ${pair.howToTell}"
                        }
                    },
                    onContinue = {
                        index += 1
                        selectedTerm = null
                        feedback = ""
                    }
                )
            }
        }
    }
}

@Composable
private fun PairRound(
    pair: Confusable,
    prompt: String,
    correctAnswer: String,
    selectedTerm: String?,
    feedback: String,
    onChoose: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("PRECISION PAIR", color = NeonCyan, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("کدام واژه دقیقاً با این جمله جور است؟", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Text("“$prompt”", modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrecisionAnswer(pair.a, selectedTerm, correctAnswer, Modifier.weight(1f)) { onChoose(pair.a) }
            PrecisionAnswer(pair.b, selectedTerm, correctAnswer, Modifier.weight(1f)) { onChoose(pair.b) }
        }
        Spacer(Modifier.height(14.dp))
        if (selectedTerm == null) {
            Text("فقط انتخاب اول ثبت می‌شود؛ حدس زدن پیاپی امتیاز نمی‌سازد.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        } else {
            Surface(
                color = if (selectedTerm == correctAnswer) SurgicalGreen.copy(alpha = .16f) else AlertRed.copy(alpha = .14f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
            ) { Text(feedback, Modifier.padding(16.dp), textAlign = TextAlign.Center) }
            Spacer(Modifier.height(14.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 14.dp)) { Text("جفت بعدی") }
        }
    }
}

@Composable
private fun PrecisionAnswer(term: String, selectedTerm: String?, correctAnswer: String, modifier: Modifier, onClick: () -> Unit) {
    val selected = selectedTerm == term
    val color = when {
        selected && term == correctAnswer -> SurgicalGreen
        selected -> AlertRed
        else -> NeonCyan
    }
    OutlinedButton(
        onClick = onClick,
        enabled = selectedTerm == null,
        modifier = modifier.height(78.dp).border(1.dp, color.copy(alpha = .45f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) { Text(term, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}

@Composable
private fun CompletionCard(
    correct: Int,
    total: Int,
    onBack: () -> Unit,
    progressViewModel: UserProgressViewModel,
    pairs: List<Confusable>,
    sessionKey: String
) {
    LaunchedEffect(sessionKey) {
        if (correct >= 3) progressViewModel.recordPracticeSession(
            subjectType = "confusable_precision",
            subjectId = pairs.map { pair ->
                if (pair.id > 0L) pair.id.toString() else pair.a
            }.sorted().joinToString("|"),
            occurrenceKey = sessionKey,
            completedItemCount = correct
        )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
        Text("$correct از $total تشخیص دقیق", style = MaterialTheme.typography.headlineLarge, color = SurgicalGreen, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text("فرق را امروز دیدی. اگر فردا دوباره درست تشخیصش بدهی، Cinnamon آن را دانشِ ماندگار حساب می‌کند.", textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) { Text("بازگشت") }
    }
}

@Composable
private fun TimeExpiredCard(onBack: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
        Text("زمان این دور تمام شد", style = MaterialTheme.typography.headlineMedium, color = AlertRed)
        Spacer(Modifier.height(12.dp))
        Text("دقت مهم‌تر از سرعت است؛ یک دور تازه را هر وقت آماده بودی شروع کن.", textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Button(onClick = onBack) { Text("بازگشت") }
    }
}
