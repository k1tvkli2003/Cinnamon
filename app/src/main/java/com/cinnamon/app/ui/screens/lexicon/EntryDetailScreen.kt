package com.cinnamon.app.ui.screens.lexicon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cinnamon.app.data.local.LexiconEntry
import com.cinnamon.app.ui.components.LevelBadge
import com.cinnamon.app.ui.components.SpeakButton
import com.cinnamon.app.ui.components.TagPill
import com.cinnamon.app.ui.theme.IpaStyle
import com.cinnamon.app.ui.theme.Springs
import com.cinnamon.app.viewmodel.LexiconViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    entryId: Long,
    onBack: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    viewModel: LexiconViewModel = viewModel()
) {
    val entry by viewModel.entry(entryId).collectAsState(initial = null)
    var related by remember { mutableStateOf<List<LexiconEntry>>(emptyList()) }

    LaunchedEffect(entry?.id) {
        entry?.let { related = viewModel.related(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entry?.term ?: "", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    entry?.let { e ->
                        IconButton(onClick = { viewModel.toggleBookmark(e) }) {
                            Icon(
                                imageVector = if (e.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (e.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val e = entry
        if (e == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero headword
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(initialOffsetY = { it / 5 }, animationSpec = Springs.nav()) + fadeIn(tween(280))
                ) {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            Modifier.background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f), Color.Transparent)
                                )
                            )
                        ) {
                            Column(Modifier.padding(22.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = e.term,
                                            style = MaterialTheme.typography.displaySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (e.ipa.isNotBlank()) {
                                            Text(text = e.ipa, style = IpaStyle, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                    SpeakButton(text = e.term)
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    LevelBadge(e.level)
                                    TagPill(e.topic)
                                    if (e.pos.isNotBlank()) {
                                        Text(e.pos, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (e.abbreviation.isNotBlank()) {
                                        TagPill(
                                            e.abbreviation,
                                            container = MaterialTheme.colorScheme.tertiaryContainer,
                                            content = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Definition + plain
            item {
                Section(title = "Definition") {
                    Text(e.definition, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    if (e.plain.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Row {
                            Box(
                                Modifier
                                    .width(3.dp)
                                    .height(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "In plain terms: ${e.plain}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Examples
            if (e.exampleClinical.isNotBlank() || e.exampleCasual.isNotBlank()) {
                item {
                    Section(title = "In use") {
                        if (e.exampleClinical.isNotBlank()) {
                            ExampleBubble(label = "On the ward", text = e.exampleClinical, accent = MaterialTheme.colorScheme.primary)
                        }
                        if (e.exampleCasual.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            ExampleBubble(label = "Everyday", text = e.exampleCasual, accent = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            // Collocations
            if (e.collocations.isNotEmpty()) {
                item {
                    Section(title = "Collocations — say it like a native") {
                        FlowChips(e.collocations)
                    }
                }
            }

            // Usage note
            if (e.usageNote.isNotBlank()) {
                item {
                    Section(title = "Usage note") {
                        Text(e.usageNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Word building
            if (e.morphemes.isNotEmpty() || e.etymology.isNotBlank()) {
                item {
                    Section(title = "Word building") {
                        if (e.morphemes.isNotEmpty()) {
                            FlowChips(e.morphemes)
                            Spacer(Modifier.height(10.dp))
                        }
                        if (e.etymology.isNotBlank()) {
                            Text(e.etymology, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Synonyms / antonyms
            if (e.synonyms.isNotEmpty() || e.antonyms.isNotEmpty()) {
                item {
                    Section(title = "Related words") {
                        if (e.synonyms.isNotEmpty()) {
                            Text("Synonyms", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(6.dp))
                            FlowChips(e.synonyms)
                        }
                        if (e.antonyms.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text("Antonyms", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.height(6.dp))
                            FlowChips(e.antonyms)
                        }
                    }
                }
            }

            // Add to review
            item {
                Button(
                    onClick = { viewModel.startLearning(e) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    enabled = e.dueAt == 0L,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Rounded.School, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (e.dueAt == 0L) "Add to review deck" else "Already in your deck",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Related by topic
            if (related.isNotEmpty()) {
                item {
                    Section(title = "More in ${e.topic}") {
                        related.forEach { r ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenEntry(r.id) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(r.term, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                LevelBadge(r.level)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ExampleBubble(label: String, text: String, accent: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.08f))
            .padding(14.dp)
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(items: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            TagPill(
                text = item,
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
