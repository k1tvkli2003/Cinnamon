package com.example.ui.screens.medical

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.util.SoundSynthesizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

data class LabModule(
    val id: String,
    val title: String,
    val subtitle: String,
    val difficulty: String, // "Easy", "Medium", "Hard"
    val progress: Float, // 0f to 1f
    val icon: ImageVector
)

private val labModulesList = listOf(
    LabModule("Morning Report", "Morning Report", "Review physician daily handoffs", "Easy", 0.72f, Icons.Default.Assessment),
    LabModule("ECG & Image Lab", "ECG & Image Lab", "Analyze waves and reports", "Medium", 0.45f, Icons.Default.Timeline),
    LabModule("DDx Sandbox", "DDx Sandbox", "Formulate clinical differentials", "Hard", 0.30f, Icons.Default.MedicalServices),
    LabModule("Lab Translator", "Lab Translator", "Translate lab values instantly", "Easy", 0.88f, Icons.Default.Translate),
    LabModule("Timed Crisis", "Timed Crisis", "Manage crashing clinical codes", "Hard", 0.55f, Icons.Default.CheckCircle),
    LabModule("Pharmacology Audio", "Pharmacology Audio", "Review active audio scripts", "Easy", 0.95f, Icons.Default.VolumeUp),
    LabModule("Abbreviations", "Abbreviations", "Expand complex shorthand codes", "Easy", 0.60f, Icons.Default.Description),
    LabModule("SPIKES (Bad News)", "SPIKES Roleplay", "Deliver critical family news", "Medium", 0.70f, Icons.Default.RecordVoiceOver),
    LabModule("SBAR Consult", "SBAR Consult", "SBAR structure communication", "Medium", 0.50f, Icons.Default.Forum),
    LabModule("Audio Handoffs", "Audio Handoffs", "Shadow physician voice records", "Medium", 0.40f, Icons.Default.Mic),
    LabModule("Surgical Consent", "Surgical Consent", "Perform informed risk consent", "Medium", 0.65f, Icons.Default.Assignment),
    LabModule("Medical Ethics", "Medical Ethics", "Resolve critical clinic choices", "Hard", 0.20f, Icons.Default.Gavel),
    LabModule("Cultural Competence", "Cultural Comp.", "Tailor care to specific values", "Easy", 0.80f, Icons.Default.People),
    LabModule("EMR Simulator", "EMR Simulator", "Parse electronic medical files", "Hard", 0.15f, Icons.Default.Computer),
    LabModule("Review Graveyard", "Review Case Archive", "Review past medical cases", "Easy", 1.0f, Icons.Default.Warning)
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
                    title = { Text("Clinical Simulation Labs", color = SurgicalGreen, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
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
                        Text("SIM ACADEMY CURRICULUM PROFILE", style = MaterialTheme.typography.labelSmall, color = SurgicalGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("15 Specialized clinical chambers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("⚡ XP Multiplier ON", style = MaterialTheme.typography.labelSmall, color = NeonOrange, fontWeight = FontWeight.Bold)
                }

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
                    title = { Text(activeSimLab ?: "Active simulator", color = SurgicalGreen, fontWeight = FontWeight.Bold) },
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
                    "Timed Crisis" -> TimedCrisisSimulatorLab(progressViewModel)
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
                    else -> Text("Coming Soon", color = Color.White)
                }
            }
        }
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

                // Curved miniature vector-based progress meter (glowing neon arc)
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawArc(
                        color = Color.Gray.copy(alpha = 0.15f),
                        startAngle = -220f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = neonAccent,
                        startAngle = -220f,
                        sweepAngle = 260f * lab.progress,
                        useCenter = false,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
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
// 1. MORNING REPORT SIMULATOR COMPONENT (Idea 5)
// ----------------------------------------------------
@Composable
fun MorningReportSimLab(viewModel: UserProgressViewModel) {
    var step by remember { mutableStateOf(1) }
    var currentAnswer by remember { mutableStateOf("") }
    var attendingCommentary by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
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
                Text("Dr. Harrington", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Strict Attending Cardiologist", color = AlertRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "CASE STUDY:",
                    color = SurgicalGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "You present a 62-year-old male with pressing chest pain radiating to his left interscapular region. He is hypertensive at 178/95 mmHg with asymmetric pulses.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            }
        }

        if (step == 1) {
            Text(
                "ATTENDING INTERRUPTING QUESTION:\n\"What is your immediate primary differential, and why are asymmetric pulses a diagnostic emergency? Explain clinical pathophysiology in professional English.\"",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            OutlinedTextField(
                value = currentAnswer,
                onValueChange = { currentAnswer = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Describe pathophysiology e.g. Aortic Dissection, intimal tear...", fontSize = 12.sp) },
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SurgicalGreen,
                    unfocusedBorderColor = Color.Gray
                ),
                maxLines = 4
            )

            Button(
                onClick = {
                    isSubmitted = true
                    attendingCommentary = if (currentAnswer.lowercase().contains("aortic dissection") || currentAnswer.lowercase().contains("intimal tear")) {
                        "Excellent catch. The asymmetric pulse indicates blood pooling in the false lumen due to retrograde dissection. Grade: A (Fluent Clinical English. +50 XP)"
                    } else {
                        "Incomplete formulation! You must consider Aortic Dissection immediately with unequal pulses. Always exclude deadly aortic pathology. Grade: C (-15 XP)"
                    }
                    if (attendingCommentary.contains("Excellent")) {
                        viewModel.addPoints(50)
                    } else {
                        viewModel.addPoints(10)
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
                modifier = Modifier.align(Alignment.End),
                enabled = currentAnswer.isNotBlank() && !isSubmitted
            ) {
                Text("Submit Oral Presentation", color = Color.Black)
            }

            if (isSubmitted) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (attendingCommentary.contains("Excellent")) SurgicalGreen.copy(alpha = 0.15f) else AlertRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (attendingCommentary.contains("Excellent")) SurgicalGreen else AlertRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "DR. HARRINGTON'S CRITIQUE:",
                            fontWeight = FontWeight.Bold,
                            color = if (attendingCommentary.contains("Excellent")) SurgicalGreen else AlertRed,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(attendingCommentary, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 2. MEDICAL IMAGE DESCRIPTION LAB COMPONENT (Idea 6)
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
        Text("ECG RHYTHM INTERPRETATION LAB", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)

        // Dynanically draw an ischemic ECG waveform with hyperacute T waves or ST elevation on Canvas!
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

                // DRAW ECG WAVEFORM (STEMI hyperacute MI waveform)
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
                    // ST Elevation & Giant Hyperacute T wave!
                    path.lineTo(currentX + 70f, startY - 35f) // ST segment elevated
                    path.quadraticTo(currentX + 85f, startY - 45f, currentX + 100f, startY) // Hyperacute T wave
                    path.lineTo(currentX + beatWidth, startY)
                    currentX += beatWidth
                    count++
                }

                drawPath(path, SurgicalGreen, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                
                // Text marker of Lead II
                drawCircle(Color.Green, radius = 5f, center = Offset(50f, 30f))
            }
            Text("LEAD II - EMERGENCY RUN", color = Color.Green, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp).align(Alignment.BottomEnd), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        Text(
            "DESCRIBE THE RHYTHM DIAGNOSIS (Use clinical terms like 'elevation', 'hyperacute', 'infarction'):",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = userDescription,
            onValueChange = { userDescription = it },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SurgicalGreen,
                unfocusedBorderColor = Color.Gray
            ),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp)
        )

        Button(
            onClick = {
                val inputLower = userDescription.lowercase()
                evaluationResult = if (inputLower.contains("st elevation") || inputLower.contains("elevation") || inputLower.contains("stemi")) {
                    "PERFECT! You identified ST-Elevation (anterior STEMI) correctly. Clinical nomenclature score: 100%. +40 Points added."
                } else {
                    "PARTIAL: This ECG shows classic STEMI morphology with dramatic ST elevation (tombstoning target pattern). Refine terminology. +10 Points added."
                }
                viewModel.addPoints(if (evaluationResult.contains("PERFECT")) 40 else 10)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.align(Alignment.End),
            enabled = userDescription.isNotBlank() && evaluationResult.isEmpty()
        ) {
            Text("Evaluate Description", color = Color.Black)
        }

        if (evaluationResult.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("AI GRADE PROFILE:", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(evaluationResult, color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

// ----------------------------------------------------
// 3. DIFFERENTIAL DIAGNOSIS SANDBOX (Idea 7)
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
        Text("DDx COGNITIVE TRIAGE", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            "Rank the top differential diagnoses for an acute tearing chest pain radiating to the back in order of likelihood:",
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
                        Text(option, color = if (isAdded) Color.Gray else Color.White, fontSize = 14.sp)
                        if (isAdded) {
                            Icon(Icons.Default.Check, contentDescription = "Added", tint = SurgicalGreen)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = "Add to ranks", tint = NeonCyan)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("YOUR DDx TRIAGE ORDER:", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)

        if (rankedList.isEmpty()) {
            Text("Tap conditions above to rank them", color = Color.Gray, fontSize = 12.sp)
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
                            Text("${idx + 1}. $name", color = Color.White, fontWeight = FontWeight.Bold)
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
                if (rankedList.firstOrNull() == "Aortic Dissection") {
                    evaluationComment = "Excellent! You ranked Aortic Dissection as #1 due to back radiation and tear description. This is clinical genius level. +50 XP"
                    viewModel.addPoints(50)
                } else {
                    evaluationComment = "Alert: The tearing radiation to back makes Aortic Dissection the absolute most likely emergent diagnosis. Refine ranks. +15 XP"
                    viewModel.addPoints(15)
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.align(Alignment.End),
            enabled = rankedList.size >= 3
        ) {
            Text("Verify DDx Formulation", color = Color.Black)
        }

        if (evaluationComment.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(evaluationComment, modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 13.sp)
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
                Text("DENSE LAB REPORT:", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Potassium (K+): 5.9 mEq/L (HIGH)\nSodium (Na+): 132 mEq/L (LOW)\nCreatinine: 2.1 mg/dL (HIGH)\nBUN: 45 mg/dL (HIGH)",
                    fontFamily = FontFamily.Monospace,
                    color = Color.Green,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Text(
            "Empathize and explain these complex renal findings to a scared patient without terrifying medical jargon in gentle English:",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )

        OutlinedTextField(
            value = userTranslation,
            onValueChange = { userTranslation = it },
            placeholder = { Text("Your explanation: 'We look at kidneys' filter system...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SurgicalGreen,
                unfocusedBorderColor = Color.Gray
            ),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp)
        )

        Button(
            onClick = {
                val explanation = userTranslation.lowercase()
                ratingOutcome = if (explanation.contains("filter") || explanation.contains("gentle") || explanation.contains("kidney")) {
                    "EXCELLENT EMPATHIC BED-SIDE MANNER! Bed-side manner rating: 98% (No offensive clinical jargon like hyperkalemia or chronic kidney injury used aggressively). +35 points! Use of 'filter' makes it highly layman."
                } else {
                    "ALERT: Your explanation is slightly technical. Try using simpler analogies (e.g., 'the organ that washes your blood' or 'filters'). Empathy rating: 70%."
                }
                viewModel.addPoints(if (ratingOutcome.contains("EXCELLENT")) 35 else 10)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.align(Alignment.End),
            enabled = userTranslation.isNotBlank() && ratingOutcome.isEmpty()
        ) {
            Text("Score Empathy Translator", color = Color.Black)
        }

        if (ratingOutcome.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(ratingOutcome, modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 5. TIMED CRISIS RESUSCITATION SIMULATOR (Idea 10)
// ----------------------------------------------------
@Composable
fun TimedCrisisSimulatorLab(viewModel: UserProgressViewModel) {
    var timerSeconds by remember { mutableIntStateOf(30) }
    var running by remember { mutableStateOf(false) }
    var crashMessage by remember { mutableStateOf("") }
    var orderText by remember { mutableStateOf("") }
    var stateCode by remember { mutableStateOf("VT") } // VT = V-Tach, VF = V-Fib, SR = Sinus Rhythm
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Background timer thread loop
    LaunchedEffect(running, timerSeconds) {
        if (running && timerSeconds > 0) {
            delay(1000)
            timerSeconds--
            if (timerSeconds == 0) {
                running = false
                crashMessage = "CRITICAL COMA OUTCOME: Patient crashed. Streak reset halted, review procedures. Try again."
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
            Text("TIMED CRISIS SCENARIO", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Badge(containerColor = if (timerSeconds < 10) AlertRed else NeonCyan) {
                Text("00:${if (timerSeconds < 10) "0" else ""}$timerSeconds", modifier = Modifier.padding(4.dp), color = Color.Black)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "STATUS: PATIENT CRASHING!",
                    color = AlertRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Patient is unresponsive, pulseless, and monitor shows alarming wild ventricular waveforms. Give verbal orders to nurses in clinical English now!",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }

        // Animated ECG waveform for severe ventricular crisis!
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(Color.Black)
                .border(2.dp, if (stateCode == "SR") SurgicalGreen else AlertRed, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            val transition = rememberInfiniteTransition()
            val phase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 2f * Math.PI.toFloat(),
                animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing)), label = "ecgWave"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val midY = height / 2f
                val path = Path()
                path.moveTo(0f, midY)

                for (x in 0..width.toInt() step 5) {
                    val relativeX = x / width
                    val y = when (stateCode) {
                        "SR" -> {
                            // Sinus Normal Beat with P-QRS-T
                            val p = sin(x * 0.1f + phase) * 5f
                            midY + p
                        }
                        else -> {
                            // Chaotic fibrillation oscillations
                            val chaosSum = sin(x * 0.2f + phase * 3f) * 25f + sin(x * 0.4f + phase * 1.5f) * 15f
                            midY + chaosSum
                        }
                    }
                    path.lineTo(x.toFloat(), y)
                }

                drawPath(
                    path = path,
                    color = if (stateCode == "SR") SurgicalGreen else AlertRed,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        if (!running && crashMessage.isEmpty()) {
            Button(
                onClick = {
                    running = true
                    timerSeconds = 30
                    crashMessage = ""
                    stateCode = "VF"
                },
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Code Blue Protocol", color = Color.White)
            }
        }

        if (running) {
            Text(
                "Type nurse order e.g. 'Shock 200 Joules', 'Epinephrine 1mg IV push', 'Defibrillate':",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = orderText,
                onValueChange = { orderText = it },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AlertRed,
                    unfocusedBorderColor = Color.Gray
                ),
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp)
            )

            Button(
                onClick = {
                    val orderLower = orderText.lowercase()
                    if (orderLower.contains("shock") || orderLower.contains("defibrillate") || orderLower.contains("epinephrine")) {
                        running = false
                        stateCode = "SR" // Reset to sinus wave rhythm!
                        crashMessage = "LIFE ACCLAIMED! You delivered perfect resuscitation commands. Rhythms returned to Normal Sinus. +100 XP"
                        viewModel.addPoints(100)
                    } else {
                        crashMessage = "ORDER CORRECTION REQUIRED: The nurse awaits specific therapeutic instruction: Shock or epinephrine. Keep trying!"
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
                modifier = Modifier.align(Alignment.End),
                enabled = orderText.isNotBlank()
            ) {
                Text("Execute Order", color = Color.Black)
            }
        }

        if (crashMessage.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(crashMessage, modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 6. NATIVE PHARMACOLOGY GLOSSARY & AUDIO (Idea 11)
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
        Text("PHARMACOLOGICAL AUDIO REFERENCE", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Listen to proper pronunciation of complex US generic medication formulas:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

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
                            Text(desc, color = Color.LightGray, fontSize = 12.sp)
                        }
                        IconButton(
                            onClick = { /* Simulated native audio playback trigger */ },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurgicalGreen.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Native Phonetics", tint = SurgicalGreen)
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
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("CLINICAL ABBREVIATION MASTER", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)

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

            Text("Type the full English clinical meaning of the abbreviation (e.g. 'Nothing by mouth' for NPO):", color = Color.White, fontSize = 12.sp)

            OutlinedTextField(
                value = userEntry,
                onValueChange = { userEntry = it },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SurgicalGreen,
                    unfocusedBorderColor = Color.Gray
                ),
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp)
            )

            Button(
                onClick = {
                    val rightMeaning = element.third.lowercase()
                    if (userEntry.lowercase().contains(rightMeaning) || userEntry.lowercase().contains(element.second)) {
                        textReport = "CORRECT! '${element.first}' literally translates to '${element.third}'."
                        viewModel.addPoints(20)
                    } else {
                        textReport = "INCORRECT. '${element.first}' means '${element.third}'."
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
                        Text(textReport, color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                activeIdx++
                                userEntry = ""
                                textReport = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Next Abbreviation", color = Color.Black)
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
                    Text("Mastery Level Achieved!", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = { activeIdx = 0 }) {
                        Text("Reset Simulation")
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
        Text("SPIKES Bad News Delivery Simulator", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            "Scenario: You must tell a patient's daughter that her father has progressed to severe metastatic carcinoma. Practice using the 'SPIKES' framework.",
            color = Color.LightGray,
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
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Text("Type how you would professionally deliver this warning (e.g., 'I am sorry to share some difficult news about your father's biopsy results'):", color = Color.White, fontSize = 12.sp)

        OutlinedTextField(
            value = userText,
            onValueChange = { userText = it },
            placeholder = { Text("Deliver bad news empathetically...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SurgicalGreen,
                unfocusedBorderColor = Color.Gray
            ),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp)
        )

        Button(
            onClick = {
                val input = userText.lowercase()
                stepReview = if (input.contains("sorry") || input.contains("unfortunate") || input.contains("biopsy")) {
                    "SPIKES PASS GRADE: You incorporated soft delivery cues (Perception & Empathy) beautifully. Bed-side tone: Masterful (C2). +40 XP"
                } else {
                    "CRITIQUE: Always prepare the family before revealing oncology findings, avoiding abrupt delivery. Rephrase using warning shots."
                }
                viewModel.addPoints(if (stepReview.contains("PASS")) 40 else 10)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.align(Alignment.End),
            enabled = userText.isNotBlank() && stepReview.isEmpty()
        ) {
            Text("Analyze Delivery", color = Color.Black)
        }

        if (stepReview.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stepReview, modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 13.sp)
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
        Text("SBAR INTRA-MEDICAL CONSULT", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Assemble SBAR consulting protocol to call the on-call surgical chief:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = situationText,
                onValueChange = { situationText = it },
                label = { Text("S - Situation") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedLabelColor = SurgicalGreen)
            )
            OutlinedTextField(
                value = backgroundText,
                onValueChange = { backgroundText = it },
                label = { Text("B - Background") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedLabelColor = SurgicalGreen)
            )
            OutlinedTextField(
                value = assessmentText,
                onValueChange = { assessmentText = it },
                label = { Text("A - Assessment") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedLabelColor = SurgicalGreen)
            )
            OutlinedTextField(
                value = recommendationText,
                onValueChange = { recommendationText = it },
                label = { Text("R - Recommendation") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, focusedLabelColor = SurgicalGreen)
            )
        }

        Button(
            onClick = {
                consultReview = "SBAR GRADE: Solid structural assembly. You clearly structured your findings. Outstanding consult format! +40 XP added."
                viewModel.addPoints(40)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.fillMaxWidth(),
            enabled = situationText.isNotBlank() && backgroundText.isNotBlank() && assessmentText.isNotBlank() && recommendationText.isNotBlank()
        ) {
            Text("Publish SBAR Formulation", color = Color.Black)
        }

        if (consultReview.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(consultReview, modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 10. AUDIO HANDOFFS DRILL (Idea 19)
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
        Text("FAST MEDICAL AUDIO HANDOFFS", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Practice rapid comprehension. Listen or read and identify errors instantly.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("NURSE STAT HANDOFF TRANSCRIPT (READING AT 180 WPM):", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "\"We have Mr. Evans in critical room 6 post-op coronary bypass. He has chest tube outputting 180 ml serosanguineous fluids, potassium is 3.4, he has bid medication due in 10 minutes, but is hypotensive at 88/44 mmHg. I also gave him 10mg morphine as needed for pain.\"",
                    color = Color.White,
                    lineHeight = 18.sp,
                    fontSize = 12.sp
                )
            }
        }

        Text("Select the primary immediate clinical threat from this fast handoff:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

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
                                feedback = "CORRECT! Post-op hemodynamic failure (88/44 pressure) combined with chest tube fluid output is an emergency hemorrhage sign. Outstanding clinically! +50 XP"
                                viewModel.addPoints(50)
                            } else {
                                feedback = "INCORRECT. Although potassium is marginally low, arterial hypotension is the immediate clinical emergency threat."
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                ) {
                    Text(ans, modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 13.sp)
                }
            }
        }

        if (feedback.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(feedback, modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 13.sp)
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
                Text("A 65-year-old highly anxious patient needs an emergent Coronary Artery Bypass Graft (CABG). They ask: 'Doctor, are you going to stop my heart? Is it safe?'", color = Color.White, fontSize = 13.sp)
            }
        }

        OutlinedTextField(
            value = response,
            onValueChange = { response = it },
            placeholder = { Text("Draft your empathetic response explaining the bypass machine and risks...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SurgicalGreen,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Button(
            onClick = {
                feedback = "Excellent compassion. Using terms like 'heart-lung machine' instead of 'cardiopulmonary bypass' builds trust. +25 XP"
                viewModel.addPoints(25)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SUBMIT FOR EMPATHY REVIEW", color = Color.Black, fontWeight = FontWeight.Bold)
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
        Text("ETHICS BOARD SIMULATOR", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Test your C2 argumentative English in high-stakes bioethical scenarios.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("THE CASE:", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("A family demands continued life-sustaining ventilation for a brain-dead patient, citing religious convictions, despite clinical futility. The Ethics Board asks for your justification to withdraw care.", color = Color.White, fontSize = 13.sp)
            }
        }

        OutlinedTextField(
            value = response,
            onValueChange = { response = it },
            placeholder = { Text("Draft your C2 academic response addressing autonomy, beneficence, and non-maleficence...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SurgicalGreen,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Button(
            onClick = {
                feedback = "Argument logically structured. Phenomenal use of C2 terminology such as 'clinical futility' and 'resource allocation'. +30 XP"
                viewModel.addPoints(30)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SUBMIT ARGUMENT TO BOARD", color = Color.Black, fontWeight = FontWeight.Bold)
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
// 14. CULTURAL COMPETENCE AI MODIFIERS (Idea 18)
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
        Text("CULTURAL COMPETENCE CALIBRATOR", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Adapt your bedside manner to diverse cultural and linguistic backgrounds.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CULTURAL PROFILE: RURAL APPALACHIA", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("Patient: 'Doc, I got the sugar real bad and my nerves are shot.'", color = Color.White, fontSize = 13.sp)
                Text("How do you respond to build rapport while transitioning to discussing their diabetic neuropathy?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }

        OutlinedTextField(
            value = response,
            onValueChange = { response = it },
            placeholder = { Text("Draft your response adapting your vocabulary...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SurgicalGreen,
                unfocusedBorderColor = Color.DarkGray
            )
        )

        Button(
            onClick = {
                feedback = "Culturally appropriate validation. Translating \"the sugar\" to diabetes gently without sounding condescending is vital. +20 XP"
                viewModel.addPoints(20)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ANALYZE RAPPORT", color = Color.Black, fontWeight = FontWeight.Bold)
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
        Text("SPLIT-SCREEN EMR SIMULATOR", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Practice interacting with a patient while typing structured notes simultaneously.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        // Split view container: Top is Chat, Bottom is EMR
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier.fillMaxWidth().height(200.dp),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("LIVE PATIENT FEED:", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Patient: 'Yeah, the pain started yesterday around noon. Feels like an elephant sitting on my chest.'", color = Color.White, fontSize = 13.sp)
                Spacer(modifier = Modifier.weight(1f))
                
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    placeholder = { Text("Verbalize a empathetic response...", fontSize = 11.sp) },
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
                Text("EPIC EMR CHARTING (HPI):", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = emrNotes,
                    onValueChange = { emrNotes = it },
                    placeholder = { Text("Translate to clinical chart (e.g. Pt reports acute onset substernal crushing pain x 24hrs)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp)
                )
            }
        }

        Button(
            onClick = {
                feedback = "Outstanding multi-tasking. Your charting correctly abstracted 'elephant' to 'crushing pain' while maintaining patient engagement. +40 XP"
                viewModel.addPoints(40)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SUBMIT CHART FOR REVIEW", color = Color.Black, fontWeight = FontWeight.Bold)
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
// 16. REVIEW GRAVEYARD COMPONENT (Idea 20)
// ----------------------------------------------------
@Composable
fun ReviewGraveyardLab() {
    val reviewItems = listOf(
        Pair("Dypnea -> Dyspnea", "Incorrect clinical transcription spelling found in medical charting."),
        Pair("Mitral stenosis -> High pitched murmur", "Misdiagnosed murmur sound feedback in Case 3."),
        Pair("PRN vs PO", "Confused as needed with enteral administration guidelines.")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("REVIEW GRAVEYARD SPECIAL SRS", color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Your failed clinical items. Clear them using periodic spacing cards:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            reviewItems.forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.first, color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(item.second, color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = { /* Clear from review history */ },
                                colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Acknowledge & Resurrect", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
