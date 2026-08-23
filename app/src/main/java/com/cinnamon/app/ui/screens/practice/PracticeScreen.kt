package com.cinnamon.app.ui.screens.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.SpeakerNotes
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinnamon.app.ui.components.SectionHeader
import com.cinnamon.app.ui.theme.pressScale
import com.cinnamon.app.viewmodel.UserProgressViewModel

@Composable
fun PracticeScreen(
    progressViewModel: UserProgressViewModel,
    onStartReview: () -> Unit,
    onStartCloze: () -> Unit,
    onStartVocabMatch: () -> Unit,
    onStartUnscramble: () -> Unit,
    onOpenProgressHub: () -> Unit,
    onOpenPatientSim: () -> Unit,
    onOpenSimLabs: () -> Unit,
    onOpenMakeItNative: () -> Unit,
    onOpenShadowing: () -> Unit
) {
    val dueNow by progressViewModel.dueNow.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Practice",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Hero: spaced repetition review
        item {
            ReviewHero(dueNow = dueNow, onClick = onStartReview)
        }

        item { SectionHeader(title = "Word games") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GameTile("Cloze Clinic", "Fill the blank in real sentences", Icons.AutoMirrored.Rounded.SpeakerNotes, MaterialTheme.colorScheme.primary, Modifier.weight(1f), onStartCloze)
                GameTile("Word Match", "Pair terms with meanings", Icons.Rounded.Extension, MaterialTheme.colorScheme.secondary, Modifier.weight(1f), onStartVocabMatch)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GameTile("Unscramble", "Rebuild advanced sentences", Icons.Rounded.ViewAgenda, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f), onStartUnscramble)
                GameTile("Learning Questboard", "Milestones, focus & rewards", Icons.Rounded.EmojiEvents, MaterialTheme.colorScheme.secondary, Modifier.weight(1f), onOpenProgressHub)
            }
        }

        item { SectionHeader(title = "Guided language practice") }
        item {
            PracticeRow("AI Patient", "Take a full history in English", Icons.Rounded.Healing, MaterialTheme.colorScheme.secondary, onOpenPatientSim)
        }
        item {
            PracticeRow("Clinical Sim Labs", "SBAR, SPIKES, morning report & ECG talk", Icons.Rounded.Science, MaterialTheme.colorScheme.primary, onOpenSimLabs)
        }
        item {
            PracticeRow("Make It Native", "Rewrite anything formal, casual or slang", Icons.Rounded.AutoAwesome, MaterialTheme.colorScheme.tertiary, onOpenMakeItNative)
        }
        item {
            PracticeRow("Shadowing Coach", "Rhythm, prosody & accent practice", Icons.Rounded.RecordVoiceOver, MaterialTheme.colorScheme.secondary, onOpenShadowing)
        }
    }
}

@Composable
private fun ReviewHero(dueNow: Int, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Style, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Spaced repetition", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    if (dueNow > 0) "$dueNow words ready to review" else "Learn new words from your deck",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun GameTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .height(150.dp)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PracticeRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
