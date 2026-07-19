package com.cinnamon.app.ui.screens.fluency

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.cinnamon.app.ui.feedback.LocalCinnamonFeedbackPreferences
import com.cinnamon.app.ui.theme.*
import com.cinnamon.app.viewmodel.UserProgressViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeFluencyPlaygroundScreen(
    onBack: () -> Unit,
    progressViewModel: UserProgressViewModel,
    initialTab: String = "Tone Slider"
) {
    val haptic = LocalHapticFeedback.current

    val tabs = listOf(
        "Tone Slider",
        "Shadowing & Rhythm",
        "Literal Translation",
        "Synonym Escalator",
        "Phrasal Verbs",
        "Text Expander",
        "Between The Lines",
        "Professional Email",
        "Spoken Rhythm",
        "Contextual Idioms",
        "C2 Debate Club",
        "Storytelling Mode",
        "Conference Sandbox",
        "Slang Decoder",
        "Vocab Vault",
        "Grammar Deep-Dive",
        "Daily C2 Word"
    )
    var selectedTab by remember {
        mutableStateOf(initialTab.takeIf { it in tabs } ?: tabs.first())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Native Fluency Sandbox", color = NeonCyan, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
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
                    NativeFluencyTabs(
                        tabs = tabs,
                        selectedTab = selectedTab,
                        compact = true,
                        onSelect = selectTab,
                        modifier = Modifier.fillMaxWidth()
                    )
                    NativeFluencyWorkspace(
                        selectedTab = selectedTab,
                        progressViewModel = progressViewModel,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    NativeFluencyTabs(
                        tabs = tabs,
                        selectedTab = selectedTab,
                        compact = false,
                        onSelect = selectTab,
                        modifier = Modifier
                            .width(135.dp)
                            .fillMaxHeight()
                    )
                    NativeFluencyWorkspace(
                        selectedTab = selectedTab,
                        progressViewModel = progressViewModel,
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
private fun NativeFluencyTabs(
    tabs: List<String>,
    selectedTab: String,
    compact: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (compact) {
        LazyRow(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs, key = { it }) { tab ->
                val isActive = selectedTab == tab
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isActive) NeonCyan.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surface,
                    border = if (isActive) BorderStroke(1.dp, NeonCyan.copy(alpha = 0.72f)) else null,
                    modifier = Modifier.clickable { onSelect(tab) }
                ) {
                    Text(
                        text = tab,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                        color = if (isActive) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            tabs.forEach { tab ->
                val isActive = selectedTab == tab
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(tab) }
                        .background(if (isActive) MaterialTheme.colorScheme.background else Color.Transparent)
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = tab,
                        color = if (isActive) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun NativeFluencyWorkspace(
    selectedTab: String,
    progressViewModel: UserProgressViewModel,
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
            label = "fluencyTabTransition"
        ) { targetTab ->
            when (targetTab) {
                "Tone Slider" -> ToneSliderComponent()
                "Shadowing & Rhythm" -> ShadowingRhythmComponent()
                "Literal Translation" -> LiteralTranslationComponent()
                "Synonym Escalator" -> SynonymEscalatorComponent(progressViewModel)
                "Phrasal Verbs" -> PhrasalVerbsComponent(progressViewModel)
                "Text Expander" -> TextExpanderComponent(progressViewModel)
                "Between The Lines" -> BetweenTheLinesComponent(progressViewModel)
                "Professional Email" -> ProfessionalEmailDrafterComponent(progressViewModel)
                "Spoken Rhythm" -> InteractivePodcastComponent()
                "Contextual Idioms" -> ContextualIdiomsComponent(progressViewModel)
                "C2 Debate Club" -> DebateClubComponent(progressViewModel)
                "Storytelling Mode" -> StorytellingComponent(progressViewModel)
                "Conference Sandbox" -> ConferenceSandboxComponent(progressViewModel)
                "Slang Decoder" -> SlangDecoderComponent(progressViewModel)
                "Vocab Vault" -> VocabVaultComponent()
                "Grammar Deep-Dive" -> GrammarDeepDiveComponent(progressViewModel)
                "Daily C2 Word" -> DailyC2WordComponent(progressViewModel)
                else -> ToneSliderComponent()
            }
        }
    }
}

// ----------------------------------------------------
// 1. THE "MAKE IT NATIVE" TONE SLIDER (Idea 22)
// ----------------------------------------------------
@Composable
fun ToneSliderComponent() {
    var textInput by remember { mutableStateOf("I think we should do this project because it will save money.") }
    var sliderValue by remember { mutableStateOf(1f) } // 0 = Street Slang, 1 = Casual, 2 = Professional, 3 = Academic

    val sliderLabels = listOf("Street Slang", "Casual", "Professional", "Academic")

    // Authored comparison variants for a small set of fixed prompts. This is not a general transformer.
    val outputSentence = remember(textInput, sliderValue) {
        val cleanInput = textInput.trim()
        if (cleanInput.isBlank()) ""
        else {
            val idx = sliderValue.toInt()
            when (idx) {
                0 -> { // Street Slang
                    if (cleanInput.contains("project")) "Yo, we gotta run this gig. It's gonna save us mad cash, period."
                    else if (cleanInput.lowercase().contains("tired")) "Man, I'm beat. I'm literally running on fumes."
                    else "Look, we gotta lock this down, it's gonna keep our wallets fat."
                }
                1 -> { // Casual
                    if (cleanInput.contains("project")) "I reckon we should go ahead with this project. It'll definitely save some bucks."
                    else if (cleanInput.lowercase().contains("tired")) "I'm so exhausted, honestly. Need to crash."
                    else "I think it's a good call to do this; it'll keep things cheaper for us."
                }
                2 -> { // Professional
                    if (cleanInput.contains("project")) "I highly recommend greenlighting this initiative; it will significantly optimize our budgetary allocations."
                    else if (cleanInput.lowercase().contains("tired")) "I am feeling quite fatigued and will take a brief recess to recuperate."
                    else "We ought to proceed with this course of action, as it offers a highly competitive return on investment."
                }
                else -> { // Academic
                    if (cleanInput.contains("project")) "It is hypothesized that implementing this empirical paradigm will maximize fiscal capitalization while curtailing unnecessary expenditures."
                    else if (cleanInput.lowercase().contains("tired")) "The subject exhibits advanced physiological and neurocognitive exhaustion, necessitating immediate absolute rest."
                    else "This pragmatic methodology facilitates optimal resource preservation, matching contemporary microeconomic paradigms."
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("REGISTER COMPARISON", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            "Choose an authored sentence below, then compare its pre-written informal and formal variants.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )

        OutlinedTextField(
            value = textInput,
            onValueChange = {},
            readOnly = true,
            label = { Text("Authored base sentence") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = sliderLabels[sliderValue.toInt()].uppercase(),
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..3f,
                    steps = 2,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }

        Text("AUTHORED REGISTER EXAMPLE:", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                Text(
                    text = outputSentence,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }

        // Action Quick Templates
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { textInput = "I am very tired." },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                modifier = Modifier.weight(1f)
            ) {
                Text("Template: Tired", color = Color.White, fontSize = 11.sp)
            }
            Button(
                onClick = { textInput = "We should start the meeting now." },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                modifier = Modifier.weight(1f)
            ) {
                Text("Template: Meeting", color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 2. GUIDED SHADOWING REHEARSAL
// ----------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShadowingRhythmComponent() {
    var isRehearsing by remember { mutableStateOf(false) }
    var isDone by remember { mutableStateOf(false) }
    var rehearsalStep by remember { mutableIntStateOf(0) }
    var sentenceIndex by remember { mutableIntStateOf(0) }
    var stressChecked by remember { mutableStateOf(false) }
    var flowChecked by remember { mutableStateOf(false) }
    var endingChecked by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion

    val practiceSentences = listOf(
        "We highly recommend greenlighting this clinical trial.",
        "The patient is experiencing acute chest pain with dyspnea.",
        "I request immediate ICU status consult due to septic shock indications."
    )
    val currentSentence = practiceSentences[sentenceIndex % practiceSentences.size]

    val stressGuide = when (sentenceIndex % 3) {
        0 -> listOf(Pair("We", false), Pair("HIGH-ly", true), Pair("recommend", false), Pair("green-LIGHT-ing", true), Pair("this", false), Pair("CLIN-i-cal", true), Pair("TRI-al", true))
        1 -> listOf(Pair("The", false), Pair("PA-tient", true), Pair("is", false), Pair("ex-PE-ri-enc-ing", true), Pair("a-CUTE", true), Pair("chest", false), Pair("PAIN", true), Pair("with", false), Pair("DYSP-ne-a", true))
        else -> listOf(Pair("I", false), Pair("re-QUEST", true), Pair("im-ME-di-ate", true), Pair("I-C-U", true), Pair("con-SULT", true), Pair("due", false), Pair("to", false), Pair("shock", false), Pair("in-di-CA-tions", true))
    }

    // Authored pacing anchors. These are a visual rehearsal guide, not microphone data.
    val referencePoints = listOf(15f, 45f, 90f, 20f, 15f, 85f, 110f, 40f, 25f, 95f, 120f, 30f, 75f, 10f)

    LaunchedEffect(isRehearsing, sentenceIndex, reduceMotion) {
        if (isRehearsing) {
            isDone = false
            rehearsalStep = 0
            stressChecked = false
            flowChecked = false
            endingChecked = false
            if (reduceMotion) {
                rehearsalStep = referencePoints.size
            } else {
                referencePoints.indices.forEach { index ->
                    delay(420)
                    rehearsalStep = index + 1
                    if (index == 0 || index == referencePoints.lastIndex) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            }
            isRehearsing = false
            isDone = true
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
            Text("SHADOWING REHEARSAL", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)

            TextButton(
                onClick = {
                    sentenceIndex++
                    isDone = false
                    rehearsalStep = 0
                }
            ) {
                Text("Next Sentence ➔", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Text(
            "Speak aloud as the visual guide glides across the key stress points. This private practice does not record, analyze, or score your voice.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )

        // Native Card Syllables
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("AUTHORED PHRASE TO REHEARSE:", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stressGuide.forEach { (word, isStressed) ->
                        Text(
                            text = word,
                            color = if (isStressed) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isStressed) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .background(
                                    if (isStressed) NeonCyan.copy(alpha = 0.1f) else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Text(
            "VISUAL PACING GUIDE",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("TARGET RHYTHM", color = Color.White, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(64.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val spacing = size.width / (referencePoints.size + 1)
                        referencePoints.forEachIndexed { index, heightVal ->
                            val x = (index + 1) * spacing
                            drawLine(
                                color = if (index < rehearsalStep) SurgicalGreen else NeonCyan.copy(alpha = 0.35f),
                                start = Offset(x, size.height / 2f - heightVal / 2f),
                                end = Offset(x, size.height / 2f + heightVal / 2f),
                                strokeWidth = if (index < rehearsalStep) 8f else 5f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { rehearsalStep.toFloat() / referencePoints.size.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                    color = SurgicalGreen,
                    trackColor = Color.DarkGray
                )
            }
        }

        Button(
            onClick = { isRehearsing = true },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRehearsing
        ) {
            Icon(
                if (isRehearsing) Icons.Default.HourglassTop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isRehearsing) "REHEARSING..." else "START REHEARSAL", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        if (isDone) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, SurgicalGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "PRIVATE SELF-CHECK",
                        color = SurgicalGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        "How did it feel? Mark what you noticed - no recording, algorithms, or automated score.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = stressChecked,
                            onClick = { stressChecked = !stressChecked },
                            label = { Text("Hit the stress points") }
                        )
                        FilterChip(
                            selected = flowChecked,
                            onClick = { flowChecked = !flowChecked },
                            label = { Text("Kept a continuous flow") }
                        )
                        FilterChip(
                            selected = endingChecked,
                            onClick = { endingChecked = !endingChecked },
                            label = { Text("Delivered a clear ending") }
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 3. LITERAL TRANSLATION DETECTOR & "WHY?" DEEP-DIVE (Ideas 33, 26)
// ----------------------------------------------------
@Composable
fun LiteralTranslationComponent() {
    val items = listOf(
        Triple(
            "جای شما خالی",
            "Your place was empty (LITERAL ERROR)",
            "You were greatly missed! // Linguistic 'Why?': English cultures emphasize the feeling of missing your emotional presence ('you were missed') rather than physical empty seats."
        ),
        Triple(
            "روی چشمم",
            "On my eyes (LITERAL ERROR)",
            "My pleasure! / With pleasure! / Happy to help! // Linguistic 'Why?': Persian expressions use high-register physical alignment ('eyes/چشم') to show intense respect, whereas native English implies conversational service-readiness."
        ),
        Triple(
            "خسته نباشید",
            "Don't be tired (LITERAL ERROR)",
            "Good work today! / Keep up the great work! // Linguistic 'Why?': English-speaking work ethics focus on positive outcome acknowledgement rather than pointing out fatigue states directly."
        )
    )

    var searchVal by remember { mutableStateOf("") }
    val filtered = items.filter { it.first.contains(searchVal) || it.third.lowercase().contains(searchVal.lowercase()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("LITERAL TRANSLATION CLEANER & 'WHY?' DEEP-DIVE", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            "Discover common idioms and Persian metaphors translated literally into broken English, and master why native flow requires unique expressions.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )

        OutlinedTextField(
            value = searchVal,
            onValueChange = { searchVal = it },
            placeholder = { Text("Search local expressions e.g. چشم", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            filtered.forEach { (local, literal, naturalPlusWhy) ->
                val splitted = naturalPlusWhy.split(" // ")
                val natural = splitted[0]
                val why = splitted.getOrNull(1) ?: ""

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(local, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Badge(containerColor = AlertRed.copy(alpha = 0.2f)) {
                                Text("LITERAL BLUNDER DECODER", color = AlertRed, modifier = Modifier.padding(4.dp), fontSize = 10.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            literal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                            fontSize = 12.sp,
                            style = TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Reference phrasing: \"$natural\"", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        if (why.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = why,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 4. THE LIVE SYNONYM ESCALATOR (Idea 40)
// ----------------------------------------------------
@Composable
fun SynonymEscalatorComponent(viewModel: UserProgressViewModel) {
    var selectedWordIndex by remember { mutableIntStateOf(0) }
    var level by remember { mutableFloatStateOf(0f) } // 0 = Generic, 1 = Rich, 2 = Elegant, 3 = C2 Masterpiece
    val haptic = LocalHapticFeedback.current

    val wordSets = listOf(
        listOf("bad", "substandard", "abysmal", "atrocious"),
        listOf("good", "competent", "exemplary", "unimpeachable"),
        listOf("important", "pivotal", "consequential", "indispensable"),
        listOf("very happy", "delighted", "exuberant", "ectatic")
    )

    val labels = listOf(
        "Stage 1: Everyday",
        "Stage 2: More specific",
        "Stage 3: Formal",
        "Stage 4: High-register"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SYNONYM INTENSITY", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Select a generic or basic word, and scale it up to magnificent collegiate C2 levels using the vertical escalator:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

        // Basic Word selector row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            wordSets.forEachIndexed { idx, set ->
                val keyword = set[0]
                val isActive = selectedWordIndex == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedWordIndex = idx
                            level = 0f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .background(if (isActive) NeonCyan else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        keyword,
                        color = if (isActive) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Vertical Stairs / Escalator Graphic
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(labels[level.toInt()].uppercase(), color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(14.dp))

                // Escalator Slider
                Slider(
                    value = level,
                    onValueChange = { level = it },
                    onValueChangeFinished = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    valueRange = 0f..3f,
                    steps = 2,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                val escalatedWord = wordSets[selectedWordIndex][level.toInt()]

                Text(
                    text = escalatedWord,
                    color = when (level.toInt()) {
                        0 -> MaterialTheme.colorScheme.onSurfaceVariant
                        1 -> MaterialTheme.colorScheme.secondary
                        2 -> NeonCyan
                        else -> Color(0xFFFFD700)
                    },
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
            }
        }

        if (level >= 2f) {
            Text(
                "Try the high-register option in your own draft. This comparison tool does not grant XP or certify fluency.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

// ----------------------------------------------------
// 5. PHRASAL VERBS MASTERY GAME (Idea 27)
// ----------------------------------------------------
@Composable
fun PhrasalVerbsComponent(viewModel: UserProgressViewModel) {
    val questions = listOf(
        Triple("The patient lost consciousness suddenly during examination. He...", "passed out", "pulled through"),
        Triple("The surgical team managed to survive the high-risk crisis. They...", "pulled through", "brought up"),
        Triple("The clinic needs to introduce the patient record topic during rounds.", "bring up", "pass out"),
        Triple("We should review the ECG reports carefully before consulting.", "go over", "pull through")
    )
    var currentIdx by remember { mutableIntStateOf(0) }
    var userAns by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("PHRASAL VERB MEMORY DRILL", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)

        if (currentIdx < questions.size) {
            val q = questions[currentIdx]
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CLINICAL CASE PHRASE:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(q.first, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(q.second, q.third).shuffled().forEach { opt ->
                    Button(
                        onClick = {
                            userAns = opt
                            if (opt == q.second) {
                                feedback = "Correct. Compare the phrasal verb with the case sentence before moving on."
                            } else {
                                feedback = "INCORRECT. The correct phrasal verb is '${q.second}'."
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.weight(1f),
                        enabled = feedback.isEmpty()
                    ) {
                        Text(opt, color = Color.White)
                    }
                }
            }

            if (feedback.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(feedback, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                currentIdx++
                                feedback = ""
                                userAns = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text("Next Case Rule", color = Color.Black)
                        }
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Phrasal-verb drill complete", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "You can replay this authored drill; it does not certify overall fluency.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { currentIdx = 0 }) {
                        Text("Replay Drill")
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 6. SMART TEXT EXPANSION PLAYGROUND (Idea 38)
// ----------------------------------------------------
@Composable
fun TextExpanderComponent(viewModel: UserProgressViewModel) {
    var rawInput by remember { mutableStateOf("Hungry. Eat?") }
    var expandedOutput by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    val expansionDatabase = mapOf(
        "Hungry. Eat?" to "I find myself quite famished; shall we venture out to secure some culinary sustenance?",
        "Call doctor." to "I must urge you to contact the senior clinical director immediately in order to secure a consultation.",
        "Need help." to "I would be immensely grateful if you could assist me with this challenging technical problem."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SMART TEXT EXPANSION ENGINE", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Type raw colloquial shorthand keys, and our engine dynamically expands it into complex, beautifully styled C2 scholarly English prose:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

        OutlinedTextField(
            value = rawInput,
            onValueChange = { rawInput = it },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        )

        Button(
            onClick = {
                expandedOutput = expansionDatabase[rawInput.trim()]
                    ?: "I would like to kindly suggest that we proceed with '${rawInput}' in a highly polished and professional manner."
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            modifier = Modifier.align(Alignment.End),
            enabled = rawInput.isNotBlank()
        ) {
            Text("Expand into C2 English", color = Color.Black)
        }

        if (expandedOutput.isNotEmpty()) {
            Text(
                "C2 EXPANDED SCHOLARLY PROSE:",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = expandedOutput,
                    modifier = Modifier.padding(14.dp),
                    color = NeonCyan,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }

        // Templates Row
        Text("Try shorthand templates:", color = MaterialTheme.colorScheme.onBackground, fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Hungry. Eat?", "Call doctor.", "Need help.").forEach { t ->
                Button(
                    onClick = { rawInput = t; expandedOutput = "" },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text(t, color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

// ----------------------------------------------------
// 7. READ BETWEEN THE LINES (SARCASM & NUANCE - Idea 29)
// ----------------------------------------------------
@Composable
fun BetweenTheLinesComponent(viewModel: UserProgressViewModel) {
    val scenarios = listOf(
        Triple(
            "An attending email: 'As I am sure you are aware, we have standard clinic schedules starting at 7 AM.'",
            "This is helpful scheduling information",
            "This is a passive-aggressive reminder that you were late (CORRECT!)"
        ),
        Triple(
            "A colleague says: 'Oh, you completed that chart perfectly. I only had to fix three spelling entries.'",
            "They are being sarcastic about your accuracy (CORRECT!)",
            "They are genuinely expressing high praise"
        )
    )

    var currentIdx by remember { mutableIntStateOf(0) }
    var reviewText by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("READ BETWEEN THE LINES", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Decode passive-aggressiveness, underlying hints, and deep professional sarcasm in Anglo-Saxon communications:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

        if (currentIdx < scenarios.size) {
            val s = scenarios[currentIdx]
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = s.first,
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            listOf(s.second, s.third).forEach { option ->
                Button(
                    onClick = {
                        if (option.contains("CORRECT")) {
                            reviewText = "That reading matches the intended social nuance in this example."
                        } else {
                            reviewText = "MISS: Pay attention to the passive wording structure typical of complex Anglo-Saxon feedback."
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = reviewText.isEmpty()
                ) {
                    Text(option.replace(" (CORRECT!)", ""), color = Color.White, textAlign = TextAlign.Center)
                }
            }

            if (reviewText.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(reviewText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                currentIdx++
                                reviewText = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text("Next Social Context", color = Color.Black)
                        }
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nuance drill complete", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "This result reflects this authored drill only, not a global mastery score.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { currentIdx = 0 }) {
                        Text("Replay drill")
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 8. PROFESSIONAL NETWORKING EMAIL DRAFTER (Idea 30)
// ----------------------------------------------------
@Composable
fun ProfessionalEmailDrafterComponent(viewModel: UserProgressViewModel) {
    var emailText by remember { mutableStateOf("Subject: I want a residency spot.\n\nDear Director,\nI want you to give me a rotation check. Thanks.") }
    var feedbackReport by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("PROFESSIONAL EMAIL COACH", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Draft a formal inquiry to a residency director, and obtain a detailed critique on politeness formulas & dynamic alignment:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

        OutlinedTextField(
            value = emailText,
            onValueChange = { emailText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        )

        Button(
            onClick = {
                val e = emailText.lowercase()
                feedbackReport = if (e.contains("kindly") || e.contains("honor") || e.contains("looking forward")) {
                    "Local guidance: the draft includes a courteous signal. Re-read it for clarity, specificity, and an appropriate request."
                } else {
                    "CRITIQUE: Your email is too demanding/direct ('I want'). Use professional cushioning like 'I am writing to inquire about the possibility of...' to avoid coming off as over-assertive."
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            modifier = Modifier.align(Alignment.End),
            enabled = emailText.isNotBlank() && feedbackReport.isEmpty()
        ) {
            Text("Show local writing guidance", color = Color.Black)
        }

        if (feedbackReport.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(feedbackReport, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 9. SELF-GUIDED SPOKEN RHYTHM PRACTICE
// ----------------------------------------------------
@Composable
fun InteractivePodcastComponent() {
    var completedCheckpoints by remember { mutableStateOf(emptySet<Int>()) }
    val checkpoints = listOf(
        "Emphasize key medical terms",
        "Observe deliberate pauses",
        "Summarize the core message aloud"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SPOKEN RHYTHM PRACTICE", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            "Read the text aloud at your own pace to practice natural phrasing and emphasis.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "PRACTICE TEXT",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    "During the handoff, I’ll state the main concern first, pause for the supporting detail, and finish with one clear question.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }
        }

        Text(
            "SELF-CHECKPOINTS · ${completedCheckpoints.size}/${checkpoints.size}",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )

        checkpoints.forEachIndexed { index, label ->
            FilterChip(
                selected = index in completedCheckpoints,
                onClick = {
                    completedCheckpoints = if (index in completedCheckpoints) {
                        completedCheckpoints - index
                    } else {
                        completedCheckpoints + index
                    }
                },
                label = { Text(label) },
                leadingIcon = if (index in completedCheckpoints) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Surface(
            color = if (completedCheckpoints.size == checkpoints.size) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "This is a self-guided exercise for independent phrasing practice; your voice is not recorded, analyzed, or evaluated.",
                modifier = Modifier.padding(14.dp),
                color = if (completedCheckpoints.size == checkpoints.size) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

// ----------------------------------------------------
// 10. CONTEXTUAL IDIOM ENGINE
// ----------------------------------------------------
@Composable
fun ContextualIdiomsComponent(viewModel: UserProgressViewModel) {
    var response by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("IDIOM REFERENCE", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Review the meaning and typical context of this authored expression, then draft a direct reply.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Authored scene:", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("\"Alright team, let's not beat around the bush with this patient's prognosis. What's our next step?\"", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }

        OutlinedTextField(
            value = response,
            onValueChange = { response = it },
            placeholder = { Text("Reply directly without ignoring the idiom...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Button(
            onClick = {
                feedback = "Reference cue: ‘beat around the bush’ means avoiding the main point. Check whether your reply addresses it directly."
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SHOW IDIOM CUE", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        if (feedback.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(feedback, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 13.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 11. C2 DEBATE CLUB
// ----------------------------------------------------
@Composable
fun DebateClubComponent(viewModel: UserProgressViewModel) {
    var argument by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("PERSPECTIVES EXERCISE", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Draft a counterargument to one authored position, then self-check its claim, reason, and example.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, Color.DarkGray), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("THE RESOLUTION:", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("\"Universal Basic Income is fundamentally detrimental to societal work ethic and economic inflation.\"", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Your role: argue the opposing position that UBI can be beneficial.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }

        OutlinedTextField(
            value = argument,
            onValueChange = { argument = it },
            placeholder = { Text("Draft your C2 scholarly opening statement...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(160.dp),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Button(
            onClick = {
                resultText = "Draft saved in this session. Re-read it for a clear claim, supporting reason, and a concrete example; this screen does not score writing."
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SHOW SELF-CHECK", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        if (resultText.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary), modifier = Modifier.fillMaxWidth()) {
                Text(resultText, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 13.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 12. STORYTELLING MODE
// ----------------------------------------------------
@Composable
fun StorytellingComponent(viewModel: UserProgressViewModel) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("STORYTELLING / STAND-UP MODE", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Practice your pacing, timing, and narrative structure for casual English conversation.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, Color.DarkGray), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("PROMPT:", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("\"Tell a 2-minute funny story about a misunderstanding you had during your first week at the hospital.\"", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MicOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Audio recording and analysis are not available here. Rehearse aloud independently.", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 13. CONFERENCE PRESENTATION SANDBOX
// ----------------------------------------------------
@Composable
fun ConferenceSandboxComponent(viewModel: UserProgressViewModel) {
    var response by remember { mutableStateOf("") }
    var audienceQuestion by remember { mutableStateOf("Dr. Smith (Audience): \"How did you account for confounding variables in your retrospective cohort?\"") }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("CONFERENCE PRESENTATION SANDBOX", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Practice delivering oral presentations and fielding spontaneous Q&A.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, Color.DarkGray), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("SAMPLE AUDIENCE QUESTION:", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(audienceQuestion, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }

        OutlinedTextField(
            value = response,
            onValueChange = { response = it },
            placeholder = { Text("Draft your articulate response dodging or answering the question...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Button(
            onClick = {
                audienceQuestion = "Self-review cue: state what the data support, name any limitation, then answer the question directly."
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SHOW RESPONSE CUE", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// ----------------------------------------------------
// 14. POP-CULTURE & SLANG DECODER
// ----------------------------------------------------
@Composable
fun SlangDecoderComponent(viewModel: UserProgressViewModel) {
    var guess by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("POP-CULTURE & SLANG DECODER", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Learn modern metaphors derived from English sports, movies, and internet culture.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, Color.DarkGray), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("DECODE THIS PHRASE:", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("\"She really knocked it out of the park with that presentation.\"", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("(Origin: Baseball)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }

        OutlinedTextField(
            value = guess,
            onValueChange = { guess = it },
            placeholder = { Text("What does this mean in plain English?", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Button(onClick = { 
            feedback = "Reference: it means to do something exceptionally well. Compare this with your own explanation."
        }, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan), modifier = Modifier.fillMaxWidth()) {
            Text("SHOW MEANING CUE", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        
        if (feedback.isNotEmpty()) {
            Text(feedback, color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// ----------------------------------------------------
// 15. IN-CHAT VOCABULARY VAULT
// ----------------------------------------------------
@Composable
fun VocabVaultComponent() {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("VOCABULARY SAMPLER", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Review these bundled examples. This screen is not your saved-word history or an SRS queue.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        val words = listOf("Exacerbate" to "To make a problem, bad situation, or negative feeling worse.", "Ubiquitous" to "Present, appearing, or found everywhere.", "Malingering" to "Falsify or exaggerate physical or psychological symptoms for a secondary reward.")
        
        words.forEach { (word, def) ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(word, color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(def, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

// ----------------------------------------------------
// 16. ADVANCED "WHY?" GRAMMAR DEEP-DIVE
// ----------------------------------------------------
@Composable
fun GrammarDeepDiveComponent(viewModel: UserProgressViewModel) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("ADVANCED GRAMMAR DEEP-DIVE", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Understand the linguistic reasoning behind native phrasing.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, Color.DarkGray), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("YOUR PHRASE:", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("\"If I will have time, I will review the chart.\"", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                
                HorizontalDivider(color = Color.DarkGray)
                
                Text("NATIVE CORRECTION:", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("\"If I have time, I will review the chart.\"", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("THE \"WHY?\"", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "This is a First Conditional sentence. Native speakers do not use the future tense ('will') in the 'if' clause. The 'if' clause establishes a possible present condition that leads to a future result.",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Text(
            "The explanation is shown above for self-review; this screen does not evaluate an answer.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// ----------------------------------------------------
// 17. DAILY C2 WORD INJECTION
// ----------------------------------------------------
@Composable
fun DailyC2WordComponent(viewModel: UserProgressViewModel) {
    var response by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("C2 WORD PRACTICE", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Use this bundled advanced word in a sentence, then reveal a self-check cue.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, NeonCyan), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TODAY's WORD:", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("Intransigent (adjective)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Definition: Unwilling or refusing to change one's views or to agree about something.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        OutlinedTextField(
            value = response,
            onValueChange = { response = it },
            placeholder = { Text("Write a sentence using 'intransigent'...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Button(onClick = { 
            feedback = "Self-check: confirm the sentence uses ‘intransigent’ as an adjective and makes its meaning clear from context."
        }, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan), modifier = Modifier.fillMaxWidth()) {
            Text("SHOW SELF-CHECK", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        
        if (feedback.isNotEmpty()) {
            Text(feedback, color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
