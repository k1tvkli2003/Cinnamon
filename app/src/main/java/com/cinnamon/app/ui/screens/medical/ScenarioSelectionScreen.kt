package com.cinnamon.app.ui.screens.medical

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import com.cinnamon.app.ui.components.FeatureCard
import com.cinnamon.app.ui.theme.AlertRed
import com.cinnamon.app.ui.theme.SurgicalGreen
import com.cinnamon.app.ui.theme.NeonCyan

data class PatientScenario(val id: String, val title: String, val mood: String, val description: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioSelectionScreen(
    onBack: () -> Unit,
    onScenarioSelected: (String, String) -> Unit
) {
    val scenarios = listOf(
        PatientScenario("chest_pain", "Acute Chest Pain", "Anxious and in moderate pain", "Middle-aged patient complaining of a crushing sensation."),
        PatientScenario("abdominal_pain", "Severe Abdominal Pain", "Lethargic and groaning", "Young adult with sharp right lower quadrant pain."),
        PatientScenario("headache", "Thunderclap Headache", "Sensory sensitive, whispering", "Patient reports the worst headache of their life starting suddenly."),
        PatientScenario("short_breath", "Shortness of Breath", "Panicked, speaking in short sentences", "Elderly patient struggling to catch their breath after walking stairs.")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Patient Case", color = SurgicalGreen) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SurgicalGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        val pagerState = rememberPagerState(pageCount = { scenarios.size })
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 48.dp),
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val scenario = scenarios[page]
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .graphicsLayer {
                            // Calculate the absolute offset for the current page from the
                            // scroll position.
                            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue

                            // We animate the scaleX + scaleY, between 85% and 100%
                            lerp(
                                start = 0.85f,
                                stop = 1f,
                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                            ).also { scale ->
                                scaleX = scale
                                scaleY = scale
                            }

                            // We animate the alpha, between 50% and 100%
                            alpha = lerp(
                                start = 0.5f,
                                stop = 1f,
                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                            )
                        },
                    onClick = { onScenarioSelected(scenario.title, scenario.mood) },
                    colors = CardDefaults.cardColors(containerColor = com.cinnamon.app.ui.theme.SurfaceDark),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = "Hospital Icon",
                            tint = if (scenario.id == "chest_pain" || scenario.id == "headache") AlertRed else SurgicalGreen,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = scenario.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = scenario.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = com.cinnamon.app.ui.theme.SlateGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
