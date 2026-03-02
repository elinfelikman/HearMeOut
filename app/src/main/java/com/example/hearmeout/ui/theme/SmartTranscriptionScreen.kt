package com.example.hearmeout

import android.Manifest
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.CardDefaults

/**
 * Data model for live transcription UI bubbles.
 * Defines speaker identity, transcribed text content, and theme color.
 */
data class LiveTranscriptMsg(
    val speakerName: String,
    val text: String,
    val color: Color
)

/**
 * SmartTranscriptionScreen: Provides real-time speech-to-text with support for
 * dynamic font scaling and automated speaker alternation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartTranscriptionScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // UI State: Using snapshotStateList for reactive updates to the message history
    val messages = remember { mutableStateListOf<LiveTranscriptMsg>() }
    var isListening by remember { mutableStateOf(false) }
    var partialText by remember { mutableStateOf("") }
    var isSpeakerA by remember { mutableStateOf(true) }

    // Accessibility Feature: Real-time dynamic font size adjustment
    var textSize by remember { mutableStateOf(18f) }

    // Speech Recognition Configuration
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "he-IL") // Hebrew localization
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    // Permission Handler for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permission Granted! Click again.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission is required.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Speech Recognition Listener Lifecycle.
     * Handles transcription results and errors, ensuring continuous listening logic.
     */
    LaunchedEffect(Unit) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    messages.add(LiveTranscriptMsg(
                        speakerName = if (isSpeakerA) "Speaker A" else "Speaker B",
                        text = matches[0],
                        color = if (isSpeakerA) Color(0xFF1E3A8A) else Color(0xFF10B981)
                    ))
                    // Alternating speaker identity to simulate diarization
                    isSpeakerA = !isSpeakerA
                }
                // Automatic restart loop for long-form transcription
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
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA))) {

            // Accessibility Controls: UI Slider to adjust transcription font size dynamically
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

            // Record Audio Button with Permission Check logic
            Button(
                onClick = {
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
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color.Red else Color(0xFF2C3E50))
            ) {
                Icon(if (isListening) Icons.Default.Stop else Icons.Default.Mic, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isListening) "Stop Recording" else "Start Recording")
            }
        }
    }
}

/**
 * TranscriptionCard: Reusable UI element for displaying individual transcribed segments.
 * [textSize] is passed dynamically to support the accessibility slider.
 */
@Composable
fun TranscriptionCard(msg: LiveTranscriptMsg, textSize: Float) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(msg.color))
            Column(modifier = Modifier.padding(16.dp)) {
                Text(msg.speakerName, fontWeight = FontWeight.Bold, color = msg.color, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                // Text size is updated reactively from the parent slider
                Text(msg.text, fontSize = textSize.sp, color = Color.Black)
            }
        }
    }
}