package com.cinnamon.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

object CinnamonThemes {
    const val TOASTED = "Toasted Dark"
    const val CREAM = "Warm Cream"
    const val MIDNIGHT = "Midnight AMOLED"
    val all = listOf(TOASTED, CREAM, MIDNIGHT)
}

private val ToastedScheme = darkColorScheme(
    primary = CinnamonGlow,
    onPrimary = OnCinnamonGlow,
    primaryContainer = EmberContainer,
    onPrimaryContainer = OnEmberContainer,
    secondary = SageTealSoft,
    onSecondary = OnSageTealSoft,
    secondaryContainer = SageNight,
    onSecondaryContainer = OnSageNight,
    tertiary = BerrySoft,
    onTertiary = OnBerrySoft,
    tertiaryContainer = BerryNight,
    onTertiaryContainer = OnBerryNight,
    background = ToastedNight,
    onBackground = WarmParchment,
    surface = RoastedSurface,
    onSurface = WarmParchment,
    surfaceVariant = EmberSurface,
    onSurfaceVariant = DuskHint,
    outline = EmberOutline,
    outlineVariant = EmberOutline,
    error = CoralError,
    onError = OnCoralError,
    errorContainer = CoralErrorContainer,
    onErrorContainer = ClayErrorContainer
)

private val CreamScheme = lightColorScheme(
    primary = CinnamonBark,
    onPrimary = OnCinnamonBark,
    primaryContainer = CinnamonGlaze,
    onPrimaryContainer = OnCinnamonGlaze,
    secondary = SageTealDeep,
    onSecondary = OnSageTealDeep,
    secondaryContainer = SageMist,
    onSecondaryContainer = OnSageMist,
    tertiary = MulledBerry,
    onTertiary = OnMulledBerry,
    tertiaryContainer = BerryCream,
    onTertiaryContainer = OnBerryCream,
    background = WarmCream,
    onBackground = WarmInk,
    surface = SoftLinen,
    onSurface = WarmInk,
    surfaceVariant = Oatmeal,
    // Small helper and status text is used densely on Oatmeal cards; WarmInk
    // keeps ordinary-size text above the AA contrast floor.
    onSurfaceVariant = WarmInk,
    outline = LinenOutline,
    outlineVariant = LinenOutline,
    error = ClayError,
    onError = OnCinnamonBark,
    errorContainer = ClayErrorContainer,
    onErrorContainer = WarmInk
)

private val MidnightScheme = darkColorScheme(
    primary = CinnamonGlow,
    onPrimary = OnCinnamonGlow,
    primaryContainer = EmberContainer,
    onPrimaryContainer = OnEmberContainer,
    secondary = SageTealSoft,
    onSecondary = OnSageTealSoft,
    secondaryContainer = SageNight,
    onSecondaryContainer = OnSageNight,
    tertiary = BerrySoft,
    onTertiary = OnBerrySoft,
    tertiaryContainer = BerryNight,
    onTertiaryContainer = OnBerryNight,
    background = MidnightBlack,
    onBackground = WarmParchment,
    surface = MidnightSurface,
    onSurface = WarmParchment,
    surfaceVariant = MidnightVariant,
    onSurfaceVariant = DuskHint,
    outline = EmberOutline,
    outlineVariant = EmberOutline,
    error = CoralError,
    onError = OnCoralError,
    errorContainer = CoralErrorContainer,
    onErrorContainer = ClayErrorContainer
)

// Radius language: inputs 13 · cards 20 · hero/sheets 26
val CinnamonShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(13.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun CinnamonTheme(
    themeName: String = CinnamonThemes.TOASTED,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeName) {
        CinnamonThemes.CREAM, "Clinical White" -> CreamScheme
        CinnamonThemes.MIDNIGHT, "AMOLED Dark Mode" -> MidnightScheme
        else -> ToastedScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CinnamonShapes,
        content = content
    )
}
