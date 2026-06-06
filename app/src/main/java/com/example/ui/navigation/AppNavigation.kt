package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.ai.Prompts
import com.example.ui.screens.chat.RoleplayChatScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.games.GamesScreen
import com.example.ui.screens.games.SentenceUnscrambleGame
import com.example.ui.screens.games.VocabMatchGame
import com.example.ui.screens.games.GamificationHubScreen
import com.example.ui.screens.medical.ClinicalSimLabsScreen
import com.example.ui.screens.fluency.NativeFluencyPlaygroundScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurgicalGreen
import com.example.viewmodel.AiViewModel
import com.example.viewmodel.UserProgressViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(progressViewModel: UserProgressViewModel = viewModel()) {
    val navController = rememberNavController()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val points by progressViewModel.points.collectAsState()
    val streak by progressViewModel.streak.collectAsState()

    val isMainTab = currentRoute == "dashboard" || currentRoute == "games" || currentRoute == "profile"

    var isFabExpanded by remember { mutableStateOf(false) }
    var showDictionaryDialog by remember { mutableStateOf(false) }
    var showAddFlashcardDialog by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            if (isMainTab && currentRoute != "dashboard") {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentRoute) {
                                "games" -> "Clinical Drills"
                                else -> "Linguistic Dashboard"
                            },
                            fontWeight = FontWeight.W600,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    actions = {
                        Row(modifier = Modifier.padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = "Streak", tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("$streak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            } else if (isMainTab && currentRoute == "dashboard") {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "MedSpeak AI",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.5).sp
                        )
                    },
                    actions = {
                        Row(modifier = Modifier.padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = "Streak", tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("$streak", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        },
        bottomBar = {
            if (isMainTab) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 1.dp,
                    windowInsets = NavigationBarDefaults.windowInsets
                ) {
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = if (currentRoute == "dashboard") Icons.Default.Home else Icons.Outlined.Home, 
                                contentDescription = "Home"
                            ) 
                        },
                        label = { Text("Home", fontWeight = if (currentRoute == "dashboard") FontWeight.Bold else FontWeight.Normal) },
                        selected = currentRoute == "dashboard",
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SurgicalGreen,
                            selectedTextColor = SurgicalGreen,
                            indicatorColor = SurgicalGreen.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            navController.navigate("dashboard") {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = if (currentRoute == "games") Icons.Default.SportsEsports else Icons.Outlined.SportsEsports, 
                                contentDescription = "Games"
                            ) 
                        },
                        label = { Text("Drills", fontWeight = if (currentRoute == "games") FontWeight.Bold else FontWeight.Normal) },
                        selected = currentRoute == "games",
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SurgicalGreen,
                            selectedTextColor = SurgicalGreen,
                            indicatorColor = SurgicalGreen.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            navController.navigate("games") {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = if (currentRoute == "profile") Icons.Default.Person else Icons.Outlined.Person, 
                                contentDescription = "Stats"
                            ) 
                        },
                        label = { Text("Stats", fontWeight = if (currentRoute == "profile") FontWeight.Bold else FontWeight.Normal) },
                        selected = currentRoute == "profile",
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SurgicalGreen,
                            selectedTextColor = SurgicalGreen,
                            indicatorColor = SurgicalGreen.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            navController.navigate("profile") {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (isMainTab) {
                // Radially animated scales and positions for luxurious radial menu
                val dictScale by animateFloatAsState(targetValue = if (isFabExpanded) 1f else 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                val dictX by animateFloatAsState(targetValue = if (isFabExpanded) (-16f) else 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                val dictY by animateFloatAsState(targetValue = if (isFabExpanded) (-74f) else 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))

                val cardScale by animateFloatAsState(targetValue = if (isFabExpanded) 1f else 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                val cardX by animateFloatAsState(targetValue = if (isFabExpanded) (-115f) else 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                val cardY by animateFloatAsState(targetValue = if (isFabExpanded) (-15f) else 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))

                val rotationDegree by animateFloatAsState(targetValue = if (isFabExpanded) 90f else 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))

                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.padding(bottom = 16.dp, end = 16.dp)
                ) {
                    // Option 1: Clinical Dictionary Lookup (Angled slide out)
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = dictX
                                translationY = dictY
                                scaleX = dictScale
                                scaleY = dictScale
                                alpha = dictScale
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), RoundedCornerShape(16.dp))
                                .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .clickable {
                                    showDictionaryDialog = true
                                    isFabExpanded = false
                                    showTooltip = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Clinical Dictionary", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = NeonCyan)
                            Spacer(Modifier.width(8.dp))
                            FloatingActionButton(
                                onClick = {
                                    showDictionaryDialog = true
                                    isFabExpanded = false
                                    showTooltip = false
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = "Dictionary", tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Option 2: Add Custom Flashcard (Different angle slide out)
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = cardX
                                translationY = cardY
                                scaleX = cardScale
                                scaleY = cardScale
                                alpha = cardScale
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), RoundedCornerShape(16.dp))
                                .border(1.dp, SurgicalGreen.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .clickable {
                                    showAddFlashcardDialog = true
                                    isFabExpanded = false
                                    showTooltip = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Add Custom Card", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = SurgicalGreen)
                            Spacer(Modifier.width(8.dp))
                            FloatingActionButton(
                                onClick = {
                                    showAddFlashcardDialog = true
                                    isFabExpanded = false
                                    showTooltip = false
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Flashcard", tint = SurgicalGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Main triggering floating button with rotational spring reaction
                    ExtendedFloatingActionButton(
                        onClick = { isFabExpanded = !isFabExpanded },
                        icon = {
                            Icon(
                                if (isFabExpanded) Icons.Default.Close else Icons.Default.MedicalServices,
                                contentDescription = "Action Hub",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = rotationDegree
                                }
                            )
                        },
                        text = { Text(if (isFabExpanded) "Close" else "Clinical Hub") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        expanded = isFabExpanded
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding),
            enterTransition = { androidx.compose.animation.scaleIn(initialScale = 0.92f, animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) },
            exitTransition = { androidx.compose.animation.scaleOut(targetScale = 1.08f, animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) },
            popEnterTransition = { androidx.compose.animation.scaleIn(initialScale = 1.08f, animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) },
            popExitTransition = { androidx.compose.animation.scaleOut(targetScale = 0.92f, animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) }
        ) {
            composable("dashboard") {
                DashboardScreen(
                    onNavigateToMakeItNative = { navController.navigate("native_fluency_playground/Tone Slider") },
                    onNavigateToPatientChat = { navController.navigate("scenario_select") },
                    onNavigateToClinicalSimLabs = { navController.navigate("clinical_sim_labs") },
                    onNavigateToShadowingCoach = { navController.navigate("native_fluency_playground/Shadowing & Rhythm") }
                )
            }

            composable("native_fluency_playground/{tab}") { backStackEntry ->
                val tab = backStackEntry.arguments?.getString("tab") ?: "Tone Slider"
                NativeFluencyPlaygroundScreen(
                    onBack = { navController.popBackStack() },
                    progressViewModel = progressViewModel,
                    initialTab = tab
                )
            }

            composable("clinical_sim_labs") {
                ClinicalSimLabsScreen(
                    onBack = { navController.popBackStack() },
                    progressViewModel = progressViewModel
                )
            }
            
            composable("scenario_select") {
                com.example.ui.screens.medical.ScenarioSelectionScreen(
                    onBack = { navController.popBackStack() },
                    onScenarioSelected = { scenario, mood -> 
                        navController.navigate("patient_chat/${scenario.replace(" ","_")}/${mood.replace(" ","_")}")
                    }
                )
            }
            
            composable("games") {
                GamesScreen(
                    onNavigateToVocabMatch = { navController.navigate("vocab_match") },
                    onNavigateToUnscramble = { navController.navigate("unscramble") },
                    onNavigateToProgressZone = { navController.navigate("gamification_hub") }
                )
            }

            composable("gamification_hub") {
                GamificationHubScreen(
                    onBack = { navController.popBackStack() },
                    progressViewModel = progressViewModel
                )
            }

            composable("vocab_match") {
                VocabMatchGame(
                    onBack = { navController.popBackStack() },
                    progressViewModel = progressViewModel
                )
            }

            composable("unscramble") {
                SentenceUnscrambleGame(
                    onBack = { navController.popBackStack() },
                    progressViewModel = progressViewModel
                )
            }

            composable("profile") {
                ProfileScreen(viewModel = progressViewModel)
            }
            
            composable("make_it_native") {
                val aiViewModel: AiViewModel = viewModel()
                LaunchedEffect(Unit) {
                    aiViewModel.initSystemPrompt(Prompts.MAKE_IT_NATIVE_SYSTEM)
                }
                RoleplayChatScreen(
                    title = "Make It Native",
                    viewModel = aiViewModel,
                    onBack = { navController.popBackStack() },
                    accentColor = NeonCyan,
                    progressViewModel = progressViewModel
                )
            }

            composable("patient_chat/{scenario}/{mood}") { backStackEntry ->
                 val scenario = backStackEntry.arguments?.getString("scenario")?.replace("_", " ") ?: "General checkup"
                 val mood = backStackEntry.arguments?.getString("mood")?.replace("_", " ") ?: "Neutral"
                 
                 val aiViewModel: AiViewModel = viewModel()
                 LaunchedEffect(scenario, mood) {
                     aiViewModel.initSystemPrompt(Prompts.getStandardizedPatientPrompt(scenario, mood))
                 }
                 RoleplayChatScreen(
                     title = "Case: $scenario",
                     viewModel = aiViewModel,
                     onBack = { navController.popBackStack() },
                     accentColor = SurgicalGreen,
                     progressViewModel = progressViewModel
                 )
            }
        }
    }

    // Predefined Clinical Terms Map for Action Hub lookup (Ideas 61, 63)
    val dictionaryTerms = listOf(
        DictionaryItem("Myocardial Infarction", "Heart Attack", "Coagulative necrosis of cardiac muscle from ischemia.", "The patient is having a myocardial infarction.", "Cardiology"),
        DictionaryItem("Dyspnea", "Shortness of breath", "Subjective difficulty or distress in breathing.", "Evaluate the client for sudden onset dyspnea.", "Pulmonology"),
        DictionaryItem("Cephalea", "Headache", "Pain localized in any part of the head or upper neck.", "Report severe acute cephalea following trauma.", "Neurology"),
        DictionaryItem("Epistaxis", "Nosebleed", "Hemorrhage or bleeding from the nasal cavity.", "Apply anterior pressure to control epistasis.", "ENT"),
        DictionaryItem("Pruritus", "Severe itching", "Unpleasant cutaneous sensation triggering scratch reflex.", "Patient reports severe pruritus on arms.", "Dermatology"),
        DictionaryItem("Syncope", "Fainting / passing out", "Transient loss of consciousness with spontaneous recovery.", "Syncope occurred upon immediate standing.", "General Medicine"),
        DictionaryItem("Hydatid Metamorphosis", "Cystic tissue transformation", "Conversion of healthy cells into benign parasitic sacs.", "Rule out hydatid metamorphosis in parasitic endemic areas.", "Parasitology"),
        DictionaryItem("Hematemesis", "Vomiting blood", "Vomiting of bright red blood or coffee-ground material.", "Look for hematemesis indicating upper GI bleed.", "Gastroenterology"),
        DictionaryItem("Orthopnea", "Difficulty breathing lying down", "Shortness of breath that occurs when lying flat.", "Orthopnea is improved by propping with 3 pillows.", "Cardiology")
    )

    if (showDictionaryDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredTerms = dictionaryTerms.filter {
            it.term.lowercase().contains(searchQuery.lowercase()) ||
            it.laymanName.lowercase().contains(searchQuery.lowercase()) ||
            it.clinicalDefinition.lowercase().contains(searchQuery.lowercase())
        }

        AlertDialog(
            onDismissRequest = { showDictionaryDialog = false },
            title = {
                Text(
                    "Clinical Reference Dictionary",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Search professional medical terminology and instantly bridge clinical concepts to clear bedside layman terminology.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search term, definition, category...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTerms) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.term, fontWeight = FontWeight.Bold, color = NeonCyan, style = MaterialTheme.typography.bodyMedium)
                                        Text(item.category, style = MaterialTheme.typography.labelSmall, color = SurgicalGreen)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Layman equivalent: ${item.laymanName}", fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(item.clinicalDefinition, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Usage Example: \"${item.usageExample}\"", style = MaterialTheme.typography.bodySmall, color = NeonCyan.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDictionaryDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Close Reference", color = Color.Black)
                }
            }
        )
    }

    if (showAddFlashcardDialog) {
        var termInput by remember { mutableStateOf("") }
        var laymanInput by remember { mutableStateOf("") }
        var categoryInput by remember { mutableStateOf("") }
        var successMsg by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddFlashcardDialog = false },
            title = {
                Text(
                    "Create Custom Study Card",
                    color = SurgicalGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Formulate a clinical phrase to store in your study cards and earn points.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = termInput,
                        onValueChange = { termInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Clinical term (e.g., Dyspnea)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SurgicalGreen)
                    )
                    OutlinedTextField(
                        value = laymanInput,
                        onValueChange = { laymanInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Layman equivalent (e.g., Shortness of breath)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SurgicalGreen)
                    )
                    OutlinedTextField(
                        value = categoryInput,
                        onValueChange = { categoryInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Category (e.g., Cardiology)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SurgicalGreen)
                    )
                    if (successMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(successMsg, color = SurgicalGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (termInput.isNotBlank() && laymanInput.isNotBlank()) {
                            progressViewModel.addPoints(100)
                            successMsg = "Success! Saved card & credited 100 XP"
                            termInput = ""
                            laymanInput = ""
                            categoryInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurgicalGreen)
                ) {
                    Text("Save Card", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFlashcardDialog = false }) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}

data class DictionaryItem(
    val term: String,
    val laymanName: String,
    val clinicalDefinition: String,
    val usageExample: String,
    val category: String
)
