package com.example.hearmeout.model

import java.util.*

/**
 * Data class representing a sound detected by the TFLite Audio Classifier.
 * Holds information about the sound's danger level, intensity (decibels), and estimated distance.
 *
 */
data class DetectedSound(
    val name: String,
    val isDangerous: Boolean,
    val timestamp: Long,
    val decibels: Double,
    val distance: Double
)

/**
 * Data class representing a single message in the Phone Relay Screen.
 * [isMe] is true if the message was typed by the user, false if transcribed from the other person.
 * [timestamp] is used for displaying message time in the UI.
 *
 */
data class RelayMessage(
    val text: String,
    val isMe: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)