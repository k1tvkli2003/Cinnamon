package com.cinnamon.app.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinnamon.app.data.ai.ChatMessage
import com.cinnamon.app.data.local.AppDatabase
import com.cinnamon.app.data.local.Flashcard





import com.cinnamon.app.viewmodel.AiViewModel
import com.cinnamon.app.ui.feedback.LocalCinnamonFeedbackPreferences
import com.cinnamon.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

enum class RoleplayPracticeMode {
    NativeCoach,
    StandardizedPatient
}

@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current
) {
    val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion
    var displayedText by remember { mutableStateOf("") }
    
    LaunchedEffect(text, reduceMotion) {
        if (reduceMotion) {
            displayedText = text
            return@LaunchedEffect
        }
        displayedText = ""
        // Keep the response legible while it appears; this is a local visual effect.
        val chunks = text.split(" ")
        for (i in chunks.indices) {
            displayedText += chunks[i] + " "
            delay(20)
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
    practiceMode: RoleplayPracticeMode,
    accentColor: Color = NeonCyan
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var currentInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion

    // Make it Native Extras
    var toneValue by remember { mutableFloatStateOf(1f) } // 0 = Slang, 1 = Casual, 2 = Formal, 3 = Academic
    val toneLabel = when (toneValue.toInt()) {
        0 -> "Street Slang & Idiomatic"
        1 -> "Casual C2 English"
        2 -> "Formal C2 English"
        else -> "Academic & High-Linguistic"
    }

    // Audio Visualizer states
    var isRecording by remember { mutableStateOf(false) }

    // Local visual practice parameters. They do not imply a live speech provider.
    var speechSpeed by remember { mutableStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var emotionalPreset by remember { mutableStateOf("Panicked/Dyspneic") }
    var showVoicePracticeControls by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Scenario-note reference state. This app does not grade clinical performance.
    var showSoapDialog by remember { mutableStateOf(false) }

    // Dynamic state modifiers
    val isMedical = practiceMode == RoleplayPracticeMode.StandardizedPatient

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
            // A small local acknowledgement, not an evaluation of the learner or patient.
            if (isMedical && messages.size > 2) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                            Text("Nuance practice mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    modifier = Modifier.semantics {
                                        selected = speechSpeed == speed
                                    },
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
                            Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Scenario note reference", tint = SurgicalGreen)
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

                // A local visual rehearsal aid; this does not record or transcribe audio.
                AnimatedVisibility(
                    visible = isRecording,
                    enter = if (reduceMotion) EnterTransition.None else expandVertically() + fadeIn(),
                    exit = if (reduceMotion) ExitTransition.None else shrinkVertically() + fadeOut()
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
                            "Voice practice preview (microphone not connected)",
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
                    val micPulseScale = if (reduceMotion) {
                        1f
                    } else {
                        val micPulseTransition = rememberInfiniteTransition(label = "micPulse")
                        micPulseTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.25f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(700, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "micPulseScale"
                        ).value
                    }

                    Box(
                        modifier = Modifier
                            .padding(start = 3.dp)
                            .size(48.dp)
                            .toggleable(
                                value = isRecording,
                                role = Role.Switch,
                                onValueChange = { recording ->
                                    isRecording = recording
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .graphicsLayer {
                                    if (isRecording) {
                                        scaleX = micPulseScale
                                        scaleY = micPulseScale
                                    }
                                }
                                .clip(CircleShape)
                                .background(if (isRecording) AlertRed.copy(alpha = 0.28f) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Voice practice preview",
                                tint = if (isRecording) AlertRed else accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 2. Continuous borderless Text Field
                    TextField(
                        value = currentInput,
                        onValueChange = { currentInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        placeholder = { Text(if (isRecording) "Voice practice preview" else "Type a practice message...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 14.sp) },
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
                            .padding(end = 3.dp)
                            .size(48.dp)
                            .clickable(enabled = isInputValid) {
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
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isInputValid) accentColor else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (isInputValid) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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
            // Fictional scene cues, deliberately separated from clinical monitoring or scoring.
            if (isMedical) {
                RoleplayCuePanel(
                    emotionalPreset = emotionalPreset,
                    messageCount = messages.size
                )
            }

            // Local voice-practice settings; no live synthesis service is connected.
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                showVoicePracticeControls = !showVoicePracticeControls
                                scope.launch {
                                    com.cinnamon.app.ui.util.SoundSynthesizer.playSynthesizedSound(
                                        com.cinnamon.app.ui.util.SoundSynthesizer.SoundType.CLICK
                                    )
                                }
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Voice practice controls",
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
                                    text = "Visual practice: $emotionalPreset @ ${speechSpeed}x",
                                    fontSize = 9.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    color = accentColor
                                )
                            }
                        }
                        Text(
                            if (showVoicePracticeControls) "Collapse ▲" else "Practice settings ▼",
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = showVoicePracticeControls,
                        enter = if (reduceMotion) EnterTransition.None else fadeIn() + expandVertically(),
                        exit = if (reduceMotion) ExitTransition.None else fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Preset Row
                            Text("Practice scenario:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            val presetsList = listOf("Calm Clinical", "Panicked/Dyspneic", "Hysterical Crying", "Severe Gasping")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectableGroup(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presetsList.forEach { preset ->
                                    val isSelected = emotionalPreset == preset
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .selectable(
                                                selected = isSelected,
                                                role = Role.RadioButton,
                                                onClick = {
                                                    emotionalPreset = preset
                                                    scope.launch {
                                                        // Trigger special custom frequency sweeping sounds depending on preset emotion
                                                        val soundType = when (preset) {
                                                            "Calm Clinical" -> com.cinnamon.app.ui.util.SoundSynthesizer.SoundType.CLICK
                                                            "Panicked/Dyspneic" -> com.cinnamon.app.ui.util.SoundSynthesizer.SoundType.SWOOSH
                                                            "Hysterical Crying" -> com.cinnamon.app.ui.util.SoundSynthesizer.SoundType.POP
                                                            else -> com.cinnamon.app.ui.util.SoundSynthesizer.SoundType.SUCCESS
                                                        }
                                                        com.cinnamon.app.ui.util.SoundSynthesizer.playSynthesizedSound(soundType)
                                                    }
                                                }
                                            ),
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
                            Text("Practice pacing:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val speedLevels = listOf(0.5f, 0.75f, 1.0f, 1.25f)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectableGroup(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                speedLevels.forEach { speed ->
                                    val isSelected = speechSpeed == speed
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .selectable(
                                                selected = isSelected,
                                                role = Role.RadioButton,
                                                onClick = {
                                                    speechSpeed = speed
                                                    scope.launch {
                                                        com.cinnamon.app.ui.util.SoundSynthesizer.playSynthesizedSound(com.cinnamon.app.ui.util.SoundSynthesizer.SoundType.POP)
                                                    }
                                                }
                                            ),
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
                        EmptyChatState(accentColor = accentColor, isMedical = isMedical)
                    }
                }
                items(displayMessages) { msg ->
                    ChatBubble(msg, accentColor)
                }
                if (isLoading) {
                    item {
                        TypingIndicatorBubble(accentColor = accentColor)
                    }
                }
            }
        }
    }

    // Reference scaffold — never a clinical record or automatic grading result.
    if (showSoapDialog) {
        AlertDialog(
            onDismissRequest = { showSoapDialog = false },
            title = {
                Text(
                    "Scenario note reference",
                    color = SurgicalGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "A study scaffold for the fictional scene. It is not a medical record, and it is not derived from or grading your replies.",
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
                                    "SCENE CUE: The fictional patient is worried and describes chest pressure. Keep the exchange focused on clear English practice.",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "LANGUAGE FOCUS: acknowledge the concern, signpost your next question, and choose one plain-language explanation.",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "SELF-CHECK: Read your last reply. Did it sound clear, kind, and specific? This screen leaves that judgment with you; no empathy or clinical score is calculated.",
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
                    Text("Close reference", color = MaterialTheme.colorScheme.onSecondary)
                }
            }
        )
    }
}

@Composable
fun RoleplayCuePanel(emotionalPreset: String, messageCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SurgicalGreen.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "ROLEPLAY SCENE CUES",
                style = MaterialTheme.typography.titleSmall.copy(letterSpacing = 1.sp),
                fontWeight = FontWeight.Bold,
                color = SurgicalGreen
            )
            Text(
                "Fictional prompt · not a live patient monitor or clinical assessment",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SceneCuePill("Scene: $emotionalPreset")
                SceneCuePill("Turns: $messageCount")
            }
            Text(
                "Self-check: acknowledge the concern, ask one clear follow-up, then signpost what comes next.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SceneCuePill(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = SurgicalGreen.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, SurgicalGreen.copy(alpha = 0.28f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = SurgicalGreen
        )
    }
}

@Composable
fun VoiceWaveformCanvas(accentColor: Color) {
    val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion
    val phase = if (reduceMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "wave")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        ).value
    }

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
    accentColor: Color
) {
    val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion
    val isUser = message.role == "user"
    val context = LocalContext.current
    val flashcards = remember { AppDatabase.getDatabase(context).flashcardDao() }
    val scope = rememberCoroutineScope()
    var showOptions by remember { mutableStateOf(false) }
    var translationText by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf("") }
    var cardSaved by remember { mutableStateOf(false) }

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
                enter = if (reduceMotion) EnterTransition.None else expandVertically() + fadeIn(),
                exit = if (reduceMotion) ExitTransition.None else shrinkVertically() + fadeOut()
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
                            scope.launch {
                                runCatching {
                                    flashcards.insertFlashcard(
                                        Flashcard(
                                            frontText = message.content,
                                            backText = translationText.ifBlank {
                                                "Revisit this phrase and explain it in your own words."
                                            },
                                            category = if (isUser) "Roleplay" else "Clinical roleplay"
                                        )
                                    )
                                }.onSuccess {
                                    cardSaved = true
                                    toastMessage = "Saved to your flashcards."
                                }.onFailure {
                                    toastMessage = "Couldn’t save this card. Please try again."
                                }
                            }
                        },
                        enabled = !cardSaved
                    ) {
                        Text(
                            if (cardSaved) "✓ Saved" else "⭐ Add Card",
                            fontSize = 11.sp,
                            color = SurgicalGreen,
                            fontWeight = FontWeight.Bold
                        )
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Tip", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            caughtJargons.forEach { (jargon, layman) ->
                                Text(
                                    text = "Jargon Detected: '${jargon.replaceFirstChar { it.uppercase() }}'. Prefer layman term: '$layman'.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
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
fun EmptyChatState(accentColor: Color, isMedical: Boolean) {
    val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion
    val title = if (isMedical) "FICTIONAL SCENE READY" else "NUANCE WORKSPACE READY"
    val description = if (isMedical) {
        "Start a fictional language exchange. This app offers authored practice cues, not a live monitor, clinical score, or patient outcome."
    } else {
        "Type a phrase to explore tone and wording. This practice space does not record voice analysis or assign a fluency score."
    }
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
            val scale = if (reduceMotion) {
                1f
            } else {
                val transition = rememberInfiniteTransition(label = "pulse")
                transition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                ).value
            }
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
                    contentDescription = "Practice workspace",
                    tint = accentColor,
                    modifier = Modifier
                        .size(40.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                )
            }
            Text(
                title,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                description,
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
    val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion
    val alpha = if (reduceMotion) {
        0.55f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        ).value
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Column {
            Text("Practice partner is typing...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
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
    val reduceMotion = LocalCinnamonFeedbackPreferences.current.reduceMotion
    val offset = if (reduceMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "bouncing_dot")
        transition.animateFloat(
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
        ).value
    }

    Box(
        modifier = Modifier
            .size(6.dp)
            .offset(y = offset.dp)
            .clip(CircleShape)
            .background(color)
    )
}
