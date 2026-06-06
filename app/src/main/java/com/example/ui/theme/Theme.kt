package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Premium Theme Color Schemes (Slate Clinical Default & Clinical White Luxe)
private val MinimalistDarkScheme = darkColorScheme(
    primary = SurgicalGreen,
    onPrimary = Color.Black,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    tertiary = AlertRed,
    background = SpaceBlack, // Onyx Slate Clinical deep `#0A0B10`
    surface = DarkSlate, // `#11131C`
    surfaceVariant = SurfaceDark, // `#181A26`
    onBackground = TextWhite, // `#F2F4F8`
    onSurface = TextWhite,
    onSurfaceVariant = SlateGray // `#7E8694`
)

private val ClinicalWhiteScheme = lightColorScheme(
    primary = ModernBlue, // Modern refined clinical blue `#0066FF`
    onPrimary = Color.White,
    secondary = SurgicalGreen,
    onSecondary = HighEndNavy,
    tertiary = AlertRed,
    background = SterileWhite, // Sterile Luxury ultra-soft light off-white with blue tone `#F4F7FB`
    surface = CrispWhite, // Crisp white card background
    surfaceVariant = Color(0xFFE6EEF8),
    onBackground = HighEndNavy,
    onSurface = HighEndNavy,
    onSurfaceVariant = SlateGray
)

private val AmoledDarkScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    secondary = SurgicalGreen,
    onSecondary = Color.Black,
    tertiary = AlertRed,
    background = Color(0xFF000000), // Pitch pure black
    surface = Color(0xFF0A0A0A), // Minimalist pitch gray
    surfaceVariant = Color(0xFF121212),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFA0A0A0)
)

@Composable
fun MyApplicationTheme(
    themeName: String = "Minimalist Dark",
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeName) {
        "Clinical White" -> ClinicalWhiteScheme
        "AMOLED Dark Mode" -> AmoledDarkScheme
        else -> MinimalistDarkScheme
    }
    
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
