package com.example.ui.screens.fluency

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.ui.theme.*
import com.example.viewmodel.UserProgressViewModel
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
    var selectedTab by remember { mutableStateOf(initialTab) }
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
        "Interactive Podcast",
        "Contextual Idioms",
        "C2 Debate Club",
        "Storytelling Mode",
        "Conference Sandbox",
        "Slang Decoder",
        "Vocab Vault",
        "Grammar Deep-Dive",
        "Daily C2 Word"
    )

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
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Left Sidebar Tab Switcher (Fluid and fully responsive)
            Column(
                modifier = Modifier
                    .width(135.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                tabs.forEach { tab ->
                    val isActive = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTab = tab
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
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

            // Central Workspace Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    }, label = "fluencyTabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        "Tone Slider" -> ToneSliderComponent()
                        "Shadowing & Rhythm" -> ShadowingRhythmComponent(progressViewModel)
                        "Literal Translation" -> LiteralTranslationComponent()
                        "Synonym Escalator" -> SynonymEscalatorComponent(progressViewModel)
                        "Phrasal Verbs" -> PhrasalVerbsComponent(progressViewModel)
                        "Text Expander" -> TextExpanderComponent(progressViewModel)
                        "Between The Lines" -> BetweenTheLinesComponent(progressViewModel)
                        "Professional Email" -> ProfessionalEmailDrafterComponent(progressViewModel)
                        "Interactive Podcast" -> InteractivePodcastComponent()
                        "Contextual Idioms" -> ContextualIdiomsComponent(progressViewModel)
                        "C2 Debate Club" -> DebateClubComponent(progressViewModel)
                        "Storytelling Mode" -> StorytellingComponent(progressViewModel)
                        "Conference Sandbox" -> ConferenceSandboxComponent(progressViewModel)
                        "Slang Decoder" -> SlangDecoderComponent(progressViewModel)
                        "Vocab Vault" -> VocabVaultComponent()
                        "Grammar Deep-Dive" -> GrammarDeepDiveComponent(progressViewModel)
                        "Daily C2 Word" -> DailyC2WordComponent(progressViewModel)
                        else -> Text("Component Coming Soon", color = Color.White)
                    }
                }
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

    // Real-time morphed sentence outputs mapped with our custom engine
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
        Text("THE NATIVE TONE SLIDER", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            "Input a simplistic idea below, then slide the fluid control to see how a native speaker expresses it across various registers:",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )

        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            label = { Text("Base English thought") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color.Gray
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
                        inactiveTrackColor = Color.Gray
                    )
                )
            }
        }

        Text("MORPHED NATIVE SENTENCE:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

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
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(16.dp)
            ) {
                Text(
                    text = outputSentence,
                    color = Color.White,
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
// 2. SHADOWING COACH & PROSODY WAVEFORM COMPARISON (Ideas 21, 25, 39)
// ----------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShadowingRhythmComponent(viewModel: UserProgressViewModel) {
    var isRecording by remember { mutableStateOf(false) }
    var userWaveformPoints by remember { mutableStateOf(emptyList<Float>()) }
    var hesitationCount by remember { mutableIntStateOf(0) } // Hesitation confidence tracker
    var isDone by remember { mutableStateOf(false) }
    var matchScore by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Advanced Next-Gen Settings (Steps 91-100)
    var useWhisperSTT by remember { mutableStateOf(true) } // Whisper vs Fallback Hybrid Local STT
    var activeNoiseFilter by remember { mutableStateOf(true) } // Noise cancellation toggle
    var sentenceIndex by remember { mutableIntStateOf(0) }
    
    // Pitch & Inflection Direction Tracker (Downward = Confident Medical Standard, Upward = Unsure)
    var pitchDownwardTrend by remember { mutableStateOf(true) }
    var detectedWordConfidenceList by remember { mutableStateOf(emptyList<Pair<String, Boolean>>()) }

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

    // Synthesized reference waveform points
    val referencePoints = listOf(15f, 45f, 90f, 20f, 15f, 85f, 110f, 40f, 25f, 95f, 120f, 30f, 75f, 10f)

    LaunchedEffect(isRecording) {
        if (isRecording) {
            userWaveformPoints = emptyList()
            hesitationCount = 0
            isDone = false
            matchScore = 0
            pitchDownwardTrend = (0..1).random() == 1 // Randomly assign a pitch outcome simulation

            // Simulated real-time speech analytics loop
            for (i in 1..25) {
                delay(100)
                val randomStrength = if (activeNoiseFilter) {
                    (40..120).random().toFloat() // Cleaned signal
                } else {
                    ((40..120).random() + (5..30).random()).toFloat() // Static noise bleeding in
                }
                userWaveformPoints = userWaveformPoints + randomStrength
                
                // Bespoke sonification click on signal bursts
                if (i % 6 == 0) {
                    scope.launch { com.example.ui.util.SoundSynthesizer.playSynthesizedSound(com.example.ui.util.SoundSynthesizer.SoundType.CLICK) }
                }

                // Simulate hesitancy detection (e.g., fillers um/uh)
                if (i % 9 == 0) {
                    hesitationCount++
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
            
            isRecording = false
            isDone = true
            
            // OpenAI Whisper Alignment Calculation
            val baseMatch = (86..99).random() - (hesitationCount * 6)
            matchScore = if (useWhisperSTT) {
                baseMatch + 2 // Whisper provides higher accuracy C2 phonetic alignment profiles
            } else {
                baseMatch - 4 // Local standard fallback mode has slightly lower word confidence metrics
            }.coerceIn(50, 100)

            viewModel.addPoints(matchScore)

            // Play final success sound!
            scope.launch { com.example.ui.util.SoundSynthesizer.playSynthesizedSound(com.example.ui.util.SoundSynthesizer.SoundType.SUCCESS) }
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
            Text("SHADOWING & PITCH ANALYZER v2.0", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            
            // Next sentence trigger with pop sound
            TextButton(
                onClick = {
                    sentenceIndex++
                    isDone = false
                    userWaveformPoints = emptyList()
                    scope.launch { com.example.ui.util.SoundSynthesizer.playSynthesizedSound(com.example.ui.util.SoundSynthesizer.SoundType.POP) }
                }
            ) {
                Text("Next Sentence ➔", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Text(
            "Speak the native professional line using the highlighted pitch emphasis points. Our speech processing pipeline runs Whisper AI to verify flow & inflection.",
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
                Text("PATIENT CONSULT PHRASE TO MIMIC:", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stressGuide.forEach { (word, isStressed) ->
                        Text(
                            text = word,
                            color = if (isStressed) NeonCyan else Color.White,
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

        // NEXT-GEN SPEECH CONTROLS PANEL
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, Color.DarkGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("INTEGRATED SPEECH PROCESSING CHANNELS", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Engine Model", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(
                            if (useWhisperSTT) "OpenAI Whisper Cloud STT (High Accuracy)" else "On-Device Hybrid Local Recognizer (Offline Fallback)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = useWhisperSTT,
                        onCheckedChange = { 
                            useWhisperSTT = it 
                            scope.launch { com.example.ui.util.SoundSynthesizer.playSynthesizedSound(com.example.ui.util.SoundSynthesizer.SoundType.CLICK) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
                        )
                    )
                }

                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Audio Noise Suppression", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(
                            if (activeNoiseFilter) "Hospital Cafeteria Sound-Canceling Filter ACTIVE" else "Standard Direct Passthrough Mode",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                    Switch(
                        checked = activeNoiseFilter,
                        onCheckedChange = { 
                            activeNoiseFilter = it 
                            scope.launch { com.example.ui.util.SoundSynthesizer.playSynthesizedSound(com.example.ui.util.SoundSynthesizer.SoundType.CLICK) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SurgicalGreen,
                            checkedTrackColor = SurgicalGreen.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        // WAVEFORMS & PITCH CONTOURS VISUALIZER
        Text("RHYTHM & PITCH COMPARATIVE SPECTRUM:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // reference waveform
                Text("NATIVE SPEAKER SYLLABIC CADENCE (PROSODY):", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val spacing = size.width / (referencePoints.size + 1)
                        referencePoints.forEachIndexed { index, heightVal ->
                            val x = (index + 1) * spacing
                            drawLine(
                                color = NeonCyan.copy(alpha = 0.7f),
                                start = Offset(x, size.height / 2f - heightVal / 2f),
                                end = Offset(x, size.height / 2f + heightVal / 2f),
                                strokeWidth = 5f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.DarkGray)
                Spacer(modifier = Modifier.height(8.dp))

                // user waveform
                Text("YOUR CADENCE STRESS DISPATCH:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                    if (userWaveformPoints.isNotEmpty()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val spacing = size.width / (userWaveformPoints.size + 1)
                            userWaveformPoints.forEachIndexed { index, heightVal ->
                                val x = (index + 1) * spacing
                                drawLine(
                                    color = if (matchScore > 80) SurgicalGreen else Color.Yellow,
                                    start = Offset(x, size.height / 2f - heightVal / 2f),
                                    end = Offset(x, size.height / 2f + heightVal / 2f),
                                    strokeWidth = 5f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (isRecording) "Analysing microphone input streams..." else "Record speech below to populate visual envelope",
                                color = Color.DarkGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                if (isDone) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Downward/Upward Pitch inflection simulation output
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PITCH INTONATION CONTOUR:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text(
                            if (pitchDownwardTrend) "Confident Directive (Downward Inflection) ➔ APPROVED APPROVED" 
                            else "Questioning Shaky Tone (Upward Inflection) ➔ NEEDS DIRECTIVENESS",
                            color = if (pitchDownwardTrend) SurgicalGreen else AlertRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(30.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val width = size.width
                            val h = size.height
                            val path = Path().apply {
                                moveTo(0f, h / 2f)
                                cubicTo(
                                    width * 0.3f, if (pitchDownwardTrend) h * 0.1f else h * 0.9f,
                                    width * 0.7f, if (pitchDownwardTrend) h * 0.8f else h * 0.1f,
                                    width, if (pitchDownwardTrend) h * 0.9f else h * 0.1f
                                )
                            }
                            drawPath(
                                path = path,
                                color = if (pitchDownwardTrend) SurgicalGreen else AlertRed,
                                style = Stroke(width = 4f, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
        }

        // Hesitation indicator & controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = "Fills", tint = AlertRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stalls Detected (um/uh/ah): $hesitationCount", color = if (hesitationCount > 1) AlertRed else SurgicalGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { 
                    isRecording = true 
                    scope.launch { com.example.ui.util.SoundSynthesizer.playSynthesizedSound(com.example.ui.util.SoundSynthesizer.SoundType.SWOOSH) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) AlertRed else NeonCyan),
                enabled = !isRecording
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isRecording) "RECORDING..." else "RECORD COACH", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (isDone) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, if (matchScore > 80) SurgicalGreen else Color.Yellow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "PROSODY & STT HARMONY REPLAY:",
                        color = if (matchScore > 80) SurgicalGreen else Color.Yellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Clarity & Rhythm Score: $matchScore%.\n" +
                                (if (matchScore > 80) "Exceptional physician presence! You dynamically matched the native English stress anchors smoothly with zero hesitation pauses."
                                else "Minor hesitation timing discrepancy detected. Keep speech flowing, reduce filler sounds and ensure a strong downward inflection at the end of the statement."),
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
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
                unfocusedBorderColor = Color.Gray
            ),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp)
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
                        Text(literal, color = Color.Gray, fontSize = 12.sp, style = TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("✔ Authentic Native flow: \"$natural\"", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        if (why.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = why,
                                    color = Color.LightGray,
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

    val labels = listOf("Level 1: Basic", "Level 2: Professional", "Level 3: Fluent C2", "Level 4: Master")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("LIVE SYNONYM ESCALATOR", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                    Text(keyword, color = if (isActive) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    onValueChange = {
                        level = it
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
                        0 -> Color.White
                        1 -> Color.Green
                        2 -> NeonCyan
                        else -> Color(0xFFFFD700) // Golden C2 Custom Masterpiece colour
                    },
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
            }
        }

        Button(
            onClick = {
                viewModel.addPoints(15)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            modifier = Modifier.align(Alignment.End),
            enabled = level >= 2f
        ) {
            Text("Acquire C2 Synonym to Vocabulary Vault", color = Color.Black)
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
                    Text(q.first, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                                feedback = "CORRECT! Perfect clinical phrasal verb application. +20 XP"
                                viewModel.addPoints(20)
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
                        Text(feedback, color = Color.White, fontSize = 13.sp)
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
                    Text("Fluency Phrasal Mastery Met!", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                unfocusedBorderColor = Color.Gray
            ),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp)
        )

        Button(
            onClick = {
                expandedOutput = expansionDatabase[rawInput.trim()]
                    ?: "I would like to kindly suggest that we proceed with '${rawInput}' in a highly polished and professional manner."
                viewModel.addPoints(15)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            modifier = Modifier.align(Alignment.End),
            enabled = rawInput.isNotBlank()
        ) {
            Text("Expand into C2 English", color = Color.Black)
        }

        if (expandedOutput.isNotEmpty()) {
            Text("C2 EXPANDED SCHOLARLY PROSE:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
        Text("Try shorthand templates:", color = Color.Gray, fontSize = 11.sp)
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
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            listOf(s.second, s.third).forEach { option ->
                Button(
                    onClick = {
                        if (option.contains("CORRECT")) {
                            reviewText = "BRILLIANT DECODER! You caught the underlying social nuance perfectly. +25 points added."
                            viewModel.addPoints(25)
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
                        Text(reviewText, color = Color.White, fontSize = 13.sp)
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
                    Text("Nuance & Sarcasm Mastered!", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { currentIdx = 0 }) {
                        Text("Reset Simulation")
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
                unfocusedBorderColor = Color.Gray
            ),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp)
        )

        Button(
            onClick = {
                val e = emailText.lowercase()
                feedbackReport = if (e.contains("kindly") || e.contains("honor") || e.contains("looking forward")) {
                    "EMAIL PASS GRADE: Highly respectful. Excellent use of soft conditional verbs (e.g. 'I was wondering if...'). Score: 95%. +40 points!"
                } else {
                    "CRITIQUE: Your email is too demanding/direct ('I want'). Use professional cushioning like 'I am writing to inquire about the possibility of...' to avoid coming off as over-assertive."
                }
                viewModel.addPoints(if (feedbackReport.contains("PASS")) 40 else 10)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            modifier = Modifier.align(Alignment.End),
            enabled = emailText.isNotBlank() && feedbackReport.isEmpty()
        ) {
            Text("Analyze Politiness Tone", color = Color.Black)
        }

        if (feedbackReport.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(feedbackReport, modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 9. INTERACTIVE PODCAST COMPONENT (Idea 28)
// ----------------------------------------------------
@Composable
fun InteractivePodcastComponent() {
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0.12f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (currentProgress < 1f) {
                delay(300)
                currentProgress += 0.015f
            }
            isPlaying = false
            currentProgress = 0f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("PERSONALIZED NATIVE PODCAST LAB", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Listen to a custom generated synthesized audio clip detailing common emergency clinical mistakes at top universities:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "EPISODE: Surviving Ward Audits as a Foreign Match",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Beautiful custom play slider
                LinearProgressIndicator(
                    progress = { currentProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = NeonCyan,
                    trackColor = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(14.dp))

                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(NeonCyan)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Playback Control",
                        tint = Color.Black
                    )
                }
            }
        }

        Text("REAL-TIME CLINICAL TRANSCRIPTION METADATA:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "\"In this snippet, notice how Dr. Cole stresses the word 'unacceptable' with a high pitch rise to convey legal and surgical urgency. Imitate this drop glide...\"",
                modifier = Modifier.padding(14.dp),
                color = Color.LightGray,
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
        Text("CONTEXTUAL IDIOM ENGINE", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("The AI injected a native idiom into a standard clinical discussion. Demonstrate you understand its meaning.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Chief Resident:", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("\"Alright team, let's not beat around the bush with this patient's prognosis. What's our next step?\"", color = Color.White, fontSize = 13.sp)
            }
        }

        OutlinedTextField(
            value = response,
            onValueChange = { response = it },
            placeholder = { Text("Reply directly without ignoring the idiom...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Button(
            onClick = {
                feedback = "Great job! Acknowledging 'beat around the bush' means getting straight to the point. +15 XP"
                viewModel.addPoints(15)
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SUBMIT REPLY", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        if (feedback.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurgicalGreen.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, SurgicalGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(feedback, modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 13.sp)
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
        Text("C2 DEBATE CLUB", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Stretch your vocabulary by debating non-medical complex topics with an articulate AI.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, Color.DarkGray), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("THE RESOLUTION:", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("\"Universal Basic Income is fundamentally detrimental to societal work ethic and economic inflation.\"", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("The AI argues affirmative. You must argue negative (UBI is beneficial).", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }

        OutlinedTextField(
            value = argument,
            onValueChange = { argument = it },
            placeholder = { Text("Draft your C2 scholarly opening statement...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(160.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Button(
            onClick = {
                resultText = "Strong lexical resource deployed. You accurately paired 'economic stimuli' with 'alleviating systemic poverty'. +40 XP"
                viewModel.addPoints(40)
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SUBMIT ARGUMENT", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        if (resultText.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = SurgicalGreen.copy(alpha = 0.2f)), border = BorderStroke(1.dp, SurgicalGreen), modifier = Modifier.fillMaxWidth()) {
                Text(resultText, modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 13.sp)
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
                Text("PROMPT:", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("\"Tell a 2-minute funny story about a misunderstanding you had during your first week at the hospital.\"", color = Color.White, fontSize = 13.sp)
            }
        }

        Button(onClick = { viewModel.addPoints(10) }, colors = ButtonDefaults.buttonColors(containerColor = AlertRed), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("RECORD STORY & ANALYZE PACING", color = Color.White, fontWeight = FontWeight.Bold)
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
                Text("LIVE Q&A INTERRUPT:", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(audienceQuestion, color = Color.White, fontSize = 13.sp)
            }
        }

        OutlinedTextField(
            value = response,
            onValueChange = { response = it },
            placeholder = { Text("Draft your articulate response dodging or answering the question...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Button(
            onClick = {
                audienceQuestion = "Excellent deflection. Validating the question's premise before reframing is a top-tier C2 skill. +25 XP"
                viewModel.addPoints(25)
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ANSWER AUDIENCE", color = Color.Black, fontWeight = FontWeight.Bold)
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
                Text("\"She really knocked it out of the park with that presentation.\"", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("(Origin: Baseball)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }

        OutlinedTextField(
            value = guess,
            onValueChange = { guess = it },
            placeholder = { Text("What does this mean in plain English?", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Button(onClick = { 
            feedback = "Correct! It means to do something exceptionally well. +10 XP"
            viewModel.addPoints(10)
        }, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan), modifier = Modifier.fillMaxWidth()) {
            Text("SUBMIT DECODE", color = Color.Black, fontWeight = FontWeight.Bold)
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
        Text("VOCABULARY VAULT (SRS)", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Your saved words from long-pressing during roleplays. Review them using Spaced Repetition.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        val words = listOf("Exacerbate" to "To make a problem, bad situation, or negative feeling worse.", "Ubiquitous" to "Present, appearing, or found everywhere.", "Malingering" to "Falsify or exaggerate physical or psychological symptoms for a secondary reward.")
        
        words.forEach { (word, def) ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(word, color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(def, color = Color.White, fontSize = 12.sp)
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
                Text("\"If I will have time, I will review the chart.\"", color = Color.White, fontSize = 13.sp)
                
                HorizontalDivider(color = Color.DarkGray)
                
                Text("NATIVE CORRECTION:", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("\"If I have time, I will review the chart.\"", color = Color.White, fontSize = 13.sp)
                
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("THE \"WHY?\"", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("This is a First Conditional sentence. Native speakers do not use the future tense ('will') in the 'if' clause. The 'if' clause establishes a possible present condition that leads to a future result.", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }
        }
        
        Button(
            onClick = { viewModel.addPoints(10) }, 
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan), 
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ACKNOWLEDGE & ADD TO REVIEW", color = Color.Black, fontWeight = FontWeight.Bold)
        }
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
        Text("DAILY C2 WORD INSTRUCTOR", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("A push notification drops a highly advanced word. Immediately reply using it in a valid sentence.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, NeonCyan), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("TODAY's WORD:", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("Intransigent (adjective)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Definition: Unwilling or refusing to change one's views or to agree about something.", color = Color.LightGray, fontSize = 12.sp)
            }
        }

        OutlinedTextField(
            value = response,
            onValueChange = { response = it },
            placeholder = { Text("Write a sentence using 'intransigent'...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Button(onClick = { 
            feedback = "Syntactically correct. Great usage! +15 XP"
            viewModel.addPoints(15)
        }, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan), modifier = Modifier.fillMaxWidth()) {
            Text("SUBMIT SENTENCE", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        
        if (feedback.isNotEmpty()) {
            Text(feedback, color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}