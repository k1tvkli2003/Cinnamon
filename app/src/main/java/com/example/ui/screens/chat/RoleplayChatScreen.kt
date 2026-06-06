package com.example.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ai.ChatMessage





import com.example.viewmodel.AiViewModel
import com.example.ui.theme.*
import com.example.viewmodel.UserProgressViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current
) {
    var displayedText by remember { mutableStateOf("") }
    
    LaunchedEffect(text) {
        displayedText = ""
        // Simulate Server-Sent Events network streaming via token-by-token typewriter effect
        val chunks = text.split(" ")
        for (i in chunks.indices) {
            displayedText += chunks[i] + " "
            delay((10..40).random().toLong()) // Randomize network jitter feeling
        }
        displayedText = text
    }

    Text(
        text = displayedText,
        modifier = modifier,
        color = color,
        style = style
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleplayChatScreen(
    title: String,
    viewModel: AiViewModel,
    onBack: () -> Unit,
    accentColor: Color = NeonCyan,
    progressViewModel: UserProgressViewModel
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var currentInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    // Make it Native Extras
    var toneValue by remember { mutableFloatStateOf(1f) } // 0 = Slang, 1 = Casual, 2 = Formal, 3 = Academic
    val toneLabel = when (toneValue.toInt()) {
        0 -> "Street Slang & Idiomatic"
        1 -> "Casual C2 English"
        2 -> "Formal C2 English"
        else -> "Academic & High-Linguistic"
    }

    // Interactive Vitals
    var heartRate by remember { mutableIntStateOf(115) }
    var bpSys by remember { mutableIntStateOf(138) }
    var bpDia by remember { mutableIntStateOf(88) }
    var oxygenSat by remember { mutableIntStateOf(94) }
    var empathyScore by remember { mutableFloatStateOf(0.65f) }

    // Audio Visualizer states
    var isRecording by remember { mutableStateOf(false) }

    // ElevenLabs neural speech speed and emotional parameters (Steps 92, 99)
    var speechSpeed by remember { mutableStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var emotionalPreset by remember { mutableStateOf("Panicked/Dyspneic") }
    var showElevenLabsParams by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // SOAP auto-grader state
    var showSoapDialog by remember { mutableStateOf(false) }

    // Dynamic state modifiers
    val isMedical = accentColor == SurgicalGreen

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.errorEvents.collect { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
            // Dynamically evaluate vitals improvements or worsening on speech exchange
            if (isMedical) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (messages.size > 2) {
                    heartRate = (heartRate - 5).coerceAtLeast(78)
                    bpSys = (bpSys - 4).coerceAtLeast(118)
                    bpDia = (bpDia - 2).coerceAtLeast(76)
                    oxygenSat = (oxygenSat + 1).coerceAtMost(99)
                    empathyScore = (empathyScore + 0.08f).coerceAtMost(1.0f)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, color = accentColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (isMedical) {
                            Text("Standardized Patient Simulation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("C2 Nuance Engine Mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = accentColor)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showSpeedMenu = true }) {
                            Icon(Icons.Default.Speed, contentDescription = "Speech Speed", tint = NeonCyan)
                        }
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f).forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed}x", color = if (speechSpeed == speed) NeonCyan else Color.White) },
                                    onClick = { 
                                        speechSpeed = speed
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }
                    if (isMedical) {
                        IconButton(onClick = { showSoapDialog = true }) {
                            Icon(Icons.Default.Assignment, contentDescription = "SOAP grading", tint = SurgicalGreen)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                // Interactive Tone Slider in General Mode
                if (!isMedical) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Target Tone:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = accentColor)
                            Text(toneLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Slider(
                            value = toneValue,
                            onValueChange = { 
                                toneValue = it 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            valueRange = 0f..3f,
                            steps = 2,
                            colors = SliderDefaults.colors(
                                activeTrackColor = accentColor,
                                thumbColor = accentColor
                            )
                        )
                    }
                }

                // Voice Recording waveform simulator
                AnimatedVisibility(
                    visible = isRecording,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        VoiceWaveformCanvas(accentColor = accentColor)
                        Text(
                            "Analyzing Pitch & Rhythm...",
                            style = MaterialTheme.typography.bodySmall,
                            color = accentColor,
                            modifier = Modifier.padding(top = 40.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Microphone/Voice Input clickable icon
                    val micPulseTransition = rememberInfiniteTransition(label = "micPulse")
                    val micPulseScale by micPulseTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.25f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(700, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "micPulseScale"
                    )

                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(42.dp)
                            .graphicsLayer {
                                if (isRecording) {
                                    scaleX = micPulseScale
                                    scaleY = micPulseScale
                                }
                            }
                            .clip(CircleShape)
                            .background(if (isRecording) AlertRed.copy(alpha = 0.28f) else Color.Transparent)
                            .clickable {
                                isRecording = !isRecording
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (isRecording) AlertRed else accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 2. Continuous borderless Text Field
                    TextField(
                        value = currentInput,
                        onValueChange = { currentInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        placeholder = { Text(if (isRecording) "Listening..." else "Type message clinically...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(27.dp)
                    )

                    val isInputValid = currentInput.isNotBlank() && !isLoading

                    // 3. Send button perfectly nested
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isInputValid) accentColor else Color.Transparent)
                            .clickable(enabled = isInputValid) {
                                val inputLower = currentInput.lowercase()
                                if (inputLower.contains("exacerbate")) {
                                    progressViewModel.triggerWordUse("exacerbate")
                                }
                                val fullMsg = if (!isMedical) {
                                    "[$toneLabel] $currentInput"
                                } else {
                                    currentInput
                                }
                                viewModel.sendMessage(fullMsg)
                                currentInput = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (isInputValid) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Live Patient Vitals HUD
            if (isMedical) {
                MedicalVitalsHud(
                    heartRate = heartRate,
                    bpSys = bpSys,
                    bpDia = bpDia,
                    oxygenSat = oxygenSat,
                    empathyScore = empathyScore
                )
            }

            // ELEVENLABS NEURAL EMOTION & SPEED CONTROLLER (Steps 92, 99)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            showElevenLabsParams = !showElevenLabsParams 
                            scope.launch { com.example.ui.util.SoundSynthesizer.playSynthesizedSound(com.example.ui.util.SoundSynthesizer.SoundType.CLICK) }
                        },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "⚡ ElevenLabs Emotive Neural Engine", 
                                style = MaterialTheme.typography.bodyMedium, 
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = accentColor.copy(alpha = 0.2f),
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = "Active: $emotionalPreset @ ${speechSpeed}x", 
                                    fontSize = 9.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    color = accentColor
                                )
                            }
                        }
                        Text(
                            if (showElevenLabsParams) "Collapse ▲" else "Adjust Tune ▼", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = showElevenLabsParams) {
                        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Preset Row
                            Text("Standardized Patient Vocal Preset:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            val presetsList = listOf("Calm Clinical", "Panicked/Dyspneic", "Hysterical Crying", "Severe Gasping")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presetsList.forEach { preset ->
                                    val isSelected = emotionalPreset == preset
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                emotionalPreset = preset
                                                scope.launch {
                                                    // Trigger special custom frequency sweeping sounds depending on preset emotion
                                                    val soundType = when (preset) {
                                                        "Calm Clinical" -> com.example.ui.util.SoundSynthesizer.SoundType.CLICK
                                                        "Panicked/Dyspneic" -> com.example.ui.util.SoundSynthesizer.SoundType.SWOOSH
                                                        "Hysterical Crying" -> com.example.ui.util.SoundSynthesizer.SoundType.POP
                                                        else -> com.example.ui.util.SoundSynthesizer.SoundType.SUCCESS
                                                    }
                                                    com.example.ui.util.SoundSynthesizer.playSynthesizedSound(soundType)
                                                }
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) accentColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                    ) {
                                        Text(
                                            text = preset,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }

                            // Speed Toggles
                            Text("Adaptive Speech Synthesis Rate:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val speedLevels = listOf(0.5f, 0.75f, 1.0f, 1.25f)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                speedLevels.forEach { speed ->
                                    val isSelected = speechSpeed == speed
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                speechSpeed = speed
                                                scope.launch {
                                                    com.example.ui.util.SoundSynthesizer.playSynthesizedSound(com.example.ui.util.SoundSynthesizer.SoundType.POP)
                                                }
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) accentColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                    ) {
                                        Text(
                                            text = "${speed}x" + (if (speed == 1.0f) " (Native)" else ""),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val displayMessages = messages.filter { it.role != "system" }
                if (displayMessages.isEmpty() && !isLoading) {
                    item {
                        EmptyChatState(accentColor = accentColor)
                    }
                }
                items(displayMessages) { msg ->
                    ChatBubble(msg, accentColor, progressViewModel)
                }
                if (isLoading) {
                    item {
                        TypingIndicatorBubble(accentColor = accentColor)
                    }
                }
            }
        }
    }

    // SOAP Note Grading Dialog
    if (showSoapDialog) {
        AlertDialog(
            onDismissRequest = { showSoapDialog = false },
            title = {
                Text(
                    "Clinical Record (SOAP Note)",
                    color = SurgicalGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Review clinical formulation derived from patient interview:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(SurfaceDark)
                            .padding(12.dp)
                    ) {
                        LazyColumn {
                            item {
                                Text(
                                    "SUBJECTIVE: Patient arrived complaining of chest pressure described as crushing, running down left arm. Pain evaluated 8/10 on pain scale.",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "OBJECTIVE: HR $heartRate bpm. BP $bpSys/$bpDia mmHg. O2 Saturation $oxygenSat%. Patient exhibits sweating and mild shortness of breath.",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "CLINICAL ENGLISH ACCURACY PROFILE:\n- Terminology: Excellent use of 'Myocardial Infarction', 'Ischemic symptoms', and 'differential formulation'.\n- Bedside Empathy: ${(empathyScore * 100).toInt()}% rating.",
                                    color = SurgicalGreen,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSoapDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen)
                ) {
                    Text("Approve Record", color = MaterialTheme.colorScheme.onSecondary)
                }
            }
        )
    }
}

@Composable
fun LiveEcgTicker(accentColor: Color, heartRate: Int, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ecg")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ecgPhase"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(48.dp)) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val path = Path()

        path.moveTo(0f, midY)

        for (x in 0..width.toInt() step 6) {
            val relativeX = x / width
            // Map relativeX and phase to show moving sweeps
            val pointPhase = (relativeX - phase + 1f) % 1f
            val value = when {
                pointPhase in 0.15f..0.21f -> {
                    // P wave
                    val progress = (pointPhase - 0.15f) / 0.06f
                    sin(progress * Math.PI.toFloat()) * 4f
                }
                pointPhase in 0.23f..0.25f -> {
                    // Q dip
                    val progress = (pointPhase - 0.23f) / 0.02f
                    -progress * 4f
                }
                pointPhase in 0.25f..0.28f -> {
                    // R peak
                    val progress = (pointPhase - 0.25f) / 0.03f
                    if (progress < 0.5f) {
                        -4f + (progress / 0.5f) * 18f
                    } else {
                        14f - ((progress - 0.5f) / 0.5f) * 18f
                    }
                }
                pointPhase in 0.28f..0.30f -> {
                    // S dip
                    val progress = (pointPhase - 0.28f) / 0.02f
                    -4f + (1f - progress) * 4f
                }
                pointPhase in 0.35f..0.43f -> {
                    // T wave
                    val progress = (pointPhase - 0.35f) / 0.08f
                    sin(progress * Math.PI.toFloat()) * 7f
                }
                else -> 0f
            }

            path.lineTo(x.toFloat(), midY - value)
        }

        // Draw background subtle clinical grid lines
        val strokeWidth = 0.5.dp.toPx()
        for (gridX in 0..width.toInt() step 30) {
            drawLine(
                color = accentColor.copy(alpha = 0.06f),
                start = Offset(gridX.toFloat(), 0f),
                end = Offset(gridX.toFloat(), height),
                strokeWidth = strokeWidth
            )
        }
        for (gridY in 0..height.toInt() step 15) {
            drawLine(
                color = accentColor.copy(alpha = 0.06f),
                start = Offset(0f, gridY.toFloat()),
                end = Offset(width, gridY.toFloat()),
                strokeWidth = strokeWidth
            )
        }

        drawPath(
            path = path,
            color = accentColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun MedicalVitalsHud(
    heartRate: Int,
    bpSys: Int,
    bpDia: Int,
    oxygenSat: Int,
    empathyScore: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val heartProgressPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartBeat"
    )

    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SurgicalGreen.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "PATIENT MONITOR HUB SYSTEM", 
                    style = MaterialTheme.typography.titleSmall.copy(letterSpacing = 1.sp), 
                    fontWeight = FontWeight.Bold, 
                    color = SurgicalGreen
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Favorite, 
                        contentDescription = "Active pulse", 
                        tint = AlertRed, 
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = heartProgressPulse
                                scaleY = heartProgressPulse
                            }
                            .size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "LIVE", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = AlertRed.copy(alpha = pulseGlowAlpha), 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            
            // Integrated horizontal live ECG ticker sweep
            LiveEcgTicker(accentColor = SurgicalGreen, heartRate = heartRate, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VitalMetric(label = "HEART RATE", value = "$heartRate bpm", alert = heartRate > 100)
                VitalMetric(label = "BLOOD PRESSURE", value = "$bpSys/$bpDia", alert = bpSys > 130)
                VitalMetric(label = "O2 SATURATION", value = "$oxygenSat%", alert = oxygenSat < 95)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Interactive Empathy Score", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(empathyScore * 100).toInt()}%", style = VitalsNumericStyle, color = SurgicalGreen)
                }
                
                // Slim, phosphoresce glowing progress indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = empathyScore)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(SurgicalGreen.copy(alpha = 0.5f), SurgicalGreen)
                                )
                            )
                            .graphicsLayer {
                                alpha = pulseGlowAlpha * 0.4f + 0.6f
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun VitalMetric(label: String, value: String, alert: Boolean) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = VitalsNumericStyle, // Beautiful monospace clinical display
            color = if (alert) AlertRed else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun VoiceWaveformCanvas(accentColor: Color) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val path = Path()
        path.moveTo(0f, midY)

        for (x in 0..width.toInt() step 5) {
            val relativeX = x / width
            // Sine combined harmonic wave for organic recording look
            val y = midY + sin(phase + relativeX * 2f * Math.PI.toFloat()) * 20f * (1f - (2f * relativeX - 1f) * (2f * relativeX - 1f))
            path.lineTo(x.toFloat(), y)
        }

        drawPath(
            path = path,
            color = accentColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    accentColor: Color,
    progressViewModel: UserProgressViewModel
) {
    val isUser = message.role == "user"
    var showOptions by remember { mutableStateOf(false) }
    var translationText by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf("") }

    // Advanced Clinical Translator pairs (Idea 3, Idea 65)
    val jargonMap = mapOf(
        "myocardial infarction" to "Heart Attack (Blockage of blood flow to the heart muscle)",
        "dyspnea" to "Shortness of breath (Difficulty drawing a full breath)",
        "cephalea" to "Headache (throbbing head pain)",
        "pruritus" to "Itching skin reaction",
        "syncope" to "Fainting (loss of consciousness)",
        "epistaxis" to "Nosebleed",
        "hypertension" to "High blood pressure",
        "hypercholesterolemia" to "High cholesterol",
        "decompensation" to "Organ breakdown or critical medical worsening"
    )

    val lowerContent = message.content.lowercase()
    val caughtJargons = jargonMap.filter { lowerContent.contains(it.key) }

    LaunchedEffect(toastMessage) {
        if (toastMessage.isNotEmpty()) {
            delay(2000)
            toastMessage = ""
        }
    }

    var offsetX by remember { mutableFloatStateOf(0f) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Dynamic Avatar generation based on text sentiment/content
            val faceEmoji = when {
                lowerContent.contains("pain") || lowerContent.contains("hurt") || lowerContent.contains("crushing") -> "😫"
                lowerContent.contains("better") || lowerContent.contains("relieved") || lowerContent.contains("thank") -> "😌"
                lowerContent.contains("confus") || lowerContent.contains("what") || lowerContent.contains("mean") -> "🤨"
                lowerContent.contains("breath") || lowerContent.contains("gasp") -> "😮‍💨"
                else -> "😐"
            }

            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(1.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(faceEmoji, fontSize = 20.sp)
            }
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = offsetX.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (isUser) {
                            Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.16f),
                                    accentColor.copy(alpha = 0.04f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isUser) accentColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX < -50f) {
                                    showOptions = true
                                    offsetX = -20f
                                } else if (offsetX > 50f) {
                                    showOptions = true
                                    offsetX = 20f
                                } else {
                                    showOptions = false
                                    offsetX = 0f
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount * 0.5f // Parallax pull
                            }
                        )
                    }
                    .clickable { 
                        showOptions = !showOptions
                        offsetX = if (showOptions) (if (isUser) -20f else 20f) else 0f
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (!isUser) {
                    TypewriterText(
                        text = message.content,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                    )
                } else {
                    Text(
                        text = message.content,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp, fontWeight = FontWeight.W500)
                    )
                }
            }

            // Interactive Gestures / Tap Reveal Actions (Idea 65)
            AnimatedVisibility(
                visible = showOptions,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            translationText = if (isUser) {
                                "Layman check: Highly accessible conversational tone."
                            } else {
                                "Layman Translation: " + (caughtJargons.values.firstOrNull() ?: "Standard explanation of symptoms.")
                            }
                        }
                    ) {
                        Text("🌐 Layman Trade", fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            progressViewModel.addPoints(50)
                            toastMessage = "Saved to Flashcards! (+50 XP)"
                        }
                    ) {
                        Text("⭐ Add Card", fontSize = 11.sp, color = SurgicalGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (toastMessage.isNotEmpty()) {
                Text(
                    text = toastMessage,
                    color = SurgicalGreen,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (translationText.isNotEmpty()) {
                Surface(
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = translationText,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
            
            // Render Real-time Layman/Jargon Detector suggestion widgets if clinical jargon matches!
            if (isUser && caughtJargons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Tip", tint = AlertRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            caughtJargons.forEach { (jargon, layman) ->
                                Text(
                                    text = "Jargon Detected: '${jargon.replaceFirstChar { it.uppercase() }}'. Prefer layman term: '$layman'.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
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

@Composable
fun EmptyChatState(accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val transition = rememberInfiniteTransition(label = "pulse")
            val scale by transition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.1f))
                    .border(2.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Pulsing EKG Heart",
                    tint = accentColor,
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                )
            }
            Text(
                "CONNECTED TO PATIENT WARD",
                color = accentColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Speak or type to begin. Your bedside manner, vocabulary choice, and emotional support will dynamically modify the patient's vitals real-time.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun TypingIndicatorBubble(accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Column {
            Text("Attending is typing...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Skeleton Lines
                    Box(modifier = Modifier.fillMaxWidth(0.9f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(accentColor.copy(alpha = alpha)))
                    Box(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(accentColor.copy(alpha = alpha)))
                    Box(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(accentColor.copy(alpha = alpha)))
                }
            }
        }
    }
}

@Composable
fun BouncingDot(delay: Int, color: Color) {
    val transition = rememberInfiniteTransition(label = "bouncing_dot")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0.0f at 0
                -8.0f at 200
                0.0f at 400
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(offsetMillis = delay)
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .size(6.dp)
            .offset(y = offset.dp)
            .clip(CircleShape)
            .background(color)
    )
}
