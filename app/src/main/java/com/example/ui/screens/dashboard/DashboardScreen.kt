package com.example.ui.screens.dashboard

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FeatureCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurgicalGreen
import com.example.ui.theme.AlertRed
import com.example.ui.theme.SlateGray
import com.example.ui.util.SoundSynthesizer
import kotlinx.coroutines.launch

@Composable
fun MiniAudioEqualizer(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "eq")
    val heights = listOf(
        transition.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse), label = "eqBar1"
        ),
        transition.animateFloat(
            initialValue = 0.2f, targetValue = 0.8f,
            animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse), label = "eqBar2"
        ),
        transition.animateFloat(
            initialValue = 0.4f, targetValue = 0.9f,
            animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse), label = "eqBar3"
        )
    )

    Row(
        modifier = modifier.height(14.dp).width(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { heightVal ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction = heightVal.value)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun DashboardScreen(
    onNavigateToMakeItNative: () -> Unit,
    onNavigateToPatientChat: () -> Unit,
    onNavigateToClinicalSimLabs: () -> Unit,
    onNavigateToShadowingCoach: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. PHYSICAL GYROSCOPE & SENSOR PARALLAX LOGIC (Step 96)
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) 
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    // Smoothly map gravity/acceleration to tilt degrees (clamped)
                    tiltX = (event.values[0] * 2.5f).coerceIn(-12f, 12f)
                    tiltY = (event.values[1] * 2.5f).coerceIn(-12f, 12f)
                } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                    tiltX = (event.values[1] * 15f).coerceIn(-12f, 12f)
                    tiltY = (event.values[0] * 15f).coerceIn(-12f, 12f)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (gyroSensor != null) {
            sensorManager.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // 2. BACKUP SHIMMERING PARALLAX WAVE (If physical sensors are in flat static state or not present)
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val automaticFloatX by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerX"
    )
    val automaticFloatY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerY"
    )

    // Merge physical gyro tilts with auto drift
    val finalTiltX = if (tiltX != 0f) tiltX else automaticFloatX
    val finalTiltY = if (tiltY != 0f) tiltY else automaticFloatY

    // 3. BACKGROUND LOCK-SCREEN PLAYER SIMULATION STATE (Step 97)
    var shadowPlayActive by remember { mutableStateOf(false) }

    // Pulsing Glow Animation for Critical Triage
    val infiniteTransitionGlow = rememberInfiniteTransition(label = "pulseGlow")
    val pulseGlow by infiniteTransitionGlow.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp) // Generous space for Spotify Miniplayer
        ) {
        // QUICK TRIAGE SECTION (Redesigned Critical Alert - Step 2)
        item {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Quick Triage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, AlertRed.copy(alpha = pulseGlow * 0.4f + 0.15f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Heart pulsing indicator circle
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AlertRed.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MedicalServices,
                                contentDescription = "Triage",
                                tint = AlertRed,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = 0.9f + pulseGlow * 0.3f
                                        scaleY = 0.9f + pulseGlow * 0.3f
                                    }
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "🚨 CRITICAL DISPATCH",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertRed
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Room 14",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Patient in Room 14 needs immediate consult (Severe dyspnea)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    SoundSynthesizer.playSynthesizedSound(SoundSynthesizer.SoundType.SUCCESS)
                                }
                                onNavigateToPatientChat()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Attend", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // CLINICAL ARENA CARDS WITH TILT REACTION APPLIED
        item {
            Text(
                text = "Clinical Modules",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.W600,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = -finalTiltX * 1.5f
                        translationY = -finalTiltY * 1.5f
                        rotationZ = (finalTiltX / 6f)
                    }
            ) {
                FeatureCard(
                    title = "AI Standardized Patient",
                    subtitle = "Take a structured medical history with live vitals feedback",
                    icon = Icons.Default.Healing,
                    accentColor = SurgicalGreen,
                    onClick = {
                        scope.launch { SoundSynthesizer.playSynthesizedSound(SoundSynthesizer.SoundType.SWOOSH) }
                        onNavigateToPatientChat()
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = finalTiltX * 1.2f
                        translationY = finalTiltY * 1.2f
                        rotationZ = -(finalTiltY / 5f)
                    }
            ) {
                FeatureCard(
                    title = "Clinical Sim Labs",
                    subtitle = "Active training: Crashing Crisis, ECG Lab, SBAR, Morning Report & SPIKES",
                    icon = Icons.Default.Science,
                    accentColor = SurgicalGreen,
                    onClick = {
                        scope.launch { SoundSynthesizer.playSynthesizedSound(SoundSynthesizer.SoundType.SWOOSH) }
                        onNavigateToClinicalSimLabs()
                    }
                )
            }
        }
        
        item {
            Text(
                text = "Language & Shadowing",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.W600,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp, top = 8.dp)
            )
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = -finalTiltX * 0.8f
                        translationY = finalTiltY * 0.8f
                    }
            ) {
                FeatureCard(
                    title = "Make It Native",
                    subtitle = "Nuance translation: Formal, Casual, Slang",
                    icon = Icons.Default.RecordVoiceOver,
                    accentColor = NeonCyan,
                    onClick = {
                        scope.launch { SoundSynthesizer.playSynthesizedSound(SoundSynthesizer.SoundType.SWOOSH) }
                        onNavigateToMakeItNative()
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = finalTiltX * 1.1f
                        translationY = -finalTiltY * 1.1f
                    }
            ) {
                FeatureCard(
                    title = "Shadowing Coach",
                    subtitle = "Active training: Rhythm, Prosody & Accent waveform analytics",
                    icon = Icons.Default.SmartToy,
                    accentColor = NeonCyan,
                    onClick = {
                        scope.launch { SoundSynthesizer.playSynthesizedSound(SoundSynthesizer.SoundType.SWOOSH) }
                        onNavigateToShadowingCoach()
                    }
                )
            }
        }

        // BOTTOM SPACER FOR BETTER SCROLLABILITY OVER FLOATING PLAYER
        item {
            Spacer(modifier = Modifier.height(110.dp))
        }
    }

    // SPOTIFY-STYLE FLOATING MINI PLAYER (Section 2 - spotify mini player)
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .shadow(16.dp, RoundedCornerShape(38.dp)),
            shape = RoundedCornerShape(38.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (shadowPlayActive) SurgicalGreen.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Glassmorphic indicator artwork
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (shadowPlayActive) SurgicalGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (shadowPlayActive) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = null,
                        tint = if (shadowPlayActive) SurgicalGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Track and Player Stats
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Podcast Shadowing",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (shadowPlayActive) {
                            MiniAudioEqualizer(color = SurgicalGreen)
                        }
                    }
                    Text(
                        text = if (shadowPlayActive) "Active • Background Broadcast" else "Audio Suspended",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (shadowPlayActive) SurgicalGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Micro controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = {
                            scope.launch { SoundSynthesizer.playSynthesizedSound(SoundSynthesizer.SoundType.POP) }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = {
                            shadowPlayActive = !shadowPlayActive
                            scope.launch {
                                val sound = if (shadowPlayActive) SoundSynthesizer.SoundType.SUCCESS else SoundSynthesizer.SoundType.SWOOSH
                                SoundSynthesizer.playSynthesizedSound(sound)
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(if (shadowPlayActive) SurgicalGreen else MaterialTheme.colorScheme.onSurface, CircleShape)
                    ) {
                        Icon(
                            if (shadowPlayActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "PlayPause",
                            tint = if (shadowPlayActive) Color.Black else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch { SoundSynthesizer.playSynthesizedSound(SoundSynthesizer.SoundType.POP) }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
}
