package com.example.hearmeout

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

/**
 * Global CompositionLocal to provide High Contrast mode status throughout the app tree.
 * This ensures that any UI component can observe accessibility changes without manual prop drilling.
 */
val LocalHighContrast = compositionLocalOf { false }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("HearMeOutPrefs", Context.MODE_PRIVATE)

            // State for accessibility settings loaded from SharedPreferences
            var isHighContrast by remember { mutableStateOf(prefs.getBoolean("high_contrast", false)) }
            var fontScale by remember { mutableStateOf(prefs.getFloat("font_scale", 1f)) }

            // Navigation state management
            var currentScreen by remember { mutableStateOf("splash") }
            val currentDensity = LocalDensity.current

            /**
             * Providing global accessibility values using CompositionLocalProvider.
             * This allows all child composables to react to theme and font scale changes instantly.
             */
            CompositionLocalProvider(
                LocalHighContrast provides isHighContrast,
                LocalDensity provides Density(density = currentDensity.density, fontScale = fontScale)
            ) {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = if (isHighContrast) Color.Black else Color(0xFF001F3F)
                    ) {
                        when (currentScreen) {
                            "splash" -> AppSplashScreen(onTimeout = {
                                val wantsRememberMe = prefs.getBoolean("remember_me", false)
                                val isUserLoggedIn = FirebaseAuth.getInstance().currentUser != null
                                if (wantsRememberMe && isUserLoggedIn) {
                                    currentScreen = "dashboard"
                                } else {
                                    currentScreen = "login"
                                }
                            })
                            "login" -> LoginScreen(onLoginSuccess = { currentScreen = "dashboard" })
                            "dashboard" -> HearMeOutDashboard(
                                onNavigateToRelay = { currentScreen = "relay" },
                                onNavigateToTranscription = { currentScreen = "transcription" },
                                onNavigateToRadar = { currentScreen = "radar" },
                                onNavigateToMap = { currentScreen = "map" },
                                onNavigateToHistory = { currentScreen = "history" },
                                onNavigateToSettings = { currentScreen = "settings" }
                            )
                            "relay" -> PhoneRelayScreen(onBack = { currentScreen = "dashboard" })
                            "transcription" -> GroupTranscriptionScreen(onBack = { currentScreen = "dashboard" })
                            "history" -> HistoryScreen(onBack = { currentScreen = "dashboard" })
                            "radar" -> SafetyRadarScreen(onBack = { currentScreen = "dashboard" })
                            "map" -> AccessibilityMapScreen(onBack = { currentScreen = "dashboard" })
                            "settings" -> SettingsScreen(
                                onBack = { currentScreen = "dashboard" },
                                onLogout = { currentScreen = "login" },
                                onNavigateToTalk = { currentScreen = "relay" },
                                onNavigateToRadar = { currentScreen = "radar" },
                                onNavigateToMap = { currentScreen = "map" },
                                onHighContrastChange = {
                                    isHighContrast = it
                                    prefs.edit().putBoolean("high_contrast", it).apply()
                                },
                                currentFontScale = fontScale,
                                onFontScaleChange = {
                                    fontScale = it
                                    prefs.edit().putFloat("font_scale", it).apply()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * AppSplashScreen: Initial landing screen.
 * Handles user authentication checks and timed navigation.
 */
@Composable
fun AppSplashScreen(onTimeout: () -> Unit) {
    val isHighContrast = LocalHighContrast.current
    val bgColor = if (isHighContrast) Color.Black else Color(0xFF001F3F)
    val textColor = if (isHighContrast) Color.Yellow else Color(0xFF40E0D0)
    val subTextColor = if (isHighContrast) Color.White else Color.White.copy(alpha = 0.7f)

    LaunchedEffect(Unit) {
        delay(2000) // 2-second delay for splash brand exposure
        onTimeout()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.hearmeout_logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(140.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "HearMeOut", color = textColor, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Your Sixth Sense", color = subTextColor, fontSize = 18.sp)
        }
    }
}

/**
 * HearMeOutDashboard: The primary navigation hub.
 * Features a localized UI (Hebrew/English) and accessibility-aware components.
 */
@Composable
fun HearMeOutDashboard(
    onNavigateToRelay: () -> Unit,
    onNavigateToTranscription: () -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var isHebrew by remember { mutableStateOf(true) }
    val layoutDirection = if (isHebrew) LayoutDirection.Rtl else LayoutDirection.Ltr

    val isHighContrast = LocalHighContrast.current
    val textColor = if (isHighContrast) Color.Yellow else Color(0xFF40E0D0)
    val subTextColor = if (isHighContrast) Color.White else Color.White.copy(alpha = 0.8f)

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "HearMeOut", color = textColor, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (isHebrew) "החוש השישי שלך" else "Your Sixth Sense",
                        color = subTextColor,
                        fontSize = 18.sp
                    )
                }

                Button(
                    onClick = { isHebrew = !isHebrew },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHighContrast) Color.DarkGray else Color(0xFF40E0D0)
                    )
                ) {
                    Icon(
                        Icons.Default.Translate,
                        contentDescription = "Switch Language",
                        tint = if (isHighContrast) Color.Yellow else Color(0xFF001F3F),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isHebrew) "EN" else "עב",
                        color = if (isHighContrast) Color.Yellow else Color(0xFF001F3F),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Dashboard Grid Items mapped to their respective navigation callbacks
            MenuCard(
                title = if (isHebrew) "ממסר שיחות טלפון" else "Phone Relay Assistant",
                subtitle = if (isHebrew) "תמלול והקראת טקסט בזמן אמת" else "Real-time transcription",
                icon = Icons.Default.Phone,
                containerColor = if (isHighContrast) Color.DarkGray else Color(0xFF40E0D0),
                onClick = onNavigateToRelay
            )
            MenuCard(
                title = if (isHebrew) "תמלול חכם" else "Smart Transcription",
                subtitle = if (isHebrew) "זיהוי דוברים וסיכומים" else "Speaker diarization",
                icon = Icons.Default.Mic,
                containerColor = if (isHighContrast) Color.DarkGray else Color(0xFF40E0D0),
                onClick = onNavigateToTranscription
            )
            MenuCard(
                title = if (isHebrew) "רדאר בטיחות" else "Safety Radar",
                subtitle = if (isHebrew) "התראה חזותית על צלילי חירום" else "Visual awareness of sounds",
                icon = Icons.Default.NotificationsActive,
                containerColor = if (isHighContrast) Color.DarkGray else Color(0xFFFFD54F),
                onClick = onNavigateToRadar
            )
            MenuCard(
                title = if (isHebrew) "מפת נגישות" else "Accessibility Map",
                subtitle = if (isHebrew) "ניטור אקוסטי סביבתי" else "Acoustic monitoring",
                icon = Icons.Default.Map,
                containerColor = if (isHighContrast) Color.DarkGray else Color(0xFFFFD54F),
                onClick = onNavigateToMap
            )
            MenuCard(
                title = if (isHebrew) "היסטוריית תמלולים" else "Transcription History",
                subtitle = if (isHebrew) "צפייה וחיפוש בשיחות עבר" else "View past conversations",
                icon = Icons.Default.History,
                containerColor = if (isHighContrast) Color.DarkGray else Color(0xFFB0BEC5),
                onClick = onNavigateToHistory
            )
            MenuCard(
                title = if (isHebrew) "הגדרות וצלילים מותאמים" else "Settings & Custom Sounds",
                subtitle = if (isHebrew) "אימון האפליקציה לזיהוי הצלילים שלך" else "Train the app",
                icon = Icons.Default.Settings,
                containerColor = if (isHighContrast) Color.DarkGray else Color(0xFFB0BEC5),
                onClick = onNavigateToSettings
            )
        }
    }
}

/**
 * MenuCard: A stylized navigation card with dynamic contrast settings.
 * Designed to provide a large, accessible touch target for users.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuCard(title: String, subtitle: String, icon: ImageVector, containerColor: Color, onClick: () -> Unit) {
    val isHighContrast = LocalHighContrast.current
    val contentColor = if (isHighContrast) Color.Yellow else Color(0xFF001F3F)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = contentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, color = contentColor.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
    }
}