package com.example.hearmeout

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * GroupTranscriptionScreen: Provides real-time speech-to-text transcription for group conversations.
 * Supports speaker diarization (mocked by alternating speakers), dynamic text resizing for accessibility,
 * and integration with Room Database for persistence and Android Sharesheet for export.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupTranscriptionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Initialize Local Database
    val db = remember { AppDatabase.getDatabase(context) }

    // State management for live transcription messages
    val messages = remember { mutableStateListOf<LiveTranscriptMsg>() }
    var isListening by remember { mutableStateOf(false) }
    var partialText by remember { mutableStateOf("") }
    var isSpeakerA by remember { mutableStateOf(true) }

    // UI Accessibility State: Dynamic font scaling
    var textSize by remember { mutableStateOf(18f) }

    // Speech Recognition Setup
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "he-IL")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    // Runtime Permission Handling for Record Audio
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permission Granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission is required for transcription", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Set up the RecognitionListener to handle various speech events.
     * Partial results are shown for better user feedback, while final results are added to the list.
     */
    LaunchedEffect(Unit) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    // Logic to alternate between speakers to simulate group conversation
                    messages.add(LiveTranscriptMsg(
                        speakerName = if (isSpeakerA) "Speaker A" else "Speaker B",
                        text = matches[0],
                        color = if (isSpeakerA) Color(0xFF1E3A8A) else Color(0xFF10B981)
                    ))
                    isSpeakerA = !isSpeakerA
                }
                // Continue listening automatically for continuous transcription
                if (isListening) speechRecognizer.startListening(speechIntent)
            }

            override fun onPartialResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) partialText = matches[0]
            }

            override fun onError(error: Int) { isListening = false }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Transcription", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = {
                            messages.clear()
                            partialText = ""
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear Session", tint = Color.Red)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA))) {

            // Accessibility Feature: Real-time font size slider
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("A", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = textSize,
                    onValueChange = { textSize = it },
                    valueRange = 14f..40f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text("A", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(16.dp)) {
                items(messages) { msg -> TranscriptionCard(msg = msg, textSize = textSize) }

                if (partialText.isNotEmpty()) {
                    item { Text("Listening: $partialText...", color = Color.Gray, modifier = Modifier.padding(8.dp)) }
                }
            }

            // Action Buttons Row: Controls for recording and exporting
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Recording Toggle Button
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val status = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        if (status == PackageManager.PERMISSION_GRANTED) {
                            if (isListening) {
                                speechRecognizer.stopListening()
                                isListening = false
                            } else {
                                speechRecognizer.startListening(speechIntent)
                                isListening = true
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color.Red else Color(0xFF2C3E50)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(if (isListening) Icons.Default.Stop else Icons.Default.Mic, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isListening) "Stop" else "Recording")
                }

                // Database Persistence & Sharing Logic
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (messages.isEmpty()) {
                            Toast.makeText(context, "No transcript to save", Toast.LENGTH_SHORT).show()
                        } else {
                            val transcriptText = messages.joinToString(separator = "\n\n") { msg ->
                                "${msg.speakerName}: ${msg.text}"
                            }

                            // Save to Room Database on IO Thread
                            scope.launch(Dispatchers.IO) {
                                val newEntry = TranscriptEntity(
                                    title = "Conversation ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(java.util.Date())}",
                                    content = transcriptText,
                                    timestamp = System.currentTimeMillis()
                                )
                                db.transcriptDao().insert(newEntry)
                            }

                            // Implicit Intent for Sharing/Exporting data
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, transcriptText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Save Transcript"))
                            Toast.makeText(context, "Saved to History!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3E50)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Transcript")
                }
            }
        }
    }
}