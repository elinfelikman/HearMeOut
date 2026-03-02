package com.example.hearmeout

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data model for messages within the Relay Screen.
 * [timestamp] is used to provide a familiar chat-like experience with message times.
 */
data class RelayMessage(
    val text: String,
    val isMe: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * PhoneRelayScreen: Enables real-time phone call assistance for the hearing impaired.
 * Features integration with Android's Speech-to-Text (STT) and Text-to-Speech (TTS) engines,
 * a dual-language UI, and specialized Accessibility High-Contrast themes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneRelayScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("HearMeOutPrefs", Context.MODE_PRIVATE)

    // Accessibility & TTS Settings
    val isTtsEnabled = prefs.getBoolean("enable_tts", true)
    val speechSpeed = prefs.getFloat("speech_speed", 1f)

    // Language & Locale state management
    var isHebrew by remember { mutableStateOf(true) }
    val layoutDirection = if (isHebrew) LayoutDirection.Rtl else LayoutDirection.Ltr
    val currentLocale = if (isHebrew) Locale("iw", "IL") else Locale.US
    val currentLangCode = if (isHebrew) "he-IL" else "en-US"

    // Functional states for the chat interface
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var messages by remember { mutableStateOf(listOf<RelayMessage>()) }
    var inputText by remember { mutableStateOf("") }

    // UX Enhancement: Quick replies for efficient communication during live calls
    val quickRepliesHebrew = listOf("שלום, אני כבד שמיעה ומשתמש באפליקציה", "אנא דבר לאט וברור", "כן", "לא", "תודה!", "אפשר לחזור על זה שוב?")
    val quickRepliesEnglish = listOf("Hello, I'm using a transcription app", "Please speak slower", "Yes", "No", "Thank you!", "Could you repeat that?")
    val activeQuickReplies = if (isHebrew) quickRepliesHebrew else quickRepliesEnglish

    /**
     * STT Launcher: Captures spoken audio from the other party and transcribes it to the UI.
     */
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                messages = messages + RelayMessage(spokenText, isMe = false)
            }
        }
    }

    // Dynamic Theming based on High Contrast settings
    val isHighContrast = LocalHighContrast.current
    val bgColor = if (isHighContrast) Color.Black else Color(0xFFF0F4F8)
    val topBarBg = if (isHighContrast) Color.Black else Color.White
    val textColor = if (isHighContrast) Color.Yellow else Color.Black

    val myMessageBg = if (isHighContrast) Color.DarkGray else Color(0xFF001F3F)
    val myMessageText = if (isHighContrast) Color.Yellow else Color.White

    val theirMessageBg = if (isHighContrast) Color.DarkGray else Color(0xFFE0E0E0)
    val theirMessageText = if (isHighContrast) Color.Yellow else Color.Black

    /**
     * Text-To-Speech Lifecycle Management.
     * Ensures engine resources are properly released when the screen is disposed.
     */
    DisposableEffect(Unit) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = currentLocale
                tts?.setSpeechRate(speechSpeed)
            }
        }
        tts = textToSpeech

        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    /**
     * Unified function to update message state and trigger the TTS voice engine.
     */
    fun sendMessageAndSpeak(text: String) {
        messages = messages + RelayMessage(text, true)
        if (isTtsEnabled) {
            tts?.language = currentLocale
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (isHebrew) "ממסר שיחות טלפון" else "Phone Relay", fontWeight = FontWeight.Bold, color = textColor) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                        }
                    },
                    actions = {
                        // Utility: Clear current conversation session
                        if (messages.isNotEmpty()) {
                            IconButton(onClick = { messages = emptyList() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = Color.Red)
                            }
                        }
                        // Language switcher button
                        Button(
                            onClick = {
                                isHebrew = !isHebrew
                                tts?.language = if (isHebrew) Locale("iw", "IL") else Locale.US
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isHighContrast) Color.DarkGray else Color(0xFF40E0D0)),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = if (isHebrew) "EN" else "עב",
                                color = if (isHighContrast) Color.Yellow else Color(0xFF001F3F),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarBg)
                )
            },
            bottomBar = {
                Column(modifier = Modifier.background(topBarBg)) {
                    // Quick Reply Chips for rapid communication
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeQuickReplies) { reply ->
                            SuggestionChip(
                                onClick = { sendMessageAndSpeak(reply) },
                                label = { Text(reply, color = if (isHighContrast) Color.Yellow else Color(0xFF001F3F), fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isHighContrast) Color.DarkGray else Color(0xFFE0F7FA)
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = if (isHighContrast) Color.Yellow else Color(0xFF40E0D0)
                                )
                            )
                        }
                    }

                    // Input controls: Microphone (STT), TextField (Type), and Send (TTS)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLangCode)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, if (isHebrew) "מקשיב לצד השני..." else "Listening to the other person...")
                                }
                                try {
                                    speechRecognizerLauncher.launch(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            modifier = Modifier
                                .background(if (isHighContrast) Color.DarkGray else Color(0xFF4CAF50), CircleShape)
                                .padding(4.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Listen", tint = if (isHighContrast) Color.Yellow else Color.White)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(if (isHebrew) "הקלד הודעה..." else "Type to speak...", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isHighContrast) Color.Yellow else Color(0xFF001F3F),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FloatingActionButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    sendMessageAndSpeak(inputText)
                                    inputText = ""
                                }
                            },
                            containerColor = myMessageBg,
                            shape = CircleShape,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = myMessageText)
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(padding)
            ) {
                if (messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (isHebrew) "הקלד כדי לדבר, או לחץ על המיקרופון כדי להקשיב." else "Type to speak, or tap Mic to listen.", color = Color.Gray)
                    }
                } else {
                    // Chat Timeline: Displays transcribed and typed messages with timestamps
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        reverseLayout = false
                    ) {
                        items(messages) { message ->
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val timeString = timeFormat.format(Date(message.timestamp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = if (message.isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (message.isMe) myMessageBg else theirMessageBg,
                                            shape = if (message.isMe) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                                            else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                                        )
                                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
                                ) {
                                    Column(horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start) {
                                        Text(
                                            text = message.text,
                                            color = if (message.isMe) myMessageText else theirMessageText,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        // WhatsApp-style timestamp
                                        Text(
                                            text = timeString,
                                            color = (if (message.isMe) myMessageText else theirMessageText).copy(alpha = 0.6f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}