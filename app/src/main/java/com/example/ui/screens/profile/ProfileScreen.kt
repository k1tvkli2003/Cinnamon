package com.example.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp



import com.example.ui.theme.*
import com.example.viewmodel.UserProgressViewModel

@Composable
fun ProfileScreen(
    viewModel: UserProgressViewModel
) {
    val points by viewModel.points.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val rank by viewModel.rank.collectAsState()
    val defibrillators by viewModel.defibrillators.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val cardiologyDepth by viewModel.cardiologyDepth.collectAsState()
    val pulmonologyDepth by viewModel.pulmonologyDepth.collectAsState()
    val generalCareDepth by viewModel.generalCareDepth.collectAsState()
    val weeklyXpDistribution by viewModel.weeklyXpDistribution.collectAsState()
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Clinical Rank and Profile Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SurgicalGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🩺", style = MaterialTheme.typography.headlineMedium)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "DR. MEDICAL STUDENT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Badge(containerColor = SurgicalGreen) {
                                Text(
                                    rank,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Level ${(points / 500) + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Clinical Knowledge Depth Donut Row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, SurgicalGreen.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "CLINICAL KNOWLEDGE DEPTH",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SurgicalGreen
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ClinicalDonutChart(label = "Cardiology", progress = cardiologyDepth, color = SurgicalGreen)
                        ClinicalDonutChart(label = "Pulmonology", progress = pulmonologyDepth, color = NeonCyan)
                        ClinicalDonutChart(label = "General Care", progress = generalCareDepth, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Dynamic Theme Switcher Card (Idea 60)
        item {
            val selectedTheme by viewModel.selectedTheme.collectAsState()
            val themeOptions = listOf("Minimalist Dark", "Clinical White", "AMOLED Dark Mode")
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "PREMIUM THEME CONSOLE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Instantly alter the application color schematic to match your operational environment, with dynamic light and high-accuracy OLED modes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        themeOptions.chunked(2).forEach { rowOptions ->
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowOptions.forEach { themeName ->
                                    val isSelected = themeName == selectedTheme
                                    Button(
                                        onClick = {
                                            viewModel.updateTheme(themeName)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) NeonCyan else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            themeName,
                                            color = if (isSelected) Color.Black else Color.White,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Streak Rescue / Defibrillator Section (Idea 49)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = "Defibrillator", tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Streak Defibrillator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Badge(containerColor = NeonCyan) {
                            Text("$defibrillators Available", color = Color.Black, modifier = Modifier.padding(4.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Automatically rescues your streak if you miss a day. Prevent streak death!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                if (viewModel.rescueStreak()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                            enabled = defibrillators > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text("Use Defibrillator", color = Color.Black)
                        }
                        
                        OutlinedButton(
                            onClick = {
                                if (viewModel.buyDefibrillator()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                            border = BorderStroke(1.dp, SurgicalGreen)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Star", tint = SurgicalGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buy for 300 pts", color = SurgicalGreen)
                        }
                    }
                }
            }
        }

        // XP Weekly Distribution Analytics Graph (Idea 48)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "WEEKLY REVENUE & XP PROFILE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SurgicalGreen
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AnalyticsSparkline(
                        data = weeklyXpDistribution,
                        lineColor = SurgicalGreen
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                            Text(day, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Achievements Section (Idea 45)
        item {
            Text(
                "UNLOCKED CLINICAL MILESTONES",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(achievements) { achievement ->
            // Dynamic hologram metallic / pearlescent shift simulation (Idea 45)
            val shimmerTransition = rememberInfiniteTransition(label = "holo")
            val translationProgress by shimmerTransition.animateFloat(
                initialValue = -300f,
                targetValue = 900f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "shimmerTranslation"
            )

            val holoBrush = if (achievement.unlocked) {
                Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.04f),
                        SurgicalGreen.copy(alpha = 0.12f),
                        NeonCyan.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    start = Offset(translationProgress, 0f),
                    end = Offset(translationProgress + 180f, 320f)
                )
            } else {
                Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (achievement.unlocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (achievement.unlocked) BorderStroke(1.dp, SurgicalGreen.copy(alpha = 0.3f)) else null
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(holoBrush)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            achievement.icon,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                achievement.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (achievement.unlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                achievement.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (achievement.unlocked) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = "Earned",
                                tint = SurgicalGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Custom canvas-drawn visual graph (Idea 48 & 64)
@Composable
fun AnalyticsSparkline(data: List<Int>, lineColor: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val width = size.width
        val height = size.height
        val maxVal = data.maxOrNull() ?: 100
        val points = data.mapIndexed { index, value ->
            Offset(
                x = index * (width / (data.size - 1)),
                y = height - (value.toFloat() / maxVal * height * 0.8f) - (height * 0.1f)
            )
        }

        val path = Path()
        if (points.isNotEmpty()) {
            path.moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                // Smooth bezier curve drawing
                val prevPoint = points[i - 1]
                val currentPoint = points[i]
                path.cubicTo(
                    (prevPoint.x + currentPoint.x) / 2f, prevPoint.y,
                    (prevPoint.x + currentPoint.x) / 2f, currentPoint.y,
                    currentPoint.x, currentPoint.y
                )
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4.dp.toPx())
        )

        // Draw glowing circular joint points
        points.forEach { pt ->
            drawCircle(
                color = lineColor,
                radius = 6.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = Color.Black,
                radius = 3.dp.toPx(),
                center = pt
            )
        }
    }
}

@Composable
fun ClinicalDonutChart(label: String, progress: Float, color: Color, modifier: Modifier = Modifier) {
    var triggerAnim by remember { mutableStateOf(false) }
    LaunchedEffect(progress) {
        triggerAnim = true
    }
    val sweepAngle by animateFloatAsState(
        targetValue = if (triggerAnim) progress * 360f else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "sweepAngle"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 6.dp.toPx()
                // Background Track
                drawArc(
                    color = color.copy(alpha = 0.15f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
                // Active Arc
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Text(
                "${(progress * 100).toInt()}%",
                style = VitalsNumericStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

