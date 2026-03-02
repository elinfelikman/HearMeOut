package com.example.hearmeout

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

/**
 * SettingsScreen: The central hub for user preferences and accessibility customizations.
 * Data is persisted locally using Android SharedPreferences to ensure settings remain
 * consistent across app launches.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToTalk: () -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToMap: () -> Unit,
    onHighContrastChange: (Boolean) -> Unit,
    currentFontScale: Float,
    onFontScaleChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("HearMeOutPrefs", Context.MODE_PRIVATE)

    // Persistent States: Loading user preferences from SharedPreferences
    var enableTTS by remember { mutableStateOf(prefs.getBoolean("enable_tts", true)) }
    var speechSpeed by remember { mutableStateOf(prefs.getFloat("speech_speed", 1f)) }
    var radarSensitivity by remember { mutableStateOf(prefs.getFloat("radar_sensitivity", 0.75f)) }
    var hapticFeedback by remember { mutableStateOf(prefs.getBoolean("haptic_feedback", true)) }
    var visualAlerts by remember { mutableStateOf(prefs.getBoolean("visual_alerts", true)) }
    var selectedVoice by remember { mutableStateOf(prefs.getString("selected_voice", "Female, Hebrew (IL)") ?: "Female, Hebrew (IL)") }

    // Mock keyword alert list for demonstration
    val keywordAlerts = remember { mutableStateListOf("Fire", "Emergency", "Yosef") }

    // Dialog Visibility States
    var showAddKeywordDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }

    // Accessibility-aware UI styling
    val isHighContrast = LocalHighContrast.current
    val bgColor = if (isHighContrast) Color.Black else Color.White
    val textColor = if (isHighContrast) Color.Yellow else Color.Black
    val subTextColor = if (isHighContrast) Color.White else Color.Gray
    val dividerColor = if (isHighContrast) Color.DarkGray else Color(0xFFF0F0F0)
    val bottomBarColor = if (isHighContrast) Color.DarkGray else Color.White

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = bottomBarColor, tonalElevation = 8.dp) {
                NavigationBarItem(selected = false, onClick = onNavigateToTalk, icon = { Icon(Icons.Default.Chat, "Talk", tint = textColor) }, label = { Text("Talk", color = textColor) })
                NavigationBarItem(selected = false, onClick = onNavigateToRadar, icon = { Icon(Icons.Default.Sensors, "Radar", tint = textColor) }, label = { Text("Radar", color = textColor) })
                NavigationBarItem(selected = false, onClick = onBack, icon = { Icon(Icons.Default.Home, "Home", tint = textColor) }, label = { Text("Home", color = textColor) })
                NavigationBarItem(selected = false, onClick = onNavigateToMap, icon = { Icon(Icons.Default.LocationOn, "Map", tint = textColor) }, label = { Text("Map", color = textColor) })
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Settings, "Settings", tint = bgColor) },
                    label = { Text("Settings", color = textColor) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = if (isHighContrast) Color.Yellow else Color(0xFFE3F2FD))
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(bgColor).padding(padding).verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Accessibility Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Customize your HearMeOut experience", fontSize = 14.sp, color = subTextColor)
            }

            HorizontalDivider(color = dividerColor, thickness = 1.dp)

            // Section: Voice Profile (TTS Customization)
            SettingsSectionHeader(Icons.Default.VolumeUp, "Voice Profile", "Text-to-speech settings", textColor, subTextColor)

            SettingsSwitchRow("Enable TTS", "Voice output for text", enableTTS, textColor, subTextColor) {
                enableTTS = it
                prefs.edit().putBoolean("enable_tts", it).apply()
            }

            SettingsClickableRow("Voice Selection", selectedVoice, textColor, subTextColor) { showVoiceDialog = true }

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Speech Speed", fontSize = 16.sp, color = textColor)
                    Text("${"%.1f".format(speechSpeed)}x", fontSize = 14.sp, color = subTextColor)
                }
                Slider(
                    value = speechSpeed,
                    onValueChange = {
                        speechSpeed = it
                        prefs.edit().putFloat("speech_speed", it).apply()
                    },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = textColor, activeTrackColor = textColor)
                )
            }

            HorizontalDivider(color = dividerColor, thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))

            // Section: Sound Radar (AI Sensitivity & Feedback)
            SettingsSectionHeader(Icons.Default.Sensors, "Sound Radar", "Detection sensitivity", textColor, subTextColor)

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sensitivity", fontSize = 16.sp, color = textColor)
                    Text("${(radarSensitivity * 100).toInt()}%", fontSize = 14.sp, color = subTextColor)
                }
                Slider(
                    value = radarSensitivity,
                    onValueChange = {
                        radarSensitivity = it
                        prefs.edit().putFloat("radar_sensitivity", it).apply()
                    },
                    colors = SliderDefaults.colors(thumbColor = textColor, activeTrackColor = textColor)
                )
            }

            SettingsSwitchRow("Haptic Feedback", "Vibrate on detection", hapticFeedback, textColor, subTextColor) {
                hapticFeedback = it
                prefs.edit().putBoolean("haptic_feedback", it).apply()
            }
            SettingsSwitchRow("Visual Alerts", "Flash screen on sounds", visualAlerts, textColor, subTextColor) {
                visualAlerts = it
                prefs.edit().putBoolean("visual_alerts", it).apply()
            }

            HorizontalDivider(color = dividerColor, thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))

            // Section: Custom Keywords (Recognition Training)
            SettingsSectionHeader(Icons.Default.Notifications, "Keyword Alerts", "Get notified for specific words", textColor, subTextColor)

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                keywordAlerts.forEach { keyword ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(keyword, fontSize = 16.sp, color = textColor)
                        Text("Remove", color = Color(0xFFD32F2F), fontSize = 14.sp, modifier = Modifier.clickable { keywordAlerts.remove(keyword) })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(8.dp)).clickable { showAddKeywordDialog = true }
                        .drawBehind {
                            drawRoundRect(color = subTextColor.copy(alpha = 0.5f), style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)), cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ Add Keyword", fontSize = 16.sp, color = textColor, fontWeight = FontWeight.Medium)
                }
            }

            HorizontalDivider(color = dividerColor, thickness = 1.dp, modifier = Modifier.padding(top = 24.dp))

            // Section: Display & Accessibility Customization
            Column(modifier = Modifier.padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 8.dp)) {
                Text("Display & Contrast", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
            }

            SettingsSwitchRow("High Contrast Mode", "Enhanced visibility (Applies everywhere)", isHighContrast, textColor, subTextColor) {
                onHighContrastChange(it)
            }

            val fontLabel = when (currentFontScale) {
                0.8f -> "Small (80%)"
                1.0f -> "Standard (100%)"
                1.2f -> "Large (120%)"
                1.5f -> "Extra Large (150%)"
                else -> "Standard (100%)"
            }
            SettingsClickableRow("Font Size", fontLabel, textColor, subTextColor) { showFontDialog = true }

            Spacer(modifier = Modifier.height(30.dp))

            // Authentication Management: Sign out logic
            Button(
                onClick = {
                    prefs.edit().putBoolean("remember_me", false).apply()
                    FirebaseAuth.getInstance().signOut()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Logout Icon", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        // --- Selection Dialogs (Modals) ---
        if (showFontDialog) {
            FontSizeDialog(currentScale = currentFontScale, onDismiss = { showFontDialog = false }, onSelect = { onFontScaleChange(it); showFontDialog = false }, bgColor = bgColor, textColor = textColor)
        }

        if (showVoiceDialog) {
            VoiceSelectionDialog(currentVoice = selectedVoice, onDismiss = { showVoiceDialog = false }, onSelect = { selectedVoice = it; prefs.edit().putString("selected_voice", it).apply(); showVoiceDialog = false }, bgColor = bgColor, textColor = textColor)
        }

        if (showAddKeywordDialog) {
            AddKeywordDialog(onDismiss = { showAddKeywordDialog = false }, onSave = { newKeyword -> keywordAlerts.add(newKeyword); showAddKeywordDialog = false })
        }
    }
}

// ==========================================
// UI Components & Dialogs
// ==========================================

@Composable
fun SettingsSectionHeader(icon: ImageVector, title: String, subtitle: String, textColor: Color, subTextColor: Color) {
    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text(subtitle, fontSize = 14.sp, color = subTextColor)
        }
    }
}

@Composable
fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, textColor: Color, subTextColor: Color, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = textColor)
            Text(subtitle, fontSize = 14.sp, color = subTextColor)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = if (LocalHighContrast.current) Color.Yellow else Color(0xFF001F3F)))
    }
}

@Composable
fun SettingsClickableRow(title: String, subtitle: String, textColor: Color, subTextColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 16.sp, color = textColor)
            Text(subtitle, fontSize = 14.sp, color = subTextColor)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Navigate", tint = subTextColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSelectionDialog(currentVoice: String, onDismiss: () -> Unit, onSelect: (String) -> Unit, bgColor: Color, textColor: Color) {
    val voices = listOf("Female, Hebrew (IL)", "Male, Hebrew (IL)", "Female, English (US)", "Male, English (UK)")
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = bgColor,
        title = { Text("Select Voice", color = textColor, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                voices.forEach { voice ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(voice) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentVoice == voice, onClick = { onSelect(voice) }, colors = RadioButtonDefaults.colors(selectedColor = if (LocalHighContrast.current) Color.Yellow else Color(0xFF001F3F)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(voice, color = textColor, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = textColor) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSizeDialog(currentScale: Float, onDismiss: () -> Unit, onSelect: (Float) -> Unit, bgColor: Color, textColor: Color) {
    val options = listOf(0.8f to "Small (80%)", 1.0f to "Standard (100%)", 1.2f to "Large (120%)", 1.5f to "Extra Large (150%)")
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = bgColor,
        title = { Text("Select Font Size", color = textColor, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { (scale, label) ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(scale) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = currentScale == scale, onClick = { onSelect(scale) }, colors = RadioButtonDefaults.colors(selectedColor = if (LocalHighContrast.current) Color.Yellow else Color(0xFF001F3F)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = textColor, fontSize = (16 * scale).sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = textColor) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddKeywordDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var recordingState by remember { mutableStateOf(0) }
    var keywordName by remember { mutableStateOf("") }
    LaunchedEffect(recordingState) {
        if (recordingState == 1) { delay(3000); recordingState = 2 }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Keyword", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Record a specific sound or keyword you want HearMeOut to recognize.", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.size(80.dp).background(color = when (recordingState) { 0 -> Color.LightGray; 1 -> Color.Red.copy(alpha = 0.8f); else -> Color(0xFF4CAF50) }, shape = CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Mic, contentDescription = "Microphone", tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                when (recordingState) {
                    0 -> Button(onClick = { recordingState = 1 }) { Text("Start Recording") }
                    1 -> Text("Listening...", color = Color.Red, fontWeight = FontWeight.Bold)
                    2 -> { OutlinedTextField(value = keywordName, onValueChange = { keywordName = it }, label = { Text("Keyword Name") }, singleLine = true) }
                }
            }
        },
        confirmButton = { if (recordingState == 2) { Button(onClick = { if (keywordName.isNotBlank()) onSave(keywordName) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF001F3F))) { Text("Save", color = Color.White) } } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }
    )
}