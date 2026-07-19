package com.cinnamon.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cinnamon.app.data.ai.AiConversationMode
import com.cinnamon.app.data.startup.AppStartupCoordinator
import com.cinnamon.app.data.startup.AppStartupState
import com.cinnamon.app.ui.screens.chat.RoleplayChatScreen
import com.cinnamon.app.ui.screens.chat.RoleplayPracticeMode
import com.cinnamon.app.ui.components.RewardPresentationHost
import com.cinnamon.app.ui.screens.fluency.NativeFluencyPlaygroundScreen
import com.cinnamon.app.ui.screens.games.GamificationHubScreen
import com.cinnamon.app.ui.screens.games.SentenceUnscrambleGame
import com.cinnamon.app.ui.screens.games.VocabMatchGame
import com.cinnamon.app.ui.screens.home.HomeScreen
import com.cinnamon.app.ui.screens.lexicon.EntryDetailScreen
import com.cinnamon.app.ui.screens.lexicon.LexiconScreen
import com.cinnamon.app.ui.screens.medical.ClinicalSimLabsScreen
import com.cinnamon.app.ui.screens.medical.ScenarioSelectionScreen
import com.cinnamon.app.ui.screens.medical.PatientScenarioCatalog
import com.cinnamon.app.ui.screens.practice.ClozeClinicGame
import com.cinnamon.app.ui.screens.practice.PracticeScreen
import com.cinnamon.app.ui.screens.practice.ReviewSessionScreen
import com.cinnamon.app.ui.screens.profile.ProfileScreen
import com.cinnamon.app.viewmodel.AiViewModel
import com.cinnamon.app.viewmodel.UserProgressViewModel

private data class TabItem(
    val route: String,
    val label: String,
    val filled: ImageVector,
    val outlined: ImageVector
)

private val tabs = listOf(
    TabItem("home", "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    TabItem("lexicon", "Lexicon", Icons.AutoMirrored.Rounded.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    TabItem("practice", "Practice", Icons.Rounded.School, Icons.Outlined.School),
    TabItem("profile", "Profile", Icons.Rounded.Person, Icons.Outlined.Person)
)

@Composable
fun AppNavigation(progressViewModel: UserProgressViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val startupState by AppStartupCoordinator.state.collectAsState()
    val startupReady = startupState == AppStartupState.Ready
    val pendingRewardPresentation = if (startupReady) {
        progressViewModel.pendingRewardPresentation.collectAsState().value
    } else {
        null
    }
    val snackbarHostState = remember { SnackbarHostState() }

    val isMainTab = startupReady && tabs.any { it.route == currentRoute }

    LaunchedEffect(progressViewModel) {
        progressViewModel.progressErrorEvents.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = if (event.retryable) "Retry" else null,
                duration = SnackbarDuration.Long
            )
            if (event.retryable && result == SnackbarResult.ActionPerformed) {
                progressViewModel.retryLastPracticeSession()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (isMainTab) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.filled else tab.outlined,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    tab.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding),
            enterTransition = { scaleIn(initialScale = 0.94f, animationSpec = tween(280)) + fadeIn(tween(280)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { scaleIn(initialScale = 1.04f, animationSpec = tween(280)) + fadeIn(tween(280)) },
            popExitTransition = { scaleOut(targetScale = 0.94f, animationSpec = tween(220)) + fadeOut(tween(220)) }
        ) {
            // ── Main tabs ──
            composable("home") {
                HomeScreen(
                    progressViewModel = progressViewModel,
                    onOpenEntry = { id -> navController.navigate("entry/$id") },
                    onStartReview = { navController.navigate("review") },
                    onOpenModule = { route -> navController.navigate(route) }
                )
            }
            composable("lexicon") {
                LexiconScreen(onOpenEntry = { id -> navController.navigate("entry/$id") })
            }
            composable("practice") {
                PracticeScreen(
                    progressViewModel = progressViewModel,
                    onStartReview = { navController.navigate("review") },
                    onStartCloze = { navController.navigate("cloze") },
                    onStartVocabMatch = { navController.navigate("vocab_match") },
                    onStartUnscramble = { navController.navigate("unscramble") },
                    onOpenProgressHub = { navController.navigate("gamification_hub") },
                    onOpenPatientSim = { navController.navigate("scenario_select") },
                    onOpenSimLabs = { navController.navigate("clinical_sim_labs") },
                    onOpenMakeItNative = { navController.navigate("make_it_native") },
                    onOpenShadowing = { navController.navigate("native_fluency_playground/Shadowing & Rhythm") }
                )
            }
            composable("profile") {
                ProfileScreen(viewModel = progressViewModel)
            }

            // ── Lexicon detail ──
            composable("entry/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                EntryDetailScreen(
                    entryId = id,
                    onBack = { navController.popBackStack() },
                    onOpenEntry = { newId -> navController.navigate("entry/$newId") }
                )
            }

            // ── Practice activities ──
            composable("review") {
                ReviewSessionScreen(
                    onBack = { navController.popBackStack() },
                    onOpenEntry = { id -> navController.navigate("entry/$id") }
                )
            }
            composable("cloze") {
                ClozeClinicGame(
                    onBack = { navController.popBackStack() },
                    progressViewModel = progressViewModel
                )
            }
            composable("vocab_match") {
                VocabMatchGame(onBack = { navController.popBackStack() }, progressViewModel = progressViewModel)
            }
            composable("unscramble") {
                SentenceUnscrambleGame(onBack = { navController.popBackStack() }, progressViewModel = progressViewModel)
            }
            composable("gamification_hub") {
                GamificationHubScreen(
                    onBack = { navController.popBackStack() },
                    progressViewModel = progressViewModel,
                    onStartReview = { navController.navigate("review") },
                    onOpenPractice = { navController.navigate("practice") }
                )
            }

            // ── AI clinical-English modules ──
            composable("clinical_sim_labs") {
                ClinicalSimLabsScreen(onBack = { navController.popBackStack() }, progressViewModel = progressViewModel)
            }
            composable("native_fluency_playground/{tab}") { backStackEntry ->
                val tab = backStackEntry.arguments?.getString("tab") ?: "Tone Slider"
                NativeFluencyPlaygroundScreen(
                    onBack = { navController.popBackStack() },
                    progressViewModel = progressViewModel,
                    initialTab = tab
                )
            }
            composable("scenario_select") {
                ScenarioSelectionScreen(
                    onBack = { navController.popBackStack() },
                    onScenarioSelected = { scenario ->
                        navController.navigate("patient_chat/${scenario.id}")
                    }
                )
            }
            composable("make_it_native") {
                NativeFluencyPlaygroundScreen(
                    onBack = { navController.popBackStack() },
                    progressViewModel = progressViewModel,
                    initialTab = "Tone Slider"
                )
            }
            composable("patient_chat/{scenarioId}") { backStackEntry ->
                val scenarioId = backStackEntry.arguments?.getString("scenarioId").orEmpty()
                val scenario = PatientScenarioCatalog.findById(scenarioId) ?: PatientScenarioCatalog.fallback
                val aiViewModel: AiViewModel = viewModel()
                LaunchedEffect(scenario.id) {
                    aiViewModel.configureSession(
                        mode = AiConversationMode.StandardizedPatient,
                        scenarioId = scenario.id
                    )
                }
                RoleplayChatScreen(
                    title = "Case: ${scenario.title}",
                    viewModel = aiViewModel,
                    onBack = { navController.popBackStack() },
                    practiceMode = RoleplayPracticeMode.StandardizedPatient,
                    accentColor = MaterialTheme.colorScheme.primary
                )
            }
        }
            pendingRewardPresentation?.let { receipt ->
                RewardPresentationHost(
                    receipt = receipt,
                    onDismiss = { progressViewModel.acknowledgeRewardPresentation(receipt.receiptId) },
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}
