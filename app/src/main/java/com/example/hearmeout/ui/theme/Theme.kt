package com.example.hearmeout.ui.theme

import androidx.compose.material3.darkColorScheme

/**
 * HearMeOut Dark Color Scheme.
 * Optimized for high-contrast accessibility and reduced eye strain.
 * This scheme maps custom brand colors to Material Design 3 theme tokens.
 */
private val DarkColorScheme = darkColorScheme(
    // Main brand color for key UI elements
    primary = Turquoise,

    // Accent color for alerts and safety-critical notifications
    secondary = SafetyOrange,

    // Deep navy background to provide maximum contrast for text
    background = BackgroundNavy,

    // Surface color for cards and elevated components
    surface = DarkBlue,

    // High-contrast text colors ensuring WCAG compliance
    onPrimary = WhiteText,
    onBackground = WhiteText
)