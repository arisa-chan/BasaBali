package com.basabali.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Balinese-inspired palette: temple gold, frangipani cream, lagoon teal
val Gold = Color(0xFFD99A2B)
val GoldDeep = Color(0xFFB3781E)
val Teal = Color(0xFF0F766E)
val Cream = Color(0xFFFFF9EE)
val CreamCard = Color(0xFFFFFDF6)
val Ink = Color(0xFF2B2118)
val InkSoft = Color(0xFF7A6A58)
val GreenOk = Color(0xFF4C9A52)
val RedBad = Color(0xFFD9534F)
val GrayLock = Color(0xFF9C938A)
/** Light purple highlight marking a vocabulary entry the learner has never met. */
val NewWordPurple = Color(0xFF8E6BC8)
val NewWordPurpleDark = Color(0xFFC3A6F0)
val NightBg = Color(0xFF17130E)
val NightCard = Color(0xFF221C15)
val NightInk = Color(0xFFF2E9DA)
val NightInkSoft = Color(0xFFB4A68F)

private val LightColors = lightColorScheme(
    primary = GoldDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBE9C8),
    onPrimaryContainer = Ink,
    secondary = Teal,
    onSecondary = Color.White,
    background = Cream,
    onBackground = Ink,
    surface = CreamCard,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1E7D3),
    onSurfaceVariant = InkSoft,
    error = RedBad,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF241703),
    primaryContainer = Color(0xFF4A3310),
    onPrimaryContainer = NightInk,
    secondary = Color(0xFF4FD1C5),
    onSecondary = Color(0xFF062825),
    background = NightBg,
    onBackground = NightInk,
    surface = NightCard,
    onSurface = NightInk,
    surfaceVariant = Color(0xFF2C241B),
    onSurfaceVariant = NightInkSoft,
    error = Color(0xFFE8837F),
    onError = Color(0xFF3B0907)
)

@Composable
fun BasaBaliTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
