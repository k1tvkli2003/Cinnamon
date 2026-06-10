package com.cinnamon.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// CINNAMON PALETTE — warm spice family, hue ≈ 25°
// Three moods: Warm Cream (light), Toasted (dark, default), Midnight (AMOLED)
// ─────────────────────────────────────────────────────────────────────────────

// Light — Warm Cream
val CinnamonBark = Color(0xFFAD5F24)        // primary: rich cinnamon bark
val OnCinnamonBark = Color(0xFFFFF8F2)
val CinnamonGlaze = Color(0xFFFFDCC2)       // primary container
val OnCinnamonGlaze = Color(0xFF5C3000)
val SageTealDeep = Color(0xFF3E7E72)        // secondary: cooled sage-teal
val OnSageTealDeep = Color(0xFFF4FBF8)
val SageMist = Color(0xFFD3EDE5)            // secondary container
val OnSageMist = Color(0xFF123830)
val MulledBerry = Color(0xFF9D4458)         // tertiary: mulled-wine berry
val OnMulledBerry = Color(0xFFFFF7F7)
val BerryCream = Color(0xFFFFD9DF)          // tertiary container
val OnBerryCream = Color(0xFF3F0A1B)
val WarmCream = Color(0xFFFBF5EF)           // background — never pure white
val WarmInk = Color(0xFF211A13)             // text — never pure black
val SoftLinen = Color(0xFFFFFCF8)           // surface
val Oatmeal = Color(0xFFF2E5D8)             // surface variant
val MochaHint = Color(0xFF7A6B5C)           // hint / secondary text (light)
val LinenOutline = Color(0xFFD9C7B4)
val ClayError = Color(0xFFB3433A)
val ClayErrorContainer = Color(0xFFFFDAD5)
val SuccessFern = Color(0xFF2E7D5B)
val AmberHoney = Color(0xFF9A6B0B)

// Dark — Toasted (default)
val ToastedNight = Color(0xFF1A1310)        // background: warm near-black
val RoastedSurface = Color(0xFF221A15)      // surface
val EmberSurface = Color(0xFF2E241D)        // surface variant
val CinnamonGlow = Color(0xFFEFA266)        // primary on dark
val OnCinnamonGlow = Color(0xFF46260A)
val EmberContainer = Color(0xFF6B4014)      // primary container (dark)
val OnEmberContainer = Color(0xFFFFDCC2)
val SageTealSoft = Color(0xFF86CCBC)        // secondary on dark
val OnSageTealSoft = Color(0xFF0E312A)
val SageNight = Color(0xFF275247)
val OnSageNight = Color(0xFFC8F0E4)
val BerrySoft = Color(0xFFE79DAD)           // tertiary on dark
val OnBerrySoft = Color(0xFF4A1525)
val BerryNight = Color(0xFF7A3B4C)
val OnBerryNight = Color(0xFFFFD9DF)
val WarmParchment = Color(0xFFF2E5DA)       // text on dark
val DuskHint = Color(0xFFA8988A)            // hint on dark
val EmberOutline = Color(0xFF41342B)
val CoralError = Color(0xFFFF8A7E)
val OnCoralError = Color(0xFF400C08)
val CoralErrorContainer = Color(0xFF6E2F28)
val SuccessMint = Color(0xFF7FD8AC)
val HoneyGold = Color(0xFFF2BD64)

// AMOLED — Midnight (deliberate true-black for OLED panels)
val MidnightBlack = Color(0xFF000000)
val MidnightSurface = Color(0xFF0E0A07)
val MidnightVariant = Color(0xFF1A130E)

// ─────────────────────────────────────────────────────────────────────────────
// Legacy aliases — kept so the AI roleplay / sim-lab / fluency screens adopt
// the cinnamon family without touching a thousand call sites.
// ─────────────────────────────────────────────────────────────────────────────
val NeonCyan = SageTealSoft
val SurgicalGreen = SuccessMint
val NeonOrange = HoneyGold
val AlertRed = CoralError
val SlateGray = DuskHint
val SpaceBlack = ToastedNight
val DarkSlate = RoastedSurface
val SurfaceDark = EmberSurface
val TextWhite = WarmParchment
val SterileWhite = WarmCream
val HighEndNavy = WarmInk
val CrispWhite = SoftLinen
val ModernBlue = CinnamonBark
