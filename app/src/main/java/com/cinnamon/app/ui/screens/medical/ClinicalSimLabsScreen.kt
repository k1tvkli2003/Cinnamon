package com.cinnamon.app.ui.screens.medical

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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

import com.cinnamon.app.ui.theme.*
import com.cinnamon.app.ui.feedback.LocalCinnamonFeedbackPreferences
import com.cinnamon.app.viewmodel.UserProgressViewModel
import com.cinnamon.app.ui.util.SoundSynthesizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.sin

data class LabModule(
    val id: String,
    val title: String,
    val subtitle: String,
    val difficulty: String,
    val mode: String,
    val icon: ImageVector
)

private val labModulesList = listOf(
    LabModule("Morning Report", "Morning Report", "Practice handoff vocabulary in an authored scene", "Easy", "REFERENCE CUE", Icons.Default.Assessment),
    LabModule("ECG & Image Lab", "ECG & Image Lab", "Describe an authored ECG-vocabulary prompt", "Medium", "REFERENCE CUE", Icons.Default.Timeline),
    LabModule("DDx Sandbox", "DDx Sandbox", "Practice differential-language vocabulary", "Hard", "SELF-REVIEW", Icons.Default.MedicalServices),
    LabModule("Lab Translator", "Lab Translator", "Rewrite lab terminology in plain language", "Easy", "SELF-REVIEW", Icons.Default.Translate),
    LabModule("Timed Crisis", "Timed Vocabulary", "Practice a timed fictional handoff phrase", "Hard", "WRITTEN PROMPT", Icons.Default.CheckCircle),
    LabModule("Pharmacology Audio", "Pharmacology Terms", "Read pharmacology wording cues; no audio analysis", "Easy", "WRITTEN PROMPT", Icons.AutoMirrored.Filled.VolumeUp),
    LabModule("Abbreviations", "Abbreviations", "Expand bundled shorthand examples", "Easy", "REFERENCE CUE", Icons.Default.Description),
    LabModule("SPIKES (Bad News)", "SPIKES Roleplay", "Practice compassionate wording in a fictional prompt", "Medium", "SELF-REVIEW", Icons.Default.RecordVoiceOver),
    LabModule("SBAR Consult", "SBAR Consult", "Build a written SBAR language structure", "Medium", "WRITTEN PROMPT", Icons.Default.Forum),
    LabModule("Audio Handoffs", "Handoff Transcript", "Read an authored handoff transcript; no audio recording", "Medium", "WRITTEN PROMPT", Icons.Default.Mic),
    LabModule("Surgical Consent", "Surgical Consent", "Practice plain-language wording in a fictional prompt", "Medium", "SELF-REVIEW", Icons.AutoMirrored.Filled.Assignment),
    LabModule("Medical Ethics", "Medical Ethics", "Practice structured argument language in a fictional prompt", "Hard", "SELF-REVIEW", Icons.Default.Gavel),
    LabModule("Cultural Competence", "Cultural Comp.", "Practice language choices in authored scenarios", "Easy", "SELF-REVIEW", Icons.Default.People),
    LabModule("EMR Simulator", "Record Vocabulary", "Practice vocabulary from fictional record snippets", "Hard", "REFERENCE CUE", Icons.Default.Computer),
    LabModule("Review Graveyard", "Prompt Archive", "Revisit authored language prompts", "Easy", "REFERENCE CUE", Icons.Default.Warning)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalSimLabsScreen(
    onBack: () -> Unit,
    progressViewModel: UserProgressViewModel
) {
    var activeSimLab by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    if (activeSimLab == null) {
        // LAB CATALOGUE HUB MODE (Section 4 - staggered grid catalogue)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Clinical-English Practice Labs", color = SurgicalGreen, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SurgicalGreen)
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
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp)
            ) {
                // High-End Header stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("LANGUAGE PRACTICE CATALOG", style = MaterialTheme.typography.labelSmall, color = SurgicalGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("15 authored practice modules", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Language practice only", style = MaterialTheme.typography.labelSmall, color = NeonOrange, fontWeight = FontWeight.Bold)
                }
                Text(
                    "These are vocabulary and communication prompts, not clinical decision support or competency assessment.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Smooth responsive 2-column staggered layout
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val col1 = labModulesList.filterIndexed { index, _ -> index % 2 == 0 }
                    val col2 = labModulesList.filterIndexed { index, _ -> index % 2 != 0 }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        col1.forEach { lab ->
                            LabGridCard(lab) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch { SoundSynthesizer.playSynthesizedSound(SoundSynthesizer.SoundType.SWOOSH) }
                                activeSimLab = lab.id
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        col2.forEach { lab ->
                            LabGridCard(lab) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch { SoundSynthesizer.playSynthesizedSound(SoundSynthesizer.SoundType.SWOOSH) }
                                activeSimLab = lab.id
                            }
                        }
                    }
                }
            }
        }
    } else {
        // ACTIVE SIMULATION CHAMBER MODE (Full Bleed Arena)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(activeSimLab ?: "Active practice lab", color = SurgicalGreen, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { activeSimLab = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Sim Hub", tint = SurgicalGreen)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (activeSimLab) {
                    "Morning Report" -> MorningReportSimLab(progressViewModel)
                    "ECG & Image Lab" -> ImageDescriptionLab(progressViewModel)
                    "DDx Sandbox" -> DifferentialDiagnosisSandbox(progressViewModel)
                    "Lab Translator" -> LabValueTranslatorLab(progressViewModel)
                    "Timed Crisis" -> TimedVocabularySprintLab()
                    "Pharmacology Audio" -> PharmacologyGlossaryLab()
                    "Abbreviations" -> AbbreviationsExpanderLab(progressViewModel)
                    "SPIKES (Bad News)" -> SpikesRoleplayLab(progressViewModel)
                    "SBAR Consult" -> SbarConsultationLab(progressViewModel)
                    "Audio Handoffs" -> AudioHandoffsLab(progressViewModel)
                    "Surgical Consent" -> SurgicalConsentLab(progressViewModel)
                    "Medical Ethics" -> MedicalEthicsLab(progressViewModel)
                    "Cultural Competence" -> CulturalCompetenceLab(progressViewModel)
                    "EMR Simulator" -> EmrSimulatorLab(progressViewModel)
                    "Review Graveyard" -> ReviewGraveyardLab()
                    else -> UnavailablePracticeLab(onReturnToCatalog = { activeSimLab = null })
                }
            }
        }
    }
}

@Composable
private fun UnavailablePracticeLab(onReturnToCatalog: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Practice module unavailable",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "This module is not part of the installed practice catalog. Return to the catalog to choose an available authored activity.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onReturnToCatalog) { Text("Return to practice catalog") }
    }
}

@Composable
fun LabGridCard(
    lab: LabModule,
    onClick: () -> Unit
) {
    // Determine luxury difficulty lighting schemes
    val (colorScheme, neonAccent) = when (lab.difficulty) {
        "Easy" -> Pair(SurgicalGreen.copy(alpha = 0.08f), SurgicalGreen)
        "Medium" -> Pair(NeonCyan.copy(alpha = 0.08f), NeonCyan)
        else -> Pair(NeonOrange.copy(alpha = 0.08f), NeonOrange)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, neonAccent.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = lab.icon,
                        contentDescription = lab.title,
                        tint = neonAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = neonAccent.copy(alpha = 0.14f)
                ) {
                    Text(
                        lab.mode,
                        color = neonAccent,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = lab.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = lab.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Premium micro difficulty tag
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colorScheme
            ) {
                Text(
                    text = lab.difficulty,
                    color = neonAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------
// 1. AUTHORED HANDOFF STRUCTURE PRACTICE
// ----------------------------------------------------
@Composable
fun MorningReportSimLab(viewModel: UserProgressViewModel) {
    var step by remember { mutableStateOf(1) }
    var currentAnswer by remember { mutableStateOf("") }
    var attendingCommentary by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text("👴", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Dr. Harrington (fictional)", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("AUTHORED LANGUAGE-PRACTICE PERSONA", color = AlertRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "FICTIONAL CASE EXCERPT:",
                    color = SurgicalGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "During a fictional handoff, the speaker reports sudden chest discomfort, marked hypertension, and unequal pulses; they want a senior review.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }

        if (step == 1) {
            Text(
                "HANDOFF STRUCTURE PRACTICE\nRewrite the excerpt as one concise handoff opening with the main concern, two supporting facts, and one clear request.",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            OutlinedTextField(
                value = currentAnswer,
                onValueChange = { currentAnswer = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Type your structured handoff opening here...", fontSize = 12.sp) },
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SurgicalGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                maxLines = 4
            )

            Button(
                onClick = {
                    isSubmitted = true
                    attendingCommentary = "Model structure: Main concern—sudden chest discomfort; supporting facts—marked hypertension and unequal pulses; request—please review this fictional case. Compare structure, not clinical correctness."
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
                modifier = Modifier.align(Alignment.End),
                enabled = currentAnswer.isNotBlank() && !isSubmitted
            ) {
                Text("View model structure", color = Color.Black)
            }

            if (isSubmitted) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurgicalGreen.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SurgicalGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "REFERENCE CUE:",
                            fontWeight = FontWeight.Bold,
                            color = SurgicalGreen,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(attendingCommentary, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 2. ABSTRACT WAVEFORM LANGUAGE PRACTICE
// ----------------------------------------------------
@Composable
fun ImageDescriptionLab(viewModel: UserProgressViewModel) {
    var userDescription by remember { mutableStateOf("") }
    var evaluationResult by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("WAVEFORM DESCRIPTION", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)

        // Abstract authored waveform for geometric language practice only.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, SurgicalGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridStroke = Stroke(width = 0.5.dp.toPx())
                // Draw lead grid lines
                for (x in 0..size.width.toInt() step 30) {
                    drawLine(Color.DarkGray.copy(alpha = 0.5f), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                }
                for (y in 0..size.height.toInt() step 30) {
                    drawLine(Color.DarkGray.copy(alpha = 0.5f), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)
                }

                // Draw a deliberately abstract repeating shape; it is not diagnostic data.
                val path = Path()
                path.moveTo(0f, size.height / 2f)
                
                var currentX = 0f
                val beatWidth = 120f
                var count = 0
                while (currentX < size.width) {
                    val startY = size.height / 2f
                    path.lineTo(currentX + 20f, startY) // Isoelectric line
                    path.lineTo(currentX + 30f, startY - 10f) // P wave
                    path.lineTo(currentX + 40f, startY + 5f) // Q wave
                    path.lineTo(currentX + 48f, startY - 50f) // R wave
                    path.lineTo(currentX + 54f, startY + 20f) // S wave
                    path.lineTo(currentX + 70f, startY - 35f)
                    path.quadraticTo(currentX + 85f, startY - 45f, currentX + 100f, startY)
                    path.lineTo(currentX + beatWidth, startY)
                    currentX += beatWidth
                    count++
                }

                drawPath(path, SurgicalGreen, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                
                // Text marker of Lead II
                drawCircle(SurgicalGreen, radius = 5f, center = Offset(50f, 30f))
            }
            Text("AUTHORED SHAPE · NOT AN ECG", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp).align(Alignment.BottomEnd), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        Text(
            "Describe the visible shape using terms such as rise, fall, peak, and level segment. This is not an interpretation task.",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = userDescription,
            onValueChange = { userDescription = it },
            placeholder = { Text("Describe the visible shape changes here...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SurgicalGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        )

        Button(
            onClick = {
                evaluationResult = "Wording cue: an initial rapid rise, a brief peak, a gradual fall, and a level segment. Compare only the geometric language; no clinical inference is made."
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.align(Alignment.End),
            enabled = userDescription.isNotBlank() && evaluationResult.isEmpty()
        ) {
            Text("View wording cue", color = Color.Black)
        }

        if (evaluationResult.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("REFERENCE CUE:", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(evaluationResult, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        }
    }
}

// ----------------------------------------------------
// 3. TERMINOLOGY STUDY-ORDER ORGANIZER
// ----------------------------------------------------
@Composable
fun DifferentialDiagnosisSandbox(viewModel: UserProgressViewModel) {
    val options = listOf("Myocardial Infarction", "Aortic Dissection", "Pulmonary Embolism", "GERD / Esophagitis")
    var rankedList by remember { mutableStateOf(emptyList<String>()) }
    var evaluationComment by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("STUDY ORDER ORGANIZER", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            "Arrange these bundled medical terms into your preferred personal study sequence. This does not rank likelihood or recommend a differential.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val isAdded = rankedList.contains(option)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isAdded) {
                            rankedList = rankedList + option
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                    colors = CardDefaults.cardColors(containerColor = if (isAdded) Color.DarkGray else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            option,
                            color = if (isAdded) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        if (isAdded) {
                            Icon(Icons.Default.Check, contentDescription = "Added", tint = SurgicalGreen)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = "Add to study order", tint = NeonCyan)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("YOUR STUDY ORDER:", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)

        if (rankedList.isEmpty()) {
            Text("Tap terms above to add them", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rankedList.forEachIndexed { idx, name ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${idx + 1}. $name", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = AlertRed,
                                modifier = Modifier.clickable {
                                    rankedList = rankedList.filter { it != name }
                                }
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                evaluationComment = "Your on-screen study sequence is ready. It reflects your preference only; no clinical priority, likelihood, or diagnostic relationship is inferred."
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.align(Alignment.End),
            enabled = rankedList.size >= 3
        ) {
            Text("Confirm study order", color = Color.Black)
        }

        if (evaluationComment.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(evaluationComment, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 4. LAB VALUE TRANSLATOR LAB (Idea 8)
// ----------------------------------------------------
@Composable
fun LabValueTranslatorLab(viewModel: UserProgressViewModel) {
    var userTranslation by remember { mutableStateOf("") }
    var ratingOutcome by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("CLINICAL LAB RESULTS TRANSLATOR", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("AUTHORED LAB-VOCABULARY EXCERPT:", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Potassium (K+): 5.9 mEq/L (HIGH)\nSodium (Na+): 132 mEq/L (LOW)\nCreatinine: 2.1 mg/dL (HIGH)\nBUN: 45 mg/dL (HIGH)",
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Text(
            "Empathize and explain these complex renal findings to a scared patient without terrifying medical jargon in gentle English:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        OutlinedTextField(
            value = userTranslation,
            onValueChange = { userTranslation = it },
            placeholder = { Text("Your explanation: 'We look at kidneys' filter system...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SurgicalGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        )

        Button(
            onClick = {
                val explanation = userTranslation.lowercase()
                ratingOutcome = if (explanation.contains("filter") || explanation.contains("gentle") || explanation.contains("kidney")) {
                    "Language cue found: compare your wording with the plain-language examples. This screen does not measure empathy or bedside manner."
                } else {
                    "Try a plain-language analogy such as ‘the organ that filters blood’. This is writing guidance, not an empathy score."
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.align(Alignment.End),
            enabled = userTranslation.isNotBlank() && ratingOutcome.isEmpty()
        ) {
            Text("Check plain-language cue", color = Color.Black)
        }

        if (ratingOutcome.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(ratingOutcome, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 5. TIMED VOCABULARY SPRINT
// ----------------------------------------------------
@Composable
fun TimedVocabularySprintLab() {
    var timerSeconds by remember { mutableIntStateOf(45) }
    var running by remember { mutableStateOf(false) }
    var roundMessage by remember { mutableStateOf("") }
    var phraseText by remember { mutableStateOf("") }
    var signalState by remember { mutableStateOf("PROMPT") }
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isForeground by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isForeground = true
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> isForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // This timer is a pacing aid for a written language cue, never a patient state.
    LaunchedEffect(running, timerSeconds, isForeground) {
        if (running && isForeground && timerSeconds > 0) {
            delay(1000)
            timerSeconds--
            if (timerSeconds == 0) {
                running = false
                roundMessage = "Round ended. No streak, patient outcome, or clinical score changed. Restart whenever you want another vocabulary sprint."
            }
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
            Text("TIMED VOCABULARY SPRINT", color = NeonOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Badge(containerColor = if (timerSeconds < 10) NeonOrange else NeonCyan) {
                Text("00:" + (if (timerSeconds < 10) "0" else "") + timerSeconds, modifier = Modifier.padding(4.dp), color = Color.Black)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "AUTHORED LANGUAGE CUE",
                    color = NeonOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "This fictional handoff prompt lets you rehearse neutral communication vocabulary. It is not a patient simulation, clinical protocol, or care instruction.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        // Abstract signal art makes the timed round feel alive without implying
        // an ECG, diagnosis, or real patient state.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(Color.Black)
                .border(2.dp, if (signalState == "COMPLETE") SurgicalGreen else NeonOrange, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion
            val phase = if (running && isForeground && !reduceMotion) {
                val transition = rememberInfiniteTransition(label = "languageSignal")
                val animatedPhase by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 2f * Math.PI.toFloat(),
                    animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing)),
                    label = "languageSignalPhase"
                )
                animatedPhase
            } else {
                0f
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val midY = height / 2f
                val path = Path()
                path.moveTo(0f, midY)

                for (x in 0..width.toInt() step 5) {
                    val y = when (signalState) {
                        "COMPLETE" -> midY + sin(x * 0.1f + phase) * 5f
                        else -> {
                            val pulse = sin(x * 0.2f + phase * 3f) * 18f + sin(x * 0.4f + phase * 1.5f) * 9f
                            midY + pulse
                        }
                    }
                    path.lineTo(x.toFloat(), y)
                }

                drawPath(
                    path = path,
                    color = if (signalState == "COMPLETE") SurgicalGreen else NeonOrange,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        if (!running && roundMessage.isEmpty()) {
            Button(
                onClick = {
                    running = true
                    timerSeconds = 45
                    roundMessage = ""
                    phraseText = ""
                    signalState = "PROMPT"
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start 45-second language round", color = Color.Black)
            }
        }

        if (running) {
            Text(
                "Write one neutral reference phrase from this fictional prompt (for example, ‘rapid response’, ‘clear handoff’, or ‘monitoring update’):",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = phraseText,
                onValueChange = { phraseText = it },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AlertRed,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            )

            Button(
                onClick = {
                    val phraseLower = phraseText.lowercase()
                    val matchedReferencePhrase = listOf(
                        "rapid response",
                        "handoff",
                        "monitor",
                        "request help",
                        "clarify"
                    ).any(phraseLower::contains)
                    if (matchedReferencePhrase) {
                        running = false
                        signalState = "COMPLETE"
                        roundMessage = "Reference phrase matched in this authored language cue. It is not a clinical assessment or protocol."
                    } else {
                        roundMessage = "Reference cue: try a neutral handoff or help-request phrase from the prompt. No clinical score is recorded."
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
                modifier = Modifier.align(Alignment.End),
                enabled = phraseText.isNotBlank()
            ) {
                Text("Check reference phrase", color = Color.Black)
            }
        }

        if (roundMessage.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(roundMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Button(
                        onClick = {
                            timerSeconds = 45
                            roundMessage = ""
                            phraseText = ""
                            signalState = "PROMPT"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Start another language round", color = Color.Black)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 6. PHARMACOLOGY PRONUNCIATION GUIDE
// ----------------------------------------------------
@Composable
fun PharmacologyGlossaryLab() {
    val drugs = listOf(
        Pair("Atorvastatin", "uh-TOR-vuh-stat-in // Lipophilic HMG-CoA reductase inhibitor"),
        Pair("Levothyroxine", "lee-voh-thy-ROK-seen // Synthesized thyroid hormone replacement"),
        Pair("Lisinopril", "lye-SIN-oh-pril // Peptidyl dipeptidase vasodilator agent"),
        Pair("Metoprolol", "meh-TOE-pro-lol // Cardioselective beta-1 adrenergic blocker"),
        Pair("Clopidogrel", "kloh-PID-oh-grel // P2Y12 platelet aggregation inhibitor")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("PHARMACOLOGY PRONUNCIATION GUIDE", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Read the syllable guides for these generic-name vocabulary examples. No audio playback is connected.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            drugs.forEach { (name, desc) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurgicalGreen.copy(alpha = 0.2f))
                                .padding(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Syllable guide", tint = SurgicalGreen)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 7. MEDICAL ABBREVIATIONS EXPANDER LAB (Idea 12)
// ----------------------------------------------------
@Composable
fun AbbreviationsExpanderLab(viewModel: UserProgressViewModel) {
    val items = listOf(
        Triple("NPO", "nil per os", "Nothing by mouth"),
        Triple("PRN", "pro re nata", "As needed"),
        Triple("BID", "bis in die", "Twice a day"),
        Triple("PO", "per os", "By mouth"),
        Triple("Q8H", "quaque 8 hora", "Every 8 hours")
    )
    var activeIdx by remember { mutableIntStateOf(0) }
    var userEntry by remember { mutableStateOf("") }
    var textReport by remember { mutableStateOf("") }
    var answerIsCorrect by remember { mutableStateOf(false) }
    var practiceSessionKey by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(activeIdx, practiceSessionKey) {
        if (activeIdx >= items.size) {
            viewModel.recordPracticeSession(
                subjectType = "abbreviation_reference",
                subjectId = items.joinToString(separator = "|") { it.first.lowercase() },
                occurrenceKey = practiceSessionKey,
                completedItemCount = items.size
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("ABBREVIATION REFERENCE ROUND", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)

        if (activeIdx < items.size) {
            val element = items[activeIdx]
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("EXPAND ABBREVIATION:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(element.first, color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 34.sp)
                }
            }

            Text(
                "Type the full English reference meaning (for example, ‘Nothing by mouth’ for NPO). This checks authored vocabulary only, never a care decision.",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 12.sp
            )

            OutlinedTextField(
                value = userEntry,
                onValueChange = { userEntry = it },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SurgicalGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            )

            Button(
                onClick = {
                    val rightMeaning = element.third.lowercase()
                    answerIsCorrect = userEntry.lowercase().contains(rightMeaning) || userEntry.lowercase().contains(element.second)
                    textReport = if (answerIsCorrect) {
                        "Reference wording matched: '${element.first}' means '${element.third}'."
                    } else {
                        "Reference cue: '${element.first}' means '${element.third}'. Revise your wording, then try this item again."
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
                enabled = userEntry.isNotBlank() && textReport.isEmpty()
            ) {
                Text("Verify Expansion", color = Color.Black)
            }

            if (textReport.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(textReport, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (answerIsCorrect) {
                                    activeIdx++
                                }
                                userEntry = ""
                                textReport = ""
                                answerIsCorrect = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(if (answerIsCorrect) "Next reference item" else "Try this item again", color = Color.Black)
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
                    Text("Reference round complete", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "All five authored expansions were matched. Saving one completed vocabulary-practice result.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            activeIdx = 0
                            userEntry = ""
                            textReport = ""
                            answerIsCorrect = false
                            practiceSessionKey = UUID.randomUUID().toString()
                        }
                    ) {
                        Text("Start a fresh round")
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 8. BREAKING BAD NEWS (SPIKES PROTOCOL - Idea 13)
// ----------------------------------------------------
@Composable
fun SpikesRoleplayLab(viewModel: UserProgressViewModel) {
    var userText by remember { mutableStateOf("") }
    var stepReview by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SPIKES Wording Practice", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            "Scenario: You must tell a patient's daughter that her father has progressed to severe metastatic carcinoma. Practice using the 'SPIKES' framework.",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("SPIKES STEPS:", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(
                    "S - Setting up the interview\nP - Assessing the patient's Perception\nI - Obtaining the patient's Invitation\nK - Giving Knowledge and information\nE - Addressing the patient's Emotions\nS - Strategy & Summary",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Text("Type how you would professionally deliver this warning (e.g., 'I am sorry to share some difficult news about your father's biopsy results'):", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)

        OutlinedTextField(
            value = userText,
            onValueChange = { userText = it },
            placeholder = { Text("Deliver bad news empathetically...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SurgicalGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        )

        Button(
            onClick = {
                val input = userText.lowercase()
                stepReview = if (input.contains("sorry") || input.contains("unfortunate") || input.contains("biopsy")) {
                    "Language cue found. Compare your draft with the prompt’s example; this screen does not grade communication competency."
                } else {
                    "Writing cue: consider a gentle transition before introducing difficult information. This is educational copy, not clinical guidance."
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.align(Alignment.End),
            enabled = userText.isNotBlank() && stepReview.isEmpty()
        ) {
            Text("Show writing cue", color = Color.Black)
        }

        if (stepReview.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stepReview, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 9. INTERPROFESSIONAL CONSULTATION (SBAR - Idea 14)
// ----------------------------------------------------
@Composable
fun SbarConsultationLab(viewModel: UserProgressViewModel) {
    var situationText by remember { mutableStateOf("") }
    var backgroundText by remember { mutableStateOf("") }
    var assessmentText by remember { mutableStateOf("") }
    var recommendationText by remember { mutableStateOf("") }
    var consultReview by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SBAR WRITING PRACTICE", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Build an authored SBAR writing structure. It is not a live consult or medical instruction.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = situationText,
                onValueChange = { situationText = it },
                label = { Text("S - Situation") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = SurgicalGreen
                )
            )
            OutlinedTextField(
                value = backgroundText,
                onValueChange = { backgroundText = it },
                label = { Text("B - Background") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = SurgicalGreen
                )
            )
            OutlinedTextField(
                value = assessmentText,
                onValueChange = { assessmentText = it },
                label = { Text("A - Assessment") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = SurgicalGreen
                )
            )
            OutlinedTextField(
                value = recommendationText,
                onValueChange = { recommendationText = it },
                label = { Text("R - Recommendation") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = SurgicalGreen
                )
            )
        }

        Button(
            onClick = {
                consultReview = "Structure captured. Re-read the four sections for clarity and completeness; this screen does not issue a clinical grade."
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.fillMaxWidth(),
            enabled = situationText.isNotBlank() && backgroundText.isNotBlank() && assessmentText.isNotBlank() && recommendationText.isNotBlank()
        ) {
            Text("Show structure cue", color = Color.Black)
        }

        if (consultReview.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(consultReview, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 10. HANDOFF TRANSCRIPT DRILL
// ----------------------------------------------------
@Composable
fun AudioHandoffsLab(viewModel: UserProgressViewModel) {
    var step by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("HANDOFF TRANSCRIPT DRILL", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Read an authored handoff transcript and identify its reference phrase. No audio playback or triage support is connected.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AUTHORED HANDOFF TRANSCRIPT:", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "\"We have Mr. Evans in critical room 6 post-op coronary bypass. He has chest tube outputting 180 ml serosanguineous fluids, potassium is 3.4, he has bid medication due in 10 minutes, but is hypotensive at 88/44 mmHg. I also gave him 10mg morphine as needed for pain.\"",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    fontSize = 12.sp
                )
            }
        }

        Text("Which reference phrase does this authored vocabulary prompt expect?", color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        val answers = listOf(
            "Hypokalemia (K+ 3.4) affecting surgery recovery",
            "Severe Hypotension (88/44 mmHg) suggesting bleeding or shock",
            "Morphine side effect on breathing speed"
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            answers.forEach { ans ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = feedback.isEmpty()) {
                            if (ans.contains("Hypotension")) {
                                feedback = "Reference answer matched this authored vocabulary scenario. It is not a patient-triage tool."
                            } else {
                                feedback = "Reference cue: compare the selected phrase with the authored prompt. This screen does not triage patients or direct care."
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                ) {
                    Text(ans, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        }

        if (feedback.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(feedback, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 12. SURGICAL CONSENT ROLEPLAY (Idea 16)
// ----------------------------------------------------
@Composable
fun SurgicalConsentLab(viewModel: UserProgressViewModel) {
    var response by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SURGICAL CONSENT CLINIC", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Learn how to translate complex surgical procedures and risks into reassuring layman's terms.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PATIENT SCENARIO:", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("A 65-year-old highly anxious patient needs an emergent Coronary Artery Bypass Graft (CABG). They ask: 'Doctor, are you going to stop my heart? Is it safe?'", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }

        OutlinedTextField(
            value = response,
            onValueChange = { response = it },
            placeholder = { Text("Draft your empathetic response explaining the bypass machine and risks...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(150.dp),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SurgicalGreen,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Button(
            onClick = {
                feedback = "Plain-language cue: compare technical and everyday phrasing. This screen does not score compassion or consent quality."
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SHOW SELF-REVIEW CUE", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        if (feedback.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurgicalGreen.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, SurgicalGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(feedback, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 13. MEDICAL ETHICS BOARD DEBATE (Idea 17)
// ----------------------------------------------------
@Composable
fun MedicalEthicsLab(viewModel: UserProgressViewModel) {
    var response by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("ETHICS PERSPECTIVES EXERCISE", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Practice a structured C2 argument around one fictional bioethics prompt; no ethics judgment or care recommendation is produced.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("THE CASE:", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("A family demands continued life-sustaining ventilation for a brain-dead patient, citing religious convictions, despite clinical futility. The Ethics Board asks for your justification to withdraw care.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }

        OutlinedTextField(
            value = response,
            onValueChange = { response = it },
            placeholder = { Text("Draft your C2 academic response addressing autonomy, beneficence, and non-maleficence...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SurgicalGreen,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Button(
            onClick = {
                feedback = "Self-review cue: state your claim, a reason, and an acknowledged limitation. This screen does not score ethics reasoning."
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SHOW SELF-REVIEW CUE", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        if (feedback.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurgicalGreen.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, SurgicalGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(feedback, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 14. AUTHORED CULTURAL-LANGUAGE SELF-REVIEW
// ----------------------------------------------------
@Composable
fun CulturalCompetenceLab(viewModel: UserProgressViewModel) {
    var response by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("CULTURAL-LANGUAGE SELF-REVIEW", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Draft a clear, respectful reply to one authored fictional prompt, then reveal communication tips. This is not a cultural-competence assessment.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CULTURAL PROFILE: RURAL APPALACHIA", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("Patient: 'Doc, I got the sugar real bad and my nerves are shot.'", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text("How do you respond to build rapport while transitioning to discussing their diabetic neuropathy?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }

        OutlinedTextField(
            value = response,
            onValueChange = { response = it },
            placeholder = { Text("Draft your response adapting your vocabulary...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SurgicalGreen,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Button(
            onClick = {
                feedback = "Self-review cue: use the person’s own words, then check that your explanation remains clear and respectful."
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SHOW COMMUNICATION TIPS", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        if (feedback.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurgicalGreen.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, SurgicalGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(feedback, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 15. CONTEXTUAL EMR INTERFACE (Idea 15)
// ----------------------------------------------------
@Composable
fun EmrSimulatorLab(viewModel: UserProgressViewModel) {
    var chatInput by remember { mutableStateOf("") }
    var emrNotes by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("CASE SCENARIO & REFERENCE NOTE", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Rewrite one authored fictional statement in a concise practice note. This is not a live patient feed, real chart, or charting evaluation.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        // Split view container: Top is Chat, Bottom is EMR
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier.fillMaxWidth().height(200.dp),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("AUTHORED FICTIONAL SCENE:", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Patient: 'Yeah, the pain started yesterday around noon. Feels like an elephant sitting on my chest.'", color = Color.White, fontSize = 13.sp)
                Spacer(modifier = Modifier.weight(1f))
                
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    placeholder = { Text("Verbalize an empathetic response...", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp)
                )
            }
        }
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth().height(200.dp),
            border = BorderStroke(1.dp, SurgicalGreen.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("PRACTICE NOTE:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = emrNotes,
                    onValueChange = { emrNotes = it },
                    placeholder = { Text("Translate to clinical chart (e.g. Pt reports acute onset substernal crushing pain x 24hrs)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                )
            }
        }

        Button(
            onClick = {
                feedback = "Reference cue: compare the metaphor with the authored plain-language description. This screen does not evaluate charting."
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SHOW REFERENCE CUE", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        if (feedback.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurgicalGreen.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, SurgicalGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(feedback, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 16. REVIEW GRAVEYARD COMPONENT (Idea 20)
// ----------------------------------------------------
@Composable
fun ReviewGraveyardLab() {
    val reviewItems = listOf(
        Pair("Dypnea → Dyspnea", "Spelling reference: dyspnea is the standard English spelling."),
        Pair("high pitched → high-pitched", "Hyphenation reference: use a hyphen when the phrase modifies a noun."),
        Pair("PRN / PO", "Vocabulary reference: these are different abbreviations and should not be used interchangeably.")
    )
    var revealedItems by remember { mutableStateOf(emptySet<Int>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("COMMON CORRECTIONS", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Study these bundled examples of typical wording improvements. They are not your personal error history.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            reviewItems.forEachIndexed { index, item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.first, color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (index in revealedItems) {
                            Text(item.second, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = {
                                    revealedItems = if (index in revealedItems) {
                                        revealedItems - index
                                    } else {
                                        revealedItems + index
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    if (index in revealedItems) "Hide reference note" else "View reference note",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
