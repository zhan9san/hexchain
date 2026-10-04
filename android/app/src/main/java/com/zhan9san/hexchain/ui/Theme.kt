package com.zhan9san.hexchain.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** A-13: colour-blind-safe round colours, the same in light and dark mode. */
val RoundColours = listOf(
    Color(0xFF0072B2), // blue
    Color(0xFFE69F00), // orange
    Color(0xFFCC79A7), // purple
    Color(0xFF009E73), // teal
)

/** A-8, A-10: chart colours with light and dark variants. */
data class ChartColours(
    val tones: List<Color>,
    val border: Color,
    val digit: Color,
)

private val LightChart = ChartColours(
    tones = listOf(Color(0xFFF3F1EC), Color(0xFFE8EEF1), Color(0xFFEFEAF2), Color(0xFFECF0E6)),
    border = Color(0xFFB4B4B4),
    digit = Color(0xFF2B2B2B),
)

private val DarkChart = ChartColours(
    tones = listOf(Color(0xFF2B2D31), Color(0xFF2A3136), Color(0xFF312D35), Color(0xFF2D322B)),
    border = Color(0xFF5A5F66),
    digit = Color(0xFFE6E6E6),
)

val LocalChartColours = staticCompositionLocalOf { LightChart }

@Composable
fun HexChainTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        CompositionLocalProvider(LocalChartColours provides if (dark) DarkChart else LightChart) {
            content()
        }
    }
}
