package com.example.hearmeout.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * HearMeOut Typography Configuration.
 * Styles are optimized for high legibility, which is critical for
 * hearing-impaired users who rely heavily on visual text communication.
 */
val Typography = Typography(
    /**
     * BodyLarge: Used for primary content such as conversation transcripts.
     * LineHeight and LetterSpacing are tuned to improve character recognition
     * and reduce visual fatigue during long reading sessions.
     */
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )

    /* * Additional typography tokens can be customized here to override
     * Material 3 defaults for titles, labels, and headlines.
     */
)