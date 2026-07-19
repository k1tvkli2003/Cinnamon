package com.cinnamon.app.ui.screens.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinnamon.app.ui.components.SectionHeader
import com.cinnamon.app.ui.feedback.LocalCinnamonFeedbackPreferences
import com.cinnamon.app.ui.theme.CinnamonThemes
import com.cinnamon.app.ui.theme.VitalsNumericStyle
import com.cinnamon.app.viewmodel.UserProgressViewModel

@Composable
fun ProfileScreen(viewModel: UserProgressViewModel) {
    val points by viewModel.points.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val rank by viewModel.rank.collectAsState()
    val progressionLevel by viewModel.progressionLevel.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val weeklyXp by viewModel.weeklyXpDistribution.collectAsState()
    val totalWords by viewModel.totalWords.collectAsState()
    val masteredWords by viewModel.masteredWords.collectAsState()
    val dailyGoal by viewModel.dailyGoalXp.collectAsState()
    val reviewedToday by viewModel.reviewedToday.collectAsState()
    val xpToday by viewModel.xpToday.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val soundEffectsEnabled by viewModel.soundEffectsEnabled.collectAsState()
    val hapticFeedbackEnabled by viewModel.hapticFeedbackEnabled.collectAsState()
    val reduceMotion by viewModel.reduceMotion.collectAsState()
    val haptic = LocalHapticFeedback.current
    val feedbackPreferences = LocalCinnamonFeedbackPreferences.current

    val scheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Identity header
        item {
            Surface(shape = RoundedCornerShape(26.dp), color = scheme.surface, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(scheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) { Text("🤎", style = MaterialTheme.typography.headlineMedium) }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Your study profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.clip(CircleShape).background(scheme.primary).padding(horizontal = 10.dp, vertical = 3.dp)
                            ) { Text(rank, color = scheme.onPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.width(8.dp))
                            Text("Level $progressionLevel", style = MaterialTheme.typography.bodySmall, color = scheme.secondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Headline stats
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("$points", "total XP", Icons.Rounded.Verified, scheme.primary, Modifier.weight(1f))
                MetricCard("$streak", "day streak", Icons.Rounded.LocalFireDepartment, scheme.tertiary, Modifier.weight(1f))
                MetricCard("$masteredWords", "mastered", Icons.AutoMirrored.Rounded.MenuBook, scheme.secondary, Modifier.weight(1f))
            }
        }

        // Vocabulary mastery progress
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = scheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("VOCABULARY MASTERY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = scheme.primary)
                    Spacer(Modifier.height(12.dp))
                    val pct = if (totalWords > 0) masteredWords.toFloat() / totalWords else 0f
                    val animated by animateFloatAsState(
                        targetValue = pct,
                        animationSpec = if (feedbackPreferences.reduceMotion) snap() else tween(900),
                        label = "mastery"
                    )
                    LinearProgressIndicator(
                        progress = { animated },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        color = scheme.primary,
                        trackColor = scheme.surfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$masteredWords of $totalWords words at long-term retention",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }

        // Only signals that the product actually records are shown here.
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = scheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("TODAY’S VERIFIED SIGNALS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = scheme.secondary)
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        EvidenceStat("$reviewedToday", "reviews committed", scheme.primary)
                        EvidenceStat("$xpToday", "XP settled", scheme.secondary)
                        EvidenceStat("$dailyGoal", "today’s target", scheme.tertiary)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "These numbers come from completed Room ledger events, not estimated clinical confidence.",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }

        // Weekly XP sparkline
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = scheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("THIS WEEK", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = scheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Sparkline(data = weeklyXp, lineColor = scheme.primary, dotInner = scheme.surface)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { d ->
                            Text(d, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Daily goal selector
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = scheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("DAILY GOAL", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = scheme.secondary)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(40 to "Casual", 80 to "Regular", 120 to "Focused", 160 to "Full").forEach { (goal, label) ->
                            val selected = dailyGoal == goal
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(13.dp))
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = {
                                            viewModel.setDailyGoal(goal)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    ),
                                shape = RoundedCornerShape(13.dp),
                                color = if (selected) scheme.primary else scheme.surfaceVariant
                            ) {
                                Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$goal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (selected) scheme.onPrimary else scheme.onSurface)
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) scheme.onPrimary.copy(alpha = 0.85f) else scheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Theme switcher
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = scheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("APPEARANCE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = scheme.tertiary)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CinnamonThemes.all.forEach { themeName ->
                            val selected = themeName == selectedTheme
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(13.dp))
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = {
                                            viewModel.updateTheme(themeName)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    ),
                                shape = RoundedCornerShape(13.dp),
                                color = if (selected) scheme.primary else scheme.surfaceVariant,
                                border = if (selected) null else BorderStroke(1.dp, scheme.outline)
                            ) {
                                Text(
                                    themeName,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp).fillMaxWidth(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = if (selected) scheme.onPrimary else scheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(20.dp), color = scheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 10.dp)) {
                    Text(
                        "ACCESSIBILITY & FEEDBACK",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.secondary
                    )
                    PreferenceToggleRow(
                        label = "Sound effects",
                        checked = soundEffectsEnabled,
                        onCheckedChange = viewModel::setSoundEffectsEnabled
                    )
                    PreferenceToggleRow(
                        label = "Haptic feedback",
                        checked = hapticFeedbackEnabled,
                        onCheckedChange = viewModel::setHapticFeedbackEnabled
                    )
                    PreferenceToggleRow(
                        label = "Reduce motion",
                        checked = reduceMotion,
                        onCheckedChange = viewModel::setReduceMotion
                    )
                    Text(
                        "Adjusting these preferences will not affect your learning progress or rewards.",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }

        item { SectionHeader(title = "Catalog milestones") }

        items(achievements) { achievement ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (achievement.reached) scheme.surface else scheme.surface.copy(alpha = 0.55f),
                border = if (achievement.reached) BorderStroke(1.dp, scheme.primary.copy(alpha = 0.3f)) else null
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(achievement.icon, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(end = 12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            achievement.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (achievement.reached) scheme.onSurface else scheme.onSurfaceVariant
                        )
                        Text(achievement.description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        Text(
                            "Evidence: ${achievement.progress}/${achievement.target}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (achievement.reached) {
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = "Evidence threshold reached", tint = scheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun MetricCard(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp)) }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun EvidenceStat(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.14f))
                .border(BorderStroke(1.dp, color.copy(alpha = 0.32f)), CircleShape)
        ) {
            Text(value, style = VitalsNumericStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = color)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Sparkline(data: List<Int>, lineColor: Color, dotInner: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
        if (data.size < 2) return@Canvas
        val width = size.width
        val height = size.height
        val maxVal = (data.maxOrNull() ?: 1).coerceAtLeast(1)
        val pts = data.mapIndexed { i, v ->
            Offset(
                x = i * (width / (data.size - 1)),
                y = height - (v.toFloat() / maxVal * height * 0.8f) - (height * 0.1f)
            )
        }
        val path = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 1 until pts.size) {
                val prev = pts[i - 1]; val cur = pts[i]
                cubicTo((prev.x + cur.x) / 2f, prev.y, (prev.x + cur.x) / 2f, cur.y, cur.x, cur.y)
            }
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = 4.dp.toPx()))
        pts.forEach { pt ->
            drawCircle(color = lineColor, radius = 6.dp.toPx(), center = pt)
            drawCircle(color = dotInner, radius = 3.dp.toPx(), center = pt)
        }
    }
}
