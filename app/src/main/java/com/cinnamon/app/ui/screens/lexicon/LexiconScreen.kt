package com.cinnamon.app.ui.screens.lexicon

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cinnamon.app.data.local.Abbreviation
import com.cinnamon.app.data.local.Confusable
import com.cinnamon.app.data.local.LexiconEntry
import com.cinnamon.app.data.local.Morpheme
import com.cinnamon.app.data.local.PhraseEntry
import com.cinnamon.app.ui.components.EmptyState
import com.cinnamon.app.ui.components.FilterPill
import com.cinnamon.app.ui.components.LevelBadge
import com.cinnamon.app.ui.components.TagPill
import com.cinnamon.app.ui.theme.HeadwordListStyle
import com.cinnamon.app.ui.theme.IpaStyle
import com.cinnamon.app.viewmodel.LexiconViewModel

private val lexiconTabs = listOf("Words", "Roots", "Abbrev.", "Phrases", "Mix-ups")

@Composable
fun LexiconScreen(
    onOpenEntry: (Long) -> Unit,
    viewModel: LexiconViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val query by viewModel.query.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Lexicon",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (totalCount > 0) {
                TagPill(
                    text = "$totalCount words",
                    container = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Search
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.query.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            placeholder = { Text("Search the whole reference…") },
            singleLine = true,
            shape = RoundedCornerShape(13.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.query.value = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
            )
        )

        // Content tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 20.dp,
            divider = {}
        ) {
            lexiconTabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (selectedTab == index) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn(tween(220)).togetherWith(fadeOut(tween(150))) },
            label = "lexiconTab"
        ) { tab ->
            when (tab) {
                0 -> WordsTab(viewModel, totalCount, onOpenEntry)
                1 -> RootsTab(viewModel)
                2 -> AbbreviationsTab(viewModel)
                3 -> PhrasesTab(viewModel)
                else -> MixupsTab(viewModel)
            }
        }
    }
}

// ─── Words ───────────────────────────────────────────────────────────────────

@Composable
private fun WordsTab(
    viewModel: LexiconViewModel,
    totalCount: Int,
    onOpenEntry: (Long) -> Unit
) {
    val entries by viewModel.entries.collectAsState()
    val domain by viewModel.domainFilter.collectAsState()
    val level by viewModel.levelFilter.collectAsState()

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { FilterPill("All", domain == "all") { viewModel.domainFilter.value = "all" } }
            item { FilterPill("Clinical", domain == "clinical") { viewModel.domainFilter.value = "clinical" } }
            item { FilterPill("General", domain == "general") { viewModel.domainFilter.value = "general" } }
            item {
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = 1.dp, height = 28.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
            item { FilterPill("Any level", level == "all") { viewModel.levelFilter.value = "all" } }
            item { FilterPill("B2+", level == "B2+") { viewModel.levelFilter.value = "B2+" } }
            item { FilterPill("C1", level == "C1") { viewModel.levelFilter.value = "C1" } }
            item { FilterPill("C2", level == "C2") { viewModel.levelFilter.value = "C2" } }
        }

        if (entries.isEmpty()) {
            if (totalCount == 0) {
                EmptyState(
                    icon = Icons.Rounded.MenuBook,
                    title = "Brewing the lexicon…",
                    message = "First launch only — the full dictionary is being poured into the app."
                )
            } else {
                EmptyState(
                    icon = Icons.Rounded.SearchOff,
                    title = "Nothing matches",
                    message = "Try a shorter fragment, or clear a filter."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    WordRow(entry = entry, onClick = { onOpenEntry(entry.id) })
                }
            }
        }
    }
}

@Composable
private fun WordRow(entry: LexiconEntry, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (entry.domain == "clinical") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    )
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.term,
                    style = HeadwordListStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = listOf(entry.ipa, entry.pos).filter { it.isNotBlank() }.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            LevelBadge(entry.level)
        }
    }
}

// ─── Roots ───────────────────────────────────────────────────────────────────

@Composable
private fun RootsTab(viewModel: LexiconViewModel) {
    val morphemes by viewModel.morphemes.collectAsState()
    val kind by viewModel.morphemeKind.collectAsState()

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { FilterPill("All", kind == "all") { viewModel.morphemeKind.value = "all" } }
            item { FilterPill("Prefixes", kind == "prefix") { viewModel.morphemeKind.value = "prefix" } }
            item { FilterPill("Roots", kind == "root") { viewModel.morphemeKind.value = "root" } }
            item { FilterPill("Suffixes", kind == "suffix") { viewModel.morphemeKind.value = "suffix" } }
        }
        if (morphemes.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.SearchOff,
                title = "No building blocks here",
                message = "Adjust the search or pick another kind."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(morphemes, key = { it.id }) { m -> MorphemeRow(m) }
            }
        }
    }
}

@Composable
private fun MorphemeRow(m: Morpheme) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = m.form,
                    style = HeadwordListStyle,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                TagPill(m.kind)
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = m.meaning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (m.origin.isNotBlank()) {
                Text(
                    text = m.origin,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (m.examples.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    m.examples.take(3).forEach { example ->
                        TagPill(
                            text = example,
                            container = MaterialTheme.colorScheme.surfaceVariant,
                            content = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─── Abbreviations ───────────────────────────────────────────────────────────

@Composable
private fun AbbreviationsTab(viewModel: LexiconViewModel) {
    val abbreviations by viewModel.abbreviations.collectAsState()
    if (abbreviations.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.SearchOff,
            title = "No abbreviations found",
            message = "Try the short form or part of the expansion."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(abbreviations, key = { it.id }) { a -> AbbreviationRow(a) }
        }
    }
}

@Composable
private fun AbbreviationRow(a: Abbreviation) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = a.short,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = a.expansion,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (a.context.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = a.context,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Phrases ─────────────────────────────────────────────────────────────────

@Composable
private fun PhrasesTab(viewModel: LexiconViewModel) {
    val phrases by viewModel.phrases.collectAsState()
    val categories by viewModel.phraseCategories.collectAsState()
    val selected by viewModel.phraseCategory.collectAsState()

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { FilterPill("All", selected == "all") { viewModel.phraseCategory.value = "all" } }
            items(categories) { category ->
                FilterPill(category, selected == category) { viewModel.phraseCategory.value = category }
            }
        }
        if (phrases.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.SearchOff,
                title = "No phrases found",
                message = "Search by any word inside the phrase or its meaning."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(phrases, key = { it.id }) { p -> PhraseRow(p) }
            }
        }
    }
}

@Composable
private fun PhraseRow(p: PhraseEntry) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "“${p.phrase}”",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = p.meaning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (p.example.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = p.example,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TagPill(p.category)
                TagPill(
                    text = p.register,
                    container = MaterialTheme.colorScheme.surfaceVariant,
                    content = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Mix-ups ─────────────────────────────────────────────────────────────────

@Composable
private fun MixupsTab(viewModel: LexiconViewModel) {
    val confusables by viewModel.confusables.collectAsState()
    if (confusables.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.SearchOff,
            title = "No mix-ups found",
            message = "Search either of the two confusable words."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(confusables, key = { it.id }) { c -> ConfusableRow(c) }
        }
    }
}

@Composable
private fun ConfusableRow(c: Confusable) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = c.a,
                    style = HeadwordListStyle,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "  vs  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = c.b,
                    style = HeadwordListStyle,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = c.howToTell,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            if (c.exampleA.isNotBlank()) {
                Text(
                    text = c.exampleA,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                )
            }
            if (c.exampleB.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = c.exampleB,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
                )
            }
        }
    }
}
