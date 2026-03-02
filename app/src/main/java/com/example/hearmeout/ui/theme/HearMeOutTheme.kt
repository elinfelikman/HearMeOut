package com.example.hearmeout.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Custom Color Scheme defining the visual identity of HearMeOut.
 * DarkColorScheme is prioritized for accessibility and eye comfort.
 */
private val DarkColorScheme = darkColorScheme(
    primary = DarkBlue,
    secondary = Turquoise,
    tertiary = SafetyOrange,
    background = BackgroundNavy,
    surface = DarkBlue,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = WhiteText,
    onSurface = WhiteText
)

/**
 * HearMeOutTheme: The central styling wrapper for the application.
 * This Composable ensures consistent UI across all screens and provides
 * a centralized point for design modifications (Single Source of Truth).
 */
@Composable
fun HearMeOutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // For this specific accessibility app, we maintain a consistent
    // high-contrast dark theme even if the system is in light mode.
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}