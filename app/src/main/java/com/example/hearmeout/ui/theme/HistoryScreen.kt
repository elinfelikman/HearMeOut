package com.example.hearmeout

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * HistoryScreen: Manages and displays the list of saved conversation transcripts.
 * Utilizes Room Database with Flow to provide real-time updates when data changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    // Collecting transcripts as state from the DAO using Flow for reactive UI updates
    val historyList by db.transcriptDao().getAllTranscripts().collectAsState(initial = emptyList())

    // State to manage the "Navigation" to a specific transcript detail view
    var selectedTranscript by remember { mutableStateOf<TranscriptEntity?>(null) }

    if (selectedTranscript != null) {
        // Display detailed view for the selected conversation
        TranscriptDetailScreen(
            transcript = selectedTranscript!!,
            onBack = { selectedTranscript = null }
        )
    } else {
        // Display the list of all saved transcripts
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Transcription History", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (historyList.isNotEmpty()) {
                            IconButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    db.transcriptDao().deleteAll()
                                }
                                Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = Color.Red)
                            }
                        }
                    }
                )
            }
        ) { padding ->
            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No history saved yet", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    items(historyList) { item ->
                        HistoryItem(transcript = item, onClick = { selectedTranscript = item })
                    }
                }
            }
        }
    }
}

/**
 * TranscriptDetailScreen: Displays the full text of a selected conversation.
 * Features a scrollable view to handle long transcripts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptDetailScreen(transcript: TranscriptEntity, onBack: () -> Unit) {
    val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(transcript.timestamp))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transcript Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()) // Ensures accessibility for long conversations
        ) {
            Text(text = transcript.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF001F3F))
            Text(text = date, fontSize = 14.sp, color = Color.Gray)

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = transcript.content,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                color = Color.Black
            )
        }
    }
}

/**
 * HistoryItem: A preview card for a single transcript in the history list.
 */
@Composable
fun HistoryItem(transcript: TranscriptEntity, onClick: () -> Unit) {
    val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(transcript.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = transcript.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF001F3F))
                Text(text = date, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = transcript.content,
                maxLines = 2, // Preview only the first two lines
                fontSize = 14.sp,
                color = Color.DarkGray,
                lineHeight = 20.sp
            )
        }
    }
}