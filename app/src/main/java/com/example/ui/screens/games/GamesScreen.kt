package com.example.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.FeatureCard
import com.example.ui.theme.SurgicalGreen

@Composable
fun GamesScreen(
    onNavigateToVocabMatch: () -> Unit,
    onNavigateToUnscramble: () -> Unit,
    onNavigateToProgressZone: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            FeatureCard(
                title = "Vocab Match",
                subtitle = "Timed vocabulary matching challenge",
                icon = Icons.Default.Timer,
                onClick = onNavigateToVocabMatch
            )
        }
        item {
            FeatureCard(
                title = "Sentence Unscramble",
                subtitle = "Put the words in the correct order",
                icon = Icons.Default.Sort,
                onClick = onNavigateToUnscramble
            )
        }
        item {
            FeatureCard(
                title = "Clinical Progression Hub",
                subtitle = "Active training: Daily Quests, RPG Skill Tree, Guild Wars & Boss Battles",
                icon = Icons.Default.EmojiEvents,
                accentColor = SurgicalGreen,
                onClick = onNavigateToProgressZone
            )
        }
    }
}
