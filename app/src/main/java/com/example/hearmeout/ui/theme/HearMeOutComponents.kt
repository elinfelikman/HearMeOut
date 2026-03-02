package com.example.hearmeout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hearmeout.ui.theme.Turquoise
import com.example.hearmeout.ui.theme.WhiteText

/**
 * MainMenuButton: A reusable custom UI component used across the dashboard.
 * Following the DRY (Don't Repeat Yourself) principle to maintain consistent
 * styling and reduce code duplication.
 */
@Composable
fun MainMenuButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    // Utilizing Material3 Card to provide visual depth and elevation (UI Hierarchy)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Ensuring proper spacing between UI elements
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Turquoise) // Using global theme colors
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp) // Optimized internal padding for better touch target and readability
        ) {
            Text(
                text = title,
                color = WhiteText,
                fontSize = 20.sp,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = WhiteText.copy(alpha = 0.7f), // Secondary text styling using alpha for visual hierarchy
                fontSize = 14.sp
            )
        }
    }
}