package com.cinnamon.app.ui.screens.games

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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

import com.cinnamon.app.ui.theme.*
import com.cinnamon.app.viewmodel.UserProgressViewModel
import com.cinnamon.app.viewmodel.Quest
import com.cinnamon.app.viewmodel.Replay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamificationHubScreen(
    onBack: () -> Unit,
    progressViewModel: UserProgressViewModel
) {
    var selectedTab by remember { mutableStateOf("Quests & Guilds") }
    val haptic = LocalHapticFeedback.current

    val tabs = listOf(
        "Quests & Guilds",
        "RPG Skill Tree",
        "Weekly Bosses",
        "Match-3 Vocab",
        "Audio Escape",
        "Case Replays"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinical Progression Zone", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SurgicalGreen)
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
            // RPG Navigation Sidebar
            Column(
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
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
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = tab,
                            color = if (isActive) SurgicalGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Gamified Action Area
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
                    }, label = "gamificationHubTransition"
                ) { targetTab ->
                    when (targetTab) {
                        "Quests & Guilds" -> QuestsAndGuildsComponent(progressViewModel)
                        "RPG Skill Tree" -> RpgSkillTreeComponent(progressViewModel)
                        "Weekly Bosses" -> WeeklyBossBattlesComponent(progressViewModel)
                        "Match-3 Vocab" -> Match3VocabComponent(progressViewModel)
                        "Audio Escape" -> AudioEscapeRoomComponent(progressViewModel)
                        "Case Replays" -> CaseReplaysComponent(progressViewModel)
                        else -> Text("Section Coming Soon", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// TAB 1: DAILY QUESTS, FACTIONS/GUILDS, ANONYMOUS LEADERBOARD, MMR ADAPTIVE DIFFICULTIES
// --------------------------------------------------------------------------------------
@Composable
fun QuestsAndGuildsComponent(viewModel: UserProgressViewModel) {
    val points by viewModel.points.collectAsState()
    val dailyQuests by viewModel.dailyQuests.collectAsState()
    val selectedFaction by viewModel.selectedFaction.collectAsState()
    val factionProgress by viewModel.factionProgress.collectAsState()
    val eloRating by viewModel.eloRating.collectAsState()
    val streak by viewModel.streak.collectAsState()

    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Daily Clinical Quests (Idea 47)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, SurgicalGreen.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("DAILY CLINICAL QUESTS", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Badge(containerColor = SurgicalGreen.copy(alpha = 0.2f)) {
                            Text("+50 XP Each", color = SurgicalGreen, modifier = Modifier.padding(4.dp), fontSize = 10.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    dailyQuests.forEachIndexed { index, quest ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = quest.completed,
                                onCheckedChange = {
                                    if (!quest.completed) {
                                        viewModel.completeQuest(index)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = SurgicalGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quest.title,
                                    color = if (quest.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text("Progress: ${quest.progress}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }
                        }
                        if (index < dailyQuests.size - 1) {
                            HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // Factions & Guilds (Idea 55)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CLINICAL SPECIALTY GUILDS", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("All guild contributors pool weekly XP to unlock clinical item packages for all members.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

                    if (selectedFaction == "None") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.joinFaction("Team Cardiology")
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🫀", fontSize = 20.sp)
                                    Text("Team Cardiology", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.joinFaction("Team Neurology")
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🧠", fontSize = 20.sp)
                                    Text("Team Neurology", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Badge(containerColor = if (selectedFaction == "Team Cardiology") AlertRed else NeonCyan) {
                                Text("COMBAT INTEGRATION: $selectedFaction", color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Your Guild Weekly Goal Progress: $factionProgress / 10,000 pts", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                            LinearProgressIndicator(
                                progress = { factionProgress.toFloat() / 10000f },
                                color = if (selectedFaction == "Team Cardiology") AlertRed else NeonCyan,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                            )
                            Button(
                                onClick = {
                                    viewModel.addPoints(50)
                                    viewModel.completeQuest(0) // increment activity contribution
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background),
                                border = BorderStroke(1.dp, SurgicalGreen)
                            ) {
                                Text("Donate 50 XP to Guild Vault", color = SurgicalGreen)
                            }
                        }
                    }
                }
            }
        }

        // Anonymous Global Leaderboard (Idea 43)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GLOBAL CLINICAL LEADERBOARD (ANONYMOUS)", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    val leaderboardMembers = listOf(
                        Triple("1. MedGenius_TX", "72 Days", "4,200 XP"),
                        Triple("2. RetroSuture", "48 Days", "3,850 XP"),
                        Triple("3. Dr_Stethoscope", "$streak Days", "$points XP (YOU)"),
                        Triple("4. ECG_Wizard", "19 Days", "1,220 XP"),
                        Triple("5. ThoracicGuru", "14 Days", "980 XP")
                    )

                    leaderboardMembers.forEach { (user, streakInfo, xp) ->
                        val isSelf = user.contains("(YOU)")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelf) MaterialTheme.colorScheme.background else Color.Transparent)
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(user, color = if (isSelf) SurgicalGreen else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = if (isSelf) FontWeight.Bold else FontWeight.Normal)
                            Row {
                                Text(streakInfo, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(xp, color = if (isSelf) SurgicalGreen else NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // MMR/Elo Adaptive Difficulty Gauge (Idea 51)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("MMR / CLINICAL DIFFICULTY ELIGIBILITY", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your current dynamic rating is $eloRating. At higher ELO ratings, the standardized patients speak faster, use complex layout structures, and demand perfect clinical politeness.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Adaptive Difficulty Dynamic Level:", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        Badge(containerColor = MaterialTheme.colorScheme.background, contentColor = SurgicalGreen) {
                            Text(if (eloRating >= 1400) "CRITICAL MASTER (C2)" else "INTERMEDIATE CLINICIAN", modifier = Modifier.padding(4.dp), fontWeight = FontWeight.Bold)
                        }
                    }

                    Slider(
                        value = eloRating.toFloat(),
                        onValueChange = {
                            viewModel.increaseElo((it - eloRating).toInt())
                        },
                        valueRange = 800f..2000f,
                        colors = SliderDefaults.colors(
                            thumbColor = SurgicalGreen,
                            activeTrackColor = SurgicalGreen
                        )
                    )
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// TAB 2: RPG-STYLE SKILL TREE (Idea 42)
// --------------------------------------------------------------------------------------
@Composable
fun RpgSkillTreeComponent(viewModel: UserProgressViewModel) {
    val skillPoints by viewModel.skillPoints.collectAsState()
    val unlockedSkills by viewModel.unlockedSkills.collectAsState()
    val haptic = LocalHapticFeedback.current

    val skills = listOf(
        Triple("Basic Latin Roots", "Fundamental terminology stems.", "Anatomy Matcher"),
        Triple("Bedside Intonation", "Soft vocal modifiers.", "Basic Latin Roots"),
        Triple("Complex Prepositions", "Required for Cardiology consult.", "Bedside Intonation"),
        Triple("Passive Assertiveness", "Expressing emergency urgency.", "Complex Prepositions"),
        Triple("SPIKES Cushioning", "Oncology bad news protocol.", "Passive Assertiveness")
    )

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
            Column {
                Text("RPG CLINICAL SKILL TREE", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Unlock high-tier skills to access expert roleplay zones.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            Badge(containerColor = NeonCyan, contentColor = Color.Black) {
                Text("$skillPoints SP", fontWeight = FontWeight.Black, modifier = Modifier.padding(6.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val dashOffset = infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 50f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "dashOffset"
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                skills.forEachIndexed { index, (skillName, desc, prereq) ->
                    val isUnlocked = unlockedSkills.contains(skillName)
                    val canUnlock = unlockedSkills.contains(prereq) || prereq == "Anatomy Matcher"

                    // Animate node glow
                    val glowAlpha = if (isUnlocked) {
                        infiniteTransition.animateFloat(
                            initialValue = 0.5f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "glow"
                        ).value
                    } else 0f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(if (isUnlocked) 10.dp else 0.dp, RoundedCornerShape(12.dp), spotColor = NeonCyan.copy(alpha = glowAlpha))
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUnlocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background
                            ),
                            border = if (isUnlocked) BorderStroke(1.5.dp, NeonCyan.copy(alpha = glowAlpha)) 
                                    else if (canUnlock) BorderStroke(1.dp, SurgicalGreen.copy(alpha = 0.6f)) 
                                    else BorderStroke(1.dp, Color.DarkGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(if (isUnlocked) NeonCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.background)
                                        .border(2.dp, if (isUnlocked) NeonCyan else Color.DarkGray, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isUnlocked) "⚡" else "🔒",
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(skillName, color = if (isUnlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 14.sp)
                                    if (prereq != "Anatomy Matcher" && !isUnlocked) {
                                        Text("Requires: $prereq", color = AlertRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (!isUnlocked) {
                                    Button(
                                        onClick = {
                                            if (viewModel.investSkillPoint(skillName)) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        },
                                        enabled = canUnlock && skillPoints > 0,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SurgicalGreen,
                                            disabledContainerColor = Color.DarkGray
                                        )
                                    ) {
                                        Text("Unlock", color = if (canUnlock) Color.Black else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Badge(containerColor = NeonCyan.copy(alpha = 0.2f)) {
                                        Text("ACTIVE", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Visual Connective Arrow / Pipe
                    if (index < skills.size - 1) {
                        val nextSkillUnlocked = unlockedSkills.contains(skills[index + 1].first)
                        val pipeColor = if (nextSkillUnlocked) NeonCyan else Color.DarkGray
                        
                        Canvas(modifier = Modifier.width(4.dp).height(24.dp)) {
                            drawLine(
                                color = pipeColor,
                                start = Offset(size.width / 2, 0f),
                                end = Offset(size.width / 2, size.height),
                                strokeWidth = 4.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(10f, 10f),
                                    phase = if (nextSkillUnlocked) -dashOffset.value else 0f
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// TAB 3: WEEKLY BOSS BATTLES (Idea 44) & SECRET MODIFIERS (Idea 53)
// --------------------------------------------------------------------------------------
@Composable
fun WeeklyBossBattlesComponent(viewModel: UserProgressViewModel) {
    var timerSeconds by remember { mutableIntStateOf(180) } // 3 minutes strictly!
    var battleActive by remember { mutableStateOf(false) }
    var inputDiagnosis by remember { mutableStateOf("") }
    var isModifierAnxious by remember { mutableStateOf(false) }
    var isModifierIntoxicated by remember { mutableStateOf(false) }
    var battleStatus by remember { mutableStateOf("") }

    val haptic = LocalHapticFeedback.current
    val isWeekend = true // Weekend XP Multipler mock checker (Idea 52)
    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(battleStatus) {
        if (battleStatus.contains("VICTORY")) {
            showConfetti = true
            delay(3000)
            showConfetti = false
        }
    }

    if (showConfetti) {
        // Simple confetti simulator
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan, Color.Magenta)
            val random = java.util.Random()
            for (i in 0..100) {
                val randX = random.nextFloat() * size.width
                val randY = random.nextFloat() * size.height
                val color = colors.random()
                drawCircle(color = color, radius = (5..15).random().toFloat(), center = Offset(randX, randY))
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bossPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LaunchedEffect(battleActive) {
        if (battleActive) {
            while (timerSeconds > 0 && battleActive) {
                delay(1000)
                timerSeconds--
            }
            if (timerSeconds == 0) {
                battleActive = false
                battleStatus = "TIME IS UP! The patient decompensated before you could identify the hidden diagnosis."
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
            Text("WEEKLY EPIC PATIENT BOSS", color = AlertRed, fontWeight = FontWeight.Black, fontSize = 16.sp)
            if (isWeekend) {
                Badge(containerColor = Color(0xFFFFD700), contentColor = Color.Black) {
                    Text("2X XP Weekend Active", fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (battleActive && timerSeconds < 60) 20.dp else 0.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = AlertRed.copy(alpha = if (battleActive) pulseAlpha else 0f)
                )
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (battleActive && timerSeconds < 30) AlertRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(if (battleActive) 2.dp else 1.dp, if (battleActive) AlertRed.copy(alpha = pulseAlpha) else Color.DarkGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("BOSS: Friday Patient #821", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        val minute = timerSeconds / 60
                        val second = timerSeconds % 60
                        val timeString = String.format("%02d:%02d", minute, second)
                        Text(
                            "⏰ $timeString", 
                            color = if (timerSeconds < 30) AlertRed else NeonCyan, 
                            fontWeight = FontWeight.Black, 
                            fontSize = if (timerSeconds < 30) 18.sp else 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "CASE LOG: A 62-year-old male arrives clutching his chest, projecting pain radiating to his left shoulder. He feels extreme epigastric nausea. He has a historic record of hypertension but claims this pain feels entirely like a 'ripping' sensation down his back rather than simple indigestion.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Secret Unlockable Patient Modifiers (Idea 53)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("SECRET UNLOCKABLE MODIFIERS", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Inject extreme behavioral modifiers to boost victory XP by 1.5x!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if(isModifierAnxious) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isModifierAnxious,
                            enabled = !battleActive,
                            onCheckedChange = {
                                isModifierAnxious = it
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                        )
                        Text("Anxious", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                    }

                    Row(
                        modifier = Modifier
                            .weight(1.5f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if(isModifierIntoxicated) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isModifierIntoxicated,
                            enabled = !battleActive,
                            onCheckedChange = {
                                isModifierIntoxicated = it
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                        )
                        Text("Intoxicated Status", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                    }
                }
            }
        }

        if (battleActive) {
            OutlinedTextField(
                value = inputDiagnosis,
                onValueChange = { inputDiagnosis = it },
                label = { Text("What is the Hidden Emergency Diagnosis?") },
                placeholder = { Text("e.g. Aortic Dissection", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AlertRed, unfocusedBorderColor = Color.Gray),
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp)
            )

            Button(
                onClick = {
                    battleActive = false
                    val cleanDiag = inputDiagnosis.lowercase().trim()
                    if (cleanDiag.contains("aortic dissection") || cleanDiag.contains("dissection")) {
                        var rewarded = if (isWeekend) 300 else 150
                        if (isModifierAnxious) rewarded = (rewarded * 1.5).toInt()
                        if (isModifierIntoxicated) rewarded = (rewarded * 1.5).toInt()
                        viewModel.addPoints(rewarded)
                        battleStatus = "VICTORY! True Diagnosis Verified: AORTIC DISSECTION (Ripping pain down back is the tell-tale clue!). You saved the patient under time limits. XP gained: $rewarded!"
                    } else {
                        battleStatus = "DEFEAT! Incorrect diagnosis. The patient suffered a lethal ruptured Aorta. Re-evaluate clinical markers!"
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("SUBMIT CLINICAL DIAGNOSIS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        } else {
            Button(
                onClick = {
                    battleActive = true
                    timerSeconds = 180
                    battleStatus = ""
                    inputDiagnosis = ""
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed.copy(alpha = 0.8f)),
                border = BorderStroke(2.dp, AlertRed),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("ENGAGE BOSS BATTLE NOW", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }

        if (battleStatus.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (battleStatus.contains("VICTORY")) SurgicalGreen.copy(alpha=0.1f) else AlertRed.copy(alpha=0.1f)),
                border = BorderStroke(2.dp, if (battleStatus.contains("VICTORY")) SurgicalGreen else AlertRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = battleStatus,
                    color = if (battleStatus.contains("VICTORY")) SurgicalGreen else AlertRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// TAB 4: FLASHCARD MATCH-3 GAMEPLAY (Idea 46)
// --------------------------------------------------------------------------------------
@Composable
fun Match3VocabComponent(viewModel: UserProgressViewModel) {
    var matchScore by remember { mutableIntStateOf(0) }
    var selectedId1 by remember { mutableStateOf<Int?>(null) }
    var selectedId2 by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    // Flashcard components paired!
    val flashcardsMaster = remember {
        mutableStateListOf(
            MatchTile(1, "Dyspnea", "Shortness of Breath", false),
            MatchTile(2, "Cephalgia", "Severe Headache", false),
            MatchTile(3, "Syncope", "Fainting Episode", false),
            MatchTile(4, "Myalgia", "Muscle Pains", false),
            MatchTile(5, "Shortness of Breath", "Dyspnea", false),
            MatchTile(6, "Severe Headache", "Cephalgia", false),
            MatchTile(7, "Fainting Episode", "Syncope", false),
            MatchTile(8, "Muscle Pains", "Myalgia", false)
        )
    }

    LaunchedEffect(selectedId1, selectedId2) {
        if (selectedId1 != null && selectedId2 != null) {
            val tile1 = flashcardsMaster.find { it.id == selectedId1 }
            val tile2 = flashcardsMaster.find { it.id == selectedId2 }

            if (tile1 != null && tile2 != null) {
                // If they align/match name to matchName
                if (tile1.matchedTerm == tile2.term) {
                    delay(400)
                    flashcardsMaster.remove(tile1)
                    flashcardsMaster.remove(tile2)
                    matchScore += 50
                    viewModel.addPoints(25)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } else {
                    delay(500)
                }
            }
            selectedId1 = null
            selectedId2 = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("FLASHCARD MATCH-3 PUZZLE", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Align clinical Latin medical terms with laying descriptions to match & clear!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            Badge(containerColor = SurgicalGreen, contentColor = Color.Black) {
                Text("$matchScore Pts", fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
            }
        }

        if (flashcardsMaster.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BOARD CLEARED! Perfect Vocab Alignment.", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        matchScore = 0
                        flashcardsMaster.addAll(
                            listOf(
                                MatchTile(1, "Dyspnea", "Shortness of Breath", false),
                                MatchTile(2, "Cephalgia", "Severe Headache", false),
                                MatchTile(3, "Syncope", "Fainting Episode", false),
                                MatchTile(4, "Myalgia", "Muscle Pains", false),
                                MatchTile(5, "Shortness of Breath", "Dyspnea", false),
                                MatchTile(6, "Severe Headache", "Cephalgia", false),
                                MatchTile(7, "Fainting Episode", "Syncope", false),
                                MatchTile(8, "Muscle Pains", "Myalgia", false)
                            ).shuffled()
                        )
                    }) {
                        Text("Reset Vocab Grid")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().height(260.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(flashcardsMaster.size) { index ->
                    val item = flashcardsMaster[index]
                    val isSelected = selectedId1 == item.id || selectedId2 == item.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                            .clickable {
                                if (selectedId1 == null) {
                                    selectedId1 = item.id
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else if (selectedId2 == null && selectedId1 != item.id) {
                                    selectedId2 = item.id
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) NeonCyan else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(1.dp, if (isSelected) Color.White else Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.term,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class MatchTile(val id: Int, val term: String, val matchedTerm: String, val matched: Boolean)

// --------------------------------------------------------------------------------------
// TAB 5: AUDIO ESCAPE ROOM (Idea 50)
// --------------------------------------------------------------------------------------
@Composable
fun AudioEscapeRoomComponent(viewModel: UserProgressViewModel) {
    var step by remember { mutableIntStateOf(1) }
    var soundPlaying by remember { mutableStateOf(false) }
    var rawEstimate by remember { mutableStateOf("") }
    var verdictFeedback by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(soundPlaying) {
        if (soundPlaying) {
            delay(3000)
            soundPlaying = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("AUDIO ESCAPE CLINICAL ROOM", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("Analyze the room's auditory clues (ambient telemetry beat frequency & siren records) and dictate your solutions to escape secure ward doors.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            border = BorderStroke(1.dp, SurgicalGreen.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "AUDIO MODULE PLAYING: Vital Monitors & Siren",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Beautiful custom pulse waveform animation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val scale1 = rememberInfiniteTransition().animateFloat(
                        initialValue = 10f, targetValue = 50f,
                        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "osc1"
                    )
                    val scale2 = rememberInfiniteTransition().animateFloat(
                        initialValue = 40f, targetValue = 10f,
                        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse), label = "osc2"
                    )
                    val scale3 = rememberInfiniteTransition().animateFloat(
                        initialValue = 15f, targetValue = 60f,
                        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse), label = "osc3"
                    )

                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.width(6.dp).height(if (soundPlaying) scale1.value.dp else 12.dp).background(NeonCyan))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.width(6.dp).height(if (soundPlaying) scale2.value.dp else 22.dp).background(SurgicalGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.width(6.dp).height(if (soundPlaying) scale3.value.dp else 8.dp).background(Color.Yellow))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.width(6.dp).height(if (soundPlaying) scale1.value.dp else 16.dp).background(AlertRed))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        soundPlaying = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (soundPlaying) AlertRed else NeonCyan)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (soundPlaying) Icons.Default.VolumeUp else Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (soundPlaying) "LISTENING CLUES..." else "PLAY AUDIO DISPATCH LOG", color = Color.Black)
                    }
                }
            }
        }

        Text("AMBULANCE DISPATCH DIALOGUE INTERCEPT:", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(
            "\"We hold a 35-year-old female presenting severe respiratory distress. Airway is patent but she sounds... *loud whistling tone on auscultation* ...breathing is extremely fast.\"",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            style = TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        )

        OutlinedTextField(
            value = rawEstimate,
            onValueChange = { rawEstimate = it },
            label = { Text("What term describes high-pitched whistling during expiration?") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SurgicalGreen),
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp)
        )

        Button(
            onClick = {
                val clean = rawEstimate.lowercase().trim()
                if (clean.contains("wheeze") || clean.contains("wheezing") || clean.contains("stridor")) {
                    verdictFeedback = "DOOR UNLOCKED! Proper term identified (Wheezing / Stridor). You gained emergency clinic clearance. +50 XP!"
                    viewModel.addPoints(50)
                } else {
                    verdictFeedback = "INCORRECT AUDIO DECODING. Listen closely! The high-pitched whistling during asthma/COPD on auscultation relates directly to Wheezing."
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen),
            enabled = rawEstimate.isNotBlank()
        ) {
            Text("DICTATE CLINICAL DIAGNOSIS", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        if (verdictFeedback.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, if (verdictFeedback.contains("UNLOCKED")) SurgicalGreen else AlertRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = verdictFeedback,
                    modifier = Modifier.padding(14.dp),
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// TAB 6: ROLEPLAY REPLAYS WITH FLOATING AI SUMMARY (Idea 54)
// --------------------------------------------------------------------------------------
@Composable
fun CaseReplaysComponent(viewModel: UserProgressViewModel) {
    val replays by viewModel.replays.collectAsState()
    var selectedReplay by remember { mutableStateOf<Replay?>(null) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("SAVED CLINICAL REPLAYS", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("Replay past verbal audits & AI roleplays with floating evaluative commentaries.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

        replays.forEach { replay ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedReplay = replay
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                border = if (selectedReplay?.title == replay.title) BorderStroke(1.5.dp, NeonCyan) else null
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(replay.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(replay.date, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Interactive Audio/SBAR records logged.", color = NeonCyan, fontSize = 11.sp)
                }
            }
        }

        if (selectedReplay != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                border = BorderStroke(1.dp, SurgicalGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AI CRITIQUE AUDIT OVERLAY", color = SurgicalGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        IconButton(onClick = { selectedReplay = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = AlertRed)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedReplay!!.commentary,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
